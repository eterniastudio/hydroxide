package net.axther.hydroxide.modules.channels;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

public final class ChatChannelsModule implements HydroModule, Listener {

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
        context.commands().register("channel", channelCommand());
        ChannelCommandCatalog.quickCommands().forEach(command ->
                context.commands().register(command.command(), quickChannelCommand(command)));
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onReload(HydroxideContext context) {
        reloadRouter();
    }

    private CommandService channelCommand() {
        return new CommandService(HydroCommand.builder("channel")
                .permission("hydroxide.command.channel")
                .playerOnly(true)
                .usage("/{label} <channel> [message]")
                .executor(ctx -> handleChannel((Player) ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> ctx.arguments().size() <= 1
                        ? CommandUtils.matching(ctx.argument(0), channelIds())
                        : List.of())
                .build(), context.messages());
    }

    private CommandService quickChannelCommand(ChannelCommandCatalog.QuickCommand command) {
        return new CommandService(HydroCommand.builder(command.command())
                .aliases(command.aliases().toArray(String[]::new))
                .permission(command.permission())
                .playerOnly(true)
                .usage("/{label} [message]")
                .executor(ctx -> handleQuickChannel((Player) ctx.sender(), command.channelId(), ctx.arguments()))
                .build(), context.messages());
    }

    private void handleChannel(Player player, String label, List<String> args) {
        String channelId = args.isEmpty() ? "" : args.get(0).toLowerCase(Locale.ROOT);
        if (channelId.isBlank()) {
            context.message(player, "channels.usage", Map.of("label", label));
            return;
        }
        int messageStart = 1;
        handleChannelSelection(player, channelId, args, messageStart);
    }

    private void handleQuickChannel(Player player, String channelId, List<String> args) {
        handleChannelSelection(player, channelId, args, 0);
    }

    private void handleChannelSelection(Player player, String channelId, List<String> args, int messageStart) {
        ChatChannel channel = router.channel(channelId).orElse(null);
        if (channel == null || !player.hasPermission(channel.permission())) {
            context.message(player, "channels.denied", Map.of("channel", channelId));
            return;
        }
        if (args.size() > messageStart) {
            sendChannel(player, channelId, CommandUtils.joinArgs(args.toArray(String[]::new), messageStart));
        } else {
            focused.put(player.getUniqueId(), channelId);
            context.message(player, "channels.focused", Map.of("channel", channel.displayName()));
        }
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
        event.renderer((source, sourceDisplayName, message, viewer) -> prefix(selected)
                .append(sourceDisplayName)
                .append(context.messages().component("channels.chat-separator", Map.of()))
                .append(message));
    }

    private void sendChannel(Player sender, String channelId, String message) {
        ChatChannel channel = router.channel(channelId).orElseThrow();
        Component rendered = context.messages().component("channels.message-format", Map.of(
                "channel", channel.displayName(),
                "player", sender.getName(),
                "message", message
        ));
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

    private Component prefix(ChatChannel channel) {
        return context.messages().component("channels.chat-prefix", Map.of("channel", channel.displayName()));
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
