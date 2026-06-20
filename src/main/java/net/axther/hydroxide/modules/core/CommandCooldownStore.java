package net.axther.hydroxide.modules.core;

import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class CommandCooldownStore {

    private final YamlStore store;

    CommandCooldownStore(YamlStore store) {
        this.store = store;
    }

    List<CommandCooldownTracker.Entry> load(long nowMillis) {
        return read(store.load(), nowMillis);
    }

    void save(List<CommandCooldownTracker.Entry> entries) {
        YamlConfiguration yaml = new YamlConfiguration();
        write(yaml, entries);
        store.save(yaml);
    }

    static List<CommandCooldownTracker.Entry> read(YamlConfiguration yaml, long nowMillis) {
        ConfigurationSection section = yaml.getConfigurationSection("entries");
        if (section == null) {
            return List.of();
        }
        List<CommandCooldownTracker.Entry> entries = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String player = entry.getString("player", "");
            String command = entry.getString("command", "");
            long readyAt = entry.getLong("ready-at", 0L);
            if (command.isBlank() || readyAt <= nowMillis) {
                continue;
            }
            try {
                entries.add(new CommandCooldownTracker.Entry(UUID.fromString(player), command, readyAt));
            } catch (IllegalArgumentException ignored) {
                // Ignore stale or manually edited invalid UUID entries.
            }
        }
        return entries;
    }

    static void write(YamlConfiguration yaml, List<CommandCooldownTracker.Entry> entries) {
        yaml.set("entries", null);
        for (int index = 0; index < entries.size(); index++) {
            CommandCooldownTracker.Entry entry = entries.get(index);
            String path = "entries." + index;
            yaml.set(path + ".player", entry.playerId().toString());
            yaml.set(path + ".command", entry.key());
            yaml.set(path + ".ready-at", entry.readyAtMillis());
        }
    }
}
