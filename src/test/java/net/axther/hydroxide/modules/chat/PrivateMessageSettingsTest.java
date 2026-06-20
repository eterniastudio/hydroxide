package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.modules.options.PlayerOption;
import net.axther.hydroxide.modules.options.PlayerOptionsService;
import net.axther.hydroxide.storage.YamlStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateMessageSettingsTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultsToAcceptingPrivateMessagesWhenNoOverrideExists() {
        PrivateMessageSettings settings = new PrivateMessageSettings(store(), Optional.empty());

        assertTrue(settings.enabled(UUID.randomUUID()));
    }

    @Test
    void fallbackStorePersistsPrivateMessagePreferenceWhenOptionsModuleIsUnavailable() {
        UUID player = UUID.randomUUID();
        PrivateMessageSettings settings = new PrivateMessageSettings(store(), Optional.empty());

        settings.set(player, false);

        assertFalse(new PrivateMessageSettings(store(), Optional.empty()).enabled(player));
    }

    @Test
    void playerOptionsServiceTakesPriorityOverFallbackChatStore() {
        UUID player = UUID.randomUUID();
        FakePlayerOptionsService service = new FakePlayerOptionsService(false);
        ChatControlStore store = store();
        new PrivateMessageSettings(store, Optional.empty()).set(player, true);

        PrivateMessageSettings settings = new PrivateMessageSettings(store, Optional.of(service));

        assertFalse(settings.enabled(player));

        settings.set(player, true);

        assertTrue(service.enabled(player, PlayerOption.PRIVATE_MESSAGES));
    }

    private ChatControlStore store() {
        return new ChatControlStore(new YamlStore(tempDir.resolve("chat-controls.yml").toFile()));
    }

    private static final class FakePlayerOptionsService implements PlayerOptionsService {

        private boolean enabled;

        private FakePlayerOptionsService(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public boolean enabled(UUID playerId, PlayerOption option) {
            return enabled;
        }

        @Override
        public void set(UUID playerId, PlayerOption option, boolean enabled) {
            this.enabled = enabled;
        }
    }
}
