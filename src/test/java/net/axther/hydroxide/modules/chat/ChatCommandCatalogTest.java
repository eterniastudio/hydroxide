package net.axther.hydroxide.modules.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatCommandCatalogTest {

    @Test
    void includesPrivateMessageControlCommands() {
        assertTrue(ChatCommandCatalog.commands().containsAll(List.of(
                "broadcast",
                "me",
                "clearchat",
                "message",
                "reply",
                "chat",
                "msgtoggle",
                "ignore",
                "socialspy",
                "commandspy",
                "mutechat",
                "chatcolor",
                "colors",
                "colorpicker",
                "colorlimits"
        )));
    }
}
