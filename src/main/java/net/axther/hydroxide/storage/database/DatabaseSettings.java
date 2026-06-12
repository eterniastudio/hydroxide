package net.axther.hydroxide.storage.database;

import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.Locale;

public record DatabaseSettings(
        Type type,
        String sqliteFileName,
        String mysqlHost,
        int mysqlPort,
        String mysqlDatabase,
        String username,
        String password,
        boolean useSsl,
        PoolSettings pool
) {

    public enum Type {
        SQLITE,
        MYSQL
    }

    public static DatabaseSettings from(ConfigurationSection config) {
        Type type = config.getString("storage.type", "sqlite").toLowerCase(Locale.ROOT).equals("mysql")
                ? Type.MYSQL
                : Type.SQLITE;
        return new DatabaseSettings(
                type,
                config.getString("storage.sqlite.file-name", "database.db"),
                config.getString("storage.mysql.host", "localhost"),
                config.getInt("storage.mysql.port", 3306),
                config.getString("storage.mysql.database", "hydroxide"),
                config.getString("storage.mysql.username", "root"),
                config.getString("storage.mysql.password", ""),
                config.getBoolean("storage.mysql.use-ssl", false),
                new PoolSettings(
                        config.getInt("storage.pool.maximum-size", 10),
                        config.getInt("storage.pool.minimum-idle", 2),
                        config.getLong("storage.pool.connection-timeout-ms", 30000L),
                        config.getLong("storage.pool.idle-timeout-ms", 600000L),
                        config.getLong("storage.pool.max-lifetime-ms", 1800000L)
                )
        );
    }

    public static DatabaseSettings sqlite(String fileName) {
        return new DatabaseSettings(
                Type.SQLITE,
                fileName,
                "localhost",
                3306,
                "hydroxide",
                "root",
                "",
                false,
                PoolSettings.defaults()
        );
    }

    public String jdbcUrl(File dataFolder) {
        if (isSqlite()) {
            return "jdbc:sqlite:" + new File(dataFolder, sqliteFileName).getAbsolutePath();
        }
        return "jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase
                + "?useSSL=" + useSsl + "&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    }

    public String driverClassName() {
        return isSqlite() ? "org.sqlite.JDBC" : "com.mysql.cj.jdbc.Driver";
    }

    public boolean isSqlite() {
        return type == Type.SQLITE;
    }

    public int effectiveMaximumPoolSize() {
        return isSqlite() ? 1 : Math.max(1, pool.maximumSize());
    }

    public int effectiveMinimumIdle() {
        return Math.min(Math.max(0, pool.minimumIdle()), effectiveMaximumPoolSize());
    }

    public record PoolSettings(
            int maximumSize,
            int minimumIdle,
            long connectionTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs
    ) {
        public static PoolSettings defaults() {
            return new PoolSettings(10, 2, 30000L, 600000L, 1800000L);
        }
    }
}
