package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAccountIndexTest {

    @Test
    void findsAccountsSharingStoredIpHashByPlayerName() {
        YamlConfiguration yaml = new YamlConfiguration();
        UUID steve = UUID.randomUUID();
        UUID alex = UUID.randomUUID();
        UUID notch = UUID.randomUUID();
        put(yaml, steve, "Steve", "hash-a");
        put(yaml, alex, "Alex", "hash-a");
        put(yaml, notch, "Notch", "hash-b");

        List<AdminAccountIndex.Account> matches = AdminAccountIndex.from(yaml)
                .find("Steve", input -> "unused");

        assertEquals(List.of("Alex", "Steve"), matches.stream().map(AdminAccountIndex.Account::name).toList());
    }

    @Test
    void findsAccountsByRawIpViaProvidedHasherOrDirectHash() {
        YamlConfiguration yaml = new YamlConfiguration();
        UUID steve = UUID.randomUUID();
        UUID alex = UUID.randomUUID();
        put(yaml, steve, "Steve", "hash-a");
        put(yaml, alex, "Alex", "hash-b");

        AdminAccountIndex index = AdminAccountIndex.from(yaml);

        assertEquals(List.of("Steve"), index.find("127.0.0.1", input -> "hash-a").stream()
                .map(AdminAccountIndex.Account::name)
                .toList());
        assertEquals(List.of("Alex"), index.find("hash-b", input -> "wrong").stream()
                .map(AdminAccountIndex.Account::name)
                .toList());
    }

    @Test
    void ignoresPlayersWithoutStoredIpHash() {
        YamlConfiguration yaml = new YamlConfiguration();
        UUID steve = UUID.randomUUID();
        yaml.set("players." + steve + ".name", "Steve");

        assertTrue(AdminAccountIndex.from(yaml).find("Steve", input -> "hash").isEmpty());
    }

    @Test
    void groupsOnlyAccountsSharingStoredIpHash() {
        YamlConfiguration yaml = new YamlConfiguration();
        put(yaml, UUID.randomUUID(), "Steve", "hash-a");
        put(yaml, UUID.randomUUID(), "Alex", "hash-a");
        put(yaml, UUID.randomUUID(), "Notch", "hash-b");
        put(yaml, UUID.randomUUID(), "Herobrine", "hash-c");
        put(yaml, UUID.randomUUID(), "Builder", "hash-c");

        List<AdminAccountIndex.AccountGroup> groups = AdminAccountIndex.from(yaml).duplicateGroups();

        assertEquals(2, groups.size());
        assertEquals("hash-a", groups.get(0).ipHash());
        assertEquals(List.of("Alex", "Steve"), groups.get(0).accounts().stream()
                .map(AdminAccountIndex.Account::name)
                .toList());
        assertEquals("hash-c", groups.get(1).ipHash());
        assertEquals(List.of("Builder", "Herobrine"), groups.get(1).accounts().stream()
                .map(AdminAccountIndex.Account::name)
                .toList());
    }

    private void put(YamlConfiguration yaml, UUID uuid, String name, String ipHash) {
        yaml.set("players." + uuid + ".name", name);
        yaml.set("players." + uuid + ".ip-hash", ipHash);
    }
}
