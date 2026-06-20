package net.axther.hydroxide.modules.admin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminGiveCommandParserTest {

    @Test
    void parsesGiveTargetMaterialAmountAndCmiStyleItemFlags() {
        AdminGiveCommandParser.Request request = AdminGiveCommandParser
                .parseGive(List.of("Steve", "minecraft:diamond_sword", "2", "n", "VIP", "Blade", "l", "Line", "one|Line", "two", "e", "sharpness:5", "unbreakable", "-slot:4", "-s"))
                .orElseThrow();

        assertEquals("Steve", request.targetName().orElseThrow());
        assertEquals(Material.DIAMOND_SWORD, request.material());
        assertEquals(2, request.amount());
        assertEquals("VIP Blade", request.name().orElseThrow());
        assertEquals(List.of("Line one", "Line two"), request.lore());
        assertEquals(List.of(new AdminGiveCommandParser.EnchantmentSpec("sharpness", 5)), request.enchantments());
        assertTrue(request.unbreakable());
        assertEquals(4, request.slot().orElseThrow());
        assertTrue(request.silent());
    }

    @Test
    void parsesGiveAllWithDefaultAmountAndUnstackFlag() {
        AdminGiveCommandParser.Request request = AdminGiveCommandParser
                .parseGiveAll(List.of("stone", "unstack"))
                .orElseThrow();

        assertTrue(request.targetName().isEmpty());
        assertEquals(Material.STONE, request.material());
        assertEquals(1, request.amount());
        assertTrue(request.unstack());
    }

    @Test
    void rejectsMissingArgumentsInvalidAmountsAndUnknownMaterials() {
        assertTrue(AdminGiveCommandParser.parseGive(List.of()).isEmpty());
        assertTrue(AdminGiveCommandParser.parseGive(List.of("Steve", "not_a_real_item")).isEmpty());
        assertTrue(AdminGiveCommandParser.parseGive(List.of("Steve", "stone", "0")).isEmpty());
        assertTrue(AdminGiveCommandParser.parseGiveAll(List.of()).isEmpty());
        assertTrue(AdminGiveCommandParser.parseGiveAll(List.of("stone", "-4")).isEmpty());
    }

    @Test
    void rejectsInvalidEnchantmentSpecsAndSlots() {
        assertTrue(AdminGiveCommandParser.parseGive(List.of("Steve", "stone", "e", "sharpness")).isEmpty());
        assertTrue(AdminGiveCommandParser.parseGive(List.of("Steve", "stone", "-slot:-1")).isEmpty());
    }
}
