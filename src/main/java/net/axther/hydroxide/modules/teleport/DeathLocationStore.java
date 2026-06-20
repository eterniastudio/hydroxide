package net.axther.hydroxide.modules.teleport;

import net.axther.hydroxide.storage.StoredLocation;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Optional;
import java.util.UUID;

final class DeathLocationStore {

    private final YamlStore store;

    DeathLocationStore(YamlStore store) {
        this.store = store;
    }

    void remember(UUID playerId, String playerName, StoredLocation location) {
        YamlConfiguration yaml = store.load();
        write(yaml, playerId, playerName, location);
        store.save(yaml);
    }

    Optional<StoredLocation> lastDeath(UUID playerId) {
        return read(store.load(), playerId);
    }

    static void write(YamlConfiguration yaml, UUID playerId, String playerName, StoredLocation location) {
        String path = playerPath(playerId);
        yaml.set(path + ".name", playerName);
        location.writeTo(yaml.createSection(path + ".location"));
    }

    static Optional<StoredLocation> read(YamlConfiguration yaml, UUID playerId) {
        ConfigurationSection section = yaml.getConfigurationSection(playerPath(playerId) + ".location");
        return StoredLocation.readFrom(section);
    }

    private static String playerPath(UUID playerId) {
        return "players." + playerId;
    }
}
