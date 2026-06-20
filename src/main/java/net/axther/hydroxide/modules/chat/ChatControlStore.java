package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class ChatControlStore {

    private static final String IGNORES = "ignores";
    private static final String SOCIAL_SPY = "social-spy";
    private static final String COMMAND_SPY = "command-spy";
    private static final String PRIVATE_MESSAGES = "private-messages";
    private static final String PRIVATE_CHAT_TARGETS = "private-chat-targets";
    private static final String CHAT_COLORS = "chat-colors";
    private static final String GLOBAL_MUTE = "global-mute";

    private final YamlStore store;

    ChatControlStore(YamlStore store) {
        this.store = store;
    }

    boolean addIgnore(UUID owner, UUID ignored) {
        if (owner.equals(ignored)) {
            return false;
        }
        Set<UUID> ignoredPlayers = new LinkedHashSet<>(ignoredPlayers(owner));
        if (!ignoredPlayers.add(ignored)) {
            return false;
        }
        saveIgnoredPlayers(owner, ignoredPlayers);
        return true;
    }

    boolean removeIgnore(UUID owner, UUID ignored) {
        Set<UUID> ignoredPlayers = new LinkedHashSet<>(ignoredPlayers(owner));
        if (!ignoredPlayers.remove(ignored)) {
            return false;
        }
        saveIgnoredPlayers(owner, ignoredPlayers);
        return true;
    }

    int clearIgnores(UUID owner) {
        Set<UUID> ignoredPlayers = ignoredPlayers(owner);
        if (ignoredPlayers.isEmpty()) {
            return 0;
        }
        YamlConfiguration yaml = store.load();
        yaml.set(ignorePath(owner), null);
        store.save(yaml);
        return ignoredPlayers.size();
    }

    boolean isIgnored(UUID owner, UUID ignored) {
        return ignoredPlayers(owner).contains(ignored);
    }

    Set<UUID> ignoredPlayers(UUID owner) {
        YamlConfiguration yaml = store.load();
        Set<UUID> ignoredPlayers = new LinkedHashSet<>();
        for (String value : yaml.getStringList(ignorePath(owner))) {
            try {
                ignoredPlayers.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed external edits while preserving valid entries.
            }
        }
        return Set.copyOf(ignoredPlayers);
    }

    void setSocialSpy(UUID player, boolean enabled) {
        YamlConfiguration yaml = store.load();
        yaml.set(SOCIAL_SPY + "." + player, enabled ? true : null);
        store.save(yaml);
    }

    boolean isSocialSpyEnabled(UUID player) {
        return store.load().getBoolean(SOCIAL_SPY + "." + player, false);
    }

    Set<UUID> socialSpies() {
        return enabledUuidKeys(SOCIAL_SPY);
    }

    void setCommandSpy(UUID player, boolean enabled) {
        YamlConfiguration yaml = store.load();
        yaml.set(COMMAND_SPY + "." + player, enabled ? true : null);
        store.save(yaml);
    }

    boolean isCommandSpyEnabled(UUID player) {
        return store.load().getBoolean(COMMAND_SPY + "." + player, false);
    }

    Set<UUID> commandSpies() {
        return enabledUuidKeys(COMMAND_SPY);
    }

    private Set<UUID> enabledUuidKeys(String path) {
        YamlConfiguration yaml = store.load();
        ConfigurationSection section = yaml.getConfigurationSection(path);
        if (section == null) {
            return Set.of();
        }
        Set<UUID> spies = new LinkedHashSet<>();
        for (String key : section.getKeys(false)) {
            if (!section.getBoolean(key, false)) {
                continue;
            }
            try {
                spies.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed keys from manual config edits.
            }
        }
        return Set.copyOf(spies);
    }

    boolean privateMessagesEnabled(UUID player) {
        return store.load().getBoolean(PRIVATE_MESSAGES + "." + player, true);
    }

    void setPrivateMessages(UUID player, boolean enabled) {
        YamlConfiguration yaml = store.load();
        yaml.set(PRIVATE_MESSAGES + "." + player, enabled ? null : false);
        store.save(yaml);
    }

    Optional<UUID> privateChatTarget(UUID player) {
        String value = store.load().getString(PRIVATE_CHAT_TARGETS + "." + player, "");
        if (value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    void setPrivateChatTarget(UUID player, UUID target) {
        YamlConfiguration yaml = store.load();
        yaml.set(PRIVATE_CHAT_TARGETS + "." + player, target.toString());
        store.save(yaml);
    }

    void clearPrivateChatTarget(UUID player) {
        YamlConfiguration yaml = store.load();
        yaml.set(PRIVATE_CHAT_TARGETS + "." + player, null);
        store.save(yaml);
    }

    String chatColor(UUID player) {
        return store.load().getString(CHAT_COLORS + "." + player, "");
    }

    void setChatColor(UUID player, String hex) {
        YamlConfiguration yaml = store.load();
        yaml.set(CHAT_COLORS + "." + player, hex);
        store.save(yaml);
    }

    void clearChatColor(UUID player) {
        YamlConfiguration yaml = store.load();
        yaml.set(CHAT_COLORS + "." + player, null);
        store.save(yaml);
    }

    void setGlobalMute(GlobalChatMute mute) {
        YamlConfiguration yaml = store.load();
        yaml.set(GLOBAL_MUTE + ".issuer", mute.issuer());
        yaml.set(GLOBAL_MUTE + ".reason", mute.reason());
        yaml.set(GLOBAL_MUTE + ".created-at", mute.createdAt().toString());
        yaml.set(GLOBAL_MUTE + ".expires-at", mute.expiresAt().toString());
        store.save(yaml);
    }

    Optional<GlobalChatMute> globalMute(Instant now) {
        YamlConfiguration yaml = store.load();
        if (!yaml.isConfigurationSection(GLOBAL_MUTE)) {
            return Optional.empty();
        }
        try {
            GlobalChatMute mute = new GlobalChatMute(
                    yaml.getString(GLOBAL_MUTE + ".issuer", "Console"),
                    yaml.getString(GLOBAL_MUTE + ".reason", "Chat muted."),
                    Instant.parse(yaml.getString(GLOBAL_MUTE + ".created-at", Instant.EPOCH.toString())),
                    Instant.parse(yaml.getString(GLOBAL_MUTE + ".expires-at", Instant.EPOCH.toString()))
            );
            if (mute.expired(now)) {
                yaml.set(GLOBAL_MUTE, null);
                store.save(yaml);
                return Optional.empty();
            }
            return Optional.of(mute);
        } catch (RuntimeException exception) {
            yaml.set(GLOBAL_MUTE, null);
            store.save(yaml);
            return Optional.empty();
        }
    }

    boolean clearGlobalMute() {
        YamlConfiguration yaml = store.load();
        if (!yaml.isConfigurationSection(GLOBAL_MUTE)) {
            return false;
        }
        yaml.set(GLOBAL_MUTE, null);
        store.save(yaml);
        return true;
    }

    private void saveIgnoredPlayers(UUID owner, Set<UUID> ignoredPlayers) {
        YamlConfiguration yaml = store.load();
        if (ignoredPlayers.isEmpty()) {
            yaml.set(ignorePath(owner), null);
        } else {
            yaml.set(ignorePath(owner), ignoredPlayers.stream()
                    .map(UUID::toString)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList());
        }
        store.save(yaml);
    }

    private String ignorePath(UUID owner) {
        return IGNORES + "." + owner;
    }
}
