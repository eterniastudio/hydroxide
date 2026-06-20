package net.axther.hydroxide.modules.jail;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.StoredLocation;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class JailModule implements HydroModule, Listener {

    private HydroxideContext context;
    private YamlStore jailStore;
    private YamlStore punishmentStore;
    private final Map<UUID, JailSentence> jailed = new HashMap<>();
    private BukkitTask releaseTask;

    @Override
    public String id() {
        return "jail";
    }

    @Override
    public String displayName() {
        return "Jail";
    }

    @Override
    public String description() {
        return "Persistent punishment isolation with command, chat, and interaction restrictions.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.jailStore = new YamlStore(new File(context.plugin().getDataFolder(), "jails.yml"));
        this.punishmentStore = new YamlStore(new File(context.plugin().getDataFolder(), "punishments.yml"));
        loadSentences();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("setjail", setJailCommand());
        context.commands().register("jail", jailCommand());
        context.commands().register("togglejail", toggleJailCommand());
        context.commands().register("unjail", unjailCommand());
        context.commands().register("jails", jailsCommand());
        context.commands().register("deljail", deleteJailCommand());
        releaseTask = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::releaseExpired, 20L, 20L);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        if (releaseTask != null) {
            releaseTask.cancel();
        }
        saveSentences();
    }

    private CommandService setJailCommand() {
        return new CommandService(HydroCommand.builder("setjail")
                .permission("hydroxide.command.setjail")
                .playerOnly(true)
                .usage("/{label} <name>")
                .executor(ctx -> setJail(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .build(), context.messages());
    }

    private CommandService jailCommand() {
        return new CommandService(HydroCommand.builder("jail")
                .permission("hydroxide.command.jail")
                .usage("/{label} <player> [duration] [jail] [cellId] [-s] [r:<reason>]")
                .executor(ctx -> jail(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .completer(this::jailCompletions)
                .build(), context.messages());
    }

    private CommandService toggleJailCommand() {
        return new CommandService(HydroCommand.builder("togglejail")
                .permission("hydroxide.command.togglejail")
                .usage("/{label} <player> [duration] [jail] [cellId] [-s] [r:<reason>]")
                .executor(ctx -> toggleJail(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .completer(this::jailCompletions)
                .build(), context.messages());
    }

    private CommandService unjailCommand() {
        return new CommandService(HydroCommand.builder("unjail")
                .permission("hydroxide.command.unjail")
                .usage("/{label} <player> [-s]")
                .executor(ctx -> unjail(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .completer(ctx -> ctx.arguments().size() == 1 ? CompletionUtils.onlinePlayers(ctx.argument(0)) : List.of())
                .build(), context.messages());
    }

    private CommandService jailsCommand() {
        return new CommandService(HydroCommand.builder("jails")
                .permission("hydroxide.command.jails")
                .usage("/{label} [jail] [cellId]")
                .executor(ctx -> listJails(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .completer(this::jailsCompletions)
                .build(), context.messages());
    }

    private CommandService deleteJailCommand() {
        return new CommandService(HydroCommand.builder("deljail")
                .permission("hydroxide.command.deljail")
                .usage("/{label} <name>")
                .executor(ctx -> deleteJail(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .completer(ctx -> ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), jailNames()) : List.of())
                .build(), context.messages());
    }

    private List<String> jailCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CompletionUtils.onlinePlayers(ctx.argument(0));
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new java.util.ArrayList<>(jailNames());
            values.addAll(List.of("5m", "10m", "30m", "1h", "-s", "r:"));
            return CommandUtils.matching(ctx.argument(1), values);
        }
        if (ctx.arguments().size() == 3) {
            if (JailCommandParser.parseDurationHint(ctx.argument(1)).isPresent()) {
                return CommandUtils.matching(ctx.argument(2), jailNames());
            }
            return CommandUtils.matching(ctx.argument(2), List.of("5m", "10m", "30m", "1h", "300", "3600", "-s", "r:"));
        }
        return List.of();
    }

    private List<String> jailsCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), jailBaseNames());
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), cellIds(ctx.argument(0)));
        }
        return List.of();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        JailSentence sentence = jailed.get(event.getPlayer().getUniqueId());
        if (sentence != null) {
            teleportToJail(event.getPlayer(), sentence.jailName());
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (jailed.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            context.message(event.getPlayer(), "jail.restriction.chat", Map.of());
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!jailed.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        String root = event.getMessage().split(" ")[0].toLowerCase(Locale.ROOT);
        List<String> whitelist = context.plugin().getConfig().getStringList("jail.allowed-commands");
        if (whitelist.stream().noneMatch(root::equalsIgnoreCase)) {
            event.setCancelled(true);
            context.message(event.getPlayer(), "jail.restriction.command", Map.of());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        cancelIfJailed(event.getPlayer(), event);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        cancelIfJailed(event.getPlayer(), event);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        cancelIfJailed(event.getPlayer(), event);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && jailed.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private boolean setJail(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        if (args.length == 0) {
            context.message(sender, "jail.set.usage", Map.of("label", label));
            return true;
        }
        YamlConfiguration yaml = jailStore.load();
        String jailName = args[0].toLowerCase(Locale.ROOT);
        StoredLocation.from(player.getLocation()).writeTo(yaml.createSection("jails." + jailName));
        jailStore.save(yaml);
        context.message(sender, "jail.set.success", Map.of("jail", jailName));
        return true;
    }

    private boolean jail(CommandSender sender, String label, String[] args) {
        JailCommandParser.Request request = JailCommandParser
                .parse(List.of(args), jailNames(), defaultJailDuration())
                .orElse(null);
        if (request == null) {
            context.message(sender, "jail.jail.usage", Map.of("label", label));
            return true;
        }
        return jailPlayer(sender, request, "jail.jail.player-offline", "jail.jail.no-cells",
                "jail.jail.missing-cell", "jail.jail.success");
    }

    private boolean toggleJail(CommandSender sender, String label, String[] args) {
        JailToggleCommandParser.Request request = JailToggleCommandParser
                .parse(List.of(args), jailNames(), defaultJailDuration())
                .orElse(null);
        if (request == null) {
            context.message(sender, "jail.toggle.usage", Map.of("label", label));
            return true;
        }
        Player target = CommandUtils.onlinePlayer(request.targetName()).orElse(null);
        if (target == null) {
            context.message(sender, "jail.toggle.player-offline", Map.of("target", request.targetName()));
            return true;
        }
        if (jailed.containsKey(target.getUniqueId())) {
            release(target.getUniqueId());
            if (!request.silent()) {
                context.message(sender, "jail.toggle.released", Map.of("target", target.getName()));
            }
            return true;
        }
        return jailPlayer(sender, request.toJailRequest(), "jail.toggle.player-offline", "jail.toggle.no-cells",
                "jail.toggle.missing-cell", "jail.toggle.jailed");
    }

    private boolean jailPlayer(CommandSender sender, JailCommandParser.Request request, String offlineKey,
                               String noCellsKey, String missingCellKey, String successKey) {
        Player target = CommandUtils.onlinePlayer(request.targetName()).orElse(null);
        if (target == null) {
            context.message(sender, offlineKey, Map.of("target", request.targetName()));
            return true;
        }
        Optional<String> jailName = resolveJailName(request);
        if (jailName.isEmpty()) {
            context.message(sender, noCellsKey, Map.of());
            return true;
        }
        String resolvedJailName = jailName.get();
        if (jailLocation(resolvedJailName).isEmpty()) {
            context.message(sender, missingCellKey, Map.of("jail", resolvedJailName));
            return true;
        }
        long seconds = Math.max(1L, request.duration().toSeconds());
        UUID jailerId = sender instanceof Player player ? player.getUniqueId() : new UUID(0L, 0L);
        JailSentence sentence = new JailSentence(
                target.getUniqueId(),
                resolvedJailName.toLowerCase(Locale.ROOT),
                jailerId,
                request.reason().orElseGet(() -> context.messages().template("jail.jail.default-reason", "No reason provided")),
                Instant.now().plusSeconds(seconds),
                StoredLocation.from(target.getLocation())
        );
        jailed.put(target.getUniqueId(), sentence);
        saveSentences();
        teleportToJail(target, sentence.jailName());
        if (!request.silent()) {
            context.message(sender, successKey, Map.of("target", target.getName(), "jail", sentence.jailName(), "seconds", seconds));
        }
        return true;
    }

    private boolean unjail(CommandSender sender, String label, String[] args) {
        JailReleaseCommandParser.Request request = JailReleaseCommandParser.parse(List.of(args)).orElse(null);
        if (request == null) {
            context.message(sender, "jail.unjail.usage", Map.of("label", label));
            return true;
        }
        Player target = CommandUtils.onlinePlayer(request.targetName()).orElse(null);
        UUID targetId = target != null ? target.getUniqueId() : null;
        if (targetId == null) {
            context.message(sender, "jail.unjail.player-offline", Map.of("target", request.targetName()));
            return true;
        }
        release(targetId);
        if (!request.silent()) {
            context.message(sender, "jail.unjail.success", Map.of("target", target.getName()));
        }
        return true;
    }

    private boolean listJails(CommandSender sender, String label, String[] args) {
        if (args.length > 2) {
            context.message(sender, "jail.list.usage", Map.of("label", label));
            return true;
        }
        JailListSnapshot snapshot = JailListSnapshot.create(
                jailNames(),
                jailed.values(),
                jailedPlayerNames(),
                args.length >= 1 ? Optional.of(args[0]) : Optional.empty(),
                args.length >= 2 ? Optional.of(args[1]) : Optional.empty(),
                Instant.now()
        );
        if (snapshot.empty()) {
            context.message(sender, "jail.list.empty", Map.of());
            return true;
        }
        context.message(sender, "jail.list.header", Map.of("count", snapshot.cells().size()));
        for (int index = 0; index < snapshot.cells().size(); index++) {
            JailListSnapshot.Cell cell = snapshot.cells().get(index);
            context.message(sender, "jail.list.entry", Map.of(
                    "index", index + 1,
                    "jail", cell.jailName(),
                    "occupied", cell.prisonerCount()
            ));
            if (cell.prisoners().isEmpty()) {
                context.message(sender, "jail.list.no-prisoners", Map.of("jail", cell.jailName()));
            }
            for (JailListSnapshot.Prisoner prisoner : cell.prisoners()) {
                context.message(sender, "jail.list.prisoner", Map.of(
                        "player", prisoner.playerName(),
                        "remaining", prisoner.remainingSeconds(),
                        "reason", prisoner.reason()
                ));
            }
        }
        return true;
    }

    private boolean deleteJail(CommandSender sender, String label, String[] args) {
        if (args.length != 1) {
            context.message(sender, "jail.delete.usage", Map.of("label", label));
            return true;
        }
        YamlConfiguration yaml = jailStore.load();
        if (!JailCellIndex.delete(yaml, args[0])) {
            context.message(sender, "jail.delete.missing-cell", Map.of("jail", args[0]));
            return true;
        }
        jailStore.save(yaml);
        context.message(sender, "jail.delete.success", Map.of("jail", args[0].toLowerCase(Locale.ROOT)));
        return true;
    }

    private void releaseExpired() {
        Instant now = Instant.now();
        List<UUID> expired = jailed.values().stream()
                .filter(sentence -> sentence.expired(now))
                .map(JailSentence::playerId)
                .toList();
        expired.forEach(this::release);
    }

    private void release(UUID playerId) {
        JailSentence sentence = jailed.remove(playerId);
        if (sentence == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && sentence.previousLocation() != null) {
            toLocation(sentence.previousLocation()).ifPresent(player::teleportAsync);
            context.message(player, "jail.release.notice", Map.of());
        }
        saveSentences();
    }

    private void teleportToJail(Player player, String jailName) {
        jailLocation(jailName).flatMap(this::toLocation).ifPresent(player::teleportAsync);
    }

    private Optional<StoredLocation> jailLocation(String jailName) {
        return StoredLocation.readFrom(jailStore.load().getConfigurationSection("jails." + jailName.toLowerCase(Locale.ROOT)));
    }

    private List<String> jailNames() {
        return JailCellIndex.names(jailStore.load());
    }

    private List<String> jailBaseNames() {
        return jailNames().stream()
                .map(name -> name.split("-", 2)[0])
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<String> cellIds(String jailName) {
        String prefix = jailName.toLowerCase(Locale.ROOT) + "-";
        return jailNames().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .filter(name -> name.startsWith(prefix))
                .map(name -> name.substring(prefix.length()))
                .filter(cell -> !cell.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private Map<UUID, String> jailedPlayerNames() {
        Map<UUID, String> names = new HashMap<>();
        for (UUID playerId : jailed.keySet()) {
            Player online = Bukkit.getPlayer(playerId);
            if (online != null) {
                names.put(playerId, online.getName());
                continue;
            }
            String offlineName = Bukkit.getOfflinePlayer(playerId).getName();
            if (offlineName != null && !offlineName.isBlank()) {
                names.put(playerId, offlineName);
            }
        }
        return names;
    }

    private Duration defaultJailDuration() {
        long seconds = context.plugin().getConfig().getLong("jail.default-duration-seconds", 300L);
        return Duration.ofSeconds(Math.max(1L, seconds));
    }

    private Optional<String> resolveJailName(JailCommandParser.Request request) {
        Optional<String> configured = request.jailName().or(this::defaultJailName);
        if (configured.isEmpty()) {
            return Optional.empty();
        }
        String jailName = configured.get().toLowerCase(Locale.ROOT);
        if (request.cellId().isPresent()) {
            String combinedName = jailName + "-" + request.cellId().get();
            if (jailLocation(combinedName).isPresent()) {
                return Optional.of(combinedName);
            }
        }
        return Optional.of(jailName);
    }

    private Optional<String> defaultJailName() {
        List<String> names = jailNames();
        return names.isEmpty() ? Optional.empty() : Optional.of(names.getFirst());
    }

    private Optional<Location> toLocation(StoredLocation stored) {
        World world = Bukkit.getWorld(stored.worldName());
        return world == null ? Optional.empty() : Optional.of(stored.toLocation(world));
    }

    private void loadSentences() {
        jailed.clear();
        YamlConfiguration yaml = punishmentStore.load();
        ConfigurationSection section = yaml.getConfigurationSection("jailed");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            UUID playerId = UUID.fromString(key);
            ConfigurationSection node = section.getConfigurationSection(key);
            if (node == null) {
                continue;
            }
            jailed.put(playerId, new JailSentence(
                    playerId,
                    node.getString("jail", "default"),
                    UUID.fromString(node.getString("jailer", "00000000-0000-0000-0000-000000000000")),
                    node.getString("reason", "No reason provided"),
                    Instant.ofEpochMilli(node.getLong("release-at")),
                    StoredLocation.readFrom(node.getConfigurationSection("previous-location")).orElse(null)
            ));
        }
    }

    private void saveSentences() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (JailSentence sentence : jailed.values()) {
            String path = "jailed." + sentence.playerId();
            yaml.set(path + ".jail", sentence.jailName());
            yaml.set(path + ".jailer", sentence.jailerId().toString());
            yaml.set(path + ".reason", sentence.reason());
            yaml.set(path + ".release-at", sentence.releaseAt().toEpochMilli());
            if (sentence.previousLocation() != null) {
                sentence.previousLocation().writeTo(yaml.createSection(path + ".previous-location"));
            }
        }
        punishmentStore.save(yaml);
    }

    private void cancelIfJailed(Player player, org.bukkit.event.Cancellable event) {
        if (jailed.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

}
