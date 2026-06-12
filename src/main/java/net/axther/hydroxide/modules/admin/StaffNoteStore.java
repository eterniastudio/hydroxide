package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class StaffNoteStore {

    private final YamlConfiguration yaml;

    public StaffNoteStore(YamlConfiguration yaml) {
        this.yaml = yaml;
    }

    public void add(UUID playerId, String author, String note) {
        List<String> notes = new ArrayList<>(yaml.getStringList(path(playerId)));
        notes.add(Instant.now().toString() + " | " + author + " | " + note);
        yaml.set(path(playerId), notes);
    }

    public List<String> notes(UUID playerId) {
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section == null) {
            return List.of();
        }
        return yaml.getStringList(path(playerId));
    }

    public void clear(UUID playerId) {
        yaml.set(path(playerId), null);
    }

    private String path(UUID playerId) {
        return "players." + playerId + ".notes";
    }
}
