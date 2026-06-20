package net.axther.hydroxide.modules.mail;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailCommandCatalogTest {

    @Test
    void declaresEssentialsStyleMailActions() {
        assertEquals("mail", MailCommandCatalog.command());
        assertTrue(MailCommandCatalog.actions().containsAll(List.of(
                "read",
                "send",
                "sendtemp",
                "delete",
                "clear",
                "sendall"
        )));
    }
}
