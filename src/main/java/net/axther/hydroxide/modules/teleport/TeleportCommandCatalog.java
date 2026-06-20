package net.axther.hydroxide.modules.teleport;

import java.util.List;

final class TeleportCommandCatalog {

    private static final List<String> COMMANDS = List.of(
            "spawn",
            "setspawn",
            "home",
            "sethome",
            "delhome",
            "homes",
            "warp",
            "setwarp",
            "delwarp",
            "warps",
            "back",
            "tpa",
            "tpahere",
            "tpaall",
            "tpacancel",
            "tptoggle",
            "tpauto",
            "tpaccept",
            "tpdeny",
            "tp",
            "tphere",
            "tpo",
            "tpohere",
            "tpall",
            "tpallworld",
            "tppos",
            "tpopos",
            "dback",
            "resetback",
            "jump",
            "down",
            "world",
            "patrol"
    );

    private TeleportCommandCatalog() {
    }

    static List<String> commands() {
        return COMMANDS;
    }
}
