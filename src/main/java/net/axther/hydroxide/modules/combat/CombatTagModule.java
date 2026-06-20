package net.axther.hydroxide.modules.combat;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CombatTagModule implements HydroModule, Listener {

    private HydroxideContext context;
    private CombatTagTracker tracker;
    private BukkitTask task;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    @Override
    public String id() {
        return "combat-tag";
    }

    @Override
    public String displayName() {
        return "Combat Tag";
    }

    @Override
    public String description() {
        return "PvP combat tagging, bossbar countdowns, command blocking, and logout penalties.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.tracker = new CombatTagTracker(context.plugin().getConfig().getLong("combat-tag.duration-seconds", 15L) * 1000L);
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        task = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 20L, 20L);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        if (task != null) {
            task.cancel();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            BossBar bar = bossBars.remove(player.getUniqueId());
            if (bar != null) {
                player.hideBossBar(bar);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player victim = event.getEntity() instanceof Player player ? player : null;
        Player attacker = attacker(event.getDamager());
        if (victim == null || attacker == null || victim.equals(attacker)) {
            return;
        }
        tracker.tag(attacker.getUniqueId(), victim.getUniqueId(), System.currentTimeMillis());
        show(attacker);
        show(victim);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!tracker.tagged(event.getPlayer().getUniqueId(), System.currentTimeMillis())) {
            return;
        }
        String root = event.getMessage().split(" ")[0].toLowerCase(Locale.ROOT).replace("/", "");
        if (context.plugin().getConfig().getStringList("combat-tag.blocked-commands").contains(root)) {
            event.setCancelled(true);
            context.message(event.getPlayer(), "combat.command-blocked", Map.of("command", root));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!tracker.tagged(player.getUniqueId(), System.currentTimeMillis())) {
            return;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
        player.getInventory().clear();
        Bukkit.broadcast(context.messages().component("combat.logout-broadcast", Map.of("player", player.getName())));
        tracker.clear(player.getUniqueId());
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            tracker.remainingMillis(player.getUniqueId(), now).ifPresentOrElse(remaining -> {
                BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(), ignored -> show(player));
                long duration = context.plugin().getConfig().getLong("combat-tag.duration-seconds", 15L) * 1000L;
                bar.progress(Math.max(0.0f, Math.min(1.0f, (float) remaining / duration)));
                bar.name(context.messages().component("combat.bossbar.countdown", Map.of("seconds", (long) Math.ceil(remaining / 1000.0D))));
            }, () -> hide(player));
        }
    }

    private BossBar show(Player player) {
        BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(), ignored -> BossBar.bossBar(
                context.messages().component("combat.bossbar.title", Map.of()),
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.PROGRESS
        ));
        player.showBossBar(bar);
        return bar;
    }

    private void hide(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private Player attacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            return source instanceof Player player ? player : null;
        }
        return null;
    }
}
