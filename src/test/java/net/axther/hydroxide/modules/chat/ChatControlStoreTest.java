package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.storage.YamlStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatControlStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsIgnoredPlayersPerOwner() {
        UUID owner = UUID.randomUUID();
        UUID ignored = UUID.randomUUID();
        ChatControlStore store = store();

        assertTrue(store.addIgnore(owner, ignored));
        assertFalse(store.addIgnore(owner, ignored));
        assertTrue(store.isIgnored(owner, ignored));
        assertEquals(Set.of(ignored), store.ignoredPlayers(owner));

        assertTrue(store.removeIgnore(owner, ignored));
        assertFalse(store.isIgnored(owner, ignored));
        assertFalse(store.removeIgnore(owner, ignored));
    }

    @Test
    void clearsIgnoredPlayersAndRejectsSelfIgnore() {
        UUID owner = UUID.randomUUID();
        ChatControlStore store = store();
        store.addIgnore(owner, UUID.randomUUID());
        store.addIgnore(owner, UUID.randomUUID());

        assertFalse(store.addIgnore(owner, owner));
        assertEquals(2, store.clearIgnores(owner));
        assertEquals(0, store.clearIgnores(owner));
        assertTrue(store.ignoredPlayers(owner).isEmpty());
    }

    @Test
    void persistsSocialSpyState() {
        UUID staff = UUID.randomUUID();
        ChatControlStore store = store();

        assertFalse(store.isSocialSpyEnabled(staff));
        store.setSocialSpy(staff, true);
        assertTrue(store.isSocialSpyEnabled(staff));
        assertEquals(Set.of(staff), store.socialSpies());

        store.setSocialSpy(staff, false);
        assertFalse(store.isSocialSpyEnabled(staff));
        assertTrue(store.socialSpies().isEmpty());
    }

    @Test
    void persistsCommandSpyState() {
        UUID staff = UUID.randomUUID();
        ChatControlStore store = store();

        assertFalse(store.isCommandSpyEnabled(staff));
        store.setCommandSpy(staff, true);
        assertTrue(store.isCommandSpyEnabled(staff));
        assertEquals(Set.of(staff), store.commandSpies());

        store.setCommandSpy(staff, false);
        assertFalse(store.isCommandSpyEnabled(staff));
        assertTrue(store.commandSpies().isEmpty());
    }

    @Test
    void persistsPrivateChatFocusTarget() {
        UUID owner = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        ChatControlStore store = store();

        assertTrue(store.privateChatTarget(owner).isEmpty());

        store.setPrivateChatTarget(owner, target);
        assertEquals(target, store.privateChatTarget(owner).orElseThrow());

        store.clearPrivateChatTarget(owner);
        assertTrue(store.privateChatTarget(owner).isEmpty());
    }

    @Test
    void persistsGlobalChatMuteAndPrunesExpiredState() {
        Instant now = Instant.parse("2026-06-18T12:00:00Z");
        ChatControlStore store = store();

        store.setGlobalMute(new GlobalChatMute("Console", "Maintenance", now, now.plus(Duration.ofMinutes(10))));

        GlobalChatMute active = store.globalMute(now.plusSeconds(5)).orElseThrow();
        assertEquals("Console", active.issuer());
        assertEquals("Maintenance", active.reason());
        assertEquals(Duration.ofMinutes(9).plusSeconds(55), active.remaining(now.plusSeconds(5)));

        assertTrue(store.globalMute(now.plus(Duration.ofMinutes(11))).isEmpty());
        assertFalse(store.clearGlobalMute());
    }

    private ChatControlStore store() {
        return new ChatControlStore(new YamlStore(tempDir.resolve("chat-controls.yml").toFile()));
    }
}
