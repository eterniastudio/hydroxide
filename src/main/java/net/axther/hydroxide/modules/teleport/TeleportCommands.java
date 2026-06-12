package net.axther.hydroxide.modules.teleport;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.navigation.HomeLimitService;
import net.axther.hydroxide.storage.StoredLocation;
import net.axther.hydroxide.teleport.TeleportRequestService.TeleportRequest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TeleportCommands implements CommandExecutor, TabCompleter, Listener {

    private static final String DEFAULT_HOME = "home";
    private final HydroxideContext context;

    public TeleportCommands(HydroxideContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "spawn" -> spawn(sender);
            case "setspawn" -> setSpawn(sender);
            case "home" -> home(sender, args);
            case "sethome" -> setHome(sender, args);
            case "delhome" -> delHome(sender, args);
            case "homes" -> homes(sender);
            case "warp" -> warp(sender, args);
            case "setwarp" -> setWarp(sender, args);
            case "delwarp" -> delWarp(sender, args);
            case "warps" -> warps(sender);
            case "back" -> back(sender);
            case "tpa" -> tpa(sender, args);
            case "tpaccept" -> tpAccept(sender);
            case "tpdeny" -> tpDeny(sender);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (args.length != 1) {
            return List.of();
        }
        return switch (name) {
            case "home", "sethome", "delhome" -> sender instanceof Player player
                    ? CommandUtils.matching(args[0], context.playerData().homes(player.getUniqueId()))
                    : List.of();
            case "warp", "setwarp", "delwarp" -> CommandUtils.matching(args[0], context.warps().names());
            case "tpa" -> CommandUtils.matching(args[0], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            default -> List.of();
        };
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!sameBlock(event.getFrom(), event.getTo())) {
            context.backLocations().remember(event.getPlayer().getUniqueId(), event.getFrom());
        }
    }

    private boolean spawn(CommandSender sender) {
        if (!context.requirePermission(sender, "hydroxide.command.spawn")) {
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        teleport(player, context.spawns().get("spawn"), "<green>Teleported to spawn.");
        return true;
    }

    private boolean setSpawn(CommandSender sender) {
        if (!context.requirePermission(sender, "hydroxide.command.setspawn")) {
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        context.spawns().set("spawn", StoredLocation.from(player.getLocation()));
        context.send(sender, "<green>Spawn location set.");
        return true;
    }

    private boolean home(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.home")) {
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        String name = args.length == 0 ? DEFAULT_HOME : args[0];
        teleport(player, context.playerData().home(player.getUniqueId(), name), "<green>Teleported to home <white>" + name + "<green>.");
        return true;
    }

    private boolean setHome(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.sethome")) {
            return true;
        }
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
            context.send(sender, "<red>You have reached your home limit of <white>" + limit + "<red>.");
            return true;
        }
        context.playerData().setHome(player.getUniqueId(), name, player.getLocation());
        context.send(sender, "<green>Home <white>" + name + " <green>set.");
        return true;
    }

    private boolean delHome(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.delhome")) {
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        String name = args.length == 0 ? DEFAULT_HOME : args[0];
        boolean removed = context.playerData().removeHome(player.getUniqueId(), name);
        context.send(sender, removed ? "<green>Home removed." : "<red>That home does not exist.");
        return true;
    }

    private boolean homes(CommandSender sender) {
        if (!context.requirePermission(sender, "hydroxide.command.homes")) {
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        List<String> homes = context.playerData().homes(player.getUniqueId());
        context.send(sender, homes.isEmpty() ? "<gray>You do not have any homes." : "<green>Homes: <white>" + String.join("<gray>, <white>", homes));
        return true;
    }

    private boolean warp(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.warp")) {
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /warp <name>");
            return true;
        }
        teleport(player, context.warps().get(args[0]), "<green>Warped to <white>" + args[0] + "<green>.");
        return true;
    }

    private boolean setWarp(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.setwarp")) {
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /setwarp <name>");
            return true;
        }
        context.warps().set(args[0], StoredLocation.from(player.getLocation()));
        context.send(sender, "<green>Warp <white>" + args[0] + " <green>set.");
        return true;
    }

    private boolean delWarp(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.delwarp")) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /delwarp <name>");
            return true;
        }
        boolean removed = context.warps().remove(args[0]);
        context.send(sender, removed ? "<green>Warp removed." : "<red>That warp does not exist.");
        return true;
    }

    private boolean warps(CommandSender sender) {
        if (!context.requirePermission(sender, "hydroxide.command.warps")) {
            return true;
        }
        List<String> warps = context.warps().names();
        context.send(sender, warps.isEmpty() ? "<gray>No warps have been set." : "<green>Warps: <white>" + String.join("<gray>, <white>", warps));
        return true;
    }

    private boolean back(CommandSender sender) {
        if (!context.requirePermission(sender, "hydroxide.command.back")) {
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        teleport(player, context.backLocations().previous(player.getUniqueId()), "<green>Returned to your previous location.");
        return true;
    }

    private boolean tpa(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.tpa")) {
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /tpa <player>");
            return true;
        }
        Player target = CommandUtils.onlinePlayer(args[0]).orElse(null);
        if (target == null || target.equals(player)) {
            context.send(sender, "<red>That player is not available.");
            return true;
        }
        Duration timeout = Duration.ofSeconds(context.plugin().getConfig().getLong("teleport.request-timeout-seconds", 120L));
        context.teleportRequests().request(player, target, timeout);
        context.send(sender, "<green>Teleport request sent to <white>" + target.getName() + "<green>.");
        context.send(target, "<white>" + player.getName() + " <#44CCFF>wants to teleport to you. <gray>/tpaccept <dark_gray>or <gray>/tpdeny");
        return true;
    }

    private boolean tpAccept(CommandSender sender) {
        if (!context.requirePermission(sender, "hydroxide.command.tpaccept")) {
            return true;
        }
        Player target = requirePlayer(sender);
        if (target == null) {
            return true;
        }
        Optional<TeleportRequest> request = context.teleportRequests().accept(target);
        if (request.isEmpty()) {
            context.send(sender, "<red>You do not have any pending teleport requests.");
            return true;
        }
        Player requester = Bukkit.getPlayer(request.get().requesterId());
        if (requester == null) {
            context.send(sender, "<red>That player is no longer online.");
            return true;
        }
        teleport(requester, Optional.of(StoredLocation.from(target.getLocation())),
                "<green>Teleport request accepted by <white>" + target.getName() + "<green>.");
        context.send(target, "<green>Accepted teleport request from <white>" + requester.getName() + "<green>.");
        return true;
    }

    private boolean tpDeny(CommandSender sender) {
        if (!context.requirePermission(sender, "hydroxide.command.tpdeny")) {
            return true;
        }
        Player target = requirePlayer(sender);
        if (target == null) {
            return true;
        }
        Optional<TeleportRequest> request = context.teleportRequests().deny(target);
        if (request.isEmpty()) {
            context.send(sender, "<red>You do not have any pending teleport requests.");
            return true;
        }
        Player requester = Bukkit.getPlayer(request.get().requesterId());
        if (requester != null) {
            context.send(requester, "<red>Your teleport request to <white>" + target.getName() + " <red>was denied.");
        }
        context.send(target, "<green>Denied teleport request.");
        return true;
    }

    private void teleport(Player player, Optional<StoredLocation> optionalLocation, String successMessage) {
        if (optionalLocation.isEmpty()) {
            context.send(player, "<red>That location has not been set.");
            return;
        }
        StoredLocation stored = optionalLocation.get();
        World world = Bukkit.getWorld(stored.worldName());
        if (world == null) {
            context.send(player, "<red>World <white>" + stored.worldName() + " <red>is not loaded.");
            return;
        }
        Location destination = stored.toLocation(world);
        context.backLocations().remember(player.getUniqueId(), player.getLocation());
        player.teleportAsync(destination).thenAccept(success -> Bukkit.getScheduler().runTask(context.plugin(), () -> {
            if (success) {
                context.send(player, successMessage);
            } else {
                context.send(player, "<red>Teleport failed.");
            }
        }));
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        context.send(sender, "<red>Only players can use this command.");
        return null;
    }

    private boolean sameBlock(Location first, Location second) {
        return first.getWorld() == second.getWorld()
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}
