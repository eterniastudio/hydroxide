package net.axther.hydroxide.modules.chat;

import java.util.List;

final class ChatCommandCatalog {

    private static final List<String> COMMANDS = List.of(
            "broadcast", "me", "clearchat", "message", "reply", "chat", "msgtoggle", "ignore", "socialspy",
            "commandspy", "mutechat", "chatcolor", "colors", "colorpicker", "colorlimits"
    );

    private ChatCommandCatalog() {
    }

    static List<String> commands() {
        return COMMANDS;
    }
}
