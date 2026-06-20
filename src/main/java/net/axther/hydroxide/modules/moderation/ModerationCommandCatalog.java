package net.axther.hydroxide.modules.moderation;

import java.util.List;

final class ModerationCommandCatalog {

    private static final List<String> COMMANDS = List.of(
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
    );

    private ModerationCommandCatalog() {
    }

    static List<String> commands() {
        return COMMANDS;
    }
}
