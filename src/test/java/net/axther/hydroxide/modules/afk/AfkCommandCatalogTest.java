package net.axther.hydroxide.modules.afk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AfkCommandCatalogTest {

    @Test
    void includesManualAfkCommand() {
        assertTrue(AfkCommandCatalog.commands().contains("afk"));
    }

    @Test
    void includesAfkStatusCheckCommand() {
        assertTrue(AfkCommandCatalog.commands().contains("afkcheck"));
    }
}
