package net.axther.hydroxide.modules.options;

import java.util.UUID;

public interface PlayerOptionsService {

    boolean enabled(UUID playerId, PlayerOption option);

    void set(UUID playerId, PlayerOption option, boolean enabled);
}
