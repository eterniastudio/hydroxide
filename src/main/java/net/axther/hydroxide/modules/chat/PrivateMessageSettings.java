package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.modules.options.PlayerOption;
import net.axther.hydroxide.modules.options.PlayerOptionsService;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

final class PrivateMessageSettings {

    private final ChatControlStore fallbackStore;
    private final Supplier<Optional<PlayerOptionsService>> optionsService;

    PrivateMessageSettings(ChatControlStore fallbackStore, Optional<PlayerOptionsService> optionsService) {
        this(fallbackStore, () -> optionsService);
    }

    PrivateMessageSettings(ChatControlStore fallbackStore, Supplier<Optional<PlayerOptionsService>> optionsService) {
        this.fallbackStore = fallbackStore;
        this.optionsService = optionsService;
    }

    boolean enabled(UUID playerId) {
        return optionsService.get()
                .map(service -> service.enabled(playerId, PlayerOption.PRIVATE_MESSAGES))
                .orElseGet(() -> fallbackStore.privateMessagesEnabled(playerId));
    }

    void set(UUID playerId, boolean enabled) {
        Optional<PlayerOptionsService> service = optionsService.get();
        if (service.isPresent()) {
            service.orElseThrow().set(playerId, PlayerOption.PRIVATE_MESSAGES, enabled);
            return;
        }
        fallbackStore.setPrivateMessages(playerId, enabled);
    }
}
