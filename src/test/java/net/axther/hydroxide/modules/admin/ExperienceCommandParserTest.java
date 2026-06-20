package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceCommandParserTest {

    @Test
    void parsesActionsAndCommonAliases() {
        assertEquals(ExperienceCommandParser.Action.SHOW, ExperienceCommandParser.action("show").orElseThrow());
        assertEquals(ExperienceCommandParser.Action.GIVE, ExperienceCommandParser.action("add").orElseThrow());
        assertEquals(ExperienceCommandParser.Action.TAKE, ExperienceCommandParser.action("remove").orElseThrow());
        assertEquals(ExperienceCommandParser.Action.SET, ExperienceCommandParser.action("set").orElseThrow());
    }

    @Test
    void parsesOnlyWholeNonNegativeLevelAmounts() {
        assertEquals(15, ExperienceCommandParser.levels("15").orElseThrow());
        assertEquals(0, ExperienceCommandParser.levels("0").orElseThrow());
        assertTrue(ExperienceCommandParser.levels("-1").isEmpty());
        assertTrue(ExperienceCommandParser.levels("1.5").isEmpty());
        assertTrue(ExperienceCommandParser.levels("many").isEmpty());
    }
}
