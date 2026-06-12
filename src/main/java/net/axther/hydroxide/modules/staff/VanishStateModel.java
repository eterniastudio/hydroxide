package net.axther.hydroxide.modules.staff;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class VanishStateModel {

    private final Set<UUID> vanishedPlayers = new HashSet<>();

    private VanishStateModel() {
    }

    public static VanishStateModel empty() {
        return new VanishStateModel();
    }

    public static VanishStateModel fromPersisted(Collection<String> persistedPlayerIds, VanishSettings settings) {
        VanishStateModel model = new VanishStateModel();
        if (settings == null || !settings.persist()) {
            return model;
        }
        for (String value : persistedPlayerIds) {
            try {
                model.vanishedPlayers.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                // Malformed persisted entries are ignored by the pure state model.
            }
        }
        return model;
    }

    public VanishJoinResult applyJoin(UUID playerId, boolean op, VanishSettings settings) {
        if (isVanished(playerId)) {
            return new VanishJoinResult(true, false, VanishReason.PERSISTED_RESTORE);
        }
        if (settings != null && settings.enabled() && settings.autoVanishOps() && op) {
            vanishedPlayers.add(playerId);
            return new VanishJoinResult(true, true, VanishReason.OP_AUTO_VANISH);
        }
        return new VanishJoinResult(false, false, VanishReason.RECONCILE);
    }

    public VanishChange toggleManual(UUID playerId) {
        return isVanished(playerId)
                ? unvanish(playerId, VanishReason.MANUAL_UNVANISH)
                : vanish(playerId, VanishReason.MANUAL_VANISH);
    }

    public VanishChange vanish(UUID playerId, VanishReason reason) {
        boolean before = isVanished(playerId);
        vanishedPlayers.add(playerId);
        return new VanishChange(playerId, before, true, true, false, reason);
    }

    public VanishChange unvanish(UUID playerId, VanishReason reason) {
        boolean before = isVanished(playerId);
        vanishedPlayers.remove(playerId);
        return new VanishChange(playerId, before, false, true, true, reason);
    }

    public VanishChange fix(UUID playerId) {
        boolean vanished = isVanished(playerId);
        return new VanishChange(playerId, vanished, vanished, true, true, VanishReason.FIX);
    }

    public boolean isVanished(UUID playerId) {
        return vanishedPlayers.contains(playerId);
    }

    public void clear() {
        vanishedPlayers.clear();
    }

    public List<String> persistedSnapshot(VanishSettings settings) {
        if (settings != null && !settings.persist()) {
            return List.of();
        }
        return vanishedPlayers.stream()
                .map(UUID::toString)
                .sorted()
                .toList();
    }
}
