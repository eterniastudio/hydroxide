package net.axther.hydroxide.modules.utility;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityInfoFormatterTest {

    @Test
    void formatsTargetEntityDetails() {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000123");
        Entity entity = (Entity) Proxy.newProxyInstance(
                EntityInfoFormatterTest.class.getClassLoader(),
                new Class<?>[]{Entity.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getType" -> EntityType.ZOMBIE;
                    case "getUniqueId" -> uuid;
                    case "getName" -> "Zombie";
                    case "getLocation" -> new Location(null, -3, 72, 18);
                    case "isValid" -> true;
                    case "isDead" -> false;
                    default -> null;
                }
        );

        EntityInfoFormatter.Details details = EntityInfoFormatter.details(entity);

        assertEquals("ZOMBIE", details.type());
        assertEquals("minecraft:zombie", details.key());
        assertEquals(uuid.toString(), details.uuid());
        assertEquals("Zombie", details.name());
        assertEquals("unknown:-3,72,18", details.location());
        assertEquals(true, details.valid());
        assertEquals(false, details.dead());
    }
}
