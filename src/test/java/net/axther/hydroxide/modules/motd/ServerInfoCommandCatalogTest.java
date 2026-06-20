package net.axther.hydroxide.modules.motd;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerInfoCommandCatalogTest {

    @Test
    void includesServerInfoParityCommands() {
        assertTrue(ServerInfoCommandCatalog.commands().containsAll(List.of(
                "motd",
                "info",
                "rules",
                "ctext",
                "editctext",
                "helpop",
                "list",
                "ping",
                "gc",
                "tps",
                "servertime",
                "setmotd",
                "status",
                "maxplayers"
        )));
    }
}
