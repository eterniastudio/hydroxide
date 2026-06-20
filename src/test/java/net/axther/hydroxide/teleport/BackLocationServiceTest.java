package net.axther.hydroxide.teleport;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackLocationServiceTest {

    @Test
    void forgetRemovesStoredBackLocation() {
        BackLocationService service = new BackLocationService();
        UUID playerId = UUID.randomUUID();
        service.remember(playerId, new Location(world(), 1.0D, 64.0D, 2.0D));

        assertTrue(service.forget(playerId));
        assertTrue(service.previous(playerId).isEmpty());
    }

    @Test
    void forgetReturnsFalseWhenNoLocationWasStored() {
        BackLocationService service = new BackLocationService();

        assertFalse(service.forget(UUID.randomUUID()));
    }

    private World world() {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "world";
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> 1;
                    case "toString" -> "world";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
