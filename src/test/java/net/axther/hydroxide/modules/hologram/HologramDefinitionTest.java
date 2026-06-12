package net.axther.hydroxide.modules.hologram;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HologramDefinitionTest {

    @Test
    void serializesLinesAndLocation() {
        HologramDefinition definition = new HologramDefinition("spawn", "world", 1.5, 64.0, -2.5, List.of("<green>Hello"));
        YamlConfiguration yaml = new YamlConfiguration();

        definition.write(yaml.createSection("holograms.spawn"));

        assertEquals(definition, HologramDefinition.read("spawn", yaml.getConfigurationSection("holograms.spawn")).orElseThrow());
    }

    @Test
    void serializesDisplayTypeAndPayload() {
        HologramDefinition definition = new HologramDefinition(
                "diamond",
                "world",
                1.5,
                64.0,
                -2.5,
                HologramDisplayType.ITEM,
                "diamond",
                List.of()
        );
        YamlConfiguration yaml = new YamlConfiguration();

        definition.write(yaml.createSection("holograms.diamond"));

        assertEquals(definition, HologramDefinition.read("diamond", yaml.getConfigurationSection("holograms.diamond")).orElseThrow());
    }
}
