package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerLocationFormatterTest {

    @Test
    void convertsBukkitYawToCompassDirection() {
        assertEquals("S", PlayerLocationFormatter.compassDirection(0.0F));
        assertEquals("W", PlayerLocationFormatter.compassDirection(90.0F));
        assertEquals("N", PlayerLocationFormatter.compassDirection(180.0F));
        assertEquals("E", PlayerLocationFormatter.compassDirection(270.0F));
        assertEquals("SW", PlayerLocationFormatter.compassDirection(45.0F));
        assertEquals("SE", PlayerLocationFormatter.compassDirection(-45.0F));
    }

    @Test
    void calculatesDepthRelativeToSeaLevel() {
        assertEquals(0, PlayerLocationFormatter.depth(63, 63));
        assertEquals(7, PlayerLocationFormatter.depth(70, 63));
        assertEquals(-5, PlayerLocationFormatter.depth(58, 63));
    }
}
