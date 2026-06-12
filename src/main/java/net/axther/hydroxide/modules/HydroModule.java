package net.axther.hydroxide.modules;

import net.axther.hydroxide.HydroxideContext;

import java.util.List;

public interface HydroModule {

    String id();

    String displayName();

    String description();

    default List<String> dependencies() {
        return List.of();
    }

    default boolean defaultEnabled() {
        return true;
    }

    void onEnable(HydroxideContext context);

    default void onDisable(HydroxideContext context) {
    }

    default void onReload(HydroxideContext context) {
    }
}
