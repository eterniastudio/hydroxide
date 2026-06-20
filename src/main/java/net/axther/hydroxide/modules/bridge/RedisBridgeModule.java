package net.axther.hydroxide.modules.bridge;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RedisBridgeModule implements HydroModule, Listener {

    private HydroxideContext context;
    private ExecutorService executor;
    private volatile boolean running;

    @Override
    public String id() {
        return "redis-bridge";
    }

    @Override
    public String displayName() {
        return "Redis Bridge";
    }

    @Override
    public String description() {
        return "Optional Redis pub/sub bridge for multi-server chat synchronization.";
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        if (!context.plugin().getConfig().getBoolean("redis-bridge.enabled", false)) {
            context.plugin().getLogger().info(plain("redis-bridge.log.idle", Map.of()));
            return;
        }
        this.executor = Executors.newFixedThreadPool(2);
        this.running = true;
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        executor.submit(this::subscribeLoop);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        running = false;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (executor == null) {
            return;
        }
        BridgeMessage message = new BridgeMessage(
                serverId(),
                "chat",
                event.getPlayer().getName(),
                context.text().plain(event.message())
        );
        executor.submit(() -> publish(BridgeMessageCodec.encode(message)));
    }

    private void publish(String payload) {
        try (Socket socket = connect();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            writeCommand(writer, "PUBLISH", channel(), payload);
            writer.flush();
        } catch (IOException exception) {
            context.plugin().getLogger().warning(plain("redis-bridge.log.publish-failed", Map.of("reason", exception.getMessage())));
        }
    }

    private void subscribeLoop() {
        while (running) {
            try (Socket socket = connect();
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                writeCommand(writer, "SUBSCRIBE", channel());
                writer.flush();
                String line;
                while (running && (line = reader.readLine()) != null) {
                    if (!line.startsWith("{")) {
                        continue;
                    }
                    BridgeMessage message = BridgeMessageCodec.decode(line);
                    if (serverId().equals(message.serverId())) {
                        continue;
                    }
                    Bukkit.getScheduler().runTask(context.plugin(), () -> Bukkit.broadcast(context.messages().component(
                            "redis-bridge.chat-format",
                            Map.of("server", message.serverId(), "player", message.sender(), "message", message.message())
                    )));
                }
            } catch (IOException exception) {
                if (running) {
                    context.plugin().getLogger().warning(plain("redis-bridge.log.subscribe-failed", Map.of("reason", exception.getMessage())));
                    sleep();
                }
            }
        }
    }

    private Socket connect() throws IOException {
        Socket socket = new Socket(host(), port());
        socket.setSoTimeout((int) Duration.ofSeconds(30).toMillis());
        String password = context.plugin().getConfig().getString("redis-bridge.password", "");
        if (password != null && !password.isBlank()) {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            writeCommand(writer, "AUTH", password);
            writer.flush();
        }
        return socket;
    }

    private void writeCommand(BufferedWriter writer, String... parts) throws IOException {
        writer.write("*" + parts.length + "\r\n");
        for (String part : parts) {
            writer.write("$" + part.getBytes(StandardCharsets.UTF_8).length + "\r\n");
            writer.write(part + "\r\n");
        }
    }

    private void sleep() {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String host() {
        return context.plugin().getConfig().getString("redis-bridge.host", "127.0.0.1");
    }

    private int port() {
        return context.plugin().getConfig().getInt("redis-bridge.port", 6379);
    }

    private String channel() {
        return context.plugin().getConfig().getString("redis-bridge.channel", "hydroxide:chat");
    }

    private String serverId() {
        return context.plugin().getConfig().getString("redis-bridge.server-id", "server");
    }

    private String plain(String key, Map<String, ?> placeholders) {
        return context.text().plain(context.messages().component(key, placeholders));
    }
}
