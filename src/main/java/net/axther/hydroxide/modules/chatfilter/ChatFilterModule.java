package net.axther.hydroxide.modules.chatfilter;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ChatFilterModule implements HydroModule, Listener {

    private HydroxideContext context;
    private YamlStore store;
    private ChatFilterEngine engine;
    private HttpClient httpClient;

    @Override
    public String id() {
        return "chat-filter";
    }

    @Override
    public String displayName() {
        return "Chat Filter";
    }

    @Override
    public String description() {
        return "Regex chat filtering, spam throttling, strike escalation, and Discord webhook logging.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "filter.yml"));
        seedDefaults();
        reloadPolicy();
        this.httpClient = HttpClient.newHttpClient();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onReload(HydroxideContext context) {
        reloadPolicy();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        YamlConfiguration yaml = store.load();
        ChatFilterEngine.Result result = engine.moderate(event.getPlayer().getUniqueId(),
                context.text().plain(event.message()), System.currentTimeMillis());
        if (!result.flagged()) {
            return;
        }
        if (result.blocked()) {
            event.setCancelled(true);
        } else {
            event.message(context.text().format(result.message()));
        }
        event.getPlayer().sendMessage(context.text().format(yaml.getString("messages.warning", "<red>Please keep chat clean.")));
        postWebhook(yaml, event.getPlayer().getName(), event.getPlayer().getUniqueId().toString(), context.text().plain(event.message()), result);
        escalate(yaml, event.getPlayer().getName(), result.strikes());
    }

    private void reloadPolicy() {
        YamlConfiguration yaml = store.load();
        ChatFilterEngine.FilterMode mode = switch (yaml.getString("replacement.mode", "asterisks").toLowerCase(Locale.ROOT)) {
            case "block" -> ChatFilterEngine.FilterMode.BLOCK;
            case "random" -> ChatFilterEngine.FilterMode.RANDOM_WORD;
            default -> ChatFilterEngine.FilterMode.REPLACE_ASTERISKS;
        };
        engine = new ChatFilterEngine(new ChatFilterEngine.Policy(
                yaml.getStringList("patterns"),
                mode,
                yaml.getStringList("replacement.words"),
                yaml.getDouble("throttle.max-caps-ratio", 0.7D),
                yaml.getLong("throttle.rate-limit-millis", 1000L)
        ));
    }

    private void escalate(YamlConfiguration yaml, String playerName, int strikes) {
        int threshold = yaml.getInt("strikes.threshold", 3);
        if (threshold <= 0 || strikes < threshold) {
            return;
        }
        for (String command : yaml.getStringList("strikes.commands")) {
            String rendered = command.replace("{player}", playerName).replace("{strikes}", String.valueOf(strikes));
            Bukkit.getScheduler().runTask(context.plugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rendered));
        }
    }

    private void postWebhook(YamlConfiguration yaml, String playerName, String uuid, String original, ChatFilterEngine.Result result) {
        String url = yaml.getString("discord.webhook-url", "");
        if (url == null || url.isBlank()) {
            return;
        }
        String payload = "{\"content\":\"Hydroxide flagged chat from " + escape(playerName)
                + " (" + escape(uuid) + "): " + escape(original)
                + " | rules=" + escape(String.join(",", result.rules())) + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(throwable -> null);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void seedDefaults() {
        YamlConfiguration yaml = store.load();
        if (yaml.contains("patterns")) {
            return;
        }
        yaml.set("patterns", List.of("(?i)badword", "(?i)\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"));
        yaml.set("replacement.mode", "asterisks");
        yaml.set("replacement.words", List.of("cookie", "waffle", "hydroxide"));
        yaml.set("throttle.max-caps-ratio", 0.7D);
        yaml.set("throttle.rate-limit-millis", 1000L);
        yaml.set("strikes.threshold", 3);
        yaml.set("strikes.commands", List.of("kick {player} Please keep chat clean."));
        yaml.set("discord.webhook-url", "");
        yaml.set("messages.warning", "<red>Your message was flagged by the chat filter.");
        store.save(yaml);
    }
}
