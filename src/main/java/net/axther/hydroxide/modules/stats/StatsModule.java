package net.axther.hydroxide.modules.stats;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StatsModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

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
        context.commands().register("top", this);
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.top")) {
            return true;
        }
        String stat = args.length == 0 ? "kills" : args[0];
        Leaderboard leaderboard = new Leaderboard(service.values(stat));
        List<Leaderboard.Entry> top = leaderboard.top(10);
        context.send(sender, "<#44CCFF><bold>Top " + stat + "</bold>");
        for (int index = 0; index < top.size(); index++) {
            Leaderboard.Entry entry = top.get(index);
            String name = service.name(entry.playerId()).orElse(entry.playerId().toString());
            context.send(sender, "<gray>" + (index + 1) + ". <white>" + name + " <dark_gray>- <#44CCFF>" + entry.value());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? CommandUtils.matching(args[0], COMMON_STATS) : List.of();
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
}
