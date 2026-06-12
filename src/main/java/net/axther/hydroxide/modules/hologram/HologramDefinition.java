package net.axther.hydroxide.modules.hologram;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Optional;

public record HologramDefinition(
        String id,
        String worldName,
        double x,
        double y,
        double z,
        HologramDisplayType type,
        String payload,
        List<String> lines
) {

    public HologramDefinition(String id, String worldName, double x, double y, double z, List<String> lines) {
        this(id, worldName, x, y, z, HologramDisplayType.TEXT, "", lines);
    }

    public HologramDefinition {
        type = type == null ? HologramDisplayType.TEXT : type;
        payload = payload == null ? "" : payload;
        lines = List.copyOf(lines);
    }

    public void write(ConfigurationSection section) {
        section.set("world", worldName);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("type", type.key());
        section.set("payload", payload);
        section.set("lines", lines);
    }

    public static Optional<HologramDefinition> read(String id, ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        return Optional.of(new HologramDefinition(
                id,
                section.getString("world", "world"),
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                HologramDisplayType.from(section.getString("type", "text")).orElse(HologramDisplayType.TEXT),
                section.getString("payload", ""),
                section.getStringList("lines")
        ));
    }
}
