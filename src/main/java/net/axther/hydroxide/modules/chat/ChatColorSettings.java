package net.axther.hydroxide.modules.chat;

import java.util.Optional;
import java.util.UUID;

final class ChatColorSettings {

    private final ChatControlStore store;

    ChatColorSettings(ChatControlStore store) {
        this.store = store;
    }

    Optional<ColorPalette.Selection> selected(UUID player) {
        return ColorPalette.pick(store.chatColor(player));
    }

    void set(UUID player, ColorPalette.Selection selection) {
        store.setChatColor(player, selection.hex());
    }

    void clear(UUID player) {
        store.clearChatColor(player);
    }
}
