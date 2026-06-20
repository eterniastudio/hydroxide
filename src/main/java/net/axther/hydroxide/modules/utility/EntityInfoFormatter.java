package net.axther.hydroxide.modules.utility;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

final class EntityInfoFormatter {

    private EntityInfoFormatter() {
    }

    static Details details(Entity entity) {
        return new Details(
                entity.getType().name(),
                entity.getType().key().asString(),
                entity.getUniqueId().toString(),
                entity.getName(),
                location(entity.getLocation()),
                entity.isValid(),
                entity.isDead()
        );
    }

    private static String location(Location location) {
        String world = location.getWorld() == null ? "unknown" : location.getWorld().getName();
        return world + ":" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    record Details(String type, String key, String uuid, String name, String location, boolean valid, boolean dead) {
    }
}
