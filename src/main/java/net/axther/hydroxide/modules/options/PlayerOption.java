package net.axther.hydroxide.modules.options;

public enum PlayerOption {
    PRIVATE_MESSAGES("private-messages", true),
    TELEPORT_REQUESTS("teleport-requests", true),
    PAYMENTS("payments", true),
    PARTY_INVITES("party-invites", true),
    FRIEND_NOTIFICATIONS("friend-notifications", true),
    CHAT_CHANNEL_FOCUS("chat-channel-focus", true),
    SCOREBOARD_TABLIST("scoreboard-tablist", true),
    PARTICLES_SOUNDS("particles-sounds", true),
    BUILD_MODE_INDICATORS("build-mode-indicators", true);

    private final String key;
    private final boolean defaultValue;

    PlayerOption(String key, boolean defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String key() {
        return key;
    }

    public boolean defaultValue() {
        return defaultValue;
    }

    public static PlayerOption fromKey(String key) {
        for (PlayerOption option : values()) {
            if (option.key.equalsIgnoreCase(key) || option.name().equalsIgnoreCase(key)) {
                return option;
            }
        }
        return null;
    }
}
