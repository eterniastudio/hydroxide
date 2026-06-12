package net.axther.hydroxide.modules.options;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.UUID;

public final class PlayerOptions {

    private final YamlConfiguration yaml;

    public PlayerOptions(YamlConfiguration yaml) {
        this.yaml = yaml;
    }

    public boolean enabled(UUID playerId, PlayerOption option) {
        return yaml.getBoolean(path(playerId, option), option.defaultValue());
    }

    public void set(UUID playerId, PlayerOption option, boolean enabled) {
        yaml.set(path(playerId, option), enabled);
    }

    private String path(UUID playerId, PlayerOption option) {
        return "players." + playerId + "." + option.key();
    }
}
