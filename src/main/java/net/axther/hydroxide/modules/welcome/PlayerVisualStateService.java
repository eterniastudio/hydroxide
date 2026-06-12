package net.axther.hydroxide.modules.welcome;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public interface PlayerVisualStateService {

    void restoreIntroState(Player player);

    void claim(UUID playerId, PlayerVisualStateOwner owner, PlayerVisualStateType type);

    boolean release(UUID playerId, PlayerVisualStateOwner owner, PlayerVisualStateType type);

    Set<PlayerVisualStateType> releaseAll(UUID playerId, PlayerVisualStateOwner owner);

    Set<PlayerVisualStateType> clearPlayer(UUID playerId);

    boolean isOwnedBy(UUID playerId, PlayerVisualStateOwner owner, PlayerVisualStateType type);

    boolean hasAnyOwner(UUID playerId, PlayerVisualStateType type);

    Set<PlayerVisualStateOwner> owners(UUID playerId, PlayerVisualStateType type);
}
