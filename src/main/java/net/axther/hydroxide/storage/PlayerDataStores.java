package net.axther.hydroxide.storage;

import net.axther.hydroxide.Hydroxide;
import net.axther.hydroxide.storage.database.DatabaseManager;
import net.axther.hydroxide.storage.database.DatabaseSettings;

import java.util.Locale;

public final class PlayerDataStores {

    private PlayerDataStores() {
    }

    public static PlayerDataStore create(Hydroxide plugin) {
        String type = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase(Locale.ROOT);
        if (type.equals("yaml")) {
            plugin.getLogger().info("Using YAML player storage.");
            return new YamlPlayerDataStore(plugin);
        }

        try {
            DatabaseSettings settings = DatabaseSettings.from(plugin.getConfig());
            DatabaseManager databaseManager = DatabaseManager.open(settings, plugin.getDataFolder());
            plugin.getLogger().info("Using " + settings.type().name().toLowerCase(Locale.ROOT) + " player storage.");
            return new SqlPlayerDataStore(databaseManager);
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("SQL storage failed to initialize; falling back to YAML: " + exception.getMessage());
            return new YamlPlayerDataStore(plugin);
        }
    }
}
