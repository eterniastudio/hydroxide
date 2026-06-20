package net.axther.hydroxide.modules.moderation;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationAliasParserTest {

    @Test
    void mapsGamemodeShortcutLabels() {
        assertEquals(GameMode.CREATIVE, ModerationAliasParser.gameModeFromLabel("gmc").orElseThrow());
        assertEquals(GameMode.SURVIVAL, ModerationAliasParser.gameModeFromLabel("gms").orElseThrow());
        assertEquals(GameMode.ADVENTURE, ModerationAliasParser.gameModeFromLabel("gma").orElseThrow());
        assertEquals(GameMode.SPECTATOR, ModerationAliasParser.gameModeFromLabel("gmsp").orElseThrow());
        assertTrue(ModerationAliasParser.gameModeFromLabel("gamemode").isEmpty());
    }

    @Test
    void mapsSpeedShortcutLabels() {
        assertEquals("fly", ModerationAliasParser.speedTypeFromLabel("flyspeed").orElseThrow());
        assertEquals("walk", ModerationAliasParser.speedTypeFromLabel("walkspeed").orElseThrow());
        assertTrue(ModerationAliasParser.speedTypeFromLabel("speed").isEmpty());
    }
}
