package net.axther.hydroxide.modules.nickname;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.api.event.PlayerNicknameChangeEvent;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class NicknameModule implements HydroModule, Listener {

    private NicknameService service;
    private HydroxideContext context;
    private NicknameService.NicknamePolicy policy;
    private YamlStore nameplateYaml;
    private NameplateStore nameplateStore;

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
        this.nameplateYaml = new YamlStore(new File(context.plugin().getDataFolder(), "nameplates.yml"));
        this.nameplateStore = NameplateStore.from(nameplateYaml.load());
        nameplateStore.entries().values().forEach(entry ->
                service.cacheNameplate(entry.playerId(), entry.state()));
        context.services().nicknameService(service);
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("nickname", nicknameCommand());
        context.commands().register("realname", realNameCommand());
        context.commands().register("nameplate", nameplateCommand());

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyStoredDisplay(player);
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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyStoredDisplay(event.getPlayer());
    }

    private CommandService nicknameCommand() {
        return new CommandService(HydroCommand.builder("nickname")
                .permission("hydroxide.command.nickname")
                .usage("/{label} [player] <nickname|clear>")
                .executor(ctx -> nickname(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .completer(ctx -> nicknameCompletions(ctx.sender(), ctx.arguments()))
                .build(), context.messages());
    }

    private CommandService realNameCommand() {
        return new CommandService(HydroCommand.builder("realname")
                .permission("hydroxide.command.realname")
                .usage("/{label} <nickname>")
                .executor(ctx -> realName(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .build(), context.messages());
    }

    private CommandService nameplateCommand() {
        return new CommandService(HydroCommand.builder("nameplate")
                .permission("hydroxide.command.nameplate")
                .usage("/{label} [player] [-pref:<text>] [-suf:<text>] [-c:<color|reset>] [reset] [-s]")
                .executor(ctx -> nameplate(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> nameplateCompletions(ctx.sender(), ctx.arguments()))
                .build(), context.messages());
    }

    private List<String> nicknameCompletions(CommandSender sender, List<String> args) {
        if (args.size() <= 1) {
            java.util.ArrayList<String> suggestions = new java.util.ArrayList<>(List.of("clear"));
            if (sender.hasPermission("hydroxide.command.nickname.others")) {
                suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }
            return CommandUtils.matching(args.isEmpty() ? "" : args.get(0), suggestions);
        }
        if (args.size() == 2) {
            return CommandUtils.matching(args.get(1), List.of("clear"));
        }
        return List.of();
    }

    private List<String> nameplateCompletions(CommandSender sender, List<String> args) {
        String prefix = args.isEmpty() ? "" : args.getLast();
        if (args.size() <= 1) {
            java.util.ArrayList<String> suggestions = new java.util.ArrayList<>(List.of(
                    "reset",
                    "-pref:",
                    "-suf:",
                    "-c:red",
                    "-c:green",
                    "-c:blue",
                    "-c:reset",
                    "-s"
            ));
            suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(prefix, suggestions);
        }
        if (prefix.toLowerCase(java.util.Locale.ROOT).startsWith("-c:")) {
            String colorPrefix = prefix.substring(3);
            return NameplateCommandParser.parseColor(colorPrefix).isPresent()
                    ? List.of()
                    : CommandUtils.matching(prefix, List.of("-c:red", "-c:green", "-c:blue", "-c:gold", "-c:white", "-c:reset"));
        }
        return CommandUtils.matching(prefix, List.of("-pref:", "-suf:", "-c:", "reset", "-s"));
    }

    private boolean nickname(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.nickname")) {
            return true;
        }
        if (args.length == 0) {
            context.message(sender, "nickname.usage", Map.of("label", label));
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
                context.message(sender, "nickname.cancelled", Map.of());
                return true;
            }
            context.playerData().removeNickname(parsed.target().getUniqueId());
            service.reset(parsed.target());
            context.message(sender, "nickname.cleared", Map.of("target", parsed.target().getName()));
            return true;
        }

        if (!canUseFormatting(sender, parsed.nickname())) {
            return true;
        }
        NicknameService.ValidationResult validation = NicknameService.validateNickname(parsed.nickname(), policy);
        if (!validation.valid()) {
            context.message(sender, "nickname.validation-error", Map.of("reason", validation.message()));
            return true;
        }

        String oldNickname = context.playerData().nickname(parsed.target().getUniqueId()).orElse(null);
        PlayerNicknameChangeEvent event = new PlayerNicknameChangeEvent(parsed.target(), sender, oldNickname, parsed.nickname());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            context.message(sender, "nickname.cancelled", Map.of());
            return true;
        }

        context.playerData().setNickname(parsed.target().getUniqueId(), parsed.target().getName(), parsed.nickname());
        service.apply(parsed.target(), parsed.nickname());
        context.message(sender, "nickname.set", Map.of("target", parsed.target().getName(), "nickname", parsed.nickname()));
        if (!sender.equals(parsed.target())) {
            context.message(parsed.target(), "nickname.changed-notice", Map.of("nickname", parsed.nickname()));
        }
        return true;
    }

    private boolean nameplate(CommandSender sender, String label, List<String> args) {
        if (!context.requirePermission(sender, "hydroxide.command.nameplate")) {
            return true;
        }
        Optional<NameplateCommandParser.Request> parsed = NameplateCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "nickname.nameplate.usage", Map.of("label", label));
            return true;
        }
        NameplateCommandParser.Request request = parsed.orElseThrow();
        Player target = nameplateTarget(sender, request);
        if (target == null) {
            return true;
        }

        if (request.reset()) {
            nameplateStore.remove(target.getUniqueId());
            nameplateStore.save(nameplateYaml);
            service.resetNameplate(target);
            if (!request.silent()) {
                context.message(sender, "nickname.nameplate.reset", Map.of("target", target.getName()));
            }
            return true;
        }

        NicknameService.NameplateState state = nameplateStore.get(target.getUniqueId())
                .map(NameplateStore.StoredNameplate::state)
                .orElseGet(NicknameService.NameplateState::emptyState);
        if (request.prefix().isPresent()) {
            state = state.withPrefix(request.prefix().orElseThrow());
        }
        if (request.suffix().isPresent()) {
            state = state.withSuffix(request.suffix().orElseThrow());
        }
        if (request.colorProvided()) {
            state = request.color().isPresent()
                    ? state.withColor(request.color().orElseThrow())
                    : state.withoutColor();
        }

        nameplateStore.put(target.getUniqueId(), target.getName(), state);
        nameplateStore.save(nameplateYaml);
        service.applyNameplate(target, state);
        if (!request.silent()) {
            Map<String, Object> placeholders = Map.of(
                    "target", target.getName(),
                    "prefix", state.prefix().isBlank() ? "none" : state.prefix(),
                    "suffix", state.suffix().isBlank() ? "none" : state.suffix(),
                    "color", state.color().map(NameplateCommandParser::colorName).orElse("default")
            );
            context.message(sender, "nickname.nameplate.updated", placeholders);
            if (!sender.equals(target)) {
                context.message(target, "nickname.nameplate.target-notice", placeholders);
            }
        }
        return true;
    }

    private boolean realName(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.realname")) {
            return true;
        }
        if (args.length == 0) {
            context.message(sender, "nickname.realname.usage", Map.of("label", label));
            return true;
        }
        String nickname = CommandUtils.joinArgs(args, 0);
        service.realName(nickname)
                .ifPresentOrElse(
                        realName -> context.message(sender, "nickname.realname.found", Map.of("player", realName, "nickname", nickname)),
                        () -> context.message(sender, "nickname.realname.missing", Map.of("nickname", nickname))
                );
        return true;
    }

    private Player nameplateTarget(CommandSender sender, NameplateCommandParser.Request request) {
        if (request.targetName().isPresent()) {
            Player target = CommandUtils.onlinePlayer(request.targetName().orElseThrow()).orElse(null);
            if (target == null) {
                context.message(sender, "nickname.target-offline", Map.of());
            }
            return target;
        }
        if (sender instanceof Player player) {
            return player;
        }
        context.message(sender, "nickname.console-target-required", Map.of());
        return null;
    }

    private void applyStoredDisplay(Player player) {
        context.playerData().nickname(player.getUniqueId()).ifPresent(nickname -> service.apply(player, nickname));
        nameplateStore.get(player.getUniqueId()).ifPresent(nameplate -> service.applyNameplate(player, nameplate.state()));
    }

    private TargetAndNickname parseTarget(CommandSender sender, String[] args) {
        Player self = sender instanceof Player player ? player : null;
        if (self == null && args.length < 2) {
            context.message(sender, "nickname.console-target-required", Map.of());
            return null;
        }

        Player possibleTarget = CommandUtils.onlinePlayer(args[0]).orElse(null);
        boolean usingOtherTarget = args.length >= 2 && possibleTarget != null
                && (sender.hasPermission("hydroxide.command.nickname.others") || self == null);
        if (usingOtherTarget) {
            if (!sender.hasPermission("hydroxide.command.nickname.others") && self != null) {
                context.message(sender, "nickname.others-denied", Map.of());
                return null;
            }
            return new TargetAndNickname(possibleTarget, CommandUtils.joinArgs(args, 1));
        }

        if (self == null) {
            context.message(sender, "nickname.target-offline", Map.of());
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
            context.message(sender, "nickname.permission.rainbow", Map.of());
            return false;
        }
        if (NicknameService.requiresGradientPermission(nickname)
                && !sender.hasPermission("hydroxide.nickname.gradient")) {
            context.message(sender, "nickname.permission.gradient", Map.of());
            return false;
        }
        if (NicknameService.requiresHexPermission(nickname)
                && !sender.hasPermission("hydroxide.nickname.hex")) {
            context.message(sender, "nickname.permission.hex", Map.of());
            return false;
        }
        if (NicknameService.requiresColorPermission(nickname)
                && !sender.hasPermission("hydroxide.nickname.color")) {
            context.message(sender, "nickname.permission.color", Map.of());
            return false;
        }
        return true;
    }

    private record TargetAndNickname(Player target, String nickname) {
    }
}
