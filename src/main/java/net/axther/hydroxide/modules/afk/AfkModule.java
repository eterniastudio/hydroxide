package net.axther.hydroxide.modules.afk;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AfkModule implements HydroModule, Listener {

    private HydroxideContext context;
    private ActivityTracker tracker;
    private BukkitTask task;
    private final Set<UUID> afk = new HashSet<>();

    @Override
    public String id() {
        return "afk";
    }

    @Override
    public String displayName() {
        return "AFK Tracker";
    }

    @Override
    public String description() {
        return "Activity monitor with camera-aware AFK detection and tablist visual cues.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        tracker = new ActivityTracker(context.plugin().getConfig().getLong("afk.idle-seconds", 300L) * 1000L);
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        task = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 20L, 20L);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        activity(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tracker.clear(event.getPlayer().getUniqueId());
        afk.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() != null && changedViewOrPosition(event.getFrom(), event.getTo())) {
            activity(event.getPlayer());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        activity(event.getPlayer());
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        activity(event.getPlayer());
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (tracker.afk(player.getUniqueId(), now)) {
                markAfk(player);
            } else {
                clearAfk(player);
            }
        }
    }

    private void activity(Player player) {
        tracker.recordActivity(player.getUniqueId(), System.currentTimeMillis());
        clearAfk(player);
    }

    private void markAfk(Player player) {
        if (!afk.add(player.getUniqueId())) {
            return;
        }
        player.playerListName(context.text().format("<gray>" + player.getName() + " <dark_gray>[AFK]"));
        if (context.plugin().getConfig().getBoolean("afk.auto-kick-when-full", false)
                && Bukkit.getOnlinePlayers().size() >= Bukkit.getMaxPlayers()) {
            player.kick(context.text().format("<gray>Kicked for being AFK while the server is full."));
        }
    }

    private void clearAfk(Player player) {
        if (!afk.remove(player.getUniqueId())) {
            return;
        }
        context.services().nicknameService()
                .flatMap(service -> service.formattedNickname(player.getUniqueId()))
                .ifPresentOrElse(player::playerListName, () -> player.playerListName(net.kyori.adventure.text.Component.text(player.getName())));
    }

    private boolean changedViewOrPosition(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()
                || from.getYaw() != to.getYaw()
                || from.getPitch() != to.getPitch();
    }
}
