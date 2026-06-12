package net.axther.hydroxide.modules.integration;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.integrations.placeholderapi.HydroxidePlaceholderExpansion;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;

import java.util.List;

public final class PlaceholderIntegrationModule implements HydroModule {

    private HydroxidePlaceholderExpansion expansion;

    @Override
    public String id() {
        return "placeholderapi";
    }

    @Override
    public String displayName() {
        return "PlaceholderAPI";
    }

    @Override
    public String description() {
        return "Registers Hydroxide placeholders for nicknames and economy balances.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            context.plugin().getLogger().info("PlaceholderAPI not found; placeholders are disabled.");
            return;
        }

        expansion = new HydroxidePlaceholderExpansion(context);
        if (expansion.register()) {
            context.plugin().getLogger().info("Registered PlaceholderAPI placeholders: %hydroxide_nickname%, %hydroxide_nickname_stripped%, %hydroxide_balance%.");
        }
    }

    @Override
    public void onDisable(HydroxideContext context) {
        if (expansion != null && expansion.isRegistered()) {
            expansion.unregister();
        }
    }
}
