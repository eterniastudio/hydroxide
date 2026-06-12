package net.axther.hydroxide.modules.moderation;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.commands.CommandUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class ModerationCommands implements CommandExecutor, TabCompleter, Listener {

    private static final List<String> GAMEMODES = List.of("survival", "creative", "adventure", "spectator");
    private final HydroxideContext context;
    private final Set<UUID> godPlayers = new HashSet<>();

    public ModerationCommands(HydroxideContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "fly" -> fly(sender, args);
            case "god" -> god(sender, args);
            case "heal" -> heal(sender, args);
            case "feed" -> feed(sender, args);
            case "speed" -> speed(sender, args);
            case "gamemode" -> gameMode(sender, args);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("gamemode") && args.length == 1) {
            return CommandUtils.matching(args[0], GAMEMODES);
        }
        if ((name.equals("fly") || name.equals("god") || name.equals("heal") || name.equals("feed")) && args.length == 1) {
            return onlinePlayerNames(args[0]);
        }
        if (name.equals("gamemode") && args.length == 2) {
            return onlinePlayerNames(args[1]);
        }
        if (name.equals("speed") && args.length == 1) {
            return CompletionUtils.integerRange(args[0], 1, 10);
        }
        if (name.equals("speed") && args.length == 2) {
            return CommandUtils.matching(args[1], List.of("walk", "fly"));
        }
        if (name.equals("speed") && args.length == 3) {
            return onlinePlayerNames(args[2]);
        }
        return List.of();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && godPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private boolean fly(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.fly")) {
            return true;
        }
        Player target = targetOrSelf(sender, args, 0);
        if (target == null) {
            return true;
        }
        target.setAllowFlight(!target.getAllowFlight());
        context.send(sender, "<green>Flight " + state(target.getAllowFlight()) + " <green>for <white>" + target.getName() + "<green>.");
        if (!sender.equals(target)) {
            context.send(target, "<green>Flight " + state(target.getAllowFlight()) + "<green>.");
        }
        return true;
    }

    private boolean god(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.god")) {
            return true;
        }
        Player target = targetOrSelf(sender, args, 0);
        if (target == null) {
            return true;
        }
        boolean enabled = !godPlayers.remove(target.getUniqueId());
        if (enabled) {
            godPlayers.add(target.getUniqueId());
        }
        context.send(sender, "<green>God mode " + state(enabled) + " <green>for <white>" + target.getName() + "<green>.");
        if (!sender.equals(target)) {
            context.send(target, "<green>God mode " + state(enabled) + "<green>.");
        }
        return true;
    }

    private boolean heal(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.heal")) {
            return true;
        }
        Player target = targetOrSelf(sender, args, 0);
        if (target == null) {
            return true;
        }
        AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
        target.setHealth(maxHealth == null ? 20.0 : maxHealth.getValue());
        target.setFireTicks(0);
        context.send(sender, "<green>Healed <white>" + target.getName() + "<green>.");
        return true;
    }

    private boolean feed(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.feed")) {
            return true;
        }
        Player target = targetOrSelf(sender, args, 0);
        if (target == null) {
            return true;
        }
        target.setFoodLevel(20);
        target.setSaturation(20.0f);
        context.send(sender, "<green>Fed <white>" + target.getName() + "<green>.");
        return true;
    }

    private boolean speed(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.speed")) {
            return true;
        }
        Player target = self(sender);
        if (target == null) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /speed <1-10> [walk|fly] [player]");
            return true;
        }
        float value;
        try {
            value = Math.max(1, Math.min(10, Float.parseFloat(args[0]))) / 10.0f;
        } catch (NumberFormatException exception) {
            context.send(sender, "<red>Speed must be a number from 1 to 10.");
            return true;
        }
        String type = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : (target.isFlying() ? "fly" : "walk");
        if (args.length >= 3) {
            target = CommandUtils.onlinePlayer(args[2]).orElse(null);
            if (target == null) {
                context.send(sender, "<red>That player is not online.");
                return true;
            }
        }
        if (type.equals("fly")) {
            target.setFlySpeed(value);
        } else {
            target.setWalkSpeed(value);
        }
        context.send(sender, "<green>Set " + type + " speed for <white>" + target.getName() + " <green>to <white>" + args[0] + "<green>.");
        return true;
    }

    private boolean gameMode(CommandSender sender, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.gamemode")) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /gamemode <mode> [player]");
            return true;
        }
        GameMode mode = CommandUtils.gameMode(args[0]).orElse(null);
        if (mode == null) {
            context.send(sender, "<red>Unknown gamemode.");
            return true;
        }
        Player target = targetOrSelf(sender, args, 1);
        if (target == null) {
            return true;
        }
        target.setGameMode(mode);
        context.send(sender, "<green>Set <white>" + target.getName() + " <green>to <white>" + mode.name().toLowerCase(Locale.ROOT) + "<green>.");
        return true;
    }

    private Player targetOrSelf(CommandSender sender, String[] args, int targetIndex) {
        if (args.length > targetIndex) {
            Player target = CommandUtils.onlinePlayer(args[targetIndex]).orElse(null);
            if (target == null) {
                context.send(sender, "<red>That player is not online.");
            }
            return target;
        }
        return self(sender);
    }

    private Player self(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        context.send(sender, "<red>Console must specify a player.");
        return null;
    }

    private List<String> onlinePlayerNames(String prefix) {
        return CommandUtils.matching(prefix, Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
    }

    private String state(boolean enabled) {
        return enabled ? "<green>enabled" : "<red>disabled";
    }
}
