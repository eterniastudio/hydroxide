package net.axther.hydroxide.modules.staff;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanishStateModelTest {

    @Test
    void opPlayersRemainVisibleWhenAutoVanishIsDisabled() {
        UUID playerId = UUID.randomUUID();
        VanishStateModel model = VanishStateModel.fromPersisted(List.of(), settings(true, false));

        VanishJoinResult result = model.applyJoin(playerId, true, settings(true, false));

        assertFalse(result.vanished());
        assertFalse(model.isVanished(playerId));
        assertEquals(VanishReason.RECONCILE, result.reason());
    }

    @Test
    void autoVanishOpsIsExplicitAndRecorded() {
        UUID playerId = UUID.randomUUID();
        VanishStateModel model = VanishStateModel.fromPersisted(List.of(), settings(true, true));

        VanishJoinResult result = model.applyJoin(playerId, true, settings(true, true));

        assertTrue(result.vanished());
        assertTrue(model.isVanished(playerId));
        assertEquals(VanishReason.OP_AUTO_VANISH, result.reason());
    }

    @Test
    void persistedStateIsLoadedOnlyWhenPersistenceIsEnabled() {
        UUID playerId = UUID.randomUUID();

        assertTrue(VanishStateModel.fromPersisted(List.of(playerId.toString()), settings(true, false)).isVanished(playerId));
        assertFalse(VanishStateModel.fromPersisted(List.of(playerId.toString()), settings(false, false)).isVanished(playerId));
    }

    @Test
    void manualUnvanishRemovesStateAndRequestsRestoration() {
        UUID playerId = UUID.randomUUID();
        VanishStateModel model = VanishStateModel.fromPersisted(List.of(playerId.toString()), settings(true, false));

        VanishChange change = model.unvanish(playerId, VanishReason.MANUAL_UNVANISH);

        assertTrue(change.vanishedBefore());
        assertFalse(change.vanishedNow());
        assertTrue(change.reconcile());
        assertTrue(change.restoreVisualState());
        assertFalse(model.isVanished(playerId));
    }

    @Test
    void fixDoesNotVanishButStillRequestsRestorationAndReconcile() {
        UUID playerId = UUID.randomUUID();
        VanishStateModel model = VanishStateModel.fromPersisted(List.of(), settings(true, false));

        VanishChange change = model.fix(playerId);

        assertFalse(change.vanishedBefore());
        assertFalse(change.vanishedNow());
        assertTrue(change.reconcile());
        assertTrue(change.restoreVisualState());
        assertEquals(VanishReason.FIX, change.reason());
    }

    private VanishSettings settings(boolean persist, boolean autoVanishOps) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("vanish.persist", persist);
        yaml.set("vanish.auto-vanish-ops", autoVanishOps);
        return VanishSettings.from(yaml);
    }
}
