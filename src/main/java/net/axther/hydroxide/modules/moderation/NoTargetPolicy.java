package net.axther.hydroxide.modules.moderation;

final class NoTargetPolicy {

    private NoTargetPolicy() {
    }

    static boolean shouldCancel(boolean targetIsPlayer, boolean protectedTarget) {
        return targetIsPlayer && protectedTarget;
    }
}
