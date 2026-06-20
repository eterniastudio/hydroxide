package net.axther.hydroxide.modules.social;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
import java.util.Map;
import java.util.UUID;

public final class SocialModule implements HydroModule, Listener {

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
        context.commands().register("party", partyCommand());
        context.commands().register("p", partyChatCommand());
        context.commands().register("friend", friendCommand());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    private CommandService partyCommand() {
        return new CommandService(HydroCommand.builder("party")
                .permission("hydroxide.command.party")
                .playerOnly(true)
                .usage("/{label} <invite|accept|leave|ff|chat> ...")
                .executor(ctx -> party((Player) ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> partyCompletions(ctx.arguments()))
                .build(), context.messages());
    }

    private CommandService partyChatCommand() {
        return new CommandService(HydroCommand.builder("p")
                .permission("hydroxide.command.party")
                .playerOnly(true)
                .usage("/{label} <message>")
                .executor(ctx -> partyChat((Player) ctx.sender(), ctx.arguments()))
                .build(), context.messages());
    }

    private CommandService friendCommand() {
        return new CommandService(HydroCommand.builder("friend")
                .permission("hydroxide.command.friend")
                .playerOnly(true)
                .usage("/{label} <add|remove|list> <player>")
                .executor(ctx -> friend((Player) ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> friendCompletions(ctx.arguments()))
                .build(), context.messages());
    }

    private List<String> partyCompletions(List<String> args) {
        if (args.size() <= 1) {
            return CommandUtils.matching(args.isEmpty() ? "" : args.get(0), PARTY_ACTIONS);
        }
        if (args.size() == 2 && args.get(0).equalsIgnoreCase("invite")) {
            return net.axther.hydroxide.commands.CompletionUtils.onlinePlayers(args.get(1));
        }
        if (args.size() == 2 && args.get(0).equalsIgnoreCase("ff")) {
            return CommandUtils.matching(args.get(1), List.of("off", "on"));
        }
        return List.of();
    }

    private List<String> friendCompletions(List<String> args) {
        if (args.size() <= 1) {
            return CommandUtils.matching(args.isEmpty() ? "" : args.get(0), FRIEND_ACTIONS);
        }
        if (args.size() == 2 && (args.get(0).equalsIgnoreCase("add") || args.get(0).equalsIgnoreCase("remove"))) {
            return net.axther.hydroxide.commands.CompletionUtils.onlinePlayers(args.get(1));
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

    private void party(Player player, String label, List<String> args) {
        if (args.isEmpty()) {
            context.message(player, "social.party.usage", Map.of("label", label));
            return;
        }
        switch (args.get(0).toLowerCase(Locale.ROOT)) {
            case "invite" -> {
                Player target = args.size() > 1 ? Bukkit.getPlayerExact(args.get(1)) : null;
                if (target == null) {
                    context.message(player, "social.party.player-offline", Map.of());
                    return;
                }
                parties.invite(player.getUniqueId(), target.getUniqueId());
                context.message(player, "social.party.invited", Map.of("target", target.getName()));
                context.message(target, "social.party.invite-received", Map.of("player", player.getName()));
            }
            case "accept" -> context.message(player, parties.accept(player.getUniqueId())
                    ? "social.party.joined"
                    : "social.party.no-invite", Map.of());
            case "leave" -> {
                parties.leave(player.getUniqueId());
                context.message(player, "social.party.left", Map.of());
            }
            case "ff" -> {
                boolean allowed = args.size() > 1 && args.get(1).equalsIgnoreCase("on");
                parties.setFriendlyFire(player.getUniqueId(), allowed);
                context.message(player, "social.party.friendly-fire", Map.of("state", allowed ? "on" : "off"));
            }
            case "chat" -> partyChat(player, args.subList(1, args.size()));
            default -> context.message(player, "social.party.usage", Map.of("label", label));
        }
    }

    private void partyChat(Player player, List<String> args) {
        if (args.isEmpty()) {
            context.message(player, "social.party.chat-usage", Map.of("label", "p"));
            return;
        }
        PartyService.Party party = parties.party(player.getUniqueId()).orElse(null);
        if (party == null) {
            context.message(player, "social.party.not-in-party", Map.of());
            return;
        }
        String message = CommandUtils.joinArgs(args.toArray(String[]::new), 0);
        Component rendered = context.messages().component("social.party.chat-format", Map.of(
                "player", player.getName(),
                "message", message
        ));
        for (UUID memberId : party.members()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(rendered);
            }
        }
    }

    private void friend(Player player, String label, List<String> args) {
        if (args.isEmpty()) {
            context.message(player, "social.friend.usage", Map.of("label", label));
            return;
        }
        if (args.get(0).equalsIgnoreCase("list")) {
            List<String> friendNames = context.playerData().friends(player.getUniqueId()).stream()
                    .map(this::knownName)
                    .toList();
            context.message(player, friendNames.isEmpty() ? "social.friend.empty" : "social.friend.list",
                    Map.of("friends", String.join("<gray>, <white>", friendNames)));
            return;
        }
        if (args.size() < 2) {
            context.message(player, "social.friend.usage", Map.of("label", label));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args.get(1));
        switch (args.get(0).toLowerCase(Locale.ROOT)) {
            case "remove" -> {
                boolean removed = context.playerData().removeFriend(player.getUniqueId(), target.getUniqueId());
                context.message(player, removed ? "social.friend.removed" : "social.friend.not-friend",
                        Map.of("target", knownName(target.getUniqueId())));
            }
            case "add" -> {
                context.playerData().addFriend(player.getUniqueId(), target.getUniqueId());
                context.message(player, "social.friend.added", Map.of("target", knownName(target.getUniqueId())));
            }
            default -> context.message(player, "social.friend.usage", Map.of("label", label));
        }
    }

    private void notifyFriends(Player player, boolean joined) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (context.playerData().friends(online.getUniqueId()).contains(player.getUniqueId())) {
                context.message(online, joined ? "social.friend.notify-join" : "social.friend.notify-quit",
                        Map.of("player", player.getName()));
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
