package net.axther.hydroxide.modules.teleport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportCommandCatalogTest {

    @Test
    void includesDirectAdminTeleportCommands() {
        assertTrue(TeleportCommandCatalog.commands().containsAll(List.of(
                "tpahere",
                "tpaall",
                "tpacancel",
                "tptoggle",
                "tpauto",
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
        )));
    }
}
