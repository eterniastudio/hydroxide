package net.axther.hydroxide.storage;

import net.axther.hydroxide.storage.database.DatabaseManager;
import net.axther.hydroxide.storage.database.StorageEngine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class SqlPlayerDataStore implements PlayerDataStore {

    private final StorageEngine engine;
    private final AutoCloseable closeable;

    public SqlPlayerDataStore(StorageEngine engine) {
        this(engine, () -> {
        });
    }

    public SqlPlayerDataStore(DatabaseManager databaseManager) {
        this(new StorageEngine(databaseManager), databaseManager);
    }

    private SqlPlayerDataStore(StorageEngine engine, AutoCloseable closeable) {
        this.engine = engine;
        this.closeable = closeable;
    }

    @Override
    public void setHome(UUID playerId, String name, StoredLocation location) {
        join(engine.saveHome(playerId, name, location));
    }

    @Override
    public Optional<StoredLocation> home(UUID playerId, String name) {
        return join(engine.getHome(playerId, name));
    }

    @Override
    public boolean removeHome(UUID playerId, String name) {
        return join(engine.removeHome(playerId, name));
    }

    @Override
    public List<String> homes(UUID playerId) {
        return join(engine.homes(playerId));
    }

    @Override
    public void setNickname(UUID playerId, String playerName, String nickname) {
        join(engine.setNickname(playerId, playerName, nickname));
    }

    @Override
    public Optional<String> nickname(UUID playerId) {
        return join(engine.getNickname(playerId));
    }

    @Override
    public void removeNickname(UUID playerId) {
        join(engine.removeNickname(playerId));
    }

    @Override
    public Map<UUID, StoredNickname> nicknames() {
        return join(engine.nicknames());
    }

    @Override
    public double balance(UUID playerId, double defaultBalance) {
        return join(engine.getBalance(playerId, defaultBalance));
    }

    @Override
    public void setBalance(UUID playerId, double balance) {
        join(engine.setBalance(playerId, balance));
    }

    @Override
    public List<UUID> friends(UUID playerId) {
        return join(engine.friends(playerId));
    }

    @Override
    public void addFriend(UUID playerId, UUID friendId) {
        join(engine.addFriend(playerId, friendId));
    }

    @Override
    public boolean removeFriend(UUID playerId, UUID friendId) {
        return join(engine.removeFriend(playerId, friendId));
    }

    @Override
    public void close() {
        try {
            closeable.close();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to close SQL player data store", exception);
        }
    }

    private <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }
}
