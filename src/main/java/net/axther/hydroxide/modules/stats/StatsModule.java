package net.axther.hydroxide.modules.stats;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class StatsModule implements HydroModule, Listener {

    private static final List<String> COMMON_STATS = List.of(
            "kills",
            "deaths",
            "blocks_broken",
            "mobs_killed",
            "playtime_seconds"
    );

    private HydroxideContext context;
    private StatsService service;
    private final Map<UUID, Long> joinedAt = new HashMap<>();

    @Override
    public String id() {
        return "stats";
    }

    @Override
    public String displayName() {
        return "Statistics";
    }

    @Override
    public String description() {
        return "Persistent player statistics, playtime tracking, leaderboards, and placeholders.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.service = new StatsService(new YamlStore(new File(context.plugin().getDataFolder(), "stats.yml")));
        context.services().statsService(service);
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("top", topCommand());
        context.commands().register("playtime", playtimeCommand());
        context.commands().register("cplaytime", cPlaytimeCommand());
        context.commands().register("playtimetop", playtimeTopCommand());
        context.commands().register("editplaytime", editPlaytimeCommand());
        for (Player player : Bukkit.getOnlinePlayers()) {
            joinedAt.put(player.getUniqueId(), System.currentTimeMillis());
            service.rememberName(player.getUniqueId(), player.getName());
        }
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : joinedAt.entrySet()) {
            service.increment(entry.getKey(), "playtime_seconds", Math.max(0L, (now - entry.getValue()) / 1000L));
        }
        context.services().clearStatsService(service);
    }

    private CommandService topCommand() {
        return new CommandService(HydroCommand.builder("top")
                .permission("hydroxide.command.top")
                .usage("/{label} [stat]")
                .executor(ctx -> top(ctx.sender(), ctx.arguments().toArray(String[]::new)))
                .completer(ctx -> ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), COMMON_STATS) : List.of())
                .build(), context.messages());
    }

    private CommandService playtimeCommand() {
        return new CommandService(HydroCommand.builder("playtime")
                .permission("hydroxide.command.playtime")
                .usage("/{label} [player]")
                .executor(ctx -> playtime(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> ctx.arguments().size() == 1 ? onlinePlayerCompletions(ctx.argument(0)) : List.of())
                .build(), context.messages());
    }

    private CommandService cPlaytimeCommand() {
        return new CommandService(HydroCommand.builder("cplaytime")
                .permission("hydroxide.command.cplaytime")
                .usage("/{label} [player]")
                .playerOnly(true)
                .executor(ctx -> cPlaytime((Player) ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> ctx.arguments().size() == 1 ? onlinePlayerCompletions(ctx.argument(0)) : List.of())
                .build(), context.messages());
    }

    private CommandService playtimeTopCommand() {
        return new CommandService(HydroCommand.builder("playtimetop")
                .permission("hydroxide.command.playtimetop")
                .usage("/{label} [page]")
                .executor(ctx -> playtimeTop(ctx.sender(), ctx.label(), ctx.arguments()))
                .build(), context.messages());
    }

    private CommandService editPlaytimeCommand() {
        return new CommandService(HydroCommand.builder("editplaytime")
                .permission("hydroxide.command.editplaytime")
                .usage("/{label} [player] <add|take|set> <amount> [-s]")
                .executor(ctx -> editPlaytime(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::editPlaytimeCompletions)
                .build(), context.messages());
    }

    private List<String> editPlaytimeCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        List<String> actions = List.of("add", "take", "set");
        List<String> amounts = List.of("30m", "1h", "1d", "7d", "-s");
        if (ctx.arguments().size() == 1) {
            java.util.ArrayList<String> values = new java.util.ArrayList<>(actions);
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(current, values);
        }
        if (ctx.arguments().size() == 2) {
            if (actions.contains(ctx.argument(0).toLowerCase(java.util.Locale.ROOT))) {
                return CommandUtils.matching(current, amounts);
            }
            return CommandUtils.matching(current, actions);
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(current, amounts);
        }
        if (ctx.arguments().size() == 4 && ctx.arguments().stream().noneMatch("-s"::equalsIgnoreCase)) {
            return CommandUtils.matching(current, List.of("-s"));
        }
        return List.of();
    }

    private void top(CommandSender sender, String[] args) {
        String stat = args.length == 0 ? "kills" : args[0];
        Leaderboard leaderboard = new Leaderboard(service.values(stat));
        List<Leaderboard.Entry> top = leaderboard.top(10);
        context.message(sender, "stats.top.header", Map.of("stat", stat));
        if (top.isEmpty()) {
            context.message(sender, "stats.top.empty", Map.of("stat", stat));
            return;
        }
        for (int index = 0; index < top.size(); index++) {
            Leaderboard.Entry entry = top.get(index);
            String name = service.name(entry.playerId()).orElse(entry.playerId().toString());
            context.message(sender, "stats.top.entry", Map.of(
                    "rank", index + 1,
                    "player", name,
                    "value", entry.value()
            ));
        }
    }

    private void playtime(CommandSender sender, String label, List<String> args) {
        Optional<PlaytimeCommandParser.PlaytimeRequest> parsed = PlaytimeCommandParser.parsePlaytime(args);
        if (parsed.isEmpty()) {
            context.message(sender, "stats.playtime.usage", Map.of("label", label));
            return;
        }

        Optional<String> requested = parsed.orElseThrow().playerName();
        if (requested.isEmpty()) {
            if (!(sender instanceof Player player)) {
                context.message(sender, "stats.playtime.usage", Map.of("label", label));
                return;
            }
            sendPlaytime(sender, player.getUniqueId(), player.getName(), true);
            return;
        }

        if (!sender.hasPermission("hydroxide.command.playtime.others")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.playtime.others"));
            return;
        }

        String name = requested.orElseThrow();
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            sendPlaytime(sender, online.getUniqueId(), online.getName(), false);
            return;
        }
        Optional<UUID> playerId = service.playerId(name);
        if (playerId.isEmpty()) {
            context.message(sender, "stats.playtime.unknown", Map.of("target", name));
            return;
        }
        sendPlaytime(sender, playerId.orElseThrow(), service.name(playerId.orElseThrow()).orElse(name), false);
    }

    private void sendPlaytime(CommandSender sender, UUID playerId, String playerName, boolean self) {
        String formatted = PlaytimeFormatter.format(playtimeSeconds(playerId));
        context.message(sender, self ? "stats.playtime.self" : "stats.playtime.other", Map.of(
                "player", playerName,
                "seconds", playtimeSeconds(playerId),
                "time", formatted
        ));
    }

    private void cPlaytime(Player viewer, String label, List<String> args) {
        Optional<PlaytimeCommandParser.CPlaytimeRequest> parsed = PlaytimeCommandParser.parseCPlaytime(args);
        if (parsed.isEmpty()) {
            context.message(viewer, "stats.cplaytime.usage", Map.of("label", label));
            return;
        }

        Optional<String> requested = parsed.orElseThrow().playerName();
        if (requested.isPresent() && !viewer.hasPermission("hydroxide.command.cplaytime.others")) {
            context.message(viewer, "validation.no-permission", Map.of("permission", "hydroxide.command.cplaytime.others"));
            return;
        }

        PlaytimeTarget target = cPlaytimeTarget(viewer, requested);
        if (target == null) {
            context.message(viewer, "stats.playtime.unknown", Map.of("target", requested.orElse(viewer.getName())));
            return;
        }
        viewer.openInventory(cPlaytimeInventory(target));
    }

    private Inventory cPlaytimeInventory(PlaytimeTarget target) {
        Map<String, Object> placeholders = cPlaytimePlaceholders(target);
        Inventory inventory = Bukkit.createInventory(null, 27, context.messages().component("stats.cplaytime.title", placeholders));
        inventory.setItem(10, statItem(Material.CLOCK, "stats.cplaytime.total-name", "stats.cplaytime.total-lore", placeholders));
        inventory.setItem(11, statItem(Material.COMPASS, "stats.cplaytime.session-name", "stats.cplaytime.session-lore", placeholders));
        inventory.setItem(12, statItem(Material.DIAMOND_PICKAXE, "stats.cplaytime.blocks-name", "stats.cplaytime.blocks-lore", placeholders));
        inventory.setItem(14, statItem(Material.IRON_SWORD, "stats.cplaytime.kills-name", "stats.cplaytime.kills-lore", placeholders));
        inventory.setItem(15, statItem(Material.SKELETON_SKULL, "stats.cplaytime.deaths-name", "stats.cplaytime.deaths-lore", placeholders));
        inventory.setItem(16, statItem(Material.ZOMBIE_HEAD, "stats.cplaytime.mobs-name", "stats.cplaytime.mobs-lore", placeholders));
        return inventory;
    }

    private Map<String, Object> cPlaytimePlaceholders(PlaytimeTarget target) {
        long totalSeconds = playtimeSeconds(target.playerId());
        long sessionSeconds = target.online()
                .map(Player::getUniqueId)
                .map(joinedAt::get)
                .map(joined -> Math.max(0L, (System.currentTimeMillis() - joined) / 1000L))
                .orElse(0L);
        return Map.of(
                "player", target.name(),
                "time", PlaytimeFormatter.format(totalSeconds),
                "seconds", totalSeconds,
                "session", PlaytimeFormatter.format(sessionSeconds),
                "session_seconds", sessionSeconds,
                "kills", service.value(target.playerId(), "kills"),
                "deaths", service.value(target.playerId(), "deaths"),
                "blocks", service.value(target.playerId(), "blocks_broken"),
                "mobs", service.value(target.playerId(), "mobs_killed")
        );
    }

    private ItemStack statItem(Material material, String nameKey, String loreKey, Map<String, Object> placeholders) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(context.messages().component(nameKey, placeholders));
        meta.lore(List.of(context.messages().component(loreKey, placeholders)));
        item.setItemMeta(meta);
        return item;
    }

    private void editPlaytime(CommandSender sender, String label, List<String> args) {
        Optional<PlaytimeCommandParser.EditPlaytimeRequest> parsed = PlaytimeCommandParser.parseEditPlaytime(args);
        if (parsed.isEmpty()) {
            context.message(sender, "stats.editplaytime.usage", Map.of("label", label));
            return;
        }

        PlaytimeCommandParser.EditPlaytimeRequest request = parsed.orElseThrow();
        PlaytimeTarget target = editPlaytimeTarget(sender, request.playerName());
        if (target == null) {
            context.message(sender, "stats.editplaytime.usage", Map.of("label", label));
            return;
        }

        long current = playtimeSeconds(target.playerId());
        long next = switch (request.action()) {
            case ADD -> saturatedAdd(current, request.seconds());
            case TAKE -> Math.max(0L, current - request.seconds());
            case SET -> request.seconds();
        };
        setPlaytime(target.playerId(), target.name(), next);
        if (request.silent()) {
            return;
        }

        Map<String, Object> placeholders = Map.of(
                "player", target.name(),
                "amount", PlaytimeFormatter.format(request.seconds()),
                "time", PlaytimeFormatter.format(next),
                "seconds", next
        );
        context.message(sender, "stats.editplaytime.updated", placeholders);
        target.online().ifPresent(player -> {
            if (!sender.equals(player)) {
                context.message(player, "stats.editplaytime.target-notice", placeholders);
            }
        });
    }

    private void playtimeTop(CommandSender sender, String label, List<String> args) {
        Optional<PlaytimeCommandParser.TopRequest> parsed = PlaytimeCommandParser.parseTop(args);
        if (parsed.isEmpty()) {
            context.message(sender, "stats.playtimetop.usage", Map.of("label", label));
            return;
        }
        int page = parsed.orElseThrow().page();
        int pageSize = 10;
        List<Leaderboard.Entry> entries = new Leaderboard(playtimeValues()).top(page * pageSize);
        int fromIndex = Math.min((page - 1) * pageSize, entries.size());
        List<Leaderboard.Entry> pageEntries = entries.subList(fromIndex, entries.size()).stream()
                .limit(pageSize)
                .toList();
        if (pageEntries.isEmpty()) {
            context.message(sender, "stats.playtimetop.empty", Map.of("page", page));
            return;
        }

        context.message(sender, "stats.playtimetop.header", Map.of("page", page));
        for (int index = 0; index < pageEntries.size(); index++) {
            Leaderboard.Entry entry = pageEntries.get(index);
            String name = service.name(entry.playerId()).orElse(entry.playerId().toString());
            long rank = (long) fromIndex + index + 1L;
            context.message(sender, "stats.playtimetop.entry", Map.of(
                    "rank", rank,
                    "player", name,
                    "seconds", entry.value(),
                    "time", PlaytimeFormatter.format(entry.value())
            ));
        }
    }

    private long playtimeSeconds(UUID playerId) {
        long stored = service.value(playerId, "playtime_seconds");
        Long joined = joinedAt.get(playerId);
        if (joined == null) {
            return stored;
        }
        return stored + Math.max(0L, (System.currentTimeMillis() - joined) / 1000L);
    }

    private Map<UUID, Long> playtimeValues() {
        Map<UUID, Long> values = new HashMap<>(service.values("playtime_seconds"));
        for (UUID playerId : joinedAt.keySet()) {
            values.put(playerId, playtimeSeconds(playerId));
        }
        return values;
    }

    private void setPlaytime(UUID playerId, String name, long seconds) {
        service.rememberName(playerId, name);
        service.set(playerId, "playtime_seconds", seconds);
        if (joinedAt.containsKey(playerId)) {
            joinedAt.put(playerId, System.currentTimeMillis());
        }
    }

    private PlaytimeTarget editPlaytimeTarget(CommandSender sender, Optional<String> requestedName) {
        if (requestedName.isEmpty()) {
            if (sender instanceof Player player) {
                return new PlaytimeTarget(player.getUniqueId(), player.getName(), Optional.of(player));
            }
            return null;
        }

        String name = requestedName.orElseThrow();
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return new PlaytimeTarget(online.getUniqueId(), online.getName(), Optional.of(online));
        }
        Optional<UUID> storedPlayerId = service.playerId(name);
        if (storedPlayerId.isPresent()) {
            UUID playerId = storedPlayerId.orElseThrow();
            return new PlaytimeTarget(playerId, service.name(playerId).orElse(name), Optional.empty());
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return new PlaytimeTarget(offline.getUniqueId(), offline.getName() == null ? name : offline.getName(), Optional.empty());
    }

    private PlaytimeTarget cPlaytimeTarget(Player viewer, Optional<String> requestedName) {
        if (requestedName.isEmpty()) {
            service.rememberName(viewer.getUniqueId(), viewer.getName());
            return new PlaytimeTarget(viewer.getUniqueId(), viewer.getName(), Optional.of(viewer));
        }

        String name = requestedName.orElseThrow();
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            service.rememberName(online.getUniqueId(), online.getName());
            return new PlaytimeTarget(online.getUniqueId(), online.getName(), Optional.of(online));
        }
        Optional<UUID> storedPlayerId = service.playerId(name);
        if (storedPlayerId.isEmpty()) {
            return null;
        }
        UUID playerId = storedPlayerId.orElseThrow();
        return new PlaytimeTarget(playerId, service.name(playerId).orElse(name), Optional.empty());
    }

    private long saturatedAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private List<String> onlinePlayerCompletions(String current) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(current.toLowerCase(java.util.Locale.ROOT)))
                .toList();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        service.rememberName(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        joinedAt.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Long joined = joinedAt.remove(event.getPlayer().getUniqueId());
        if (joined != null) {
            service.increment(event.getPlayer().getUniqueId(), "playtime_seconds", Math.max(0L, (System.currentTimeMillis() - joined) / 1000L));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        service.increment(event.getPlayer().getUniqueId(), "blocks_broken", 1L);
        service.increment(event.getPlayer().getUniqueId(), "block_" + event.getBlock().getType().name().toLowerCase(java.util.Locale.ROOT), 1L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        service.increment(event.getEntity().getUniqueId(), "deaths", 1L);
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            service.increment(killer.getUniqueId(), "kills", 1L);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && !(event.getEntity() instanceof Player)) {
            service.increment(killer.getUniqueId(), "mobs_killed", 1L);
        }
    }

    public long vanilla(Player player, Statistic statistic) {
        return player.getStatistic(statistic);
    }

    private record PlaytimeTarget(UUID playerId, String name, Optional<Player> online) {
    }
}
