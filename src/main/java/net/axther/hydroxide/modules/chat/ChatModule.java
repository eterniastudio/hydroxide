package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ChatModule implements HydroModule {

    private ChatListener listener;
    private CommandSpyListener commandSpyListener;
    private ChatControlStore controls;

    @Override
    public String id() {
        return "chat";
    }

    @Override
    public String displayName() {
        return "Chat";
    }

    @Override
    public String description() {
        return "Modern Adventure chat formatting, broadcast, private messages, and replies.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        controls = new ChatControlStore(new YamlStore(new File(context.plugin().getDataFolder(), "chat-controls.yml")));
        MessageCommands messageCommands = new MessageCommands(context, controls);
        listener = new ChatListener(context, new ChatColorSettings(controls), controls, messageCommands);
        commandSpyListener = new CommandSpyListener(context, controls);
        BroadcastCommand broadcastCommand = new BroadcastCommand(context);
        ColorReferenceCommands colorCommands = new ColorReferenceCommands(context, controls);
        Bukkit.getPluginManager().registerEvents(listener, context.plugin());
        Bukkit.getPluginManager().registerEvents(commandSpyListener, context.plugin());
        context.commands().register("broadcast", broadcastCommand.command());
        context.commands().register("me", broadcastCommand.meCommand());
        context.commands().register("clearchat", broadcastCommand.clearChatCommand());
        context.commands().register("message", messageCommands.messageCommand());
        context.commands().register("reply", messageCommands.replyCommand());
        context.commands().register("chat", messageCommands.privateChatCommand());
        context.commands().register("msgtoggle", messageCommands.msgToggleCommand());
        context.commands().register("ignore", messageCommands.ignoreCommand());
        context.commands().register("socialspy", messageCommands.socialSpyCommand());
        context.commands().register("commandspy", messageCommands.commandSpyCommand());
        context.commands().register("mutechat", muteChatCommand(context));
        context.commands().register("chatcolor", colorCommands.chatColorCommand());
        context.commands().register("colors", colorCommands.colorsCommand());
        context.commands().register("colorpicker", colorCommands.colorPickerCommand());
        context.commands().register("colorlimits", colorCommands.colorLimitsCommand());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
        if (commandSpyListener != null) {
            HandlerList.unregisterAll(commandSpyListener);
        }
    }

    private CommandService muteChatCommand(HydroxideContext context) {
        return new CommandService(HydroCommand.builder("mutechat")
                .permission("hydroxide.command.mutechat")
                .usage("/{label} [time|status|off] [-s] [reason]")
                .executor(ctx -> muteChat(context, ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> muteChatCompletions(ctx.arguments()))
                .build(), context.messages());
    }

    private void muteChat(HydroxideContext context, CommandSender sender, String label, List<String> args) {
        Optional<MuteChatCommandParser.Request> parsed = MuteChatCommandParser.parse(args, defaultMuteChatDuration(context));
        if (parsed.isEmpty()) {
            context.message(sender, "chat.mutechat.usage", Map.of("label", label));
            return;
        }
        MuteChatCommandParser.Request request = parsed.orElseThrow();
        switch (request.action()) {
            case ENABLE -> enableMuteChat(context, sender, request);
            case DISABLE -> disableMuteChat(context, sender);
            case STATUS -> muteChatStatus(context, sender);
        }
    }

    private void enableMuteChat(HydroxideContext context, CommandSender sender, MuteChatCommandParser.Request request) {
        Instant now = Instant.now();
        Duration duration = request.duration().orElse(defaultMuteChatDuration(context));
        String reason = request.reason().isBlank()
                ? context.messages().template("chat.mutechat.default-reason", "Chat muted.")
                : request.reason();
        GlobalChatMute mute = new GlobalChatMute(sender.getName(), reason, now, now.plus(duration));
        controls.setGlobalMute(mute);
        Map<String, Object> placeholders = Map.of(
                "player", sender.getName(),
                "reason", reason,
                "duration", formatDuration(duration),
                "remaining", formatDuration(duration)
        );
        context.message(sender, "chat.mutechat.enabled", placeholders);
        if (!request.silent()) {
            broadcast(context, sender, "chat.mutechat.broadcast-enabled", placeholders);
        }
    }

    private void disableMuteChat(HydroxideContext context, CommandSender sender) {
        controls.clearGlobalMute();
        Map<String, Object> placeholders = Map.of("player", sender.getName());
        context.message(sender, "chat.mutechat.disabled", placeholders);
        broadcast(context, sender, "chat.mutechat.broadcast-disabled", placeholders);
    }

    private void muteChatStatus(HydroxideContext context, CommandSender sender) {
        Optional<GlobalChatMute> mute = controls.globalMute(Instant.now());
        if (mute.isEmpty()) {
            context.message(sender, "chat.mutechat.status-disabled", Map.of());
            return;
        }
        GlobalChatMute active = mute.orElseThrow();
        context.message(sender, "chat.mutechat.status-enabled", Map.of(
                "player", active.issuer(),
                "reason", active.reason(),
                "remaining", formatDuration(active.remaining(Instant.now()))
        ));
    }

    private List<String> muteChatCompletions(List<String> args) {
        if (args.size() <= 1) {
            String prefix = args.isEmpty() ? "" : args.getFirst().toLowerCase(java.util.Locale.ROOT);
            return List.of("1m", "10m", "1h", "status", "off").stream()
                    .filter(candidate -> candidate.startsWith(prefix))
                    .toList();
        }
        if (args.size() == 2 && args.get(1).isBlank()) {
            return List.of("-s");
        }
        return List.of();
    }

    private Duration defaultMuteChatDuration(HydroxideContext context) {
        String configured = context.plugin().getConfig().getString("chat.mutechat.default-duration", "1h");
        return MuteChatCommandParser.parse(List.of(configured), Duration.ofHours(1))
                .flatMap(MuteChatCommandParser.Request::duration)
                .orElse(Duration.ofHours(1));
    }

    private void broadcast(HydroxideContext context, CommandSender sender, String key, Map<String, Object> placeholders) {
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> !player.equals(sender))
                .forEach(player -> context.message(player, key, placeholders));
        if (!Bukkit.getConsoleSender().equals(sender)) {
            context.message(Bukkit.getConsoleSender(), key, placeholders);
        }
    }

    private String formatDuration(Duration duration) {
        long seconds = Math.max(0L, duration.getSeconds());
        long days = seconds / 86_400L;
        seconds %= 86_400L;
        long hours = seconds / 3_600L;
        seconds %= 3_600L;
        long minutes = seconds / 60L;
        seconds %= 60L;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d");
        }
        if (hours > 0) {
            builder.append(hours).append("h");
        }
        if (minutes > 0) {
            builder.append(minutes).append("m");
        }
        if (seconds > 0 || builder.isEmpty()) {
            builder.append(seconds).append("s");
        }
        return builder.toString();
    }
}
