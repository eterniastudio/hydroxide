package net.axther.hydroxide.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigResourceTest {

    @Test
    void declaresMaintenanceModuleToggle() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));

        assertTrue(yaml.isConfigurationSection("modules.maintenance"));
        assertTrue(yaml.isBoolean("modules.maintenance.enabled"));
        assertTrue(yaml.isConfigurationSection("modules.user-meta"));
        assertTrue(yaml.isBoolean("modules.user-meta.enabled"));
        assertTrue(yaml.isString("economy.cheques.signing-secret"));
        assertTrue(yaml.isInt("jail.default-duration-seconds"));
        assertTrue(yaml.isInt("admin-utilities.break.max-distance"));
        assertTrue(yaml.isInt("admin-utilities.antioch.fuse-ticks"));
        assertTrue(yaml.isDouble("admin-utilities.antioch.power"));
        assertTrue(yaml.isInt("admin-utilities.nuke.amount"));
        assertTrue(yaml.isDouble("admin-utilities.nuke.radius"));
        assertTrue(yaml.isDouble("admin-utilities.nuke.height"));
        assertTrue(yaml.isInt("admin-utilities.nuke.fuse-ticks"));
        assertTrue(yaml.isInt("admin-utilities.findbiome.default-radius"));
        assertTrue(yaml.isInt("admin-utilities.findbiome.max-radius"));
        assertTrue(yaml.isInt("admin-utilities.findbiome.horizontal-resolution"));
        assertTrue(yaml.isInt("admin-utilities.findbiome.vertical-resolution"));
        assertTrue(yaml.isList("command-control.disabled-commands"));
        assertTrue(yaml.isString("command-control.disabled-message"));
        assertTrue(yaml.isConfigurationSection("command-control.command-permissions"));
        assertTrue(yaml.isConfigurationSection("command-control.command-costs"));
        assertTrue(yaml.isConfigurationSection("command-control.command-warmups"));
        assertTrue(yaml.isConfigurationSection("command-control.command-cooldowns"));
        assertTrue(yaml.isBoolean("command-control.command-cooldown-persistence"));
        assertTrue(yaml.isConfigurationSection("command-control.command-worlds"));
        assertTrue(yaml.isConfigurationSection("command-control.command-aliases"));
        assertTrue(yaml.isString("chat.mutechat.default-duration"));
    }
}
