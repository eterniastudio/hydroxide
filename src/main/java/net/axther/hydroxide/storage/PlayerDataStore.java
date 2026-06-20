package net.axther.hydroxide.storage;

import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PlayerDataStore extends AutoCloseable {

    default void setHome(UUID playerId, String name, Location location) {
        setHome(playerId, name, StoredLocation.from(location));
    }

    void setHome(UUID playerId, String name, StoredLocation location);

    Optional<StoredLocation> home(UUID playerId, String name);

    boolean removeHome(UUID playerId, String name);

    List<String> homes(UUID playerId);

    void setNickname(UUID playerId, String playerName, String nickname);

    Optional<String> nickname(UUID playerId);

    void removeNickname(UUID playerId);

    Map<UUID, StoredNickname> nicknames();

    double balance(UUID playerId, double defaultBalance);

    void setBalance(UUID playerId, double balance);

    Map<UUID, Double> balances();

    List<UUID> friends(UUID playerId);

    void addFriend(UUID playerId, UUID friendId);

    boolean removeFriend(UUID playerId, UUID friendId);

    @Override
    default void close() {
    }

    record StoredNickname(UUID playerId, String playerName, String nickname) {
    }
}
