package net.axther.hydroxide.storage;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Optional;

public record StoredLocation(
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    public static StoredLocation from(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location must have a world");
        }
        return new StoredLocation(
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public static Optional<StoredLocation> readFrom(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        String worldName = section.getString("world");
        if (worldName == null || worldName.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new StoredLocation(
                worldName,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        ));
    }

    public void writeTo(ConfigurationSection section) {
        section.set("world", worldName);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("yaw", yaw);
        section.set("pitch", pitch);
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
