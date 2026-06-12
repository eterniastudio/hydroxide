package net.axther.hydroxide.teleport;

import net.axther.hydroxide.storage.StoredLocation;
import org.bukkit.Location;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BackLocationService {

    private final Map<UUID, StoredLocation> previousLocations = new ConcurrentHashMap<>();

    public void remember(UUID playerId, Location location) {
        previousLocations.put(playerId, StoredLocation.from(location));
    }

    public Optional<StoredLocation> previous(UUID playerId) {
        return Optional.ofNullable(previousLocations.get(playerId));
    }
}
