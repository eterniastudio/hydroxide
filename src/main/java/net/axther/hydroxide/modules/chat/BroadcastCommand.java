package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BroadcastCommand {

    private final HydroxideContext context;

    public BroadcastCommand(HydroxideContext context) {
        this.context = context;
    }

    public CommandService command() {
        return new CommandService(HydroCommand.builder("broadcast")
                .permission("hydroxide.command.broadcast")
                .usage("/{label} [!] <message> [-w:world,world] [-r:radius] [-c:world;x;y;z]")
                .executor(ctx -> broadcast(ctx.sender(), ctx.label(), ctx.arguments()))
                .build(), context.messages());
    }

    public CommandService meCommand() {
        return new CommandService(HydroCommand.builder("me")
                .permission("hydroxide.command.me")
                .usage("/{label} <action>")
                .executor(ctx -> me(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .build(), context.messages());
    }

    public CommandService clearChatCommand() {
        return new CommandService(HydroCommand.builder("clearchat")
                .permission("hydroxide.command.clearchat")
                .usage("/{label} [self] [-s]")
                .executor(ctx -> clearChat(ctx.sender(), ctx.label(), ctx.arguments()))
                .build(), context.messages());
    }

    private void broadcast(CommandSender sender, String label, List<String> args) {
        Optional<BroadcastCommandParser.Request> parsed = BroadcastCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "chat.broadcast.usage", Map.of("label", label));
            return;
        }

        BroadcastCommandParser.Request request = parsed.orElseThrow();
        Component component = context.messages().component(
                request.clean() ? "chat.broadcast.clean-format" : "chat.broadcast.format",
                Map.of("message", request.message())
        );
        if (!request.filtered()) {
            Bukkit.getServer().sendMessage(component);
            return;
        }

        List<Player> recipients = recipients(sender, request);
        if (recipients.isEmpty()) {
            context.message(sender, "chat.broadcast.no-recipients", Map.of());
            return;
        }
        for (Player recipient : recipients) {
            recipient.sendMessage(component);
        }
    }

    private List<Player> recipients(CommandSender sender, BroadcastCommandParser.Request request) {
        Location center = center(sender, request).orElse(null);
        double radiusSquared = request.radius().map(radius -> radius * radius).orElse(-1.0D);
        List<Player> recipients = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!request.worldNames().isEmpty()
                    && request.worldNames().stream().noneMatch(world -> world.equalsIgnoreCase(player.getWorld().getName()))) {
                continue;
            }
            if (center != null && !player.getWorld().equals(center.getWorld())) {
                continue;
            }
            if (radiusSquared >= 0.0D && (center == null || player.getLocation().distanceSquared(center) > radiusSquared)) {
                continue;
            }
            recipients.add(player);
        }
        return recipients;
    }

    private Optional<Location> center(CommandSender sender, BroadcastCommandParser.Request request) {
        if (request.center().isPresent()) {
            BroadcastCommandParser.Coordinates coordinates = request.center().orElseThrow();
            World world = Bukkit.getWorld(coordinates.worldName());
            if (world == null) {
                return Optional.empty();
            }
            return Optional.of(new Location(world, coordinates.x(), coordinates.y(), coordinates.z()));
        }
        if (request.radius().isPresent() && sender instanceof Player player) {
            return Optional.of(player.getLocation());
        }
        return Optional.empty();
    }

    private void me(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "chat.me.usage", Map.of("label", label));
            return;
        }
        String message = context.text().plain(context.text().format(CommandUtils.joinArgs(args, 0)));
        Component component = context.messages().component("chat.me.format", Map.of(
                "player", sender.getName(),
                "message", message
        ));
        Bukkit.getServer().sendMessage(component);
    }

    private void clearChat(CommandSender sender, String label, List<String> args) {
        Optional<ClearChatCommandParser.Request> parsed = ClearChatCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "chat.clearchat.usage", Map.of("label", label));
            return;
        }

        ClearChatCommandParser.Request request = parsed.orElseThrow();
        if (request.mode() == ClearChatCommandParser.Mode.SELF) {
            if (!(sender instanceof Player player)) {
                context.message(sender, "validation.player-only", Map.of());
                return;
            }
            clearChatFor(player);
            context.message(player, "chat.clearchat.self-done", Map.of());
            return;
        }

        Component blank = Component.empty();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("hydroxide.command.clearchat.bypass")) {
                continue;
            }
            for (int index = 0; index < 120; index++) {
                player.sendMessage(blank);
            }
        }
        if (!request.silent()) {
            Bukkit.getServer().sendMessage(context.messages().component("chat.clearchat.broadcast", Map.of("player", sender.getName())));
        }
        context.message(sender, "chat.clearchat.done", Map.of());
    }

    private void clearChatFor(Player player) {
        Component blank = Component.empty();
        for (int index = 0; index < 120; index++) {
            player.sendMessage(blank);
        }
    }
}
