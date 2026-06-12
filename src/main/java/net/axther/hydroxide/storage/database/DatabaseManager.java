package net.axther.hydroxide.storage.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class DatabaseManager implements AutoCloseable {

    private final DatabaseSettings settings;
    private final HikariDataSource dataSource;
    private final ExecutorService executor;

    private DatabaseManager(DatabaseSettings settings, File dataFolder) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.executor = Executors.newFixedThreadPool(settings.effectiveMaximumPoolSize(), new DatabaseThreadFactory());
        this.dataSource = new HikariDataSource(hikariConfig(settings, dataFolder));
        setupTables();
    }

    public static DatabaseManager open(DatabaseSettings settings, File dataFolder) {
        return new DatabaseManager(settings, dataFolder);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean isSqlite() {
        return settings.isSqlite();
    }

    public CompletableFuture<Void> runAsync(SqlRunnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (SQLException exception) {
                throw new CompletionException(exception);
            }
        }, executor);
    }

    public <T> CompletableFuture<T> supplyAsync(SqlSupplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (SQLException exception) {
                throw new CompletionException(exception);
            }
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdown();
        dataSource.close();
    }

    private HikariConfig hikariConfig(DatabaseSettings settings, File dataFolder) {
        File parent = settings.isSqlite() ? new File(dataFolder, settings.sqliteFileName()).getAbsoluteFile().getParentFile() : null;
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create database directory " + parent.getAbsolutePath());
        }

        HikariConfig config = new HikariConfig();
        config.setPoolName("HydroxidePool");
        config.setJdbcUrl(settings.jdbcUrl(dataFolder));
        config.setDriverClassName(settings.driverClassName());
        config.setMaximumPoolSize(settings.effectiveMaximumPoolSize());
        config.setMinimumIdle(settings.effectiveMinimumIdle());
        config.setConnectionTimeout(settings.pool().connectionTimeoutMs());
        config.setIdleTimeout(settings.pool().idleTimeoutMs());
        config.setMaxLifetime(settings.pool().maxLifetimeMs());
        if (!settings.isSqlite()) {
            config.setUsername(settings.username());
            config.setPassword(settings.password());
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        }
        return config;
    }

    private void setupTables() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hydroxide_players (
                        uuid VARCHAR(36) PRIMARY KEY,
                        username VARCHAR(16) NOT NULL DEFAULT '',
                        nickname VARCHAR(255) DEFAULT NULL,
                        balance DOUBLE NOT NULL DEFAULT 0
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hydroxide_homes (
                        uuid VARCHAR(36) NOT NULL,
                        name VARCHAR(64) NOT NULL,
                        world VARCHAR(255) NOT NULL,
                        x DOUBLE NOT NULL,
                        y DOUBLE NOT NULL,
                        z DOUBLE NOT NULL,
                        yaw FLOAT NOT NULL,
                        pitch FLOAT NOT NULL,
                        PRIMARY KEY (uuid, name)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hydroxide_friends (
                        player_uuid VARCHAR(36) NOT NULL,
                        friend_uuid VARCHAR(36) NOT NULL,
                        PRIMARY KEY (player_uuid, friend_uuid)
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize Hydroxide database tables", exception);
        }
    }

    @FunctionalInterface
    public interface SqlRunnable {
        void run() throws SQLException;
    }

    @FunctionalInterface
    public interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    private static final class DatabaseThreadFactory implements ThreadFactory {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "Hydroxide-Database-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
