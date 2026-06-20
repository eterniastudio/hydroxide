package net.axther.hydroxide.modules.motd;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class MotdModule implements HydroModule, Listener {

    private static final Pattern CUSTOM_TEXT_NAME = Pattern.compile("[A-Za-z0-9_-]+");

    private HydroxideContext context;
    private YamlStore motdStore;

    @Override
    public String id() {
        return "motd";
    }

    @Override
    public String displayName() {
        return "MOTD";
    }

    @Override
    public String description() {
        return "Customizes server list MOTD, max-player display, version text, and hover samples.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.motdStore = new YamlStore(new File(context.plugin().getDataFolder(), "motd.yml"));
        seedDefault();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        for (String command : ServerInfoCommandCatalog.commands()) {
            context.commands().register(command, command(command));
        }
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    private CommandService command(String name) {
        return switch (name) {
            case "motd" -> command("motd", "hydroxide.command.motd", "/{label}",
                    ctx -> showPage(ctx.sender(), "motd"), null);
            case "info" -> command("info", "hydroxide.command.info", "/{label}",
                    ctx -> showPage(ctx.sender(), "info"), null);
            case "rules" -> command("rules", "hydroxide.command.rules", "/{label}",
                    ctx -> showPage(ctx.sender(), "rules"), null);
            case "ctext" -> command("ctext", "hydroxide.command.ctext", "/{label} <cText> [player|all] [sourcePlayer]",
                    ctx -> customText(ctx.sender(), ctx.label(), ctx.arguments()), this::customTextCompletions);
            case "editctext" -> command("editctext", "hydroxide.command.editctext", "/{label} <list|show|set|delete|enable|disable|reload> [name] [line1 | line2]",
                    ctx -> editCustomText(ctx.sender(), ctx.label(), ctx.arguments()), this::editCustomTextCompletions);
            case "helpop" -> command("helpop", "hydroxide.command.helpop", "/{label} <message>",
                    ctx -> helpop(ctx.sender(), ctx.label(), ctx.arguments()), null);
            case "list" -> command("list", "hydroxide.command.list", "/{label}",
                    ctx -> list(ctx.sender()), null);
            case "ping" -> command("ping", "hydroxide.command.ping", "/{label} [player]",
                    ctx -> ping(ctx.sender(), ctx.arguments()),
                    ctx -> ctx.arguments().size() == 1 ? net.axther.hydroxide.commands.CompletionUtils.onlinePlayers(ctx.argument(0)) : List.of());
            case "gc" -> command("gc", "hydroxide.command.gc", "/{label}",
                    ctx -> gc(ctx.sender()), null);
            case "tps" -> command("tps", "hydroxide.command.tps", "/{label} [-spikes]",
                    ctx -> tps(ctx.sender(), ctx.label(), ctx.arguments()),
                    ctx -> ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), List.of("-spikes")) : List.of());
            case "servertime" -> command("servertime", "hydroxide.command.servertime", "/{label}",
                    ctx -> servertime(ctx.sender()), null);
            case "setmotd" -> command("setmotd", "hydroxide.command.setmotd", "/{label} <newMotd> [-s]",
                    ctx -> setMotd(ctx.sender(), ctx.label(), ctx.arguments()), null);
            case "status" -> command("status", "hydroxide.command.status", "/{label}",
                    ctx -> status(ctx.sender()), null);
            case "maxplayers" -> command("maxplayers", "hydroxide.command.maxplayers", "/{label} [amount]",
                    ctx -> maxPlayers(ctx.sender(), ctx.label(), ctx.arguments()),
                    ctx -> ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), List.of("50", "100", "250")) : List.of());
            default -> throw new IllegalArgumentException("Unknown server info command: " + name);
        };
    }

    private List<String> editCustomTextCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), List.of("list", "show", "set", "delete", "enable", "disable", "reload"));
        }
        if (ctx.arguments().size() == 2 && List.of("show", "set", "delete", "enable", "disable")
                .contains(ctx.argument(0).toLowerCase(java.util.Locale.ROOT))) {
            return CommandUtils.matching(ctx.argument(1), customTextNames());
        }
        return List.of();
    }

    private CommandService command(String name, String permission, String usage, HydroCommand.HydroCommandExecutor executor,
                                   HydroCommand.HydroTabCompleter completer) {
        return new CommandService(HydroCommand.builder(name)
                .permission(permission)
                .usage(usage)
                .executor(executor)
                .completer(completer)
                .build(), context.messages());
    }

    private void editCustomText(CommandSender sender, String label, List<String> args) {
        Optional<EditCustomTextCommandParser.Request> parsed = EditCustomTextCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "server-info.editctext.usage", Map.of("label", label));
            return;
        }
        EditCustomTextCommandParser.Request request = parsed.orElseThrow();
        switch (request.action()) {
            case LIST -> editCustomTextList(sender);
            case SHOW -> editCustomTextShow(sender, request.name().orElseThrow());
            case SET -> editCustomTextSet(sender, request.name().orElseThrow(), request.lines());
            case DELETE -> editCustomTextDelete(sender, request.name().orElseThrow());
            case ENABLE -> editCustomTextToggle(sender, request.name().orElseThrow(), true);
            case DISABLE -> editCustomTextToggle(sender, request.name().orElseThrow(), false);
            case RELOAD -> {
                seedDefault();
                context.message(sender, "server-info.editctext.reloaded", Map.of());
            }
        }
    }

    private void editCustomTextList(CommandSender sender) {
        YamlConfiguration yaml = motdStore.load();
        ConfigurationSection section = yaml.getConfigurationSection("custom-text");
        if (section == null || section.getKeys(false).isEmpty()) {
            context.message(sender, "server-info.editctext.empty", Map.of());
            return;
        }
        List<String> names = section.getKeys(false).stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        context.message(sender, "server-info.editctext.list-header", Map.of("count", names.size()));
        for (String name : names) {
            String path = "custom-text." + name;
            context.message(sender, "server-info.editctext.list-entry", Map.of(
                    "name", name,
                    "state", yaml.getBoolean(path + ".enabled", true)
                            ? context.messages().template("server-info.editctext.state.enabled", "enabled")
                            : context.messages().template("server-info.editctext.state.disabled", "disabled"),
                    "lines", yaml.getStringList(path + ".lines").size()
            ));
        }
    }

    private void editCustomTextShow(CommandSender sender, String requested) {
        YamlConfiguration yaml = motdStore.load();
        Optional<String> key = customTextKey(yaml, requested);
        if (key.isEmpty()) {
            context.message(sender, "server-info.editctext.missing", Map.of("name", requested));
            return;
        }
        String path = "custom-text." + key.orElseThrow();
        List<String> lines = yaml.getStringList(path + ".lines");
        context.message(sender, "server-info.editctext.show-header", Map.of(
                "name", key.orElseThrow(),
                "state", yaml.getBoolean(path + ".enabled", true)
                        ? context.messages().template("server-info.editctext.state.enabled", "enabled")
                        : context.messages().template("server-info.editctext.state.disabled", "disabled"),
                "lines", lines.size()
        ));
        for (int index = 0; index < lines.size(); index++) {
            context.message(sender, "server-info.editctext.line", Map.of(
                    "index", index + 1,
                    "value", context.text().literal(lines.get(index))
            ));
        }
    }

    private void editCustomTextSet(CommandSender sender, String name, List<String> lines) {
        if (!validCustomTextName(sender, name)) {
            return;
        }
        YamlConfiguration yaml = motdStore.load();
        String path = "custom-text." + name;
        yaml.set(path + ".enabled", yaml.getBoolean(path + ".enabled", true));
        yaml.set(path + ".lines", lines);
        if (saveMotd(sender, yaml)) {
            context.message(sender, "server-info.editctext.saved", Map.of("name", name, "lines", lines.size()));
        }
    }

    private void editCustomTextDelete(CommandSender sender, String requested) {
        YamlConfiguration yaml = motdStore.load();
        Optional<String> key = customTextKey(yaml, requested);
        if (key.isEmpty()) {
            context.message(sender, "server-info.editctext.missing", Map.of("name", requested));
            return;
        }
        yaml.set("custom-text." + key.orElseThrow(), null);
        if (saveMotd(sender, yaml)) {
            context.message(sender, "server-info.editctext.deleted", Map.of("name", key.orElseThrow()));
        }
    }

    private void editCustomTextToggle(CommandSender sender, String requested, boolean enabled) {
        YamlConfiguration yaml = motdStore.load();
        Optional<String> key = customTextKey(yaml, requested);
        if (key.isEmpty()) {
            context.message(sender, "server-info.editctext.missing", Map.of("name", requested));
            return;
        }
        yaml.set("custom-text." + key.orElseThrow() + ".enabled", enabled);
        if (saveMotd(sender, yaml)) {
            context.message(sender, enabled ? "server-info.editctext.enabled" : "server-info.editctext.disabled",
                    Map.of("name", key.orElseThrow()));
        }
    }

    private boolean validCustomTextName(CommandSender sender, String name) {
        if (CUSTOM_TEXT_NAME.matcher(name).matches()) {
            return true;
        }
        context.message(sender, "server-info.editctext.invalid-name", Map.of("name", name));
        return false;
    }

    private boolean saveMotd(CommandSender sender, YamlConfiguration yaml) {
        try {
            File source = motdStore.file();
            if (source.exists()) {
                File parent = source.getAbsoluteFile().getParentFile();
                File backup = new File(parent == null ? new File(".") : parent, source.getName() + ".bak");
                Files.copy(source.toPath(), backup.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            motdStore.save(yaml);
            return true;
        } catch (IOException | RuntimeException exception) {
            context.message(sender, "server-info.editctext.save-failed", Map.of("reason", exception.getMessage()));
            return false;
        }
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        YamlConfiguration yaml = motdStore.load();
        List<String> lines = yaml.getStringList("motd.lines");
        if (!lines.isEmpty()) {
            event.motd(context.text().format(String.join("\n", lines)));
        }
        if (yaml.getBoolean("player-count.enabled", false)) {
            event.setMaxPlayers(yaml.getInt("player-count.max", event.getMaxPlayers()));
            if (yaml.contains("player-count.online")) {
                event.setNumPlayers(yaml.getInt("player-count.online", event.getNumPlayers()));
            }
        }
        String versionText = yaml.getString("version-text", "");
        if (!versionText.isBlank()) {
            event.setVersion(versionText);
        }
        List<String> hover = yaml.getStringList("hover");
        if (!hover.isEmpty()) {
            event.getListedPlayers().clear();
            for (String line : hover) {
                event.getListedPlayers().add(new PaperServerListPingEvent.ListedPlayerInfo(
                        line,
                        UUID.nameUUIDFromBytes(line.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                ));
            }
        }
    }

    private List<String> customTextCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        return switch (ctx.arguments().size()) {
            case 1 -> CommandUtils.matching(ctx.argument(0), customTextNames());
            case 2, 3 -> {
                List<String> values = new java.util.ArrayList<>(List.of("all"));
                values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                yield CommandUtils.matching(ctx.argument(ctx.arguments().size() - 1), values);
            }
            default -> List.of();
        };
    }

    private void showPage(CommandSender sender, String page) {
        YamlConfiguration yaml = motdStore.load();
        List<String> lines = yaml.getStringList("commands." + page + ".lines");
        context.message(sender, "server-info." + page + ".header", Map.of("page", page));
        if (lines.isEmpty()) {
            context.message(sender, "server-info.empty", Map.of("page", page));
            return;
        }
        for (String line : lines) {
            sender.sendMessage(context.text().format(line));
        }
    }

    private void customText(CommandSender sender, String label, List<String> args) {
        Optional<CustomTextCommandParser.Request> parsed = CustomTextCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "server-info.ctext.usage", Map.of("label", label));
            return;
        }

        CustomTextCommandParser.Request request = parsed.orElseThrow();
        YamlConfiguration yaml = motdStore.load();
        Optional<String> textKey = customTextKey(yaml, request.name());
        if (textKey.isEmpty()) {
            context.message(sender, "server-info.ctext.missing", Map.of("name", request.name()));
            return;
        }

        String path = "custom-text." + textKey.orElseThrow();
        if (!yaml.getBoolean(path + ".enabled", true)) {
            context.message(sender, "server-info.ctext.missing", Map.of("name", request.name()));
            return;
        }
        List<String> lines = yaml.getStringList(path + ".lines");
        if (lines.isEmpty()) {
            context.message(sender, "server-info.ctext.empty", Map.of("name", textKey.orElseThrow()));
            return;
        }

        Optional<List<CommandSender>> recipients = customTextRecipients(sender, request);
        if (recipients.isEmpty()) {
            return;
        }
        List<CommandSender> targets = recipients.orElseThrow();
        if (targets.isEmpty()) {
            context.message(sender, "server-info.ctext.no-recipients", Map.of("target", request.targetName().orElse("self")));
            return;
        }

        for (CommandSender recipient : targets) {
            context.message(recipient, "server-info.ctext.header", Map.of("name", textKey.orElseThrow()));
            for (String line : lines) {
                recipient.sendMessage(context.text().format(renderCustomTextLine(line, recipient, request.sourceName().orElse(sender.getName()))));
            }
        }
        if (request.targetName().isPresent() && !targets.contains(sender)) {
            context.message(sender, "server-info.ctext.sent", Map.of("name", textKey.orElseThrow(), "count", targets.size()));
        }
    }

    private Optional<List<CommandSender>> customTextRecipients(CommandSender sender, CustomTextCommandParser.Request request) {
        if (request.targetName().isEmpty()) {
            return Optional.of(List.of(sender));
        }
        String targetName = request.targetName().orElseThrow();
        if (request.targetsAll()) {
            if (!context.requirePermission(sender, "hydroxide.command.ctext.others")) {
                return Optional.empty();
            }
            List<CommandSender> recipients = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());
            return Optional.of(recipients);
        }

        Player target = CommandUtils.onlinePlayer(targetName).orElse(null);
        if (target == null) {
            context.message(sender, "server-info.ctext.target-offline", Map.of("target", targetName));
            return Optional.empty();
        }
        if (!sender.equals(target) && !context.requirePermission(sender, "hydroxide.command.ctext.others")) {
            return Optional.empty();
        }
        return Optional.of(List.of(target));
    }

    private String renderCustomTextLine(String line, CommandSender recipient, String sourceName) {
        return line
                .replace("{player}", recipient.getName())
                .replace("{source}", sourceName)
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("[playerName]", recipient.getName())
                .replace("[sourcePlayer]", sourceName);
    }

    private Optional<String> customTextKey(YamlConfiguration yaml, String requested) {
        ConfigurationSection section = yaml.getConfigurationSection("custom-text");
        if (section == null) {
            return Optional.empty();
        }
        return section.getKeys(false).stream()
                .filter(key -> key.equalsIgnoreCase(requested))
                .findFirst();
    }

    private List<String> customTextNames() {
        ConfigurationSection section = motdStore.load().getConfigurationSection("custom-text");
        if (section == null) {
            return List.of();
        }
        return section.getKeys(false).stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private void helpop(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            context.message(sender, "server-info.helpop.usage", Map.of("label", label));
            return;
        }
        String message = String.join(" ", args);
        int recipients = 0;
        for (var player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("hydroxide.command.helpop.receive")) {
                context.message(player, "server-info.helpop.alert", Map.of("player", sender.getName(), "message", message));
                recipients++;
            }
        }
        if (!sender.equals(Bukkit.getConsoleSender())) {
            context.message(Bukkit.getConsoleSender(), "server-info.helpop.alert", Map.of("player", sender.getName(), "message", message));
        }
        context.message(sender, "server-info.helpop.sent", Map.of("message", message, "recipients", recipients));
    }

    private void list(CommandSender sender) {
        boolean canSeeVanished = !(sender instanceof Player) || sender.hasPermission("hydroxide.vanish.see");
        List<String> players = Bukkit.getOnlinePlayers().stream()
                .filter(player -> canSeeVanished || context.services().vanishService()
                        .map(service -> !service.isVanished(player.getUniqueId()))
                        .orElse(true))
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        context.message(sender, "server-info.list.header", Map.of(
                "online", players.size(),
                "total", Bukkit.getOnlinePlayers().size(),
                "max", Bukkit.getMaxPlayers()
        ));
        context.message(sender, players.isEmpty() ? "server-info.list.empty" : "server-info.list.players",
                Map.of("players", String.join("<gray>, <white>", players)));
    }

    private void ping(CommandSender sender, List<String> args) {
        Player target;
        if (args.isEmpty()) {
            if (sender instanceof Player player) {
                target = player;
            } else {
                context.message(sender, "server-info.ping.console-target-required", Map.of());
                return;
            }
        } else {
            target = CommandUtils.onlinePlayer(args.getFirst()).orElse(null);
            if (target == null) {
                context.message(sender, "server-info.ping.player-offline", Map.of("target", args.getFirst()));
                return;
            }
            if (!sender.equals(target) && !sender.hasPermission("hydroxide.command.ping.others")) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.ping.others"));
                return;
            }
        }
        context.message(sender, sender.equals(target) ? "server-info.ping.self" : "server-info.ping.other",
                Map.of("target", target.getName(), "ping", target.getPing()));
    }

    private void gc(CommandSender sender) {
        Runtime runtime = Runtime.getRuntime();
        ServerGcFormatter.Snapshot snapshot = ServerGcFormatter.snapshot(
                runtime.maxMemory(),
                runtime.totalMemory(),
                runtime.freeMemory(),
                Bukkit.getWorlds().size(),
                Bukkit.getOnlinePlayers().size(),
                Bukkit.getMaxPlayers()
        );
        context.message(sender, "server-info.gc.header", Map.of());
        context.message(sender, "server-info.gc.memory", Map.of(
                "used_memory", snapshot.usedMemoryMb(),
                "allocated_memory", snapshot.allocatedMemoryMb(),
                "max_memory", snapshot.maxMemoryMb()
        ));
        context.message(sender, "server-info.gc.players", Map.of(
                "online", snapshot.onlinePlayers(),
                "max_players", snapshot.maxPlayers(),
                "worlds", snapshot.worlds()
        ));
        for (World world : Bukkit.getWorlds()) {
            context.message(sender, "server-info.gc.world", Map.of(
                    "world", world.getName(),
                    "chunks", world.getLoadedChunks().length,
                    "entities", world.getEntities().size()
            ));
        }
    }

    private void tps(CommandSender sender, String label, List<String> args) {
        if (args.size() > 1 || (!args.isEmpty() && !args.getFirst().equalsIgnoreCase("-spikes"))) {
            context.message(sender, "server-info.tps.usage", Map.of("label", label));
            return;
        }
        ServerTpsFormatter.Snapshot snapshot = ServerTpsFormatter.snapshot(
                Bukkit.getTPS(),
                Bukkit.getAverageTickTime(),
                Bukkit.getTickTimes()
        );
        context.message(sender, "server-info.tps.header", Map.of());
        context.message(sender, "server-info.tps.values", Map.of(
                "one_minute", snapshot.oneMinuteTps(),
                "five_minutes", snapshot.fiveMinuteTps(),
                "fifteen_minutes", snapshot.fifteenMinuteTps()
        ));
        context.message(sender, "server-info.tps.mspt", Map.of("mspt", snapshot.averageMspt()));
        if (!args.isEmpty()) {
            context.message(sender, "server-info.tps.spikes", Map.of("worst_mspt", snapshot.worstTickMs()));
        }
    }

    private void servertime(CommandSender sender) {
        ServerTimeFormatter.Snapshot snapshot = ServerTimeFormatter.snapshot(Instant.now(), ZoneId.systemDefault());
        context.message(sender, "server-info.servertime.header", Map.of());
        context.message(sender, "server-info.servertime.local", Map.of(
                "time", snapshot.localTime(),
                "zone", snapshot.zone()
        ));
        context.message(sender, "server-info.servertime.utc", Map.of("time", snapshot.utcTime()));
    }

    private void setMotd(CommandSender sender, String label, List<String> args) {
        var parsed = SetMotdCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "server-info.setmotd.usage", Map.of("label", label));
            return;
        }

        SetMotdCommandParser.Request request = parsed.orElseThrow();
        YamlConfiguration yaml = motdStore.load();
        yaml.set("motd.lines", request.lines());
        motdStore.save(yaml);

        context.message(sender, "server-info.setmotd.saved", Map.of("lines", request.lines().size()));
        if (!request.silent()) {
            notifyMotdChange(sender, request.lines().size());
        }
    }

    private void notifyMotdChange(CommandSender sender, int lines) {
        Map<String, Object> placeholders = Map.of(
                "player", sender.getName(),
                "lines", lines
        );
        if (!sender.equals(Bukkit.getConsoleSender())) {
            context.message(Bukkit.getConsoleSender(), "server-info.setmotd.broadcast", placeholders);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("hydroxide.command.setmotd.notify") && !sender.equals(player)) {
                context.message(player, "server-info.setmotd.broadcast", placeholders);
            }
        }
    }

    private void status(CommandSender sender) {
        Runtime runtime = Runtime.getRuntime();
        ServerStatusFormatter.Snapshot snapshot = ServerStatusFormatter.snapshot(
                ManagementFactory.getRuntimeMXBean().getUptime(),
                runtime.maxMemory(),
                runtime.totalMemory(),
                runtime.freeMemory(),
                Bukkit.getWorlds().size(),
                Bukkit.getOnlinePlayers().size(),
                Bukkit.getMaxPlayers(),
                Bukkit.getTPS(),
                Bukkit.getAverageTickTime()
        );

        context.message(sender, "server-info.status.header", Map.of());
        context.message(sender, "server-info.status.version", Map.of(
                "plugin_version", context.plugin().getPluginMeta().getVersion(),
                "server_version", Bukkit.getVersion(),
                "java_version", System.getProperty("java.version", "unknown")
        ));
        context.message(sender, "server-info.status.uptime", Map.of("uptime", snapshot.uptime()));
        context.message(sender, "server-info.status.performance", Map.of(
                "tps", snapshot.oneMinuteTps(),
                "mspt", snapshot.averageMspt()
        ));
        context.message(sender, "server-info.status.memory", Map.of(
                "used_memory", snapshot.usedMemoryMb(),
                "max_memory", snapshot.maxMemoryMb()
        ));
        context.message(sender, "server-info.status.players", Map.of(
                "online", snapshot.onlinePlayers(),
                "max_players", snapshot.maxPlayers(),
                "worlds", snapshot.worlds()
        ));
    }

    private void maxPlayers(CommandSender sender, String label, List<String> args) {
        var parsed = MaxPlayersCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "server-info.maxplayers.usage", Map.of("label", label));
            return;
        }

        MaxPlayersCommandParser.Request request = parsed.orElseThrow();
        if (request.amount().isEmpty()) {
            context.message(sender, "server-info.maxplayers.current", Map.of(
                    "amount", Bukkit.getMaxPlayers(),
                    "online", Bukkit.getOnlinePlayers().size()
            ));
            return;
        }

        int amount = request.amount().orElseThrow();
        Bukkit.setMaxPlayers(amount);
        context.message(sender, "server-info.maxplayers.updated", Map.of(
                "amount", amount,
                "online", Bukkit.getOnlinePlayers().size()
        ));
    }

    private void seedDefault() {
        YamlConfiguration yaml = motdStore.load();
        boolean changed = false;
        if (!yaml.contains("motd.lines")) {
            yaml.set("motd.lines", List.of("<gradient:#44CCFF:#FFB000>Hydroxide Server</gradient>", "<gray>Modern Paper core online"));
            yaml.set("hover", List.of("Hydroxide", "Modular server core"));
            yaml.set("player-count.enabled", false);
            yaml.set("player-count.max", 100);
            yaml.set("version-text", "");
            changed = true;
        }
        if (!yaml.contains("commands.motd.lines")) {
            yaml.set("commands.motd.lines", List.of(
                    "<#44CCFF><bold>Welcome to Hydroxide</bold>",
                    "<gray>A modern modular Paper server core."
            ));
            changed = true;
        }
        if (!yaml.contains("commands.info.lines")) {
            yaml.set("commands.info.lines", List.of(
                    "<#44CCFF><bold>Server Info</bold>",
                    "<gray>Edit <white>plugins/Hydroxide/motd.yml<gray> to customize this page."
            ));
            changed = true;
        }
        if (!yaml.contains("commands.rules.lines")) {
            yaml.set("commands.rules.lines", List.of(
                    "<gray>1. Be respectful.",
                    "<gray>2. No cheating or exploiting.",
                    "<gray>3. Staff decisions are final."
            ));
            changed = true;
        }
        if (!yaml.contains("custom-text.welcome.lines")) {
            yaml.set("custom-text.welcome.enabled", true);
            yaml.set("custom-text.welcome.lines", List.of(
                    "<#44CCFF><bold>Welcome, {player}</bold>",
                    "<gray>This page is powered by <white>/ctext welcome<gray>.",
                    "<gray>Online: <white>{online}<gray>/<white>{max}<gray>."
            ));
            changed = true;
        }
        if (!yaml.contains("custom-text.discord.lines")) {
            yaml.set("custom-text.discord.enabled", true);
            yaml.set("custom-text.discord.lines", List.of(
                    "<#5865F2><bold>Discord</bold>",
                    "<gray>Edit <white>plugins/Hydroxide/motd.yml<gray> to set your community link."
            ));
            changed = true;
        }
        if (changed) {
            motdStore.save(yaml);
        }
    }
}
