package net.axther.hydroxide.messages;

import net.axther.hydroxide.text.TextFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MessageService {

    private static final String DEFAULT_PREFIX = "<#44CCFF><bold>Hydroxide</bold> <dark_gray>> <gray>";
    private static final String MISSING_TEMPLATE = "<red>Missing message: {key}";

    private final TextFormatter text;
    private final File file;
    private final Set<String> missingKeys = new LinkedHashSet<>();
    private YamlConfiguration messages;

    public MessageService(TextFormatter text, YamlConfiguration messages) {
        this(text, messages, null);
    }

    private MessageService(TextFormatter text, YamlConfiguration messages, File file) {
        this.text = text;
        this.messages = messages;
        this.file = file;
    }

    public static MessageService fromFile(TextFormatter text, File file) {
        return new MessageService(text, YamlConfiguration.loadConfiguration(file), file);
    }

    public void reload() {
        if (file != null) {
            messages = YamlConfiguration.loadConfiguration(file);
        }
        missingKeys.clear();
    }

    public Component prefix() {
        return text.format(messages.getString("core.prefix", DEFAULT_PREFIX));
    }

    public Component component(String key, Map<String, ?> placeholders) {
        String template = messages.getString(key);
        if (template == null) {
            missingKeys.add(key);
            template = MISSING_TEMPLATE;
            placeholders = withFallbackKey(placeholders, key);
        }
        return text.format(replace(template, placeholders));
    }

    public String template(String key, String fallback) {
        String template = messages.getString(key);
        if (template == null) {
            missingKeys.add(key);
            return fallback;
        }
        return template;
    }

    public Optional<String> rawTemplate(String key) {
        return messages.isString(key) ? Optional.ofNullable(messages.getString(key)) : Optional.empty();
    }

    public boolean fileBacked() {
        return file != null;
    }

    public boolean hasStringKey(String key) {
        return messages.isString(key);
    }

    public File setRawTemplate(String key, String value) throws IOException {
        if (file == null) {
            throw new IllegalStateException("Message service is not backed by a file.");
        }
        File backup = backupFile();
        messages.set(key, value);
        messages.save(file);
        missingKeys.remove(key);
        return backup;
    }

    public List<String> stringKeys() {
        return messages.getKeys(true).stream()
                .filter(messages::isString)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<MessageEntry> search(String query, int limit) {
        String normalized = normalizeSearch(query);
        int resultLimit = Math.max(1, limit);
        return messages.getKeys(true).stream()
                .filter(messages::isString)
                .filter(key -> matches(key, messages.getString(key, ""), normalized))
                .sorted(Comparator.comparing(key -> key.toLowerCase(Locale.ROOT)))
                .limit(resultLimit)
                .map(key -> new MessageEntry(key, messages.getString(key, "")))
                .toList();
    }

    public Component prefixedComponent(String key, Map<String, ?> placeholders) {
        return prefix().append(component(key, placeholders));
    }

    public void send(CommandSender sender, String key, Map<String, ?> placeholders) {
        sender.sendMessage(prefixedComponent(key, placeholders));
    }

    public Set<String> missingKeys() {
        return Collections.unmodifiableSet(missingKeys);
    }

    private Map<String, ?> withFallbackKey(Map<String, ?> placeholders, String key) {
        java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>(placeholders);
        values.putIfAbsent("key", key);
        return values;
    }

    private String replace(String template, Map<String, ?> placeholders) {
        String rendered = template;
        for (Map.Entry<String, ?> entry : placeholders.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return rendered;
    }

    private boolean matches(String key, String value, String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            return true;
        }
        return key.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private String normalizeSearch(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private File backupFile() throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        File backup = new File(parent == null ? new File(".") : parent, file.getName() + ".bak");
        if (file.exists()) {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return backup;
    }

    public record MessageEntry(String key, String value) {
    }
}
