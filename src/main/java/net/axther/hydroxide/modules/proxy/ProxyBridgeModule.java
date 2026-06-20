package net.axther.hydroxide.modules.proxy;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class ProxyBridgeModule implements HydroModule, PluginMessageListener {

    private static final String CHANNEL = "BungeeCord";
    private static final String ALERT_SUBCHANNEL = "HydroxideAlert";
    private HydroxideContext context;

    @Override
    public String id() {
        return "proxy-bridge";
    }

    @Override
    public String displayName() {
        return "Proxy Bridge";
    }

    @Override
    public String description() {
        return "BungeeCord/Velocity plugin messaging for server hops and network alerts.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        Bukkit.getMessenger().registerOutgoingPluginChannel(context.plugin(), CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(context.plugin(), CHANNEL, this);
        context.commands().register("server", serverCommand());
        context.commands().register("serverlist", serverListCommand());
        context.commands().register("sendall", sendAllCommand());
        context.commands().register("networkalert", networkAlertCommand());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(context.plugin(), CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(context.plugin(), CHANNEL, this);
    }

    private CommandService serverCommand() {
        return new CommandService(HydroCommand.builder("server")
                .permission("hydroxide.command.server")
                .usage("/{label} <server> [player] [-f]")
                .executor(ctx -> server(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::serverCompletions)
                .build(), context.messages());
    }

    private CommandService networkAlertCommand() {
        return new CommandService(HydroCommand.builder("networkalert")
                .permission("hydroxide.command.networkalert")
                .usage("/{label} <message>")
                .executor(ctx -> networkAlert(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .build(), context.messages());
    }

    private CommandService serverListCommand() {
        return new CommandService(HydroCommand.builder("serverlist")
                .permission("hydroxide.command.serverlist")
                .usage("/{label} [filter]")
                .executor(ctx -> serverList(ctx.sender(), ctx.arguments()))
                .build(), context.messages());
    }

    private CommandService sendAllCommand() {
        return new CommandService(HydroCommand.builder("sendall")
                .permission("hydroxide.command.sendall")
                .usage("/{label} <server>")
                .executor(ctx -> sendAll(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> ctx.arguments().size() == 1
                        ? CommandUtils.matching(ctx.argument(0), context.plugin().getConfig().getStringList("proxy.servers"))
                        : List.of())
                .build(), context.messages());
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(message));
            String subchannel = input.readUTF();
            if (subchannel.equals("Forward")) {
                input.readUTF();
                String childChannel = input.readUTF();
                short length = input.readShort();
                byte[] payload = new byte[length];
                input.readFully(payload);
                if (childChannel.equals(ALERT_SUBCHANNEL)) {
                    broadcast(ProxyMessageCodec.decode(payload).payload());
                }
            } else if (subchannel.equals(ALERT_SUBCHANNEL)) {
                broadcast(input.readUTF());
            }
        } catch (IOException | IllegalArgumentException exception) {
            context.plugin().getLogger().warning("Unable to parse proxy plugin message: " + exception.getMessage());
        }
    }

    private List<String> serverCompletions(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), context.plugin().getConfig().getStringList("proxy.servers"));
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new java.util.ArrayList<>(net.axther.hydroxide.commands.CompletionUtils.onlinePlayers(ctx.argument(1)));
            values.addAll(CommandUtils.matching(ctx.argument(1), List.of("-f")));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(ctx.argument(2), List.of("-f"));
        }
        return List.of();
    }

    private void server(CommandSender sender, String label, List<String> args) {
        var parsed = ProxyServerCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "proxy.server.usage", Map.of("label", label));
            return;
        }

        ProxyServerCommandParser.Request request = parsed.orElseThrow();
        Player target;
        if (request.targetName().isPresent()) {
            String targetName = request.targetName().orElseThrow();
            target = CommandUtils.onlinePlayer(targetName).orElse(null);
            if (target == null) {
                context.message(sender, "proxy.server.player-offline", Map.of("target", targetName));
                return;
            }
            if (!sender.equals(target) && !sender.hasPermission("hydroxide.command.server.others")) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.server.others"));
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                context.message(sender, "proxy.server.console-target-required", Map.of("label", label));
                return;
            }
            target = player;
        }

        try {
            target.sendPluginMessage(context.plugin(), CHANNEL, connectPayload(request.server()));
            if (sender.equals(target)) {
                context.message(sender, "proxy.server.sending", Map.of("server", request.server()));
            } else {
                context.message(sender, "proxy.server.sending-other", Map.of(
                        "server", request.server(),
                        "target", target.getName()
                ));
                if (!request.force()) {
                    context.message(target, "proxy.server.target", Map.of(
                            "server", request.server(),
                            "player", sender.getName()
                    ));
                }
            }
        } catch (IOException exception) {
            context.message(sender, "proxy.server.failed", Map.of());
        }
    }

    private void serverList(CommandSender sender, List<String> args) {
        String filter = args.isEmpty() ? "" : CommandUtils.joinArgs(args.toArray(String[]::new), 0);
        ProxyServerListFormatter.Snapshot snapshot = ProxyServerListFormatter.snapshot(
                context.plugin().getConfig().getStringList("proxy.servers"),
                filter
        );
        if (snapshot.servers().isEmpty()) {
            context.message(sender, "proxy.serverlist.empty", Map.of(
                    "filter", filter.isBlank() ? "*" : filter,
                    "total", snapshot.totalCount()
            ));
            return;
        }
        context.message(sender, "proxy.serverlist.header", Map.of(
                "filter", filter.isBlank() ? "*" : filter,
                "shown", snapshot.shownCount(),
                "total", snapshot.totalCount()
        ));
        snapshot.servers().forEach(server -> context.message(sender, "proxy.serverlist.entry", Map.of("server", server)));
    }

    private void sendAll(CommandSender sender, String label, List<String> args) {
        var parsed = ProxySendAllCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "proxy.sendall.usage", Map.of("label", label));
            return;
        }
        List<Player> players = List.copyOf(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) {
            context.message(sender, "proxy.sendall.empty", Map.of());
            return;
        }

        String server = parsed.orElseThrow().server();
        int sent = 0;
        try {
            byte[] payload = connectPayload(server);
            for (Player player : players) {
                player.sendPluginMessage(context.plugin(), CHANNEL, payload);
                sent++;
            }
        } catch (IOException | RuntimeException exception) {
            context.message(sender, "proxy.sendall.failed", Map.of("server", server, "count", sent));
            return;
        }

        context.message(sender, "proxy.sendall.sending", Map.of(
                "server", server,
                "count", sent
        ));
    }

    private byte[] connectPayload(String server) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeUTF("Connect");
        output.writeUTF(server);
        return bytes.toByteArray();
    }

    private void networkAlert(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "proxy.network-alert.usage", Map.of("label", label));
            return;
        }
        String message = CommandUtils.joinArgs(args, 0);
        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) {
            context.message(sender, "proxy.network-alert.no-carrier", Map.of());
            return;
        }
        sendForward(carrier, message);
        broadcast(message);
    }

    private void sendForward(Player carrier, String message) {
        try {
            byte[] payload = ProxyMessageCodec.encode(new ProxyMessage(ALERT_SUBCHANNEL, "ALL", message));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeUTF("Forward");
            output.writeUTF("ALL");
            output.writeUTF(ALERT_SUBCHANNEL);
            output.writeShort(payload.length);
            output.write(payload);
            carrier.sendPluginMessage(context.plugin(), CHANNEL, bytes.toByteArray());
        } catch (IOException exception) {
            context.plugin().getLogger().warning("Unable to send proxy alert: " + exception.getMessage());
        }
    }

    private void broadcast(String message) {
        String sanitized = context.text().plain(context.text().format(message));
        Bukkit.broadcast(context.messages().component("proxy.network-alert.format", Map.of("message", sanitized)));
    }
}
