package net.axther.hydroxide.modules.afk;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AfkModule implements HydroModule, Listener {

    private HydroxideContext context;
    private ActivityTracker tracker;
    private BukkitTask task;
    private final Set<UUID> afk = new HashSet<>();
    private final Set<UUID> manualAfk = new HashSet<>();

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
        for (String command : AfkCommandCatalog.commands()) {
            context.commands().register(command, command(command));
        }
        task = Bukkit.getScheduler().runTaskTimer(context.plugin(), this::tick, 20L, 20L);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        if (task != null) {
            task.cancel();
        }
        manualAfk.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        activity(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tracker.clear(event.getPlayer().getUniqueId());
        afk.remove(event.getPlayer().getUniqueId());
        manualAfk.remove(event.getPlayer().getUniqueId());
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
            if (manualAfk.contains(player.getUniqueId()) || tracker.afk(player.getUniqueId(), now)) {
                markAfk(player);
            } else {
                clearAfk(player);
            }
        }
    }

    private void activity(Player player) {
        tracker.recordActivity(player.getUniqueId(), System.currentTimeMillis());
        manualAfk.remove(player.getUniqueId());
        clearAfk(player);
    }

    private CommandService command(String name) {
        return switch (name) {
            case "afk" -> new CommandService(HydroCommand.builder("afk")
                    .permission("hydroxide.command.afk")
                    .usage("/{label} [player] [on|off]")
                    .executor(ctx -> afkCommand(ctx.sender(), ctx.label(), ctx.arguments()))
                    .completer(ctx -> {
                        if (ctx.arguments().size() == 1) {
                            java.util.ArrayList<String> values = new java.util.ArrayList<>(List.of("off", "on"));
                            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                            return CommandUtils.matching(ctx.argument(0), values);
                        }
                        if (ctx.arguments().size() == 2) {
                            return CommandUtils.matching(ctx.argument(1), List.of("off", "on"));
                        }
                        return List.of();
                    })
                    .build(), context.messages());
            case "afkcheck" -> new CommandService(HydroCommand.builder("afkcheck")
                    .permission("hydroxide.command.afkcheck")
                    .usage("/{label} [player|all]")
                    .executor(ctx -> afkCheckCommand(ctx.sender(), ctx.label(), ctx.arguments()))
                    .completer(ctx -> {
                        if (ctx.arguments().size() == 1) {
                            java.util.ArrayList<String> values = new java.util.ArrayList<>(List.of("all"));
                            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                            return CommandUtils.matching(ctx.argument(0), values);
                        }
                        return List.of();
                    })
                    .build(), context.messages());
            default -> throw new IllegalArgumentException("Unknown AFK command: " + name);
        };
    }

    private void afkCommand(CommandSender sender, String label, List<String> args) {
        TargetState targetState = targetState(sender, args);
        if (targetState == null) {
            context.message(sender, "afk.usage", Map.of("label", label));
            return;
        }
        if (!sender.equals(targetState.player()) && !sender.hasPermission("hydroxide.command.afk.others")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.afk.others"));
            return;
        }
        if (targetState.enabled()) {
            manualAfk.add(targetState.player().getUniqueId());
            markAfk(targetState.player());
        } else {
            manualAfk.remove(targetState.player().getUniqueId());
            tracker.recordActivity(targetState.player().getUniqueId(), System.currentTimeMillis());
            clearAfk(targetState.player());
        }
        if (sender.equals(targetState.player())) {
            context.message(sender, targetState.enabled() ? "afk.enabled" : "afk.disabled", Map.of());
        } else {
            context.message(sender, "afk.updated", Map.of(
                    "target", targetState.player().getName(),
                    "state", targetState.enabled() ? "enabled" : "disabled"
            ));
            context.message(targetState.player(), targetState.enabled() ? "afk.enabled" : "afk.disabled", Map.of());
        }
    }

    private void afkCheckCommand(CommandSender sender, String label, List<String> args) {
        if (args.size() > 1) {
            context.message(sender, "afk.check.usage", Map.of("label", label));
            return;
        }
        if (!args.isEmpty() && args.getFirst().equalsIgnoreCase("all")) {
            List<Player> afkPlayers = Bukkit.getOnlinePlayers().stream()
                    .map(Player.class::cast)
                    .filter(player -> afk.contains(player.getUniqueId()))
                    .sorted(java.util.Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            if (afkPlayers.isEmpty()) {
                context.message(sender, "afk.check.empty", Map.of());
                return;
            }
            context.message(sender, "afk.check.header", Map.of("count", afkPlayers.size()));
            afkPlayers.forEach(player -> context.message(sender, "afk.check.entry", statePlaceholders(player)));
            return;
        }

        Player target;
        if (args.isEmpty()) {
            if (!(sender instanceof Player player)) {
                context.message(sender, "afk.check.usage", Map.of("label", label));
                return;
            }
            target = player;
        } else {
            target = CommandUtils.onlinePlayer(args.getFirst()).orElse(null);
            if (target == null) {
                context.message(sender, "afk.player-offline", Map.of("target", args.getFirst()));
                return;
            }
        }
        context.message(sender, "afk.check.result", statePlaceholders(target));
    }

    private TargetState targetState(CommandSender sender, List<String> args) {
        if (args.isEmpty()) {
            if (sender instanceof Player player) {
                return new TargetState(player, !afk.contains(player.getUniqueId()));
            }
            context.message(sender, "afk.console-target-required", Map.of());
            return null;
        }
        if (args.size() == 1 && (args.getFirst().equalsIgnoreCase("on") || args.getFirst().equalsIgnoreCase("off"))) {
            if (sender instanceof Player player) {
                return new TargetState(player, args.getFirst().equalsIgnoreCase("on"));
            }
            context.message(sender, "afk.console-target-required", Map.of());
            return null;
        }
        Player target = CommandUtils.onlinePlayer(args.getFirst()).orElse(null);
        if (target == null) {
            context.message(sender, "afk.player-offline", Map.of("target", args.getFirst()));
            return null;
        }
        if (args.size() >= 2 && !args.get(1).equalsIgnoreCase("on") && !args.get(1).equalsIgnoreCase("off")) {
            return null;
        }
        boolean enabled = args.size() >= 2 ? args.get(1).equalsIgnoreCase("on") : !afk.contains(target.getUniqueId());
        return new TargetState(target, enabled);
    }

    private void markAfk(Player player) {
        if (!afk.add(player.getUniqueId())) {
            return;
        }
        player.playerListName(context.messages().component("afk.tab-name", Map.of("player", player.getName())));
        if (context.plugin().getConfig().getBoolean("afk.auto-kick-when-full", false)
                && Bukkit.getOnlinePlayers().size() >= Bukkit.getMaxPlayers()) {
            player.kick(context.messages().component("afk.kick-full", Map.of("player", player.getName())));
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

    private Map<String, ?> statePlaceholders(Player player) {
        boolean active = afk.contains(player.getUniqueId());
        return Map.of(
                "target", player.getName(),
                "state", active ? "AFK" : "active",
                "state_color", active ? "<yellow>" : "<green>"
        );
    }

    private boolean changedViewOrPosition(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()
                || from.getYaw() != to.getYaw()
                || from.getPitch() != to.getPitch();
    }

    private record TargetState(Player player, boolean enabled) {
    }
}
