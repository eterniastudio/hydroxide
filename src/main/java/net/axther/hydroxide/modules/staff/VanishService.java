package net.axther.hydroxide.modules.staff;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface VanishService {

    boolean isVanished(UUID playerId);

    void reconcileVisibility(Player player);

    void reconcileAllVisibility();

    default void refreshPlayerVisibility(Player player) {
        reconcileVisibility(player);
    }
}
