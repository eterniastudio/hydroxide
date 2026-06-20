package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSoundCommandParserTest {

    @Test
    void parsesPlayerTargetPitchVolumeAndSilentFlag() {
        AdminSoundCommandParser.Request request = AdminSoundCommandParser.parse(List.of(
                "BLOCK_NOTE_BLOCK_CHIME", "-p:1.25", "-v:0.6", "Alex", "-s"
        )).orElseThrow();

        assertEquals("BLOCK_NOTE_BLOCK_CHIME", request.soundName());
        assertEquals(AdminSoundCommandParser.TargetType.PLAYER, request.targetType());
        assertEquals("Alex", request.targetName().orElseThrow());
        assertEquals(1.25F, request.pitch(), 0.0001F);
        assertEquals(0.6F, request.volume(), 0.0001F);
        assertTrue(request.silent());
    }

    @Test
    void parsesAllPlayersRadiusAndLocationTarget() {
        AdminSoundCommandParser.Request all = AdminSoundCommandParser.parse(List.of(
                "minecraft:block.note_block.chime", "-all", "-r:32"
        )).orElseThrow();
        AdminSoundCommandParser.Request location = AdminSoundCommandParser.parse(List.of(
                "entity.player.levelup", "-l:Steve"
        )).orElseThrow();

        assertEquals(AdminSoundCommandParser.TargetType.ALL, all.targetType());
        assertEquals(32.0D, all.radius().orElseThrow(), 0.0001D);
        assertEquals(AdminSoundCommandParser.TargetType.PLAYER_LOCATION, location.targetType());
        assertEquals("Steve", location.targetName().orElseThrow());
    }

    @Test
    void parsesCoordinateLocation() {
        AdminSoundCommandParser.Request request = AdminSoundCommandParser.parse(List.of(
                "entity.experience_orb.pickup", "world;10.5;64;-20"
        )).orElseThrow();

        assertEquals(AdminSoundCommandParser.TargetType.COORDINATES, request.targetType());
        assertEquals("world", request.coordinates().orElseThrow().worldName());
        assertEquals(10.5D, request.coordinates().orElseThrow().x(), 0.0001D);
        assertEquals(64.0D, request.coordinates().orElseThrow().y(), 0.0001D);
        assertEquals(-20.0D, request.coordinates().orElseThrow().z(), 0.0001D);
    }

    @Test
    void rejectsMissingSoundInvalidNumbersAndDuplicateTargets() {
        assertTrue(AdminSoundCommandParser.parse(List.of()).isEmpty());
        assertTrue(AdminSoundCommandParser.parse(List.of("block.note_block.chime", "-p:nope")).isEmpty());
        assertTrue(AdminSoundCommandParser.parse(List.of("block.note_block.chime", "-v:0")).isEmpty());
        assertTrue(AdminSoundCommandParser.parse(List.of("block.note_block.chime", "-r:-1")).isEmpty());
        assertTrue(AdminSoundCommandParser.parse(List.of("block.note_block.chime", "Alex", "Steve")).isEmpty());
        assertTrue(AdminSoundCommandParser.parse(List.of("block.note_block.chime", "-all", "-l:Alex")).isEmpty());
    }
}
