package net.axther.hydroxide.modules.tablist;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TablistModule implements HydroModule, Listener {

    private static final String OBJECTIVE = "hxSidebar";

    private HydroxideContext context;
    private YamlStore store;
    private BukkitTask task;
    private int frame;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    @Override
    public String id() {
        return "tablist";
    }

    @Override
    public String displayName() {
        return "Tablist and Scoreboard";
    }

    @Override
    public String description() {
        return "Animated tablist header/footer and a low-flicker per-player scoreboard sidebar.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "tablist.yml"));
        seedDefaults();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        long interval = Math.max(10L, store.load().getLong("update-interval-ticks", 40L));
        task = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 20L, interval);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        if (task != null) {
            task.cancel();
        }
        boards.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        setup(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        boards.remove(event.getPlayer().getUniqueId());
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
        frame++;
    }

    private void setup(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(OBJECTIVE, Criteria.DUMMY, context.text().format("<#44CCFF>Hydroxide"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (int index = 0; index < Math.min(15, lines().size()); index++) {
            String entry = entry(index);
            Team team = scoreboard.registerNewTeam("hxL" + index);
            team.addEntry(entry);
            objective.getScore(entry).setScore(15 - index);
        }
        boards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
    }

    private void update(Player player) {
        Scoreboard scoreboard = boards.computeIfAbsent(player.getUniqueId(), ignored -> {
            setup(player);
            return player.getScoreboard();
        });
        Objective objective = scoreboard.getObjective(OBJECTIVE);
        if (objective == null) {
            setup(player);
            scoreboard = boards.get(player.getUniqueId());
            objective = scoreboard.getObjective(OBJECTIVE);
        }

        List<String> headerFrames = store.load().getStringList("tablist.header-frames");
        List<String> footerFrames = store.load().getStringList("tablist.footer-frames");
        player.sendPlayerListHeaderAndFooter(
                context.text().format(apply(player, headerFrames.get(frame % Math.max(1, headerFrames.size())))),
                context.text().format(apply(player, footerFrames.get(frame % Math.max(1, footerFrames.size()))))
        );

        objective.displayName(context.text().format(apply(player, store.load().getString("scoreboard.title", "<#44CCFF>Hydroxide"))));
        List<String> lines = lines();
        for (int index = 0; index < Math.min(15, lines.size()); index++) {
            Team team = scoreboard.getTeam("hxL" + index);
            if (team != null) {
                team.prefix(context.text().format(apply(player, lines.get(index))));
            }
        }
    }

    private List<String> lines() {
        List<String> lines = store.load().getStringList("scoreboard.lines");
        return lines.isEmpty() ? List.of("<gray>Online: <white>{online}") : lines;
    }

    private String entry(int index) {
        return "\u00a7" + Integer.toHexString(index);
    }

    private String apply(Player player, String input) {
        double balance = context.services().economy().map(economy -> economy.getBalance(player)).orElse(0.0D);
        double[] tps = Bukkit.getTPS();
        return input
                .replace("{player}", player.getName())
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{world}", player.getWorld().getName())
                .replace("{x}", String.valueOf(player.getLocation().getBlockX()))
                .replace("{y}", String.valueOf(player.getLocation().getBlockY()))
                .replace("{z}", String.valueOf(player.getLocation().getBlockZ()))
                .replace("{balance}", context.services().economy().map(economy -> economy.format(balance)).orElse(String.format("%.2f", balance)))
                .replace("{tps}", String.format("%.1f", tps.length == 0 ? 20.0D : tps[0]));
    }

    private void seedDefaults() {
        YamlConfiguration yaml = store.load();
        if (yaml.contains("tablist")) {
            return;
        }
        yaml.set("update-interval-ticks", 40);
        yaml.set("tablist.header-frames", List.of("<#44CCFF><bold>Hydroxide</bold>", "<#FFB000><bold>Hydroxide</bold>"));
        yaml.set("tablist.footer-frames", List.of("<gray>Online: <white>{online} <dark_gray>| <gray>TPS: <white>{tps}"));
        yaml.set("scoreboard.title", "<#44CCFF><bold>Hydroxide</bold>");
        yaml.set("scoreboard.lines", List.of(
                "<gray>Player <white>{player}",
                "<gray>Balance <white>{balance}",
                "<gray>World <white>{world}",
                "<gray>XYZ <white>{x} {y} {z}",
                "<gray>Online <white>{online}",
                "<gray>TPS <white>{tps}"
        ));
        store.save(yaml);
    }
}
