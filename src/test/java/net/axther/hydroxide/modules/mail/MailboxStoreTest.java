package net.axther.hydroxide.modules.mail;

import net.axther.hydroxide.storage.YamlStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailboxStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void listsMessagesInStableChronologicalOrder() {
        UUID recipient = UUID.randomUUID();
        MailboxStore store = store();
        MailRecord newer = new MailRecord(UUID.randomUUID(), recipient, "Console", "Second", Instant.parse("2026-06-15T12:01:00Z"));
        MailRecord older = new MailRecord(UUID.randomUUID(), recipient, "Kevin", "First", Instant.parse("2026-06-15T12:00:00Z"));

        store.save(newer);
        store.save(older);

        List<MailRecord> records = store.list(recipient);

        assertEquals(List.of(older.id(), newer.id()), records.stream().map(MailRecord::id).toList());
        assertEquals(2, store.count(recipient));
    }

    @Test
    void deleteAndClearUseVisibleOneBasedIndexes() {
        UUID recipient = UUID.randomUUID();
        MailboxStore store = store();
        MailRecord first = new MailRecord(UUID.randomUUID(), recipient, "Console", "First", Instant.parse("2026-06-15T12:00:00Z"));
        MailRecord second = new MailRecord(UUID.randomUUID(), recipient, "Console", "Second", Instant.parse("2026-06-15T12:01:00Z"));
        store.save(first);
        store.save(second);

        assertTrue(store.delete(recipient, 1));
        assertEquals(List.of(second.id()), store.list(recipient).stream().map(MailRecord::id).toList());
        assertFalse(store.delete(recipient, 10));
        assertEquals(1, store.clear(recipient));
        assertTrue(store.list(recipient).isEmpty());
    }

    @Test
    void hidesExpiredTemporaryMailFromVisibleMailbox() {
        UUID recipient = UUID.randomUUID();
        MailboxStore store = store();
        Instant now = Instant.parse("2026-06-15T12:00:00Z");
        MailRecord permanent = new MailRecord(UUID.randomUUID(), recipient, "Console", "Permanent",
                now.minusSeconds(30));
        MailRecord expired = new MailRecord(UUID.randomUUID(), recipient, "Console", "Expired",
                now.minusSeconds(60), java.util.Optional.of(now.minusSeconds(1)));
        MailRecord active = new MailRecord(UUID.randomUUID(), recipient, "Console", "Active",
                now.minusSeconds(10), java.util.Optional.of(now.plusSeconds(60)));
        store.save(expired);
        store.save(active);
        store.save(permanent);

        List<MailRecord> records = store.list(recipient, now);

        assertEquals(List.of(permanent.id(), active.id()), records.stream().map(MailRecord::id).toList());
        assertEquals(2, store.count(recipient, now));
        assertTrue(records.get(1).expiresAt().isPresent());
    }

    @Test
    void persistsTemporaryMailExpirationAcrossStoreReloads() {
        UUID recipient = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-15T12:00:00Z");
        MailboxStore store = store();
        store.save(new MailRecord(UUID.randomUUID(), recipient, "Console", "Temporary",
                now, java.util.Optional.of(now.plusSeconds(120))));

        MailboxStore reloaded = store();
        MailRecord record = reloaded.list(recipient, now).getFirst();

        assertEquals(now.plusSeconds(120), record.expiresAt().orElseThrow());
    }

    @Test
    void clearAllRemovesEveryMailboxAndReturnsRemovedCount() {
        UUID firstRecipient = UUID.randomUUID();
        UUID secondRecipient = UUID.randomUUID();
        MailboxStore store = store();
        store.save(new MailRecord(UUID.randomUUID(), firstRecipient, "Console", "First",
                Instant.parse("2026-06-15T12:00:00Z")));
        store.save(new MailRecord(UUID.randomUUID(), secondRecipient, "Console", "Second",
                Instant.parse("2026-06-15T12:01:00Z")));

        int removed = store.clearAll();

        assertEquals(2, removed);
        assertTrue(store.list(firstRecipient).isEmpty());
        assertTrue(store.list(secondRecipient).isEmpty());
    }

    @Test
    void removeMessageAcrossAllMailboxesUsesExactMessageText() {
        UUID firstRecipient = UUID.randomUUID();
        UUID secondRecipient = UUID.randomUUID();
        MailboxStore store = store();
        store.save(new MailRecord(UUID.randomUUID(), firstRecipient, "Console", "Vote reminder",
                Instant.parse("2026-06-15T12:00:00Z")));
        store.save(new MailRecord(UUID.randomUUID(), firstRecipient, "Console", "Other",
                Instant.parse("2026-06-15T12:01:00Z")));
        store.save(new MailRecord(UUID.randomUUID(), secondRecipient, "Console", "Vote reminder",
                Instant.parse("2026-06-15T12:02:00Z")));

        int removed = store.removeMessage("Vote reminder");

        assertEquals(2, removed);
        assertEquals(List.of("Other"), store.list(firstRecipient).stream().map(MailRecord::message).toList());
        assertTrue(store.list(secondRecipient).isEmpty());
    }

    private MailboxStore store() {
        return new MailboxStore(new YamlStore(tempDir.resolve("mail.yml").toFile()));
    }
}
