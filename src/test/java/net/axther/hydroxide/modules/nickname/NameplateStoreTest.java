package net.axther.hydroxide.modules.nickname;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameplateStoreTest {

    @Test
    void loadsAndSerializesNameplateEntries() {
        UUID playerId = UUID.randomUUID();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("players." + playerId + ".name", "Alex");
        yaml.set("players." + playerId + ".prefix", "&6[Admin] ");
        yaml.set("players." + playerId + ".suffix", "<gray>*");
        yaml.set("players." + playerId + ".color", "red");

        NameplateStore store = NameplateStore.from(yaml);
        NameplateStore.StoredNameplate stored = store.get(playerId).orElseThrow();

        assertEquals("Alex", stored.playerName());
        assertEquals("&6[Admin] ", stored.state().prefix());
        assertEquals("<gray>*", stored.state().suffix());
        assertEquals(NamedTextColor.RED, stored.state().color().orElseThrow());

        YamlConfiguration saved = store.toYaml();
        assertEquals("Alex", saved.getString("players." + playerId + ".name"));
        assertEquals("&6[Admin] ", saved.getString("players." + playerId + ".prefix"));
        assertEquals("<gray>*", saved.getString("players." + playerId + ".suffix"));
        assertEquals("red", saved.getString("players." + playerId + ".color"));
    }

    @Test
    void removesEmptyEntries() {
        UUID playerId = UUID.randomUUID();
        NameplateStore store = new NameplateStore();

        store.put(playerId, "Alex", new NicknameService.NameplateState("", "", null));

        assertTrue(store.get(playerId).isEmpty());
    }
}
