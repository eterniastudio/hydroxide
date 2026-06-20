package net.axther.hydroxide.modules.mail;

import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class MailboxStore {

    private static final String ROOT = "mailboxes";

    private final YamlStore store;

    MailboxStore(YamlStore store) {
        this.store = store;
    }

    MailRecord append(UUID recipient, String senderName, String message) {
        return append(recipient, senderName, message, Optional.empty());
    }

    MailRecord append(UUID recipient, String senderName, String message, Optional<Instant> expiresAt) {
        MailRecord record = new MailRecord(UUID.randomUUID(), recipient, senderName, message, Instant.now(), expiresAt);
        save(record);
        return record;
    }

    void save(MailRecord record) {
        YamlConfiguration yaml = store.load();
        String path = messagePath(record.recipient(), record.id());
        yaml.set(path + ".sender-name", record.senderName());
        yaml.set(path + ".message", record.message());
        yaml.set(path + ".created-at", record.createdAt().toString());
        yaml.set(path + ".expires-at", record.expiresAt().map(Instant::toString).orElse(null));
        store.save(yaml);
    }

    List<MailRecord> list(UUID recipient) {
        return list(recipient, Instant.now());
    }

    List<MailRecord> list(UUID recipient, Instant now) {
        YamlConfiguration yaml = store.load();
        ConfigurationSection section = yaml.getConfigurationSection(messagesPath(recipient));
        if (section == null) {
            return List.of();
        }
        List<MailRecord> records = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            parseRecord(recipient, section, key)
                    .filter(record -> !record.expired(now))
                    .ifPresent(records::add);
        }
        records.sort(Comparator.comparing(MailRecord::createdAt).thenComparing(record -> record.id().toString()));
        return List.copyOf(records);
    }

    int count(UUID recipient) {
        return count(recipient, Instant.now());
    }

    int count(UUID recipient, Instant now) {
        return list(recipient, now).size();
    }

    boolean delete(UUID recipient, int visibleIndex) {
        List<MailRecord> records = list(recipient);
        if (visibleIndex < 1 || visibleIndex > records.size()) {
            return false;
        }
        YamlConfiguration yaml = store.load();
        yaml.set(messagePath(recipient, records.get(visibleIndex - 1).id()), null);
        if (records.size() == 1) {
            yaml.set(mailboxPath(recipient), null);
        }
        store.save(yaml);
        return true;
    }

    int clear(UUID recipient) {
        List<MailRecord> records = list(recipient);
        if (records.isEmpty()) {
            return 0;
        }
        YamlConfiguration yaml = store.load();
        yaml.set(mailboxPath(recipient), null);
        store.save(yaml);
        return records.size();
    }

    int clearAll() {
        YamlConfiguration yaml = store.load();
        ConfigurationSection root = yaml.getConfigurationSection(ROOT);
        if (root == null) {
            return 0;
        }
        int removed = 0;
        for (String recipientKey : root.getKeys(false)) {
            ConfigurationSection messages = root.getConfigurationSection(recipientKey + ".messages");
            if (messages != null) {
                removed += messages.getKeys(false).size();
            }
        }
        yaml.set(ROOT, null);
        store.save(yaml);
        return removed;
    }

    int removeMessage(String message) {
        if (message == null || message.isBlank()) {
            return 0;
        }
        YamlConfiguration yaml = store.load();
        ConfigurationSection root = yaml.getConfigurationSection(ROOT);
        if (root == null) {
            return 0;
        }
        int removed = 0;
        List<String> emptyMailboxes = new ArrayList<>();
        for (String recipientKey : root.getKeys(false)) {
            String messagesPath = ROOT + "." + recipientKey + ".messages";
            ConfigurationSection messages = yaml.getConfigurationSection(messagesPath);
            if (messages == null) {
                continue;
            }
            for (String messageKey : messages.getKeys(false)) {
                if (message.equals(messages.getString(messageKey + ".message", ""))) {
                    yaml.set(messagesPath + "." + messageKey, null);
                    removed++;
                }
            }
            ConfigurationSection updatedMessages = yaml.getConfigurationSection(messagesPath);
            if (updatedMessages == null || updatedMessages.getKeys(false).isEmpty()) {
                emptyMailboxes.add(ROOT + "." + recipientKey);
            }
        }
        for (String mailboxPath : emptyMailboxes) {
            yaml.set(mailboxPath, null);
        }
        if (removed > 0) {
            store.save(yaml);
        }
        return removed;
    }

    private java.util.Optional<MailRecord> parseRecord(UUID recipient, ConfigurationSection section, String key) {
        try {
            UUID id = UUID.fromString(key);
            String message = section.getString(key + ".message", "");
            if (message.isBlank()) {
                return java.util.Optional.empty();
            }
            String senderName = section.getString(key + ".sender-name", "Unknown");
            Instant createdAt = parseInstant(section.getString(key + ".created-at", ""));
            Optional<Instant> expiresAt = parseOptionalInstant(section.getString(key + ".expires-at", ""));
            return java.util.Optional.of(new MailRecord(id, recipient, senderName, message, createdAt, expiresAt));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private Optional<Instant> parseOptionalInstant(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(value));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            return Instant.EPOCH;
        }
    }

    private String mailboxPath(UUID recipient) {
        return ROOT + "." + recipient;
    }

    private String messagesPath(UUID recipient) {
        return mailboxPath(recipient) + ".messages";
    }

    private String messagePath(UUID recipient, UUID id) {
        return messagesPath(recipient) + "." + id;
    }
}
