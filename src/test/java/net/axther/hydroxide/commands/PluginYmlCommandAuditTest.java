package net.axther.hydroxide.commands;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginYmlCommandAuditTest {

    @Test
    void vanishUsageDocumentsRepairAndStatusSubcommands() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        String usage = pluginYml.getString("commands.vanish.usage", "");

        assertTrue(usage.contains("fix"), "vanish usage should document /vanish fix [player]");
        assertTrue(usage.contains("status"), "vanish usage should document /vanish status [player]");
    }
}
