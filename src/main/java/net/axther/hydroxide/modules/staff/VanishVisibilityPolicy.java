package net.axther.hydroxide.modules.staff;

public final class VanishVisibilityPolicy {

    private VanishVisibilityPolicy() {
    }

    public static boolean shouldHide(boolean targetVanished, boolean viewerIsTarget, boolean viewerCanSeeVanished, boolean staffCanSeeVanished) {
        return targetVanished && !viewerIsTarget && !(staffCanSeeVanished && viewerCanSeeVanished);
    }
}
