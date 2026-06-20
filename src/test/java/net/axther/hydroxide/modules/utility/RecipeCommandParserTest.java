package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeCommandParserTest {

    @Test
    void defaultsToHeldItem() {
        RecipeCommandParser.Request request = RecipeCommandParser.parse(List.of()).orElseThrow();

        assertEquals(RecipeCommandParser.Source.HAND, request.source());
        assertTrue(request.material().isEmpty());
    }

    @Test
    void acceptsHeldItemAliases() {
        assertEquals(RecipeCommandParser.Source.HAND, RecipeCommandParser.parse(List.of("hand")).orElseThrow().source());
        assertEquals(RecipeCommandParser.Source.HAND, RecipeCommandParser.parse(List.of("held")).orElseThrow().source());
    }

    @Test
    void parsesMaterialName() {
        RecipeCommandParser.Request request = RecipeCommandParser.parse(List.of("diamond_pickaxe")).orElseThrow();

        assertEquals(RecipeCommandParser.Source.MATERIAL, request.source());
        assertEquals("diamond_pickaxe", request.material().orElseThrow());
    }

    @Test
    void rejectsExtraArguments() {
        assertTrue(RecipeCommandParser.parse(List.of("diamond_pickaxe", "extra")).isEmpty());
    }
}
