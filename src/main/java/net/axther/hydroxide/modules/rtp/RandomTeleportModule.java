package net.axther.hydroxide.modules.rtp;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomTeleportModule implements HydroModule, Listener {

    private HydroxideContext context;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> fallImmuneUntil = new HashMap<>();

    @Override
    public String id() {
        return "rtp";
    }

    @Override
    public String displayName() {
        return "Random Teleport";
    }

    @Override
    public String description() {
        return "Paper async chunk based random teleport with cooldowns, costs, and fall immunity.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core", "economy");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("rtp", rtpCommand());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    private CommandService rtpCommand() {
        return new CommandService(HydroCommand.builder("rtp")
                .permission("hydroxide.command.rtp")
                .usage("/{label}")
                .playerOnly(true)
                .executor(ctx -> start((Player) ctx.sender()))
                .build(), context.messages());
    }

    private void start(Player player) {
        long now = System.currentTimeMillis();
        long cooldownMillis = context.plugin().getConfig().getLong("rtp.cooldown-seconds", 60L) * 1000L;
        Long readyAt = cooldowns.get(player.getUniqueId());
        if (readyAt != null && readyAt > now) {
            context.message(player, "rtp.cooldown", Map.of("remaining", Math.ceil((readyAt - now) / 1000.0D) + "s"));
            return;
        }
        cooldowns.put(player.getUniqueId(), now + cooldownMillis);
        context.message(player, "rtp.searching", Map.of());
        find(player, 0);
    }

    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (fallImmuneUntil.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis()) {
            event.setCancelled(true);
        }
    }

    private void find(Player player, int attempts) {
        if (attempts > context.plugin().getConfig().getInt("rtp.max-attempts", 16)) {
            context.message(player, "rtp.no-location", Map.of());
            cooldowns.remove(player.getUniqueId());
            return;
        }
        World world = player.getWorld();
        int min = context.plugin().getConfig().getInt("rtp.min-radius", 250);
        int max = context.plugin().getConfig().getInt("rtp.max-radius", 5000);
        int x = randomSigned(min, max);
        int z = randomSigned(min, max);
        world.getChunkAtAsync(x >> 4, z >> 4, true).thenAccept(chunk ->
                Bukkit.getScheduler().runTask(context.plugin(), () -> {
                    Location safe = safeLocation(world, x, z);
                    if (safe == null) {
                        find(player, attempts + 1);
                        return;
                    }
                    teleport(player, safe);
                }));
    }

    private void teleport(Player player, Location location) {
        double cost = context.plugin().getConfig().getDouble("rtp.cost", 0.0D);
        if (cost > 0.0D) {
            EconomyResponse response = context.services().economy()
                    .map(economy -> economy.withdrawPlayer(player, cost))
                    .orElse(null);
            if (response == null || !response.transactionSuccess()) {
                context.message(player, "rtp.cannot-afford", Map.of());
                cooldowns.remove(player.getUniqueId());
                return;
            }
        }
        player.teleportAsync(location).thenAccept(success -> Bukkit.getScheduler().runTask(context.plugin(), () -> {
            if (!success) {
                context.message(player, "rtp.failed", Map.of());
                return;
            }
            fallImmuneUntil.put(player.getUniqueId(), System.currentTimeMillis()
                    + context.plugin().getConfig().getLong("rtp.fall-immunity-seconds", 5L) * 1000L);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 80, 0.75D, 1.0D, 0.75D, 0.1D);
            context.message(player, "rtp.success", Map.of());
        }));
    }

    private Location safeLocation(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        Location ground = new Location(world, x + 0.5D, y - 1, z + 0.5D);
        Material groundType = ground.getBlock().getType();
        Material feet = ground.clone().add(0.0D, 1.0D, 0.0D).getBlock().getType();
        Material head = ground.clone().add(0.0D, 2.0D, 0.0D).getBlock().getType();
        if (!groundType.isSolid() || groundType == Material.LAVA || groundType == Material.WATER
                || !feet.isAir() || !head.isAir()) {
            return null;
        }
        return ground.add(0.0D, 1.0D, 0.0D);
    }

    private int randomSigned(int min, int max) {
        int value = ThreadLocalRandom.current().nextInt(Math.max(0, min), Math.max(min + 1, max));
        return ThreadLocalRandom.current().nextBoolean() ? value : -value;
    }
}
