package net.axther.hydroxide.modules.mail;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MailModule implements HydroModule, Listener {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC);

    private HydroxideContext context;
    private MailboxStore store;

    @Override
    public String id() {
        return "mail";
    }

    @Override
    public String displayName() {
        return "Mail";
    }

    @Override
    public String description() {
        return "Offline player mail with persistent and temporary messages, read, delete, clear, and send-all commands.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new MailboxStore(new YamlStore(new File(context.plugin().getDataFolder(), "mail.yml")));
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register(MailCommandCatalog.command(), mailCommand());
        context.commands().register("mailall", mailAllCommand());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        int count = store.count(event.getPlayer().getUniqueId());
        if (count > 0) {
            context.message(event.getPlayer(), "mail.unread-notice", Map.of("count", count));
        }
    }

    private CommandService mailCommand() {
        return new CommandService(HydroCommand.builder("mail")
                .permission("hydroxide.command.mail")
                .usage("/{label} [read|send|sendtemp|delete|clear|sendall] ...")
                .executor(ctx -> mail(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> completions(ctx.arguments()))
                .build(), context.messages());
    }

    private CommandService mailAllCommand() {
        return new CommandService(HydroCommand.builder("mailall")
                .permission("hydroxide.command.mailall")
                .usage("/{label} <send|clear|remove> [message]")
                .executor(ctx -> mailAll(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> mailAllCompletions(ctx.arguments()))
                .build(), context.messages());
    }

    private List<String> completions(List<String> args) {
        if (args.size() <= 1) {
            return CommandUtils.matching(args.isEmpty() ? "" : args.get(0), MailCommandCatalog.actions());
        }
        String action = args.get(0).toLowerCase(Locale.ROOT);
        if (args.size() == 2 && List.of("send", "sendtemp", "read", "clear").contains(action)) {
            return CompletionUtils.onlinePlayers(args.get(1));
        }
        if (args.size() == 3 && action.equals("sendtemp")) {
            return CommandUtils.matching(args.get(2), List.of("10m", "1h", "24h", "7d"));
        }
        if (args.size() == 2 && action.equals("delete")) {
            return CompletionUtils.integerRange(args.get(1), 1, 20);
        }
        return List.of();
    }

    private List<String> mailAllCompletions(List<String> args) {
        if (args.size() <= 1) {
            return CommandUtils.matching(args.isEmpty() ? "" : args.get(0), List.of("send", "clear", "remove"));
        }
        return List.of();
    }

    private void mail(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            read(sender, label, args);
            return;
        }
        switch (args.get(0).toLowerCase(Locale.ROOT)) {
            case "read" -> read(sender, label, args);
            case "send" -> send(sender, label, args);
            case "sendtemp" -> sendTemp(sender, label, args);
            case "delete" -> delete(sender, label, args);
            case "clear" -> clear(sender, label, args);
            case "sendall" -> sendAll(sender, label, args);
            default -> context.message(sender, "mail.usage", Map.of("label", label));
        }
    }

    private void mailAll(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            context.message(sender, "mail.mailall-usage", Map.of("label", label));
            return;
        }
        switch (args.getFirst().toLowerCase(Locale.ROOT)) {
            case "send" -> {
                if (args.size() < 2) {
                    context.message(sender, "mail.mailall-usage", Map.of("label", label));
                    return;
                }
                sendAllMessage(sender, join(args, 1));
            }
            case "clear" -> {
                int removed = store.clearAll();
                context.message(sender, "mail.mailall-cleared", Map.of("count", removed));
            }
            case "remove" -> {
                if (args.size() < 2) {
                    context.message(sender, "mail.mailall-usage", Map.of("label", label));
                    return;
                }
                int removed = store.removeMessage(join(args, 1));
                context.message(sender, "mail.mailall-removed", Map.of("count", removed));
            }
            default -> context.message(sender, "mail.mailall-usage", Map.of("label", label));
        }
    }

    private void read(CommandSender sender, String label, List<String> args) {
        OfflinePlayer target = targetOrSelf(sender, args, 1);
        if (target == null) {
            return;
        }
        List<MailRecord> records = store.list(target.getUniqueId());
        if (records.isEmpty()) {
            context.message(sender, "mail.empty", Map.of("target", knownName(target)));
            return;
        }
        context.message(sender, "mail.header", Map.of("target", knownName(target), "count", records.size()));
        for (int index = 0; index < records.size(); index++) {
            MailRecord record = records.get(index);
            context.message(sender, "mail.entry", Map.of(
                    "index", index + 1,
                    "sender", record.senderName(),
                    "message", record.message(),
                    "date", DATE_FORMAT.format(record.createdAt()),
                    "expires", record.expiresAt().map(DATE_FORMAT::format)
                            .orElseGet(() -> context.messages().template("mail.never-expires", "never"))
            ));
        }
    }

    private void send(CommandSender sender, String label, List<String> args) {
        if (args.size() < 3) {
            context.message(sender, "mail.send-usage", Map.of("label", label));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args.get(1));
        String message = join(args, 2);
        store.append(target.getUniqueId(), senderName(sender), message);
        context.message(sender, "mail.sent", Map.of("target", knownName(target), "message", message));
        Player online = target.getPlayer();
        if (online != null) {
            context.message(online, "mail.received-online", Map.of("sender", senderName(sender)));
        }
    }

    private void sendTemp(CommandSender sender, String label, List<String> args) {
        if (!sender.hasPermission("hydroxide.command.mail.sendtemp")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.mail.sendtemp"));
            return;
        }
        if (args.size() < 4) {
            context.message(sender, "mail.sendtemp-usage", Map.of("label", label));
            return;
        }
        Duration duration = MailDurationParser.parse(args.get(2)).orElse(null);
        if (duration == null) {
            context.message(sender, "mail.invalid-duration", Map.of("duration", args.get(2)));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args.get(1));
        String message = join(args, 3);
        Instant expiresAt = Instant.now().plus(duration);
        store.append(target.getUniqueId(), senderName(sender), message, java.util.Optional.of(expiresAt));
        context.message(sender, "mail.sent-temp", Map.of(
                "target", knownName(target),
                "message", message,
                "duration", args.get(2),
                "expires", DATE_FORMAT.format(expiresAt)
        ));
        Player online = target.getPlayer();
        if (online != null) {
            context.message(online, "mail.received-online", Map.of("sender", senderName(sender)));
        }
    }

    private void delete(CommandSender sender, String label, List<String> args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.size() < 2) {
            context.message(sender, "mail.delete-usage", Map.of("label", label));
            return;
        }
        int index;
        try {
            index = Integer.parseInt(args.get(1));
        } catch (NumberFormatException exception) {
            context.message(sender, "mail.invalid-index", Map.of("index", args.get(1)));
            return;
        }
        if (store.delete(player.getUniqueId(), index)) {
            context.message(sender, "mail.deleted", Map.of("index", index));
        } else {
            context.message(sender, "mail.invalid-index", Map.of("index", index));
        }
    }

    private void clear(CommandSender sender, String label, List<String> args) {
        OfflinePlayer target = targetOrSelf(sender, args, 1);
        if (target == null) {
            return;
        }
        int cleared = store.clear(target.getUniqueId());
        context.message(sender, "mail.cleared", Map.of("target", knownName(target), "count", cleared));
    }

    private void sendAll(CommandSender sender, String label, List<String> args) {
        if (!sender.hasPermission("hydroxide.command.mail.sendall")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.mail.sendall"));
            return;
        }
        if (args.size() < 2) {
            context.message(sender, "mail.sendall-usage", Map.of("label", label));
            return;
        }
        sendAllMessage(sender, join(args, 1));
    }

    private void sendAllMessage(CommandSender sender, String message) {
        String senderName = senderName(sender);
        Map<UUID, OfflinePlayer> recipients = new LinkedHashMap<>();
        Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(player -> player.getName() != null || player.hasPlayedBefore())
                .forEach(player -> recipients.put(player.getUniqueId(), player));
        for (Player online : Bukkit.getOnlinePlayers()) {
            recipients.putIfAbsent(online.getUniqueId(), online);
        }
        for (OfflinePlayer recipient : recipients.values()) {
            store.append(recipient.getUniqueId(), senderName, message);
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            context.message(online, "mail.received-online", Map.of("sender", senderName));
        }
        context.message(sender, "mail.sendall-complete", Map.of("count", recipients.size()));
    }

    private OfflinePlayer targetOrSelf(CommandSender sender, List<String> args, int targetIndex) {
        if (args.size() > targetIndex) {
            if (!sender.hasPermission("hydroxide.command.mail.others")) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.mail.others"));
                return null;
            }
            return Bukkit.getOfflinePlayer(args.get(targetIndex));
        }
        if (sender instanceof Player player) {
            return player;
        }
        context.message(sender, "mail.console-target-required", Map.of());
        return null;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        context.message(sender, "mail.console-target-required", Map.of());
        return null;
    }

    private String senderName(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getName();
        }
        return context.messages().template("mail.console-sender-name", "Console");
    }

    private String knownName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    private String join(List<String> args, int startIndex) {
        return CommandUtils.joinArgs(args.toArray(String[]::new), startIndex);
    }
}
