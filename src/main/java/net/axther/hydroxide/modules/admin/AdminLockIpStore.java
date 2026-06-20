package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class AdminLockIpStore {

    private final YamlConfiguration yaml;

    AdminLockIpStore(YamlConfiguration yaml) {
        this.yaml = yaml;
    }

    boolean add(UUID playerId, String ipHash) {
        String normalized = normalize(ipHash);
        List<String> hashes = new ArrayList<>(hashes(playerId));
        if (hashes.contains(normalized)) {
            return false;
        }
        hashes.add(normalized);
        yaml.set(path(playerId), hashes);
        return true;
    }

    boolean remove(UUID playerId, String ipHash) {
        String normalized = normalize(ipHash);
        List<String> hashes = new ArrayList<>(hashes(playerId));
        boolean removed = hashes.remove(normalized);
        if (!removed) {
            return false;
        }
        if (hashes.isEmpty()) {
            clear(playerId);
        } else {
            yaml.set(path(playerId), hashes);
        }
        return true;
    }

    List<String> hashes(UUID playerId) {
        return yaml.getStringList(path(playerId)).stream()
                .map(AdminLockIpStore::normalize)
                .distinct()
                .toList();
    }

    void clear(UUID playerId) {
        yaml.set(path(playerId), null);
    }

    boolean isAllowed(UUID playerId, String currentIpHash) {
        List<String> hashes = hashes(playerId);
        return hashes.isEmpty() || hashes.contains(normalize(currentIpHash));
    }

    private static String normalize(String ipHash) {
        return ipHash == null ? "" : ipHash.trim().toLowerCase(Locale.ROOT);
    }

    private String path(UUID playerId) {
        return "players." + playerId + ".locked-ip-hashes";
    }
}
