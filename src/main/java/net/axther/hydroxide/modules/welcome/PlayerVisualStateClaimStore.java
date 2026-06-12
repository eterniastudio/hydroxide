package net.axther.hydroxide.modules.welcome;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerVisualStateClaimStore {

    private final Map<UUID, EnumMap<PlayerVisualStateType, EnumSet<PlayerVisualStateOwner>>> claims = new java.util.HashMap<>();

    public void claim(UUID playerId, PlayerVisualStateOwner owner, PlayerVisualStateType type) {
        claims.computeIfAbsent(playerId, ignored -> new EnumMap<>(PlayerVisualStateType.class))
                .computeIfAbsent(type, ignored -> EnumSet.noneOf(PlayerVisualStateOwner.class))
                .add(owner);
    }

    public boolean release(UUID playerId, PlayerVisualStateOwner owner, PlayerVisualStateType type) {
        EnumMap<PlayerVisualStateType, EnumSet<PlayerVisualStateOwner>> playerClaims = claims.get(playerId);
        if (playerClaims == null) {
            return false;
        }
        EnumSet<PlayerVisualStateOwner> owners = playerClaims.get(type);
        if (owners == null || !owners.remove(owner)) {
            return false;
        }
        cleanup(playerId, type, playerClaims, owners);
        return true;
    }

    public Set<PlayerVisualStateType> releaseAll(UUID playerId, PlayerVisualStateOwner owner) {
        EnumMap<PlayerVisualStateType, EnumSet<PlayerVisualStateOwner>> playerClaims = claims.get(playerId);
        if (playerClaims == null) {
            return Set.of();
        }
        EnumSet<PlayerVisualStateType> released = EnumSet.noneOf(PlayerVisualStateType.class);
        for (PlayerVisualStateType type : EnumSet.copyOf(playerClaims.keySet())) {
            EnumSet<PlayerVisualStateOwner> owners = playerClaims.get(type);
            if (owners != null && owners.remove(owner)) {
                released.add(type);
                cleanup(playerId, type, playerClaims, owners);
            }
        }
        return released.isEmpty() ? Set.of() : Set.copyOf(released);
    }

    public Set<PlayerVisualStateType> clearPlayer(UUID playerId) {
        EnumMap<PlayerVisualStateType, EnumSet<PlayerVisualStateOwner>> removed = claims.remove(playerId);
        if (removed == null || removed.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(removed.keySet());
    }

    public boolean isOwnedBy(UUID playerId, PlayerVisualStateOwner owner, PlayerVisualStateType type) {
        return owners(playerId, type).contains(owner);
    }

    public boolean hasAnyOwner(UUID playerId, PlayerVisualStateType type) {
        return !owners(playerId, type).isEmpty();
    }

    public Set<PlayerVisualStateOwner> owners(UUID playerId, PlayerVisualStateType type) {
        EnumMap<PlayerVisualStateType, EnumSet<PlayerVisualStateOwner>> playerClaims = claims.get(playerId);
        if (playerClaims == null) {
            return Set.of();
        }
        EnumSet<PlayerVisualStateOwner> owners = playerClaims.get(type);
        if (owners == null || owners.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(owners);
    }

    private void cleanup(
            UUID playerId,
            PlayerVisualStateType type,
            EnumMap<PlayerVisualStateType, EnumSet<PlayerVisualStateOwner>> playerClaims,
            EnumSet<PlayerVisualStateOwner> owners
    ) {
        if (owners.isEmpty()) {
            playerClaims.remove(type);
        }
        if (playerClaims.isEmpty()) {
            claims.remove(playerId);
        }
    }
}
