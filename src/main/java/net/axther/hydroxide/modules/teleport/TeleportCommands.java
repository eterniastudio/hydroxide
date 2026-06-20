package net.axther.hydroxide.modules.teleport;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.navigation.HomeLimitService;
import net.axther.hydroxide.storage.StoredLocation;
import net.axther.hydroxide.teleport.TeleportRequestService.TeleportRequest;
import net.axther.hydroxide.teleport.TeleportRequestService.TeleportRequestDirection;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class TeleportCommands implements Listener {

    private static final String DEFAULT_HOME = "home";
    private final HydroxideContext context;
    private final TeleportPreferenceStore preferences;
    private final DeathLocationStore deathLocations;
    private final PatrolTargetSelector patrolTargets = new PatrolTargetSelector();

    public TeleportCommands(HydroxideContext context) {
        this.context = context;
        this.preferences = new TeleportPreferenceStore(new net.axther.hydroxide.storage.YamlStore(
                new File(context.plugin().getDataFolder(), "teleport-preferences.yml")
        ));
        this.deathLocations = new DeathLocationStore(new net.axther.hydroxide.storage.YamlStore(
                new File(context.plugin().getDataFolder(), "death-locations.yml")
        ));
    }

    public CommandService command(String name) {
        return switch (name) {
            case "spawn" -> service("spawn", "hydroxide.command.spawn", "/{label}", true, ctx -> spawn(ctx.sender()), null);
            case "setspawn" -> service("setspawn", "hydroxide.command.setspawn", "/{label}", true, ctx -> setSpawn(ctx.sender()), null);
            case "home" -> service("home", "hydroxide.command.home", "/{label} [name]", true,
                    ctx -> home(ctx.sender(), ctx.arguments().toArray(String[]::new)), this::homeCompletions);
            case "sethome" -> service("sethome", "hydroxide.command.sethome", "/{label} [name]", true,
                    ctx -> setHome(ctx.sender(), ctx.arguments().toArray(String[]::new)), this::homeCompletions);
            case "delhome" -> service("delhome", "hydroxide.command.delhome", "/{label} [name]", true,
                    ctx -> delHome(ctx.sender(), ctx.arguments().toArray(String[]::new)), this::homeCompletions);
            case "homes" -> service("homes", "hydroxide.command.homes", "/{label}", true, ctx -> homes(ctx.sender()), null);
            case "warp" -> service("warp", "hydroxide.command.warp", "/{label} <name>", true,
                    ctx -> warp(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::warpCompletions);
            case "setwarp" -> service("setwarp", "hydroxide.command.setwarp", "/{label} <name>", true,
                    ctx -> setWarp(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::warpCompletions);
            case "delwarp" -> service("delwarp", "hydroxide.command.delwarp", "/{label} <name>", false,
                    ctx -> delWarp(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::warpCompletions);
            case "warps" -> service("warps", "hydroxide.command.warps", "/{label}", false, ctx -> warps(ctx.sender()), null);
            case "back" -> service("back", "hydroxide.command.back", "/{label}", true, ctx -> back(ctx.sender()), null);
            case "tpa" -> service("tpa", "hydroxide.command.tpa", "/{label} <player>", true,
                    ctx -> tpa(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> playerCompletions(ctx.argument(0)));
            case "tpahere" -> service("tpahere", "hydroxide.command.tpahere", "/{label} <player>", true,
                    ctx -> tpaHere(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> playerCompletions(ctx.argument(0)));
            case "tpaall" -> service("tpaall", "hydroxide.command.tpaall", "/{label}", true,
                    ctx -> tpaAll(ctx.sender(), ctx.label(), ctx.arguments()), null);
            case "tpacancel" -> service("tpacancel", "hydroxide.command.tpacancel", "/{label}", true,
                    ctx -> tpaCancel(ctx.sender()), null);
            case "tptoggle" -> service("tptoggle", "hydroxide.command.tptoggle", "/{label} [on|off]", true,
                    ctx -> tpToggle(ctx.sender(), ctx.label(), ctx.arguments()), this::stateCompletions);
            case "tpauto" -> service("tpauto", "hydroxide.command.tpauto", "/{label} [on|off]", true,
                    ctx -> tpAuto(ctx.sender(), ctx.label(), ctx.arguments()), this::stateCompletions);
            case "tpaccept" -> service("tpaccept", "hydroxide.command.tpaccept", "/{label}", true, ctx -> tpAccept(ctx.sender()), null);
            case "tpdeny" -> service("tpdeny", "hydroxide.command.tpdeny", "/{label}", true, ctx -> tpDeny(ctx.sender()), null);
            case "tp" -> service("tp", "hydroxide.command.tp", "/{label} <player> [target]", false,
                    ctx -> tp(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::twoPlayerCompletions);
            case "tphere" -> service("tphere", "hydroxide.command.tphere", "/{label} <player>", true,
                    ctx -> tpHere(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> playerCompletions(ctx.argument(0)));
            case "tpo" -> service("tpo", "hydroxide.command.tpo", "/{label} <player> [target]", false,
                    ctx -> tpDirect(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new), "hydroxide.command.tpo.others"),
                    this::twoPlayerCompletions);
            case "tpohere" -> service("tpohere", "hydroxide.command.tpohere", "/{label} <player>", true,
                    ctx -> tpHere(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> playerCompletions(ctx.argument(0)));
            case "tpall" -> service("tpall", "hydroxide.command.tpall", "/{label} [target]", false,
                    ctx -> tpAll(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)),
                    ctx -> playerCompletions(ctx.argument(0)));
            case "tpallworld" -> service("tpallworld", "hydroxide.command.tpallworld", "/{label} <world> [world;x;y;z[;yaw;pitch]] [-a]", false,
                    ctx -> tpAllWorld(ctx.sender(), ctx.label(), ctx.arguments()), this::tpAllWorldCompletions);
            case "tppos" -> service("tppos", "hydroxide.command.tppos", "/{label} <x> <y> <z> [world]", true,
                    ctx -> tpPos(ctx.sender(), ctx.label(), ctx.arguments(), "hydroxide.command.tppos.others"), null);
            case "tpopos" -> service("tpopos", "hydroxide.command.tpopos", "/{label} [-p:player] <x> <y> <z> [world] [pitch] [yaw]", false,
                    ctx -> tpPos(ctx.sender(), ctx.label(), ctx.arguments(), "hydroxide.command.tpopos.others"), null);
            case "dback" -> service("dback", "hydroxide.command.dback", "/{label} [player] [-s]", false,
                    ctx -> deathBack(ctx.sender(), ctx.label(), ctx.arguments()), this::deathBackCompletions);
            case "resetback" -> service("resetback", "hydroxide.command.resetback", "/{label} [player] [-s]", false,
                    ctx -> resetBack(ctx.sender(), ctx.label(), ctx.arguments()), this::resetBackCompletions);
            case "jump" -> service("jump", "hydroxide.command.jump", "/{label}", true, ctx -> jump(ctx.sender()), null);
            case "down" -> service("down", "hydroxide.command.down", "/{label} [player] [max] [-s]", false,
                    ctx -> down(ctx.sender(), ctx.label(), ctx.arguments()), this::downCompletions);
            case "world" -> service("world", "hydroxide.command.world", "/{label} <normal|nether|end|world> [player] [-s]", false,
                    ctx -> world(ctx.sender(), ctx.label(), ctx.arguments()), this::worldCompletions);
            case "patrol" -> service("patrol", "hydroxide.command.patrol", "/{label} [reset]", true,
                    ctx -> patrol(ctx.sender(), ctx.label(), ctx.arguments()), this::patrolCompletions);
            default -> throw new IllegalArgumentException("Unknown teleport command: " + name);
        };
    }

    private CommandService service(String name, String permission, String usage, boolean playerOnly,
                                   HydroCommand.HydroCommandExecutor executor,
                                   HydroCommand.HydroTabCompleter completer) {
        return new CommandService(HydroCommand.builder(name)
                .permission(permission)
                .usage(usage)
                .playerOnly(playerOnly)
                .executor(executor)
                .completer(completer)
                .build(), context.messages());
    }

    private List<String> homeCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() != 1 || !(ctx.sender() instanceof Player player)) {
            return List.of();
        }
        return CommandUtils.matching(ctx.argument(0), context.playerData().homes(player.getUniqueId()));
    }

    private List<String> warpCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        return ctx.arguments().size() == 1
                ? CommandUtils.matching(ctx.argument(0), context.warps().names())
                : List.of();
    }

    private List<String> playerCompletions(String prefix) {
        return CommandUtils.matching(prefix, Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
    }

    private List<String> twoPlayerCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        return ctx.arguments().size() <= 2 ? playerCompletions(ctx.arguments().isEmpty() ? "" : ctx.arguments().getLast()) : List.of();
    }

    private List<String> stateCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        return ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), List.of("off", "on")) : List.of();
    }

    private List<String> downCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(List.of("max", "-s"));
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(0), values);
        }
        if (ctx.arguments().size() <= 3) {
            return CommandUtils.matching(ctx.argument(ctx.arguments().size() - 1), List.of("max", "-s"));
        }
        return List.of();
    }

    private List<String> worldCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(List.of("normal", "nether", "end", "-s"));
            values.addAll(Bukkit.getWorlds().stream().map(World::getName).toList());
            return CommandUtils.matching(ctx.argument(0), values);
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new java.util.ArrayList<>(List.of("-s"));
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(1), values);
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(ctx.argument(2), List.of("-s"));
        }
        return List.of();
    }

    private List<String> patrolCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        return ctx.arguments().size() == 1
                ? CommandUtils.matching(ctx.argument(0), List.of("reset"))
                : List.of();
    }

    private List<String> tpAllWorldCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), Bukkit.getWorlds().stream().map(World::getName).toList());
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(ctx.argument(2), List.of("-a"));
        }
        return List.of();
    }

    private List<String> deathBackCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(playerCompletions(ctx.argument(0)));
            values.addAll(CommandUtils.matching(ctx.argument(0), List.of("-s")));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("-s"));
        }
        return List.of();
    }

    private List<String> resetBackCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(playerCompletions(ctx.argument(0)));
            values.addAll(CommandUtils.matching(ctx.argument(0), List.of("-s")));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 2 && ctx.arguments().stream().noneMatch("-s"::equalsIgnoreCase)) {
            return CommandUtils.matching(ctx.argument(1), List.of("-s"));
        }
        return List.of();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!sameBlock(event.getFrom(), event.getTo())) {
            context.backLocations().remember(event.getPlayer().getUniqueId(), event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        deathLocations.remember(player.getUniqueId(), player.getName(), StoredLocation.from(player.getLocation()));
    }

    private boolean spawn(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        teleport(player, context.spawns().get("spawn"), "teleport.spawn.success", Map.of());
        return true;
    }

    private boolean setSpawn(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        context.spawns().set("spawn", StoredLocation.from(player.getLocation()));
        context.message(sender, "teleport.spawn.set", Map.of());
        return true;
    }

    private boolean home(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        String name = args.length == 0 ? DEFAULT_HOME : args[0];
        teleport(player, context.playerData().home(player.getUniqueId(), name), "teleport.home.success", Map.of("home", name));
        return true;
    }

    private boolean setHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        String name = args.length == 0 ? DEFAULT_HOME : args[0];
        int limit = HomeLimitService.highestLimit(player::hasPermission,
                context.plugin().getConfig().getInt("navigation.default-home-limit", 3),
                context.plugin().getConfig().getInt("navigation.max-home-limit", 25));
        if (!context.playerData().homes(player.getUniqueId()).contains(name.toLowerCase(Locale.ROOT))
                && context.playerData().homes(player.getUniqueId()).size() >= limit) {
            context.message(sender, "teleport.home.limit-reached", Map.of("limit", limit));
            return true;
        }
        context.playerData().setHome(player.getUniqueId(), name, player.getLocation());
        context.message(sender, "teleport.home.set", Map.of("home", name));
        return true;
    }

    private boolean delHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        String name = args.length == 0 ? DEFAULT_HOME : args[0];
        boolean removed = context.playerData().removeHome(player.getUniqueId(), name);
        context.message(sender, removed ? "teleport.home.removed" : "teleport.home.missing", Map.of("home", name));
        return true;
    }

    private boolean homes(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        List<String> homes = context.playerData().homes(player.getUniqueId());
        context.message(sender, homes.isEmpty() ? "teleport.home.empty" : "teleport.home.list",
                Map.of("homes", String.join("<gray>, <white>", homes)));
        return true;
    }

    private boolean warp(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length == 0) {
            context.message(sender, "teleport.warp.usage", Map.of("label", label));
            return true;
        }
        teleport(player, context.warps().get(args[0]), "teleport.warp.success", Map.of("warp", args[0]));
        return true;
    }

    private boolean setWarp(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length == 0) {
            context.message(sender, "teleport.warp.set-usage", Map.of("label", label));
            return true;
        }
        context.warps().set(args[0], StoredLocation.from(player.getLocation()));
        context.message(sender, "teleport.warp.set", Map.of("warp", args[0]));
        return true;
    }

    private boolean delWarp(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "teleport.warp.delete-usage", Map.of("label", label));
            return true;
        }
        boolean removed = context.warps().remove(args[0]);
        context.message(sender, removed ? "teleport.warp.removed" : "teleport.warp.missing", Map.of("warp", args[0]));
        return true;
    }

    private boolean warps(CommandSender sender) {
        List<String> warps = context.warps().names();
        context.message(sender, warps.isEmpty() ? "teleport.warp.empty" : "teleport.warp.list",
                Map.of("warps", String.join("<gray>, <white>", warps)));
        return true;
    }

    private boolean back(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        teleport(player, context.backLocations().previous(player.getUniqueId()), "teleport.back.success", Map.of());
        return true;
    }

    private boolean deathBack(CommandSender sender, String label, List<String> args) {
        Optional<DeathBackCommandParser.Request> parsed = DeathBackCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "teleport.deathback.usage", Map.of("label", label));
            return true;
        }
        DeathBackCommandParser.Request request = parsed.orElseThrow();
        Player target;
        if (request.targetName().isPresent()) {
            if (!(sender instanceof Player self && self.getName().equalsIgnoreCase(request.targetName().orElseThrow()))
                    && !sender.hasPermission("hydroxide.command.dback.others")) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.dback.others"));
                return true;
            }
            target = online(request.targetName().orElseThrow(), sender);
            if (target == null) {
                return true;
            }
        } else {
            target = requirePlayer(sender);
            if (target == null) {
                context.message(sender, "teleport.deathback.usage", Map.of("label", label));
                return true;
            }
        }

        Optional<StoredLocation> location = deathLocations.lastDeath(target.getUniqueId());
        if (location.isEmpty()) {
            if (!request.silent()) {
                context.message(sender, "teleport.deathback.missing", Map.of("target", target.getName()));
            }
            return true;
        }
        teleportDeathBack(sender, target, location.orElseThrow(), request.silent());
        return true;
    }

    private boolean resetBack(CommandSender sender, String label, List<String> args) {
        java.util.ArrayList<String> targets = new java.util.ArrayList<>();
        boolean silent = false;
        for (String arg : args) {
            if ("-s".equalsIgnoreCase(arg)) {
                silent = true;
            } else if (arg.startsWith("-")) {
                context.message(sender, "teleport.back.reset-usage", Map.of("label", label));
                return true;
            } else {
                targets.add(arg);
            }
        }
        if (targets.size() > 1) {
            context.message(sender, "teleport.back.reset-usage", Map.of("label", label));
            return true;
        }

        Player target;
        if (targets.isEmpty()) {
            if (!(sender instanceof Player player)) {
                context.message(sender, "teleport.back.reset-usage", Map.of("label", label));
                return true;
            }
            target = player;
        } else {
            target = online(targets.getFirst(), sender);
            if (target == null) {
                return true;
            }
        }

        boolean removed = context.backLocations().forget(target.getUniqueId());
        context.message(sender, removed ? "teleport.back.reset-success" : "teleport.back.reset-missing",
                Map.of("target", target.getName()));
        if (removed && !silent && !sender.equals(target)) {
            context.message(target, "teleport.back.reset-target", Map.of("player", sender.getName()));
        }
        return true;
    }

    private boolean tpa(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length == 0) {
            context.message(sender, "teleport.tpa.usage", Map.of("label", label));
            return true;
        }
        Player target = CommandUtils.onlinePlayer(args[0]).orElse(null);
        if (target == null || target.equals(player)) {
            context.message(sender, "teleport.tpa.target-unavailable", Map.of("target", args[0]));
            return true;
        }
        if (!canRequest(player, target)) {
            context.message(sender, "teleport.tpa.target-disabled", Map.of("target", target.getName()));
            return true;
        }
        if (preferences.autoAcceptEnabled(target.getUniqueId())) {
            directTeleport(player, target.getLocation(), "teleport.tpa.auto-accepted-requester", Map.of("target", target.getName()));
            context.message(target, "teleport.tpa.auto-accepted-target", Map.of("player", player.getName()));
            return true;
        }
        Duration timeout = Duration.ofSeconds(context.plugin().getConfig().getLong("teleport.request-timeout-seconds", 120L));
        context.teleportRequests().request(player, target, timeout);
        context.message(sender, "teleport.tpa.sent", Map.of("target", target.getName()));
        context.message(target, "teleport.tpa.received", Map.of("player", player.getName()));
        return true;
    }

    private boolean tpaHere(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length == 0) {
            context.message(sender, "teleport.tpa.usage", Map.of("label", label));
            return true;
        }
        Player target = CommandUtils.onlinePlayer(args[0]).orElse(null);
        if (target == null || target.equals(player)) {
            context.message(sender, "teleport.tpa.target-unavailable", Map.of("target", args[0]));
            return true;
        }
        if (!canRequest(player, target)) {
            context.message(sender, "teleport.tpa.target-disabled", Map.of("target", target.getName()));
            return true;
        }
        if (preferences.autoAcceptEnabled(target.getUniqueId())) {
            directTeleport(target, player.getLocation(), "teleport.tpa.here-auto-accepted-target", Map.of("player", player.getName()));
            context.message(player, "teleport.tpa.here-auto-accepted-requester", Map.of("target", target.getName()));
            return true;
        }
        Duration timeout = Duration.ofSeconds(context.plugin().getConfig().getLong("teleport.request-timeout-seconds", 120L));
        context.teleportRequests().requestHere(player, target, timeout);
        context.message(sender, "teleport.tpa.here-sent", Map.of("target", target.getName()));
        context.message(target, "teleport.tpa.here-received", Map.of("player", player.getName()));
        return true;
    }

    private boolean tpaAll(CommandSender sender, String label, List<String> args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!args.isEmpty()) {
            context.message(sender, "teleport.tpa.all-usage", Map.of("label", label));
            return true;
        }

        List<Player> onlinePlayers = List.copyOf(Bukkit.getOnlinePlayers());
        Map<UUID, Player> playersById = onlinePlayers.stream()
                .collect(java.util.stream.Collectors.toMap(Player::getUniqueId, target -> target));
        TpaAllPlanner.Plan plan = TpaAllPlanner.plan(
                player.getUniqueId(),
                player.hasPermission("hydroxide.command.tptoggle.bypass"),
                onlinePlayers.stream()
                        .map(target -> new TpaAllPlanner.Candidate(
                                target.getUniqueId(),
                                preferences.requestsEnabled(target.getUniqueId())))
                        .toList()
        );
        if (plan.targetIds().isEmpty()) {
            context.message(sender, "teleport.tpa.all-none", Map.of("skipped", plan.skippedDisabled()));
            return true;
        }

        Duration timeout = Duration.ofSeconds(context.plugin().getConfig().getLong("teleport.request-timeout-seconds", 120L));
        for (UUID targetId : plan.targetIds()) {
            Player target = playersById.get(targetId);
            if (target == null) {
                continue;
            }
            context.teleportRequests().requestHere(player, target, timeout);
            context.message(target, "teleport.tpa.all-received", Map.of("player", player.getName()));
        }
        context.message(sender, "teleport.tpa.all-sent", Map.of(
                "count", plan.targetIds().size(),
                "skipped", plan.skippedDisabled()
        ));
        return true;
    }

    private boolean tpaCancel(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        Optional<TeleportRequest> request = context.teleportRequests().cancel(player);
        if (request.isEmpty()) {
            context.message(sender, "teleport.tpa.cancel-none", Map.of());
            return true;
        }
        context.message(sender, "teleport.tpa.cancelled-requester", Map.of("target", request.get().targetName()));
        Player target = Bukkit.getPlayer(request.get().targetId());
        if (target != null) {
            context.message(target, "teleport.tpa.cancelled-target", Map.of("player", player.getName()));
        }
        return true;
    }

    private boolean tpToggle(CommandSender sender, String label, List<String> args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        Optional<Boolean> state = requestedState(args, label, sender, "teleport.tptoggle.usage");
        if (state.isEmpty() && !args.isEmpty()) {
            return true;
        }
        boolean enabled = state.orElse(!preferences.requestsEnabled(player.getUniqueId()));
        preferences.setRequestsEnabled(player.getUniqueId(), enabled);
        context.message(sender, enabled ? "teleport.tptoggle.enabled" : "teleport.tptoggle.disabled", Map.of());
        return true;
    }

    private boolean tpAuto(CommandSender sender, String label, List<String> args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        Optional<Boolean> state = requestedState(args, label, sender, "teleport.tpauto.usage");
        if (state.isEmpty() && !args.isEmpty()) {
            return true;
        }
        boolean enabled = state.orElse(!preferences.autoAcceptEnabled(player.getUniqueId()));
        preferences.setAutoAcceptEnabled(player.getUniqueId(), enabled);
        context.message(sender, enabled ? "teleport.tpauto.enabled" : "teleport.tpauto.disabled", Map.of());
        return true;
    }

    private boolean tpAccept(CommandSender sender) {
        Player target = requirePlayer(sender);
        if (target == null) {
            return true;
        }
        Optional<TeleportRequest> request = context.teleportRequests().accept(target);
        if (request.isEmpty()) {
            context.message(sender, "teleport.tpa.none", Map.of());
            return true;
        }
        Player requester = Bukkit.getPlayer(request.get().requesterId());
        if (requester == null) {
            context.message(sender, "teleport.tpa.requester-offline", Map.of());
            return true;
        }
        if (request.get().direction() == TeleportRequestDirection.TARGET_TO_REQUESTER) {
            teleport(target, Optional.of(StoredLocation.from(requester.getLocation())),
                    "teleport.tpa.here-accepted-target", Map.of("player", requester.getName()));
            context.message(requester, "teleport.tpa.here-accepted-requester", Map.of("target", target.getName()));
        } else {
            teleport(requester, Optional.of(StoredLocation.from(target.getLocation())),
                    "teleport.tpa.accepted-requester", Map.of("target", target.getName()));
            context.message(target, "teleport.tpa.accepted-target", Map.of("player", requester.getName()));
        }
        return true;
    }

    private boolean tpDeny(CommandSender sender) {
        Player target = requirePlayer(sender);
        if (target == null) {
            return true;
        }
        Optional<TeleportRequest> request = context.teleportRequests().deny(target);
        if (request.isEmpty()) {
            context.message(sender, "teleport.tpa.none", Map.of());
            return true;
        }
        Player requester = Bukkit.getPlayer(request.get().requesterId());
        if (requester != null) {
            context.message(requester, "teleport.tpa.denied-requester", Map.of("target", target.getName()));
        }
        context.message(target, "teleport.tpa.denied-target", Map.of());
        return true;
    }

    private boolean tp(CommandSender sender, String label, String[] args) {
        return tpDirect(sender, label, args, "hydroxide.command.tp.others");
    }

    private boolean tpDirect(CommandSender sender, String label, String[] args, String othersPermission) {
        if (args.length == 0 || args.length > 2) {
            context.message(sender, "teleport.direct.usage", Map.of("label", label));
            return true;
        }
        if (args.length == 1) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            Player target = online(args[0], sender);
            if (target == null) {
                return true;
            }
            directTeleport(player, target.getLocation(), "teleport.direct.sent", Map.of("target", target.getName()));
            return true;
        }
        if (!sender.hasPermission(othersPermission)) {
            context.message(sender, "validation.no-permission", Map.of("permission", othersPermission));
            return true;
        }
        Player moved = online(args[0], sender);
        Player target = online(args[1], sender);
        if (moved == null || target == null) {
            return true;
        }
        directTeleport(moved, target.getLocation(), "teleport.direct.sent", Map.of("target", target.getName()));
        context.message(sender, "teleport.direct.other", Map.of("player", moved.getName(), "target", target.getName()));
        return true;
    }

    private boolean tpHere(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length != 1) {
            context.message(sender, "teleport.direct.here-usage", Map.of("label", label));
            return true;
        }
        Player target = online(args[0], sender);
        if (target == null) {
            return true;
        }
        directTeleport(target, player.getLocation(), "teleport.direct.brought-target", Map.of("target", player.getName()));
        context.message(sender, "teleport.direct.brought", Map.of("target", target.getName()));
        return true;
    }

    private boolean tpAll(CommandSender sender, String label, String[] args) {
        Player destination;
        if (args.length == 0) {
            destination = sender instanceof Player player ? player : null;
            if (destination == null) {
                context.message(sender, "teleport.direct.tpall-usage", Map.of("label", label));
                return true;
            }
        } else if (args.length == 1) {
            destination = online(args[0], sender);
            if (destination == null) {
                return true;
            }
        } else {
            context.message(sender, "teleport.direct.tpall-usage", Map.of("label", label));
            return true;
        }
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(destination)) {
                count++;
                directTeleport(player, destination.getLocation(), "teleport.direct.sent", Map.of("target", destination.getName()));
            }
        }
        context.message(sender, "teleport.direct.all", Map.of("target", destination.getName(), "count", count));
        return true;
    }

    private boolean tpAllWorld(CommandSender sender, String label, List<String> args) {
        Optional<TpAllWorldCommandParser.Request> parsed = TpAllWorldCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "teleport.direct.tpallworld-usage", Map.of("label", label));
            return true;
        }
        TpAllWorldCommandParser.Request request = parsed.get();
        World sourceWorld = Bukkit.getWorld(request.sourceWorld());
        if (sourceWorld == null) {
            context.message(sender, "teleport.location.world-unloaded", Map.of("world", request.sourceWorld()));
            return true;
        }
        Location destination = tpAllWorldDestination(sender, request).orElse(null);
        if (destination == null) {
            return true;
        }

        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(sourceWorld)) {
                count++;
                directTeleport(player, destination, "teleport.direct.sent", Map.of("target", destination.getWorld().getName()));
            }
        }
        if (count == 0) {
            context.message(sender, "teleport.direct.tpallworld-empty", Map.of("world", sourceWorld.getName()));
            return true;
        }
        context.message(sender, "teleport.direct.tpallworld-done", Map.of(
                "count", count,
                "source", sourceWorld.getName(),
                "world", destination.getWorld().getName(),
                "include_all", request.includeAll()
        ));
        return true;
    }

    private Optional<Location> tpAllWorldDestination(CommandSender sender, TpAllWorldCommandParser.Request request) {
        if (request.destination().isEmpty()) {
            if (sender instanceof Player player) {
                return Optional.of(player.getLocation());
            }
            context.message(sender, "teleport.direct.tpallworld-usage", Map.of("label", "tpallworld"));
            return Optional.empty();
        }
        TpAllWorldCommandParser.Destination parsedDestination = request.destination().orElseThrow();
        World destinationWorld = Bukkit.getWorld(parsedDestination.worldName());
        if (destinationWorld == null) {
            context.message(sender, "teleport.location.world-unloaded", Map.of("world", parsedDestination.worldName()));
            return Optional.empty();
        }
        return Optional.of(new Location(destinationWorld, parsedDestination.x(), parsedDestination.y(),
                parsedDestination.z(), parsedDestination.yaw(), parsedDestination.pitch()));
    }

    private boolean tpPos(CommandSender sender, String label, List<String> args, String othersPermission) {
        Optional<TeleportPositionParser.Request> request = TeleportPositionParser.request(args);
        if (request.isEmpty()) {
            context.message(sender, "teleport.position.usage", Map.of("label", label));
            return true;
        }
        TeleportPositionParser.Request parsedRequest = request.orElseThrow();
        Player target = positionTarget(sender, parsedRequest, othersPermission);
        if (target == null) {
            return true;
        }
        Location base = target.getLocation();
        World world = parsedRequest.worldName()
                .map(Bukkit::getWorld)
                .orElse(target.getWorld());
        if (world == null) {
            context.message(sender, "teleport.location.world-unloaded", Map.of("world", parsedRequest.worldName().orElse("unknown")));
            return true;
        }
        Optional<TeleportPositionParser.Coordinates> parsed = TeleportPositionParser.coordinates(
                parsedRequest.x(), parsedRequest.y(), parsedRequest.z(), base.getX(), base.getY(), base.getZ());
        if (parsed.isEmpty()) {
            context.message(sender, "teleport.position.usage", Map.of("label", label));
            return true;
        }
        TeleportPositionParser.Coordinates coordinates = parsed.get();
        Location destination = new Location(
                world,
                coordinates.x(),
                coordinates.y(),
                coordinates.z(),
                parsedRequest.yaw().orElse(base.getYaw()),
                parsedRequest.pitch().orElse(base.getPitch())
        );
        directTeleport(target, destination, "teleport.position.success", Map.of(
                "world", world.getName(),
                "x", formatCoordinate(coordinates.x()),
                "y", formatCoordinate(coordinates.y()),
                "z", formatCoordinate(coordinates.z())
        ));
        return true;
    }

    private Player positionTarget(CommandSender sender, TeleportPositionParser.Request request, String othersPermission) {
        if (request.targetName().isEmpty()) {
            return requirePlayer(sender);
        }
        Player target = online(request.targetName().orElseThrow(), sender);
        if (target == null) {
            return null;
        }
        if (!sender.equals(target) && !sender.hasPermission(othersPermission)) {
            context.message(sender, "validation.no-permission", Map.of("permission", othersPermission));
            return null;
        }
        return target;
    }

    private boolean jump(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        Block target = player.getTargetBlockExact(120, FluidCollisionMode.NEVER);
        if (target == null) {
            context.message(sender, "teleport.jump.no-target", Map.of());
            return true;
        }
        Location destination = target.getLocation().add(0.5D, 1.0D, 0.5D);
        destination.setYaw(player.getLocation().getYaw());
        destination.setPitch(player.getLocation().getPitch());
        directTeleport(player, destination, "teleport.jump.success", Map.of());
        return true;
    }

    private boolean down(CommandSender sender, String label, List<String> args) {
        Optional<DownCommandParser.Request> parsed = DownCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "teleport.down.usage", Map.of("label", label));
            return true;
        }
        DownCommandParser.Request request = parsed.get();
        Player target = downTarget(sender, request);
        if (target == null) {
            return true;
        }
        OptionalInt destinationY = DownTeleportPlanner.findDestinationY(target.getLocation().getBlockY(), column(target), request.max());
        if (destinationY.isEmpty()) {
            if (!request.silent()) {
                context.message(sender, "teleport.down.no-safe-location", Map.of("target", target.getName()));
            }
            return true;
        }
        Location destination = target.getLocation().clone();
        destination.setY(destinationY.getAsInt());
        teleportDown(sender, target, destination, request.silent());
        return true;
    }

    private boolean world(CommandSender sender, String label, List<String> args) {
        Optional<WorldCommandParser.Request> parsed = WorldCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "teleport.world.usage", Map.of("label", label));
            return true;
        }
        WorldCommandParser.Request request = parsed.get();
        World destinationWorld = destinationWorld(request.selector()).orElse(null);
        if (destinationWorld == null) {
            if (!request.silent()) {
                context.message(sender, "teleport.world.not-loaded", Map.of("world", request.selector().input()));
            }
            return true;
        }
        Player target = worldTarget(sender, request);
        if (target == null) {
            return true;
        }
        Location current = target.getLocation();
        Location destination = new Location(destinationWorld, current.getX(), current.getY(), current.getZ(),
                current.getYaw(), current.getPitch());
        teleportWorld(sender, target, destination, request.silent());
        return true;
    }

    private boolean patrol(CommandSender sender, String label, List<String> args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.size() > 1 || (!args.isEmpty() && !"reset".equalsIgnoreCase(args.getFirst()))) {
            context.message(sender, "teleport.patrol.usage", Map.of("label", label));
            return true;
        }
        if (!args.isEmpty()) {
            patrolTargets.reset(player.getName());
            context.message(sender, "teleport.patrol.reset", Map.of());
            return true;
        }

        Optional<String> selectedName = patrolTargets.next(player.getName(),
                Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        if (selectedName.isEmpty()) {
            context.message(sender, "teleport.patrol.none", Map.of());
            return true;
        }

        Player target = Bukkit.getPlayerExact(selectedName.orElseThrow());
        if (target == null) {
            context.message(sender, "teleport.patrol.none", Map.of());
            return true;
        }
        directTeleport(player, target.getLocation(), "teleport.patrol.success", Map.of("target", target.getName()));
        return true;
    }

    private Optional<World> destinationWorld(WorldCommandParser.WorldSelector selector) {
        if (selector.environment().isPresent()) {
            World.Environment environment = selector.environment().orElseThrow();
            return Bukkit.getWorlds().stream()
                    .filter(world -> world.getEnvironment() == environment)
                    .findFirst();
        }
        return Optional.ofNullable(Bukkit.getWorld(selector.input()));
    }

    private Player worldTarget(CommandSender sender, WorldCommandParser.Request request) {
        if (request.targetName().isEmpty()) {
            return requirePlayer(sender);
        }
        Player target = online(request.targetName().orElseThrow(), sender);
        if (target == null) {
            return null;
        }
        if (!sender.equals(target) && !sender.hasPermission("hydroxide.command.world.others")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.world.others"));
            return null;
        }
        return target;
    }

    private void teleportWorld(CommandSender sender, Player target, Location destination, boolean silent) {
        context.backLocations().remember(target.getUniqueId(), target.getLocation());
        target.teleportAsync(destination).thenAccept(success -> Bukkit.getScheduler().runTask(context.plugin(), () -> {
            if (!success) {
                if (!silent) {
                    context.message(sender, "teleport.location.failed", Map.of());
                }
                return;
            }
            if (silent) {
                return;
            }
            context.message(target, "teleport.world.success", Map.of("world", destination.getWorld().getName()));
            if (!sender.equals(target)) {
                context.message(sender, "teleport.world.success-other", Map.of(
                        "target", target.getName(),
                        "world", destination.getWorld().getName()
                ));
            }
        }));
    }

    private Player downTarget(CommandSender sender, DownCommandParser.Request request) {
        if (request.targetName().isEmpty()) {
            return requirePlayer(sender);
        }
        Player target = online(request.targetName().orElseThrow(), sender);
        if (target == null) {
            return null;
        }
        if (!sender.equals(target) && !sender.hasPermission("hydroxide.command.down.others")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.down.others"));
            return null;
        }
        return target;
    }

    private DownTeleportPlanner.Column column(Player player) {
        World world = player.getWorld();
        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        return new DownTeleportPlanner.Column() {
            @Override
            public int minY() {
                return world.getMinHeight();
            }

            @Override
            public int maxY() {
                return world.getMaxHeight();
            }

            @Override
            public boolean safeFeetAt(int feetY) {
                Block support = world.getBlockAt(x, feetY - 1, z);
                Block feet = world.getBlockAt(x, feetY, z);
                Block head = world.getBlockAt(x, feetY + 1, z);
                return support.getType().isSolid() && feet.getType().isAir() && head.getType().isAir();
            }
        };
    }

    private void teleportDown(CommandSender sender, Player target, Location destination, boolean silent) {
        context.backLocations().remember(target.getUniqueId(), target.getLocation());
        target.teleportAsync(destination).thenAccept(success -> Bukkit.getScheduler().runTask(context.plugin(), () -> {
            if (!success) {
                if (!silent) {
                    context.message(sender, "teleport.location.failed", Map.of());
                }
                return;
            }
            if (silent) {
                return;
            }
            context.message(target, "teleport.down.success", Map.of());
            if (!sender.equals(target)) {
                context.message(sender, "teleport.down.success-other", Map.of("target", target.getName()));
            }
        }));
    }

    private void teleportDeathBack(CommandSender sender, Player target, StoredLocation stored, boolean silent) {
        World world = Bukkit.getWorld(stored.worldName());
        if (world == null) {
            if (!silent) {
                context.message(sender, "teleport.location.world-unloaded", Map.of("world", stored.worldName()));
            }
            return;
        }
        context.backLocations().remember(target.getUniqueId(), target.getLocation());
        target.teleportAsync(stored.toLocation(world)).thenAccept(success -> Bukkit.getScheduler().runTask(context.plugin(), () -> {
            if (!success) {
                if (!silent) {
                    context.message(sender, "teleport.location.failed", Map.of());
                }
                return;
            }
            if (silent) {
                return;
            }
            if (sender.equals(target)) {
                context.message(target, "teleport.deathback.success", Map.of());
                return;
            }
            context.message(sender, "teleport.deathback.success-other", Map.of("target", target.getName()));
            context.message(target, "teleport.deathback.target", Map.of("player", sender.getName()));
        }));
    }

    private void teleport(Player player, Optional<StoredLocation> optionalLocation, String successKey, Map<String, ?> placeholders) {
        if (optionalLocation.isEmpty()) {
            context.message(player, "teleport.location.missing", Map.of());
            return;
        }
        StoredLocation stored = optionalLocation.get();
        World world = Bukkit.getWorld(stored.worldName());
        if (world == null) {
            context.message(player, "teleport.location.world-unloaded", Map.of("world", stored.worldName()));
            return;
        }
        Location destination = stored.toLocation(world);
        context.backLocations().remember(player.getUniqueId(), player.getLocation());
        player.teleportAsync(destination).thenAccept(success -> Bukkit.getScheduler().runTask(context.plugin(), () -> {
            if (success) {
                context.message(player, successKey, placeholders);
            } else {
                context.message(player, "teleport.location.failed", Map.of());
            }
        }));
    }

    private void directTeleport(Player player, Location destination, String successKey, Map<String, ?> placeholders) {
        context.backLocations().remember(player.getUniqueId(), player.getLocation());
        player.teleportAsync(destination).thenAccept(success -> Bukkit.getScheduler().runTask(context.plugin(), () -> {
            if (success) {
                context.message(player, successKey, placeholders);
            } else {
                context.message(player, "teleport.location.failed", Map.of());
            }
        }));
    }

    private Player online(String name, CommandSender sender) {
        Player player = CommandUtils.onlinePlayer(name).orElse(null);
        if (player == null) {
            context.message(sender, "teleport.direct.player-offline", Map.of("target", name));
        }
        return player;
    }

    private boolean canRequest(Player requester, Player target) {
        return preferences.requestsEnabled(target.getUniqueId())
                || requester.hasPermission("hydroxide.command.tptoggle.bypass");
    }

    private Optional<Boolean> requestedState(List<String> args, String label, CommandSender sender, String usageKey) {
        if (args.isEmpty()) {
            return Optional.empty();
        }
        if (args.size() > 1) {
            context.message(sender, usageKey, Map.of("label", label));
            return Optional.empty();
        }
        return switch (args.getFirst().toLowerCase(Locale.ROOT)) {
            case "on", "true", "yes", "enable", "enabled" -> Optional.of(true);
            case "off", "false", "no", "disable", "disabled" -> Optional.of(false);
            default -> {
                context.message(sender, usageKey, Map.of("label", label));
                yield Optional.empty();
            }
        };
    }

    private String formatCoordinate(double coordinate) {
        return coordinate == Math.rint(coordinate) ? Long.toString(Math.round(coordinate)) : String.format(Locale.ROOT, "%.2f", coordinate);
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        context.message(sender, "validation.player-only", Map.of());
        return null;
    }

    private boolean sameBlock(Location first, Location second) {
        return first.getWorld() == second.getWorld()
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}
