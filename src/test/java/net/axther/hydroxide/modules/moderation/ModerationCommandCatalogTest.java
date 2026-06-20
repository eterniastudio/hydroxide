package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationCommandCatalogTest {

    @Test
    void includesQualityOfLifeAndDisciplineCommands() {
        assertTrue(ModerationCommandCatalog.commands().containsAll(List.of(
                "fly",
                "tfly",
                "god",
                "tgod",
                "heal",
                "feed",
                "hunger",
                "saturation",
                "maxhp",
                "scale",
                "glow",
                "notarget",
                "playercollision",
                "cuff",
                "speed",
                "gamemode",
                "effect",
                "air",
                "falldistance",
                "kick",
                "kickall",
                "ban",
                "banlist",
                "checkban",
                "tempban",
                "unban",
                "ipban",
                "ipbanlist",
                "unbanip",
                "mute",
                "tempmute",
                "unmute",
                "warn",
                "warnings",
                "clearwarnings",
                "editwarnings"
        )));
    }
}
