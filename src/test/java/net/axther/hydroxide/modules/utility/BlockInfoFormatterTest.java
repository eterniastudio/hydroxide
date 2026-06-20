package net.axther.hydroxide.modules.utility;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockInfoFormatterTest {

    @Test
    void formatsTargetBlockDetails() {
        Block block = (Block) Proxy.newProxyInstance(
                BlockInfoFormatterTest.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getType" -> Material.STONE;
                    case "getLocation" -> new Location(null, 12, 64, -7);
                    case "getLightLevel" -> (byte) 11;
                    default -> null;
                }
        );

        BlockInfoFormatter.Details details = BlockInfoFormatter.details(block);

        assertEquals("STONE", details.material());
        assertEquals("minecraft:stone", details.key());
        assertEquals("unknown:12,64,-7", details.location());
        assertEquals(11, details.light());
        assertEquals("unknown", details.biome());
        assertEquals(false, details.solid());
    }
}
