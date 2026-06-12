package net.axther.hydroxide.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class YamlPlayerDataStore implements PlayerDataStore {

    private final JavaPlugin plugin;
    private final YamlStore friends;

    public YamlPlayerDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.friends = new YamlStore(new File(plugin.getDataFolder(), "friends.yml"));
    }

    @Override
    public void setHome(UUID playerId, String name, StoredLocation location) {
        YamlStore store = store(playerId);
        YamlConfiguration yaml = store.load();
        location.writeTo(yaml.createSection(homePath(name)));
        store.save(yaml);
    }

    @Override
    public Optional<StoredLocation> home(UUID playerId, String name) {
        return StoredLocation.readFrom(store(playerId).load().getConfigurationSection(homePath(name)));
    }

    @Override
    public boolean removeHome(UUID playerId, String name) {
        YamlStore store = store(playerId);
        YamlConfiguration yaml = store.load();
        String path = homePath(name);
        if (!yaml.contains(path)) {
            return false;
        }
        yaml.set(path, null);
        store.save(yaml);
        return true;
    }

    @Override
    public List<String> homes(UUID playerId) {
        ConfigurationSection section = store(playerId).load().getConfigurationSection("homes");
        if (section == null) {
            return List.of();
        }
        return section.getKeys(false).stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    @Override
    public void setNickname(UUID playerId, String playerName, String nickname) {
        YamlStore store = store(playerId);
        YamlConfiguration yaml = store.load();
        yaml.set("profile.last-known-name", playerName);
        yaml.set("profile.nickname", nickname);
        store.save(yaml);
    }

    @Override
    public Optional<String> nickname(UUID playerId) {
        return Optional.ofNullable(store(playerId).load().getString("profile.nickname"))
                .filter(value -> !value.isBlank());
    }

    @Override
    public void removeNickname(UUID playerId) {
        YamlStore store = store(playerId);
        YamlConfiguration yaml = store.load();
        yaml.set("profile.nickname", null);
        store.save(yaml);
    }

    @Override
    public Map<UUID, StoredNickname> nicknames() {
        File playerDirectory = new File(plugin.getDataFolder(), "data/players");
        File[] files = playerDirectory.listFiles((directory, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return Map.of();
        }

        Map<UUID, StoredNickname> nicknames = new HashMap<>();
        for (File file : files) {
            String fileName = file.getName().substring(0, file.getName().length() - ".yml".length());
            UUID playerId;
            try {
                playerId = UUID.fromString(fileName);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String nickname = yaml.getString("profile.nickname");
            String playerName = yaml.getString("profile.last-known-name");
            if (nickname != null && !nickname.isBlank() && playerName != null && !playerName.isBlank()) {
                nicknames.put(playerId, new StoredNickname(playerId, playerName, nickname));
            }
        }
        return Map.copyOf(nicknames);
    }

    @Override
    public double balance(UUID playerId, double defaultBalance) {
        YamlStore store = store(playerId);
        YamlConfiguration yaml = store.load();
        if (!yaml.contains("economy.balance")) {
            yaml.set("economy.balance", defaultBalance);
            store.save(yaml);
        }
        return yaml.getDouble("economy.balance", defaultBalance);
    }

    @Override
    public void setBalance(UUID playerId, double balance) {
        validateBalance(balance);
        YamlStore store = store(playerId);
        YamlConfiguration yaml = store.load();
        yaml.set("economy.balance", balance);
        store.save(yaml);
    }

    @Override
    public List<UUID> friends(UUID playerId) {
        return friends.load().getStringList(friendPath(playerId)).stream()
                .map(this::parseUuid)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public void addFriend(UUID playerId, UUID friendId) {
        YamlConfiguration yaml = friends.load();
        String path = friendPath(playerId);
        List<String> list = new ArrayList<>(yaml.getStringList(path));
        String id = friendId.toString();
        if (!list.contains(id)) {
            list.add(id);
        }
        yaml.set(path, list);
        friends.save(yaml);
    }

    @Override
    public boolean removeFriend(UUID playerId, UUID friendId) {
        YamlConfiguration yaml = friends.load();
        String path = friendPath(playerId);
        List<String> list = new ArrayList<>(yaml.getStringList(path));
        boolean removed = list.remove(friendId.toString());
        yaml.set(path, list);
        friends.save(yaml);
        return removed;
    }

    private YamlStore store(UUID playerId) {
        return new YamlStore(new File(plugin.getDataFolder(), "data/players/" + playerId + ".yml"));
    }

    private String homePath(String name) {
        return "homes." + normalizeName(name);
    }

    private String friendPath(UUID playerId) {
        return "friends." + playerId;
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private void validateBalance(double balance) {
        if (balance < 0.0 || Double.isNaN(balance) || Double.isInfinite(balance)
                || BigDecimal.valueOf(balance).scale() > 2) {
            throw new IllegalArgumentException("Balance must be a finite non-negative amount with at most two decimals");
        }
    }
}
