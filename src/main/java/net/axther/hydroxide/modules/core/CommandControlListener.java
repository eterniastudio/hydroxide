package net.axther.hydroxide.modules.core;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.storage.YamlStore;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class CommandControlListener implements Listener {

    private final HydroxideContext context;
    private final CommandCooldownTracker cooldownTracker = new CommandCooldownTracker();
    private final CommandCooldownStore cooldownStore;
    private final Map<UUID, PendingWarmup> warmups = new HashMap<>();
    private final Set<UUID> replayingWarmups = new HashSet<>();

    CommandControlListener(HydroxideContext context) {
        this.context = context;
        this.cooldownStore = new CommandCooldownStore(new YamlStore(new File(context.plugin().getDataFolder(), "data/command-cooldowns.yml")));
        loadCooldowns();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        applyCommandAlias(event);
        CommandControlPolicy policy = new CommandControlPolicy(context.plugin().getConfig().getStringList("command-control.disabled-commands"));
        if (!policy.blocked(event.getMessage(), event.getPlayer()::hasPermission)) {
            if (applyCommandPermission(event)) {
                return;
            }
            if (applyCommandWorldRestriction(event)) {
                return;
            }
            if (applyCommandWarmup(event)) {
                return;
            }
            if (applyCommandCooldown(event)) {
                return;
            }
            chargeCommandCost(event);
            return;
        }
        event.setCancelled(true);
        String command = event.getMessage().split("\\s+", 2)[0];
        String configuredMessage = context.plugin().getConfig().getString("command-control.disabled-message", "");
        if (configuredMessage == null || configuredMessage.isBlank()) {
            context.message(event.getPlayer(), "validation.command-disabled", Map.of("command", command));
            return;
        }
        context.send(event.getPlayer(), configuredMessage.replace("{command}", command));
    }

    private boolean applyCommandPermission(PlayerCommandPreprocessEvent event) {
        CommandPermissionPolicy policy = CommandPermissionPolicy.from(context.plugin().getConfig().getConfigurationSection("command-control.command-permissions"));
        Optional<CommandPermissionPolicy.MissingPermission> missing = policy.missing(event.getMessage(), event.getPlayer()::hasPermission);
        if (missing.isEmpty()) {
            return false;
        }
        event.setCancelled(true);
        CommandPermissionPolicy.MissingPermission missingPermission = missing.orElseThrow();
        context.message(event.getPlayer(), "validation.command-permission-required", Map.of(
                "command", missingPermission.key(),
                "permission", missingPermission.permissionList()
        ));
        return true;
    }

    private void applyCommandAlias(PlayerCommandPreprocessEvent event) {
        CommandAliasPolicy policy = CommandAliasPolicy.from(context.plugin().getConfig().getConfigurationSection("command-control.command-aliases"));
        policy.rewrite(event.getMessage(), event.getPlayer().getName()).ifPresent(event::setMessage);
    }

    private boolean applyCommandWarmup(PlayerCommandPreprocessEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (replayingWarmups.remove(playerId)) {
            return false;
        }
        CommandWarmupPolicy policy = CommandWarmupPolicy.from(context.plugin().getConfig().getConfigurationSection("command-control.command-warmups"));
        Optional<CommandWarmupPolicy.Warmup> warmup = policy.warmup(event.getMessage(), event.getPlayer()::hasPermission);
        if (warmup.isEmpty()) {
            return false;
        }
        event.setCancelled(true);
        startWarmup(event.getPlayer(), event.getMessage(), warmup.orElseThrow());
        return true;
    }

    private void startWarmup(Player player, String command, CommandWarmupPolicy.Warmup warmup) {
        cancelWarmup(player, null);
        UUID playerId = player.getUniqueId();
        Duration duration = warmup.duration();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(context.plugin(), () -> executeWarmup(player, playerId),
                Math.max(1L, (long) Math.ceil(duration.toMillis() / 50.0D)));
        warmups.put(playerId, new PendingWarmup(command, warmup.key(), player.getLocation(), task));
        context.message(player, "validation.command-warmup-started", Map.of(
                "command", warmup.key(),
                "seconds", formatSeconds(duration)
        ));
    }

    private void executeWarmup(Player player, UUID playerId) {
        PendingWarmup pending = warmups.remove(playerId);
        if (pending == null || !player.isOnline()) {
            return;
        }
        PlayerCommandPreprocessEvent replay = new PlayerCommandPreprocessEvent(player, pending.command());
        replayingWarmups.add(playerId);
        try {
            Bukkit.getPluginManager().callEvent(replay);
        } finally {
            replayingWarmups.remove(playerId);
        }
        if (replay.isCancelled()) {
            return;
        }
        String command = replay.getMessage().trim();
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (!command.isBlank()) {
            Bukkit.dispatchCommand(player, command);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        PendingWarmup warmup = warmups.get(event.getPlayer().getUniqueId());
        if (warmup == null || event.getTo() == null) {
            return;
        }
        Location to = event.getTo();
        Location origin = warmup.origin();
        if (origin.getWorld() == null || to.getWorld() == null || !origin.getWorld().equals(to.getWorld())
                || origin.distanceSquared(to) > 0.35D) {
            cancelWarmup(event.getPlayer(), "validation.command-warmup-cancelled-moved");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && warmups.containsKey(player.getUniqueId())) {
            cancelWarmup(player, "validation.command-warmup-cancelled-damaged");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (warmups.containsKey(event.getPlayer().getUniqueId())) {
            cancelWarmup(event.getPlayer(), "validation.command-warmup-cancelled-block-break");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelWarmup(event.getPlayer(), null);
        replayingWarmups.remove(event.getPlayer().getUniqueId());
    }

    void cancelAllWarmups() {
        warmups.values().forEach(warmup -> warmup.task().cancel());
        warmups.clear();
        replayingWarmups.clear();
    }

    private void cancelWarmup(Player player, String messageKey) {
        PendingWarmup warmup = warmups.remove(player.getUniqueId());
        if (warmup == null) {
            return;
        }
        warmup.task().cancel();
        if (messageKey != null) {
            context.message(player, messageKey, Map.of("command", warmup.key()));
        }
    }

    private boolean applyCommandWorldRestriction(PlayerCommandPreprocessEvent event) {
        CommandWorldPolicy policy = CommandWorldPolicy.from(context.plugin().getConfig().getConfigurationSection("command-control.command-worlds"));
        Optional<CommandWorldPolicy.Restriction> restriction = policy.restriction(
                event.getMessage(),
                event.getPlayer().getWorld().getName(),
                event.getPlayer()::hasPermission
        );
        if (restriction.isEmpty()) {
            return false;
        }
        event.setCancelled(true);
        CommandWorldPolicy.Restriction commandRestriction = restriction.orElseThrow();
        context.message(event.getPlayer(), "validation.command-world-blocked", Map.of(
                "command", commandRestriction.key(),
                "world", commandRestriction.world()
        ));
        return true;
    }

    private boolean applyCommandCooldown(PlayerCommandPreprocessEvent event) {
        CommandCooldownPolicy policy = CommandCooldownPolicy.from(context.plugin().getConfig().getConfigurationSection("command-control.command-cooldowns"));
        Optional<CommandCooldownPolicy.Cooldown> cooldown = policy.cooldown(event.getMessage(), event.getPlayer()::hasPermission);
        if (cooldown.isEmpty()) {
            return false;
        }
        Optional<CommandCooldownTracker.ActiveCooldown> active = cooldownTracker.checkAndMark(
                event.getPlayer().getUniqueId(),
                cooldown.orElseThrow(),
                System.currentTimeMillis()
        );
        if (active.isEmpty()) {
            saveCooldowns();
            return false;
        }
        CommandCooldownTracker.ActiveCooldown activeCooldown = active.orElseThrow();
        event.setCancelled(true);
        if (activeCooldown.oneUse()) {
            context.message(event.getPlayer(), "validation.command-one-use-cooldown", Map.of("command", activeCooldown.key()));
        } else {
            context.message(event.getPlayer(), "validation.command-cooldown", Map.of(
                    "command", activeCooldown.key(),
                    "remaining", formatRemaining(activeCooldown.remaining())
            ));
        }
        return true;
    }

    void saveCooldowns() {
        if (cooldownPersistenceEnabled()) {
            cooldownStore.save(cooldownTracker.snapshot(System.currentTimeMillis()));
        }
    }

    int activeCooldownCount() {
        return cooldownTracker.activeCount(System.currentTimeMillis());
    }

    int clearCooldowns(UUID playerId, String commandKey) {
        int cleared = cooldownTracker.clear(playerId, commandKey);
        if (cleared > 0) {
            saveCooldowns();
        }
        return cleared;
    }

    int clearAllCooldowns(String commandKey) {
        int cleared = cooldownTracker.clearAll(commandKey);
        if (cleared > 0) {
            saveCooldowns();
        }
        return cleared;
    }

    private void loadCooldowns() {
        if (cooldownPersistenceEnabled()) {
            cooldownTracker.restore(cooldownStore.load(System.currentTimeMillis()), System.currentTimeMillis());
        }
    }

    private boolean cooldownPersistenceEnabled() {
        return context.plugin().getConfig().getBoolean("command-control.command-cooldown-persistence", true);
    }

    private void chargeCommandCost(PlayerCommandPreprocessEvent event) {
        CommandCostPolicy policy = CommandCostPolicy.from(context.plugin().getConfig().getConfigurationSection("command-control.command-costs"));
        Optional<CommandCostPolicy.Cost> cost = policy.cost(event.getMessage(), event.getPlayer()::hasPermission);
        if (cost.isEmpty()) {
            return;
        }

        CommandCostPolicy.Cost commandCost = cost.orElseThrow();
        var economy = context.services().economy().orElse(null);
        if (economy == null) {
            event.setCancelled(true);
            context.message(event.getPlayer(), "economy.command-cost.unavailable", Map.of(
                    "command", commandCost.key(),
                    "amount", String.format(java.util.Locale.ROOT, "%.2f", commandCost.amount())
            ));
            return;
        }

        EconomyResponse withdrawal = economy.withdrawPlayer(event.getPlayer(), commandCost.amount());
        if (!withdrawal.transactionSuccess()) {
            event.setCancelled(true);
            context.message(event.getPlayer(), "economy.command-cost.insufficient", Map.of(
                    "command", commandCost.key(),
                    "amount", economy.format(commandCost.amount()),
                    "balance", economy.format(withdrawal.balance)
            ));
            return;
        }
        context.message(event.getPlayer(), "economy.command-cost.charged", Map.of(
                "command", commandCost.key(),
                "amount", economy.format(commandCost.amount()),
                "balance", economy.format(withdrawal.balance)
        ));
    }

    private String formatRemaining(java.time.Duration remaining) {
        return formatSeconds(remaining) + "s";
    }

    private String formatSeconds(java.time.Duration remaining) {
        return String.valueOf(Math.max(1L, (long) Math.ceil(remaining.toMillis() / 1000.0D)));
    }

    private record PendingWarmup(String command, String key, Location origin, BukkitTask task) {
    }
}
