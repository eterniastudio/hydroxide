package net.axther.hydroxide.modules.channels;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ChatChannelsModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

    private HydroxideContext context;
    private YamlStore store;
    private ChatChannelRouter router;
    private final Map<UUID, String> focused = new HashMap<>();

    @Override
    public String id() {
        return "channels";
    }

    @Override
    public String displayName() {
        return "Chat Channels";
    }

    @Override
    public String description() {
        return "Dynamic global, local, staff, and trade chat channels with focus and quick commands.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "channels.yml"));
        seedDefaults();
        reloadRouter();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("channel", this);
        context.commands().register("g", this);
        context.commands().register("l", this);
        context.commands().register("sc", this);
        context.commands().register("trade", this);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onReload(HydroxideContext context) {
        reloadRouter();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can use chat channels.");
            return true;
        }
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        String channelId = switch (commandName) {
            case "g" -> "global";
            case "l" -> "local";
            case "sc" -> "staff";
            case "trade" -> "trade";
            default -> args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        };
        if (channelId.isBlank()) {
            context.send(player, "<red>Usage: /" + label + " <channel> [message]");
            return true;
        }
        ChatChannel channel = router.channel(channelId).orElse(null);
        if (channel == null || !player.hasPermission(channel.permission())) {
            context.send(player, "<red>You cannot use that channel.");
            return true;
        }
        int messageStart = commandName.equals("channel") ? 1 : 0;
        if (args.length > messageStart) {
            sendChannel(player, channelId, CommandUtils.joinArgs(args, messageStart));
        } else {
            focused.put(player.getUniqueId(), channelId);
            context.send(player, "<green>Focused chat channel: <white>" + channel.displayName());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return command.getName().equalsIgnoreCase("channel") && args.length == 1
                ? CommandUtils.matching(args[0], channelIds())
                : List.of();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        String channelId = focused.getOrDefault(event.getPlayer().getUniqueId(), "global");
        ChatChannel channel = router.channel(channelId).orElse(null);
        if (channel == null || !event.getPlayer().hasPermission(channel.permission())) {
            channelId = "global";
            channel = router.channel("global").orElse(new ChatChannel("global", "Global", -1.0D, "", true));
        }
        ChatParticipant sender = participant(event.getPlayer());
        ChatChannel selected = channel;
        String selectedId = channelId;
        event.viewers().removeIf(audience -> !canReceive(audience, selectedId, sender));
        event.renderer((source, sourceDisplayName, message, viewer) -> context.text().format(prefix(selected))
                .append(sourceDisplayName)
                .append(context.text().format("<dark_gray>: <white>"))
                .append(message));
    }

    private void sendChannel(Player sender, String channelId, String message) {
        ChatChannel channel = router.channel(channelId).orElseThrow();
        Component rendered = context.text().format(prefix(channel) + sender.getName() + "<dark_gray>: <white>" + message);
        ChatParticipant participant = participant(sender);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (router.canReceive(channelId, participant, participant(viewer))) {
                viewer.sendMessage(rendered);
            }
        }
    }

    private boolean canReceive(Audience audience, String channelId, ChatParticipant sender) {
        if (!(audience instanceof Player viewer)) {
            return channelId.equals("global") || channelId.equals("staff");
        }
        return router.canReceive(channelId, sender, participant(viewer));
    }

    private ChatParticipant participant(Player player) {
        Location location = player.getLocation();
        return new ChatParticipant(
                player.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                player.getEffectivePermissions().stream().map(info -> info.getPermission()).toList()
        );
    }

    private String prefix(ChatChannel channel) {
        return "<dark_gray>[<#44CCFF>" + channel.displayName() + "</#44CCFF>] <white>";
    }

    private void reloadRouter() {
        YamlConfiguration yaml = store.load();
        ConfigurationSection section = yaml.getConfigurationSection("channels");
        if (section == null) {
            router = new ChatChannelRouter(List.of(new ChatChannel("global", "Global", -1.0D, "", true)));
            return;
        }
        List<ChatChannel> channels = section.getKeys(false).stream()
                .map(id -> new ChatChannel(
                        id,
                        section.getString(id + ".display", id),
                        section.getDouble(id + ".radius", -1.0D),
                        section.getString(id + ".permission", ""),
                        section.getBoolean(id + ".auto-join", true)
                ))
                .toList();
        router = new ChatChannelRouter(channels);
    }

    private List<String> channelIds() {
        ConfigurationSection section = store.load().getConfigurationSection("channels");
        return section == null ? List.of() : section.getKeys(false).stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private void seedDefaults() {
        YamlConfiguration yaml = store.load();
        if (yaml.contains("channels")) {
            return;
        }
        yaml.set("channels.global.display", "Global");
        yaml.set("channels.global.radius", -1);
        yaml.set("channels.global.permission", "");
        yaml.set("channels.local.display", "Local");
        yaml.set("channels.local.radius", 100);
        yaml.set("channels.local.permission", "");
        yaml.set("channels.staff.display", "Staff");
        yaml.set("channels.staff.radius", -1);
        yaml.set("channels.staff.permission", "hydroxide.channel.staff");
        yaml.set("channels.trade.display", "Trade");
        yaml.set("channels.trade.radius", -1);
        yaml.set("channels.trade.permission", "hydroxide.channel.trade");
        store.save(yaml);
    }
}
