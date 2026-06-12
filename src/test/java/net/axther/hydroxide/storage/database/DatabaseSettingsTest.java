package net.axther.hydroxide.storage.database;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSettingsTest {

    @Test
    void defaultsToLocalSqliteStorage() {
        YamlConfiguration yaml = new YamlConfiguration();

        DatabaseSettings settings = DatabaseSettings.from(yaml);

        assertEquals(DatabaseSettings.Type.SQLITE, settings.type());
        assertEquals("database.db", settings.sqliteFileName());
        assertTrue(settings.jdbcUrl(new File("plugins/Hydroxide")).replace('\\', '/')
                .endsWith("plugins/Hydroxide/database.db"));
        assertEquals(1, settings.effectiveMaximumPoolSize());
    }

    @Test
    void buildsMysqlJdbcUrlWithSslFlag() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("storage.type", "mysql");
        yaml.set("storage.mysql.host", "db.example.test");
        yaml.set("storage.mysql.port", 3307);
        yaml.set("storage.mysql.database", "hydroxide_network");
        yaml.set("storage.mysql.username", "hydroxide");
        yaml.set("storage.mysql.password", "secret");
        yaml.set("storage.mysql.use-ssl", true);
        yaml.set("storage.pool.maximum-size", 12);

        DatabaseSettings settings = DatabaseSettings.from(yaml);

        assertEquals(DatabaseSettings.Type.MYSQL, settings.type());
        assertEquals("hydroxide", settings.username());
        assertEquals("secret", settings.password());
        assertEquals(12, settings.effectiveMaximumPoolSize());
        assertEquals("jdbc:mysql://db.example.test:3307/hydroxide_network"
                + "?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true", settings.jdbcUrl(new File(".")));
        assertFalse(settings.isSqlite());
    }
}
