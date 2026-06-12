package net.axther.hydroxide.storage.database;

import net.axther.hydroxide.storage.PlayerDataStore;
import net.axther.hydroxide.storage.StoredLocation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StorageEngine {

    private final DatabaseManager database;

    public StorageEngine(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<Void> saveProfile(UUID uuid, String name, String nickname, double balance) {
        validateBalance(balance);
        return database.runAsync(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(profileUpsertSql())) {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                setNullableString(statement, 3, nickname);
                statement.setDouble(4, balance);
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> setNickname(UUID uuid, String playerName, String nickname) {
        return database.runAsync(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(nicknameUpsertSql())) {
                statement.setString(1, uuid.toString());
                statement.setString(2, playerName);
                setNullableString(statement, 3, nickname);
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> removeNickname(UUID uuid) {
        return database.runAsync(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE hydroxide_players SET nickname = NULL WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Optional<String>> getNickname(UUID uuid) {
        return database.supplyAsync(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT nickname FROM hydroxide_players WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return Optional.ofNullable(result.getString("nickname")).filter(value -> !value.isBlank());
                    }
                }
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<Map<UUID, PlayerDataStore.StoredNickname>> nicknames() {
        return database.supplyAsync(() -> {
            Map<UUID, PlayerDataStore.StoredNickname> nicknames = new HashMap<>();
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT uuid, username, nickname
                         FROM hydroxide_players
                         WHERE nickname IS NOT NULL AND nickname <> '' AND username <> ''
                         """);
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    UUID playerId = UUID.fromString(result.getString("uuid"));
                    nicknames.put(playerId, new PlayerDataStore.StoredNickname(
                            playerId,
                            result.getString("username"),
                            result.getString("nickname")
                    ));
                }
            }
            return Map.copyOf(nicknames);
        });
    }

    public CompletableFuture<Double> getBalance(UUID uuid, double fallback) {
        validateBalance(fallback);
        return database.supplyAsync(() -> {
            try (Connection connection = database.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("SELECT balance FROM hydroxide_players WHERE uuid = ?")) {
                    statement.setString(1, uuid.toString());
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            return result.getDouble("balance");
                        }
                    }
                }
                upsertBalance(connection, uuid, fallback);
                return fallback;
            }
        });
    }

    public CompletableFuture<Void> setBalance(UUID uuid, double balance) {
        validateBalance(balance);
        return database.runAsync(() -> {
            try (Connection connection = database.getConnection()) {
                upsertBalance(connection, uuid, balance);
            }
        });
    }

    public CompletableFuture<Void> saveHome(UUID uuid, String name, StoredLocation location) {
        return database.runAsync(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(homeUpsertSql())) {
                statement.setString(1, uuid.toString());
                statement.setString(2, normalizeName(name));
                statement.setString(3, location.worldName());
                statement.setDouble(4, location.x());
                statement.setDouble(5, location.y());
                statement.setDouble(6, location.z());
                statement.setFloat(7, location.yaw());
                statement.setFloat(8, location.pitch());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Optional<StoredLocation>> getHome(UUID uuid, String name) {
        return database.supplyAsync(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT world, x, y, z, yaw, pitch
                         FROM hydroxide_homes
                         WHERE uuid = ? AND name = ?
                         """)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, normalizeName(name));
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return Optional.of(new StoredLocation(
                                result.getString("world"),
                                result.getDouble("x"),
                                result.getDouble("y"),
                                result.getDouble("z"),
                                result.getFloat("yaw"),
                                result.getFloat("pitch")
                        ));
                    }
                }
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<Boolean> removeHome(UUID uuid, String name) {
        return database.supplyAsync(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM hydroxide_homes WHERE uuid = ? AND name = ?")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, normalizeName(name));
                return statement.executeUpdate() > 0;
            }
        });
    }

    public CompletableFuture<List<String>> homes(UUID uuid) {
        return database.supplyAsync(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT name FROM hydroxide_homes WHERE uuid = ? ORDER BY name")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    java.util.ArrayList<String> homes = new java.util.ArrayList<>();
                    while (result.next()) {
                        homes.add(result.getString("name"));
                    }
                    return List.copyOf(homes);
                }
            }
        });
    }

    public CompletableFuture<Void> addFriend(UUID playerId, UUID friendId) {
        return database.runAsync(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(friendUpsertSql())) {
                statement.setString(1, playerId.toString());
                statement.setString(2, friendId.toString());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Boolean> removeFriend(UUID playerId, UUID friendId) {
        return database.supplyAsync(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         DELETE FROM hydroxide_friends
                         WHERE player_uuid = ? AND friend_uuid = ?
                         """)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, friendId.toString());
                return statement.executeUpdate() > 0;
            }
        });
    }

    public CompletableFuture<List<UUID>> friends(UUID playerId) {
        return database.supplyAsync(() -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT friend_uuid
                         FROM hydroxide_friends
                         WHERE player_uuid = ?
                         ORDER BY friend_uuid
                         """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    java.util.ArrayList<UUID> friends = new java.util.ArrayList<>();
                    while (result.next()) {
                        friends.add(UUID.fromString(result.getString("friend_uuid")));
                    }
                    return List.copyOf(friends);
                }
            }
        });
    }

    private String profileUpsertSql() {
        if (database.isSqlite()) {
            return """
                    INSERT INTO hydroxide_players (uuid, username, nickname, balance)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        username = excluded.username,
                        nickname = excluded.nickname,
                        balance = excluded.balance
                    """;
        }
        return """
                INSERT INTO hydroxide_players (uuid, username, nickname, balance)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    username = VALUES(username),
                    nickname = VALUES(nickname),
                    balance = VALUES(balance)
                """;
    }

    private String nicknameUpsertSql() {
        if (database.isSqlite()) {
            return """
                    INSERT INTO hydroxide_players (uuid, username, nickname)
                    VALUES (?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        username = excluded.username,
                        nickname = excluded.nickname
                    """;
        }
        return """
                INSERT INTO hydroxide_players (uuid, username, nickname)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    username = VALUES(username),
                    nickname = VALUES(nickname)
                """;
    }

    private String balanceUpsertSql() {
        if (database.isSqlite()) {
            return """
                    INSERT INTO hydroxide_players (uuid, username, balance)
                    VALUES (?, '', ?)
                    ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance
                    """;
        }
        return """
                INSERT INTO hydroxide_players (uuid, username, balance)
                VALUES (?, '', ?)
                ON DUPLICATE KEY UPDATE balance = VALUES(balance)
                """;
    }

    private String homeUpsertSql() {
        if (database.isSqlite()) {
            return """
                    INSERT INTO hydroxide_homes (uuid, name, world, x, y, z, yaw, pitch)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uuid, name) DO UPDATE SET
                        world = excluded.world,
                        x = excluded.x,
                        y = excluded.y,
                        z = excluded.z,
                        yaw = excluded.yaw,
                        pitch = excluded.pitch
                    """;
        }
        return """
                INSERT INTO hydroxide_homes (uuid, name, world, x, y, z, yaw, pitch)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    world = VALUES(world),
                    x = VALUES(x),
                    y = VALUES(y),
                    z = VALUES(z),
                    yaw = VALUES(yaw),
                    pitch = VALUES(pitch)
                """;
    }

    private String friendUpsertSql() {
        if (database.isSqlite()) {
            return """
                    INSERT INTO hydroxide_friends (player_uuid, friend_uuid)
                    VALUES (?, ?)
                    ON CONFLICT(player_uuid, friend_uuid) DO NOTHING
                    """;
        }
        return """
                INSERT IGNORE INTO hydroxide_friends (player_uuid, friend_uuid)
                VALUES (?, ?)
                """;
    }

    private void upsertBalance(Connection connection, UUID uuid, double balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(balanceUpsertSql())) {
            statement.setString(1, uuid.toString());
            statement.setDouble(2, balance);
            statement.executeUpdate();
        }
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private void validateBalance(double balance) {
        if (balance < 0.0 || Double.isNaN(balance) || Double.isInfinite(balance)
                || BigDecimal.valueOf(balance).scale() > 2) {
            throw new IllegalArgumentException("Balance must be a finite non-negative amount with at most two decimals");
        }
    }
}
