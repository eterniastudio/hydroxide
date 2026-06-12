package net.axther.hydroxide.modules.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public final class EmbeddedApiModule implements HydroModule {

    private HydroxideContext context;
    private YamlStore store;
    private HttpServer server;
    private ApiAuthenticator authenticator;

    @Override
    public String id() {
        return "api";
    }

    @Override
    public String displayName() {
        return "Embedded API";
    }

    @Override
    public String description() {
        return "Secure embedded REST API for stats, player details, and remote console commands.";
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
        this.store = new YamlStore(new java.io.File(context.plugin().getDataFolder(), "api.yml"));
        seedDefaults();
        YamlConfiguration yaml = store.load();
        if (!yaml.getBoolean("enabled", false)) {
            context.plugin().getLogger().info("Embedded API module enabled but api.yml enabled=false; HTTP server is idle.");
            return;
        }
        Set<String> tokens = new LinkedHashSet<>(yaml.getStringList("tokens"));
        authenticator = new ApiAuthenticator(tokens);
        if (tokens.isEmpty() || tokens.contains("change-me")) {
            context.plugin().getLogger().warning("Embedded API refused to start: configure api.yml tokens first.");
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(
                    yaml.getString("bind", "127.0.0.1"),
                    yaml.getInt("port", 8765)
            ), 0);
            server.createContext("/stats", exchange -> handle(exchange, this::stats));
            server.createContext("/players", exchange -> handle(exchange, this::players));
            server.createContext("/command", this::command);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            context.plugin().getLogger().info("Hydroxide embedded API listening on " + yaml.getString("bind", "127.0.0.1") + ":" + yaml.getInt("port", 8765));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start Hydroxide embedded API", exception);
        }
    }

    @Override
    public void onDisable(HydroxideContext context) {
        if (server != null) {
            server.stop(1);
        }
    }

    private void handle(HttpExchange exchange, JsonSupplier supplier) throws IOException {
        if (!authorized(exchange)) {
            send(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            send(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        send(exchange, 200, supplier.get());
    }

    private void command(HttpExchange exchange) throws IOException {
        if (!authorized(exchange)) {
            send(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String command = readString(body, "command");
        if (command.isBlank()) {
            send(exchange, 400, "{\"error\":\"missing_command\"}");
            return;
        }
        Bukkit.getScheduler().runTask(context.plugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        send(exchange, 202, "{\"accepted\":true}");
    }

    private boolean authorized(HttpExchange exchange) {
        return authenticator != null && authenticator.authorized(exchange.getRequestHeaders().getFirst("Authorization"));
    }

    private String stats() {
        Runtime runtime = Runtime.getRuntime();
        double[] tps = Bukkit.getTPS();
        double mspt = Bukkit.getAverageTickTime();
        return "{"
                + "\"online\":" + Bukkit.getOnlinePlayers().size() + ","
                + "\"maxPlayers\":" + Bukkit.getMaxPlayers() + ","
                + "\"tps\":" + number(tps.length == 0 ? 20.0D : tps[0]) + ","
                + "\"mspt\":" + number(mspt) + ","
                + "\"memory\":{\"used\":" + (runtime.totalMemory() - runtime.freeMemory()) + ",\"max\":" + runtime.maxMemory() + "},"
                + "\"cpuLoad\":" + number(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage())
                + "}";
    }

    private String players() {
        StringBuilder builder = new StringBuilder("{\"players\":[");
        boolean first = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            String nickname = context.services().nicknameService()
                    .flatMap(service -> service.strippedNickname(player.getUniqueId()))
                    .orElse(player.getName());
            double balance = context.services().economy()
                    .map(economy -> economy.getBalance((OfflinePlayer) player))
                    .orElse(0.0D);
            builder.append("{\"uuid\":\"").append(player.getUniqueId()).append("\",")
                    .append("\"name\":\"").append(escape(player.getName())).append("\",")
                    .append("\"nickname\":\"").append(escape(nickname)).append("\",")
                    .append("\"balance\":").append(number(balance)).append('}');
        }
        return builder.append("]}").toString();
    }

    private String readString(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return "";
        }
        int colon = json.indexOf(':', keyIndex + marker.length());
        int start = json.indexOf('"', colon + 1);
        int end = json.indexOf('"', start + 1);
        return start < 0 || end < 0 ? "" : json.substring(start + 1, end).replace("\\\"", "\"");
    }

    private void send(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String number(double value) {
        return Double.isFinite(value) ? String.format(java.util.Locale.US, "%.2f", value) : "0.00";
    }

    private void seedDefaults() {
        YamlConfiguration yaml = store.load();
        if (yaml.contains("enabled")) {
            return;
        }
        yaml.set("enabled", false);
        yaml.set("bind", "127.0.0.1");
        yaml.set("port", 8765);
        yaml.set("tokens", List.of("change-me"));
        store.save(yaml);
    }

    private interface JsonSupplier {
        String get();
    }
}
