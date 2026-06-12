package net.axther.hydroxide.modules.social;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SocialModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

    private static final List<String> PARTY_ACTIONS = List.of("accept", "chat", "ff", "invite", "leave");
    private static final List<String> FRIEND_ACTIONS = List.of("add", "list", "remove");

    private HydroxideContext context;
    private PartyService parties;

    @Override
    public String id() {
        return "social";
    }

    @Override
    public String displayName() {
        return "Parties and Friends";
    }

    @Override
    public String description() {
        return "Temporary player parties, private party chat, friendly-fire protection, and persistent friends.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.parties = new PartyService();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("party", this);
        context.commands().register("p", this);
        context.commands().register("friend", this);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can use social commands.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("p")) {
            return partyChat(player, args);
        }
        if (command.getName().equalsIgnoreCase("friend")) {
            return friend(player, label, args);
        }
        return party(player, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("p")) {
            return List.of();
        }
        if (name.equals("party")) {
            if (args.length == 1) {
                return CommandUtils.matching(args[0], PARTY_ACTIONS);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
                return net.axther.hydroxide.commands.CompletionUtils.onlinePlayers(args[1]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("ff")) {
                return CommandUtils.matching(args[1], List.of("off", "on"));
            }
        }
        if (name.equals("friend")) {
            if (args.length == 1) {
                return CommandUtils.matching(args[0], FRIEND_ACTIONS);
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
                return net.axther.hydroxide.commands.CompletionUtils.onlinePlayers(args[1]);
            }
        }
        return List.of();
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player victim = event.getEntity() instanceof Player player ? player : null;
        Player attacker = attacker(event.getDamager());
        if (victim != null && attacker != null && !parties.friendlyFireAllowed(victim.getUniqueId(), attacker.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        notifyFriends(event.getPlayer(), true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        notifyFriends(event.getPlayer(), false);
    }

    private boolean party(Player player, String label, String[] args) {
        if (args.length == 0) {
            context.send(player, "<red>Usage: /" + label + " <invite|accept|leave|ff|chat> ...");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "invite" -> {
                Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : null;
                if (target == null) {
                    context.send(player, "<red>That player is not online.");
                    return true;
                }
                parties.invite(player.getUniqueId(), target.getUniqueId());
                context.send(player, "<green>Invited <white>" + target.getName() + "<green> to your party.");
                context.send(target, "<white>" + player.getName() + " <#44CCFF>invited you to a party. <gray>/party accept");
            }
            case "accept" -> context.send(player, parties.accept(player.getUniqueId())
                    ? "<green>Joined the party."
                    : "<red>You do not have a pending party invite.");
            case "leave" -> {
                parties.leave(player.getUniqueId());
                context.send(player, "<green>Left your party.");
            }
            case "ff" -> {
                boolean allowed = args.length > 1 && args[1].equalsIgnoreCase("on");
                parties.setFriendlyFire(player.getUniqueId(), allowed);
                context.send(player, "<green>Party friendly fire: <white>" + (allowed ? "on" : "off"));
            }
            case "chat" -> partyChat(player, java.util.Arrays.copyOfRange(args, 1, args.length));
            default -> context.send(player, "<red>Usage: /" + label + " <invite|accept|leave|ff|chat> ...");
        }
        return true;
    }

    private boolean partyChat(Player player, String[] args) {
        if (args.length == 0) {
            context.send(player, "<red>Usage: /p <message>");
            return true;
        }
        PartyService.Party party = parties.party(player.getUniqueId()).orElse(null);
        if (party == null) {
            context.send(player, "<red>You are not in a party.");
            return true;
        }
        String message = CommandUtils.joinArgs(args, 0);
        for (UUID memberId : party.members()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(context.text().format("<dark_gray>[<#44CCFF>Party</#44CCFF>] <white>" + player.getName() + "<dark_gray>: <white>" + message));
            }
        }
        return true;
    }

    private boolean friend(Player player, String label, String[] args) {
        if (args.length == 0) {
            context.send(player, "<red>Usage: /" + label + " <add|remove|list> <player>");
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            List<String> friendNames = context.playerData().friends(player.getUniqueId()).stream()
                    .map(this::knownName)
                    .toList();
            context.send(player, friendNames.isEmpty()
                    ? "<gray>You do not have any friends added."
                    : "<green>Friends: <white>" + String.join("<gray>, <white>", friendNames));
            return true;
        }
        if (args.length < 2) {
            context.send(player, "<red>Usage: /" + label + " <add|remove|list> <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (args[0].equalsIgnoreCase("remove")) {
            boolean removed = context.playerData().removeFriend(player.getUniqueId(), target.getUniqueId());
            context.send(player, removed ? "<green>Removed friend." : "<red>That player was not on your friend list.");
        } else {
            context.playerData().addFriend(player.getUniqueId(), target.getUniqueId());
            context.send(player, "<green>Added friend.");
        }
        return true;
    }

    private void notifyFriends(Player player, boolean joined) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (context.playerData().friends(online.getUniqueId()).contains(player.getUniqueId())) {
                context.send(online, "<white>" + player.getName() + (joined ? " <green>joined" : " <red>left") + " <gray>the server.");
            }
        }
    }

    private String knownName(UUID playerId) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return player.getName() == null ? playerId.toString() : player.getName();
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
