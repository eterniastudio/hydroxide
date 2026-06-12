package net.axther.hydroxide.modules.proxy;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class ProxyBridgeModule implements HydroModule, PluginMessageListener, CommandExecutor, TabCompleter {

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
        context.commands().register("server", this);
        context.commands().register("networkalert", this);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(context.plugin(), CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(context.plugin(), CHANNEL, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("server")) {
            return server(sender, label, args);
        }
        return networkAlert(sender, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("server") && args.length == 1) {
            return CommandUtils.matching(args[0], context.plugin().getConfig().getStringList("proxy.servers"));
        }
        return List.of();
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

    private boolean server(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.server")) {
            return true;
        }
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can switch servers.");
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " <server>");
            return true;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeUTF("Connect");
            output.writeUTF(args[0]);
            player.sendPluginMessage(context.plugin(), CHANNEL, bytes.toByteArray());
            context.send(player, "<green>Sending you to <white>" + args[0] + "<green>.");
        } catch (IOException exception) {
            context.send(player, "<red>Unable to send proxy message.");
        }
        return true;
    }

    private boolean networkAlert(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.networkalert")) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " <message>");
            return true;
        }
        String message = CommandUtils.joinArgs(args, 0);
        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) {
            context.send(sender, "<red>A player must be online to send plugin messages.");
            return true;
        }
        sendForward(carrier, message);
        broadcast(message);
        return true;
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
        Bukkit.broadcast(context.text().format("<dark_gray>[<#44CCFF>Network</#44CCFF>] <white>" + message));
    }
}
