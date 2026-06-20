package net.axther.hydroxide.modules.teleport;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldCommandParserTest {

    @Test
    void parsesWorldOnlyAndOptionalTarget() {
        WorldCommandParser.Request self = WorldCommandParser.parse(List.of("normal")).orElseThrow();
        WorldCommandParser.Request target = WorldCommandParser.parse(List.of("nether", "Alex")).orElseThrow();

        assertEquals("normal", self.selector().input());
        assertTrue(self.targetName().isEmpty());
        assertEquals("Alex", target.targetName().orElseThrow());
        assertFalse(target.silent());
    }

    @Test
    void parsesSilentFlagAroundArguments() {
        WorldCommandParser.Request request = WorldCommandParser.parse(List.of("-s", "end", "Alex")).orElseThrow();

        assertEquals("Alex", request.targetName().orElseThrow());
        assertTrue(request.silent());
    }

    @Test
    void resolvesCmiStyleEnvironmentAliases() {
        assertEquals(World.Environment.NORMAL, WorldCommandParser.selector("normal").environment().orElseThrow());
        assertEquals(World.Environment.NETHER, WorldCommandParser.selector("2").environment().orElseThrow());
        assertEquals(World.Environment.THE_END, WorldCommandParser.selector("end").environment().orElseThrow());
        assertTrue(WorldCommandParser.selector("custom_world").environment().isEmpty());
    }

    @Test
    void rejectsMissingWorldOrExtraArguments() {
        assertTrue(WorldCommandParser.parse(List.of()).isEmpty());
        assertTrue(WorldCommandParser.parse(List.of("world", "Alex", "extra")).isEmpty());
    }
}
