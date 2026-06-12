package net.axther.hydroxide.modules.nickname;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.api.event.PlayerNicknameChangeEvent;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.Locale;

public final class NicknameModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

    private NicknameService service;
    private HydroxideContext context;
    private NicknameService.NicknamePolicy policy;

    @Override
    public String id() {
        return "nickname";
    }

    @Override
    public String displayName() {
        return "Nickname";
    }

    @Override
    public String description() {
        return "Formatted nicknames for chat, tab, and above-head name tags.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.service = new NicknameService(context.text());
        this.policy = loadPolicy(context);
        service.load(context.playerData());
        context.services().nicknameService(service);
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("nickname", this);
        context.commands().register("realname", this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            context.playerData().nickname(player.getUniqueId()).ifPresent(nickname -> service.apply(player, nickname));
        }
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        if (service != null) {
            service.shutdown();
            context.services().clearNicknameService(service);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "nickname" -> nickname(sender, label, args);
            case "realname" -> realName(sender, label, args);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("nickname") && args.length == 1) {
            java.util.ArrayList<String> suggestions = new java.util.ArrayList<>(List.of("clear"));
            if (sender.hasPermission("hydroxide.command.nickname.others")) {
                suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }
            return CommandUtils.matching(args[0], suggestions);
        }
        if (command.getName().equalsIgnoreCase("nickname") && args.length <= 2) {
            return CommandUtils.matching(args[args.length - 1], List.of("clear"));
        }
        return List.of();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        context.playerData().nickname(player.getUniqueId()).ifPresent(nickname -> service.apply(player, nickname));
    }

    private boolean nickname(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.nickname")) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " [player] <nickname|clear>");
            return true;
        }

        TargetAndNickname parsed = parseTarget(sender, args);
        if (parsed == null) {
            return true;
        }
        if (parsed.nickname().equalsIgnoreCase("clear")) {
            String oldNickname = context.playerData().nickname(parsed.target().getUniqueId()).orElse(null);
            PlayerNicknameChangeEvent event = new PlayerNicknameChangeEvent(parsed.target(), sender, oldNickname, null);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                context.send(sender, "<red>Nickname change was cancelled by another plugin.");
                return true;
            }
            context.playerData().removeNickname(parsed.target().getUniqueId());
            service.reset(parsed.target());
            context.send(sender, "<green>Nickname cleared for <white>" + parsed.target().getName() + "<green>.");
            return true;
        }

        if (!canUseFormatting(sender, parsed.nickname())) {
            return true;
        }
        NicknameService.ValidationResult validation = NicknameService.validateNickname(parsed.nickname(), policy);
        if (!validation.valid()) {
            context.send(sender, "<red>" + validation.message());
            return true;
        }

        String oldNickname = context.playerData().nickname(parsed.target().getUniqueId()).orElse(null);
        PlayerNicknameChangeEvent event = new PlayerNicknameChangeEvent(parsed.target(), sender, oldNickname, parsed.nickname());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            context.send(sender, "<red>Nickname change was cancelled by another plugin.");
            return true;
        }

        context.playerData().setNickname(parsed.target().getUniqueId(), parsed.target().getName(), parsed.nickname());
        service.apply(parsed.target(), parsed.nickname());
        context.send(sender, "<green>Nickname set for <white>" + parsed.target().getName() + "<green>.");
        if (!sender.equals(parsed.target())) {
            context.send(parsed.target(), "<green>Your nickname was changed to <white>" + parsed.nickname() + "<green>.");
        }
        return true;
    }

    private boolean realName(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.realname")) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " <nickname>");
            return true;
        }
        String nickname = CommandUtils.joinArgs(args, 0);
        service.realName(nickname)
                .ifPresentOrElse(
                        realName -> context.send(sender, "<green>That nickname belongs to <white>" + realName + "<green>."),
                        () -> context.send(sender, "<red>No active nickname matched <white>" + nickname + "<red>.")
                );
        return true;
    }

    private TargetAndNickname parseTarget(CommandSender sender, String[] args) {
        Player self = sender instanceof Player player ? player : null;
        if (self == null && args.length < 2) {
            context.send(sender, "<red>Console must specify a player.");
            return null;
        }

        Player possibleTarget = CommandUtils.onlinePlayer(args[0]).orElse(null);
        boolean usingOtherTarget = args.length >= 2 && possibleTarget != null
                && (sender.hasPermission("hydroxide.command.nickname.others") || self == null);
        if (usingOtherTarget) {
            if (!sender.hasPermission("hydroxide.command.nickname.others") && self != null) {
                context.send(sender, "<red>You do not have permission to nickname other players.");
                return null;
            }
            return new TargetAndNickname(possibleTarget, CommandUtils.joinArgs(args, 1));
        }

        if (self == null) {
            context.send(sender, "<red>That player is not online.");
            return null;
        }
        return new TargetAndNickname(self, CommandUtils.joinArgs(args, 0));
    }

    private NicknameService.NicknamePolicy loadPolicy(HydroxideContext context) {
        return new NicknameService.NicknamePolicy(
                context.plugin().getConfig().getInt("nickname.max-length", 16),
                context.plugin().getConfig().getString("nickname.allowed-pattern", "^[A-Za-z0-9_ .-]+$"),
                context.plugin().getConfig().getStringList("nickname.blacklist")
        );
    }

    private boolean canUseFormatting(CommandSender sender, String nickname) {
        if (NicknameService.requiresRainbowPermission(nickname)
                && !sender.hasPermission("hydroxide.nickname.rainbow")) {
            context.send(sender, "<red>You need <white>hydroxide.nickname.rainbow <red>to use rainbow nicknames.");
            return false;
        }
        if (NicknameService.requiresGradientPermission(nickname)
                && !sender.hasPermission("hydroxide.nickname.gradient")) {
            context.send(sender, "<red>You need <white>hydroxide.nickname.gradient <red>to use gradient nicknames.");
            return false;
        }
        if (NicknameService.requiresHexPermission(nickname)
                && !sender.hasPermission("hydroxide.nickname.hex")) {
            context.send(sender, "<red>You need <white>hydroxide.nickname.hex <red>to use hex nicknames.");
            return false;
        }
        if (NicknameService.requiresColorPermission(nickname)
                && !sender.hasPermission("hydroxide.nickname.color")) {
            context.send(sender, "<red>You need <white>hydroxide.nickname.color <red>to use colored nicknames.");
            return false;
        }
        return true;
    }

    private record TargetAndNickname(Player target, String nickname) {
    }
}
