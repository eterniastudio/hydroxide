package net.axther.hydroxide.messages;

import net.axther.hydroxide.text.TextFormatter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageServiceTest {

    private final TextFormatter formatter = new TextFormatter();

    @Test
    void formatsMessageWithPlaceholdersAndMiniMessage() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("core.prefix", "<#44CCFF>Hydroxide <dark_gray>> <gray>");
        yaml.set("economy.balance", "<green>{player}<gray>: <white>{balance}");
        MessageService service = new MessageService(formatter, yaml);

        String plain = formatter.plain(service.component("economy.balance", Map.of(
                "player", "Steve",
                "balance", "$12.50"
        )));

        assertEquals("Steve: $12.50", plain);
    }

    @Test
    void prefixesChatMessagesWithConfiguredPrefix() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("core.prefix", "<#44CCFF>Hydroxide <dark_gray>> <gray>");
        yaml.set("core.reload.success", "<green>Reloaded {module}.");
        MessageService service = new MessageService(formatter, yaml);

        String plain = formatter.plain(service.prefixedComponent("core.reload.success", Map.of("module", "core")));

        assertEquals("Hydroxide > Reloaded core.", plain);
    }

    @Test
    void tracksMissingKeysAndReturnsReadableFallback() {
        MessageService service = new MessageService(formatter, new YamlConfiguration());

        String plain = formatter.plain(service.component("missing.key", Map.of()));

        assertEquals("Missing message: missing.key", plain);
        assertTrue(service.missingKeys().contains("missing.key"));
    }

    @Test
    void returnsRawTemplatesAndTracksMissingTemplateKeys() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("chat.format", "<white>{name}: {message}");
        MessageService service = new MessageService(formatter, yaml);

        assertEquals("<white>{name}: {message}", service.template("chat.format", "fallback"));
        assertEquals("fallback", service.template("chat.missing", "fallback"));
        assertTrue(service.missingKeys().contains("chat.missing"));
    }

    @Test
    void searchesStringMessageKeysAndValues() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("chat.format", "<white>{name}: {message}");
        yaml.set("economy.balance.self", "<green>Your balance is {balance}.");
        yaml.set("non-string", 42);
        MessageService service = new MessageService(formatter, yaml);

        assertEquals(List.of("chat.format", "economy.balance.self"), service.stringKeys());
        assertEquals("<white>{name}: {message}", service.rawTemplate("chat.format").orElseThrow());
        assertTrue(service.rawTemplate("non-string").isEmpty());
        assertEquals("economy.balance.self", service.search("balance", 10).getFirst().key());
        assertEquals("chat.format", service.search("message", 10).getFirst().key());
        assertTrue(service.search("missing", 10).isEmpty());
    }

    @Test
    void savesEditedTemplatesAndCreatesBackup(@TempDir File directory) throws Exception {
        File file = new File(directory, "messages.yml");
        Files.writeString(file.toPath(), """
                core:
                  prefix: "<gray>"
                  reload:
                    success: "<green>Before"
                """, StandardCharsets.UTF_8);
        MessageService service = MessageService.fromFile(formatter, file);

        File backup = service.setRawTemplate("core.reload.success", "<gold>After");

        assertTrue(backup.exists());
        assertTrue(Files.readString(backup.toPath()).contains("<green>Before"));
        assertEquals("<gold>After", YamlConfiguration.loadConfiguration(file).getString("core.reload.success"));
        assertEquals("After", formatter.plain(service.component("core.reload.success", Map.of())));
    }

    @Test
    void reloadsMessagesFromDisk(@TempDir File directory) throws Exception {
        File file = new File(directory, "messages.yml");
        Files.writeString(file.toPath(), """
                core:
                  prefix: "<gray>"
                  reload:
                    success: "<green>First"
                """, StandardCharsets.UTF_8);
        MessageService service = MessageService.fromFile(formatter, file);
        assertEquals("First", formatter.plain(service.component("core.reload.success", Map.of())));

        Files.writeString(file.toPath(), """
                core:
                  prefix: "<gray>"
                  reload:
                    success: "<green>Second"
                """, StandardCharsets.UTF_8);
        service.reload();

        assertEquals("Second", formatter.plain(service.component("core.reload.success", Map.of())));
    }
}
