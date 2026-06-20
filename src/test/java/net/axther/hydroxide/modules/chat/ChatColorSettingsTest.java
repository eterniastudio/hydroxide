package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.storage.YamlStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatColorSettingsTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultsToNoSelectedChatColor() {
        ChatColorSettings settings = new ChatColorSettings(store());

        assertTrue(settings.selected(UUID.randomUUID()).isEmpty());
    }

    @Test
    void persistsSelectedChatColorByPlayer() {
        UUID player = UUID.randomUUID();
        ChatColorSettings settings = new ChatColorSettings(store());

        settings.set(player, ColorPalette.pick("#44CCFF").orElseThrow());

        ColorPalette.Selection selection = new ChatColorSettings(store()).selected(player).orElseThrow();
        assertEquals("#44CCFF", selection.hex());
        assertEquals("aqua", selection.closest().name());
    }

    @Test
    void clearsSelectedChatColor() {
        UUID player = UUID.randomUUID();
        ChatColorSettings settings = new ChatColorSettings(store());
        settings.set(player, ColorPalette.pick("red").orElseThrow());

        settings.clear(player);

        assertTrue(new ChatColorSettings(store()).selected(player).isEmpty());
    }

    private ChatControlStore store() {
        return new ChatControlStore(new YamlStore(tempDir.resolve("chat-controls.yml").toFile()));
    }
}
