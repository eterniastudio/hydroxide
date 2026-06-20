package net.axther.hydroxide.modules.maintenance;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class MaintenanceModule implements HydroModule, Listener {

    private HydroxideContext context;
    private YamlStore store;

    @Override
    public String id() {
        return "maintenance";
    }

    @Override
    public String displayName() {
        return "Maintenance";
    }

    @Override
    public String description() {
        return "CMI-style maintenance mode with persisted state, custom message, and bypass permission.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "maintenance.yml"));
        seedDefault();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("maintenance", command());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onLogin(PlayerConnectionValidateLoginEvent event) {
        MaintenanceState state = state();
        if (!state.enabled()) {
            return;
        }
        if (hasLoginBypass(event)) {
            return;
        }
        event.kickMessage(context.messages().component("maintenance.kick", Map.of(
                "message", state.message()
        )));
    }

    private boolean hasLoginBypass(PlayerConnectionValidateLoginEvent event) {
        if (event.getConnection() instanceof PlayerLoginConnection loginConnection
                && loginConnection.getAuthenticatedProfile().getId() != null) {
            return Bukkit.getOfflinePlayer(loginConnection.getAuthenticatedProfile().getId()).isOp();
        }
        return false;
    }

    private CommandService command() {
        return new CommandService(HydroCommand.builder("maintenance")
                .permission("hydroxide.command.maintenance")
                .usage("/{label} [true|false|status] [message]")
                .executor(this::execute)
                .completer(this::complete)
                .build(), context.messages());
    }

    private List<String> complete(CommandContext context) {
        if (context.arguments().size() == 1) {
            return CommandUtils.matching(context.argument(0), List.of("true", "false", "on", "off", "status"));
        }
        return List.of();
    }

    private void execute(CommandContext commandContext) {
        MaintenanceCommandParser.Request request = MaintenanceCommandParser.parse(commandContext.arguments()).orElse(null);
        if (request == null) {
            context.message(commandContext.sender(), "maintenance.usage", Map.of("label", commandContext.label()));
            return;
        }

        switch (request.action()) {
            case STATUS -> sendStatus(commandContext.sender());
            case ENABLE -> setMaintenance(commandContext.sender(), true, request.message());
            case DISABLE -> setMaintenance(commandContext.sender(), false, request.message());
        }
    }

    private void sendStatus(CommandSender sender) {
        MaintenanceState state = state();
        context.message(sender, "maintenance.status", Map.of(
                "state", state.enabled()
                        ? context.messages().template("maintenance.state.enabled", "enabled")
                        : context.messages().template("maintenance.state.disabled", "disabled"),
                "message", state.message(),
                "updated_by", state.updatedBy().orElse("unknown"),
                "updated_at", state.updatedAt().map(Instant::toString).orElse("unknown")
        ));
    }

    private void setMaintenance(CommandSender sender, boolean enabled, java.util.Optional<String> message) {
        YamlConfiguration yaml = store.load();
        MaintenanceState current = MaintenanceState.from(yaml, defaultMessage());
        String nextMessage = message.orElse(current.message());
        MaintenanceState next = current.withEnabled(enabled, nextMessage, sender.getName(), Instant.now());
        next.writeTo(yaml);
        store.save(yaml);
        context.message(sender, enabled ? "maintenance.enabled" : "maintenance.disabled", Map.of(
                "message", next.message()
        ));
    }

    private MaintenanceState state() {
        return MaintenanceState.from(store.load(), defaultMessage());
    }

    private void seedDefault() {
        YamlConfiguration yaml = store.load();
        boolean changed = false;
        if (!yaml.isSet("enabled")) {
            yaml.set("enabled", false);
            changed = true;
        }
        if (!yaml.isSet("message")) {
            yaml.set("message", defaultMessage());
            changed = true;
        }
        if (changed) {
            store.save(yaml);
        }
    }

    private String defaultMessage() {
        return context.plugin().getConfig().getString("maintenance.default-message", "Server is currently in maintenance mode.");
    }
}
