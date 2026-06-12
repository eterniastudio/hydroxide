package net.axther.hydroxide.modules.welcome;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerVisualStateClaimStoreTest {

    @Test
    void tracksIndependentOwnersForTheSameVisualState() {
        UUID playerId = UUID.randomUUID();
        PlayerVisualStateClaimStore store = new PlayerVisualStateClaimStore();

        store.claim(playerId, PlayerVisualStateOwner.WELCOME_INTRO, PlayerVisualStateType.ENTITY_INVISIBILITY);
        store.claim(playerId, PlayerVisualStateOwner.VANISH, PlayerVisualStateType.ENTITY_INVISIBILITY);

        assertTrue(store.isOwnedBy(playerId, PlayerVisualStateOwner.WELCOME_INTRO, PlayerVisualStateType.ENTITY_INVISIBILITY));
        assertTrue(store.isOwnedBy(playerId, PlayerVisualStateOwner.VANISH, PlayerVisualStateType.ENTITY_INVISIBILITY));
        assertEquals(Set.of(PlayerVisualStateOwner.WELCOME_INTRO, PlayerVisualStateOwner.VANISH),
                store.owners(playerId, PlayerVisualStateType.ENTITY_INVISIBILITY));

        assertTrue(store.release(playerId, PlayerVisualStateOwner.WELCOME_INTRO, PlayerVisualStateType.ENTITY_INVISIBILITY));

        assertFalse(store.isOwnedBy(playerId, PlayerVisualStateOwner.WELCOME_INTRO, PlayerVisualStateType.ENTITY_INVISIBILITY));
        assertTrue(store.isOwnedBy(playerId, PlayerVisualStateOwner.VANISH, PlayerVisualStateType.ENTITY_INVISIBILITY));
        assertTrue(store.hasAnyOwner(playerId, PlayerVisualStateType.ENTITY_INVISIBILITY));
    }

    @Test
    void releasesAllClaimsForOwnerWithoutTouchingOtherOwners() {
        UUID playerId = UUID.randomUUID();
        PlayerVisualStateClaimStore store = new PlayerVisualStateClaimStore();

        store.claim(playerId, PlayerVisualStateOwner.WELCOME_INTRO, PlayerVisualStateType.TITLE);
        store.claim(playerId, PlayerVisualStateOwner.WELCOME_INTRO, PlayerVisualStateType.POTION_BLINDNESS);
        store.claim(playerId, PlayerVisualStateOwner.VANISH, PlayerVisualStateType.ENTITY_INVISIBILITY);

        Set<PlayerVisualStateType> released = store.releaseAll(playerId, PlayerVisualStateOwner.WELCOME_INTRO);

        assertEquals(Set.of(PlayerVisualStateType.TITLE, PlayerVisualStateType.POTION_BLINDNESS), released);
        assertFalse(store.hasAnyOwner(playerId, PlayerVisualStateType.TITLE));
        assertFalse(store.hasAnyOwner(playerId, PlayerVisualStateType.POTION_BLINDNESS));
        assertTrue(store.hasAnyOwner(playerId, PlayerVisualStateType.ENTITY_INVISIBILITY));
    }

    @Test
    void clearPlayerDropsEveryTrackedState() {
        UUID playerId = UUID.randomUUID();
        PlayerVisualStateClaimStore store = new PlayerVisualStateClaimStore();

        store.claim(playerId, PlayerVisualStateOwner.WELCOME_INTRO, PlayerVisualStateType.ACTION_BAR);
        store.claim(playerId, PlayerVisualStateOwner.VANISH, PlayerVisualStateType.VISIBILITY);

        Set<PlayerVisualStateType> cleared = store.clearPlayer(playerId);

        assertEquals(Set.of(PlayerVisualStateType.ACTION_BAR, PlayerVisualStateType.VISIBILITY), cleared);
        assertFalse(store.hasAnyOwner(playerId, PlayerVisualStateType.ACTION_BAR));
        assertFalse(store.hasAnyOwner(playerId, PlayerVisualStateType.VISIBILITY));
    }
}
