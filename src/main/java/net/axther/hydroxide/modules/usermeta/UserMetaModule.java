package net.axther.hydroxide.modules.usermeta;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class UserMetaModule implements HydroModule {

    private HydroxideContext context;
    private UserMetaService service;

    @Override
    public String id() {
        return "user-meta";
    }

    @Override
    public String displayName() {
        return "User Meta";
    }

    @Override
    public String description() {
        return "CMI-style per-player custom metadata with command management and PlaceholderAPI access.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.service = new UserMetaService(new UserMetaStore(new YamlStore(new File(context.plugin().getDataFolder(), "user-meta.yml"))));
        context.services().userMetaService(service);
        context.commands().register("usermeta", command());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        context.services().clearUserMetaService(service);
    }

    private CommandService command() {
        return new CommandService(HydroCommand.builder("usermeta")
                .permission("hydroxide.command.usermeta")
                .usage("/{label} <player> <add|remove|clear|list|increment> [key] [value] [-s]")
                .executor(this::execute)
                .completer(this::complete)
                .build(), context.messages());
    }

    private List<String> complete(CommandContext commandContext) {
        List<String> args = commandContext.arguments();
        if (args.size() == 1) {
            return CommandUtils.matching(args.getFirst(), Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if (args.size() == 2) {
            return CommandUtils.matching(args.get(1), List.of("add", "remove", "clear", "list", "increment"));
        }
        if (args.size() == 3 && List.of("remove", "increment").contains(args.get(1).toLowerCase(java.util.Locale.ROOT))) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args.getFirst());
            return CommandUtils.matching(args.get(2), service.all(target.getUniqueId()).keySet());
        }
        if (args.size() >= 3 && "-s".startsWith(args.getLast().toLowerCase(java.util.Locale.ROOT))) {
            return List.of("-s");
        }
        return List.of();
    }

    private void execute(CommandContext commandContext) {
        UserMetaCommandParser.Request request = UserMetaCommandParser.parse(commandContext.arguments()).orElse(null);
        if (request == null) {
            context.message(commandContext.sender(), "user-meta.usage", Map.of("label", commandContext.label()));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(request.playerName());
        String targetName = targetName(target, request.playerName());
        switch (request.action()) {
            case ADD -> add(commandContext.sender(), target, targetName, request);
            case REMOVE -> remove(commandContext.sender(), target, targetName, request);
            case CLEAR -> clear(commandContext.sender(), target, targetName, request);
            case LIST -> list(commandContext.sender(), target, targetName);
            case INCREMENT -> increment(commandContext.sender(), target, targetName, request);
        }
    }

    private void add(CommandSender sender, OfflinePlayer target, String targetName, UserMetaCommandParser.Request request) {
        String key = request.key().orElseThrow();
        String value = request.value().orElseThrow();
        service.set(target.getUniqueId(), targetName, key, value);
        context.message(sender, "user-meta.added", Map.of("target", targetName, "key", key, "value", value));
        notifyTarget(sender, target, request);
    }

    private void remove(CommandSender sender, OfflinePlayer target, String targetName, UserMetaCommandParser.Request request) {
        String key = request.key().orElseThrow();
        if (!service.remove(target.getUniqueId(), key)) {
            context.message(sender, "user-meta.missing", Map.of("target", targetName, "key", key));
            return;
        }
        context.message(sender, "user-meta.removed", Map.of("target", targetName, "key", key));
        notifyTarget(sender, target, request);
    }

    private void clear(CommandSender sender, OfflinePlayer target, String targetName, UserMetaCommandParser.Request request) {
        int removed = service.clear(target.getUniqueId());
        context.message(sender, "user-meta.cleared", Map.of("target", targetName, "count", removed));
        notifyTarget(sender, target, request);
    }

    private void list(CommandSender sender, OfflinePlayer target, String targetName) {
        Map<String, String> entries = service.all(target.getUniqueId());
        if (entries.isEmpty()) {
            context.message(sender, "user-meta.empty", Map.of("target", targetName));
            return;
        }
        context.message(sender, "user-meta.header", Map.of("target", targetName, "count", entries.size()));
        entries.forEach((key, value) -> context.message(sender, "user-meta.entry", Map.of("key", key, "value", value)));
    }

    private void increment(CommandSender sender, OfflinePlayer target, String targetName, UserMetaCommandParser.Request request) {
        String key = request.key().orElseThrow();
        Optional<Double> amount = parseFiniteDouble(request.value().orElseThrow());
        if (amount.isEmpty()) {
            context.message(sender, "user-meta.invalid-number", Map.of("target", targetName, "key", key, "value", request.value().orElseThrow()));
            return;
        }
        Optional<Double> next = service.increment(target.getUniqueId(), targetName, key, amount.orElseThrow());
        if (next.isEmpty()) {
            context.message(sender, "user-meta.invalid-number", Map.of("target", targetName, "key", key, "value", service.value(target.getUniqueId(), key).orElse("")));
            return;
        }
        String value = service.value(target.getUniqueId(), key).orElse(String.valueOf(next.orElseThrow()));
        context.message(sender, "user-meta.incremented", Map.of("target", targetName, "key", key, "value", value));
        notifyTarget(sender, target, request);
    }

    private Optional<Double> parseFiniteDouble(String input) {
        try {
            double value = Double.parseDouble(input);
            return Double.isFinite(value) ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private void notifyTarget(CommandSender sender, OfflinePlayer target, UserMetaCommandParser.Request request) {
        if (request.silent() || !target.isOnline() || target.getPlayer() == null) {
            return;
        }
        Player player = target.getPlayer();
        if (sender instanceof Player senderPlayer && senderPlayer.getUniqueId().equals(player.getUniqueId())) {
            return;
        }
        context.message(player, "user-meta.changed-notice", Map.of("key", request.key().orElse("*")));
    }

    private String targetName(OfflinePlayer target, String fallback) {
        return target.getName() == null ? fallback : target.getName();
    }
}
