package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemEnchantCommandParserTest {

    @Test
    void defaultsToAddingAnEnchantment() {
        ItemEnchantCommandParser.Request request = ItemEnchantCommandParser.parse(List.of("sharpness", "5")).orElseThrow();

        assertEquals(ItemEnchantCommandParser.Action.ADD, request.action());
        assertEquals("sharpness", request.enchantment().orElseThrow());
        assertEquals(5, request.level().orElseThrow());
    }

    @Test
    void parsesExplicitAddAction() {
        ItemEnchantCommandParser.Request request = ItemEnchantCommandParser.parse(List.of("add", "minecraft:unbreaking", "3")).orElseThrow();

        assertEquals(ItemEnchantCommandParser.Action.ADD, request.action());
        assertEquals("minecraft:unbreaking", request.enchantment().orElseThrow());
        assertEquals(3, request.level().orElseThrow());
    }

    @Test
    void parsesRemoveAction() {
        ItemEnchantCommandParser.Request request = ItemEnchantCommandParser.parse(List.of("remove", "mending")).orElseThrow();

        assertEquals(ItemEnchantCommandParser.Action.REMOVE, request.action());
        assertEquals("mending", request.enchantment().orElseThrow());
        assertTrue(request.level().isEmpty());
    }

    @Test
    void parsesClearActionAliases() {
        assertEquals(ItemEnchantCommandParser.Action.CLEAR, ItemEnchantCommandParser.parse(List.of("clear")).orElseThrow().action());
        assertEquals(ItemEnchantCommandParser.Action.CLEAR, ItemEnchantCommandParser.parse(List.of("removeall")).orElseThrow().action());
    }

    @Test
    void rejectsInvalidForms() {
        assertTrue(ItemEnchantCommandParser.parse(List.of()).isEmpty());
        assertTrue(ItemEnchantCommandParser.parse(List.of("add", "sharpness", "zero")).isEmpty());
        assertTrue(ItemEnchantCommandParser.parse(List.of("add", "sharpness", "0")).isEmpty());
        assertTrue(ItemEnchantCommandParser.parse(List.of("remove")).isEmpty());
        assertTrue(ItemEnchantCommandParser.parse(List.of("clear", "sharpness")).isEmpty());
    }
}
