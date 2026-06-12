package net.axther.hydroxide.modules.backup;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackupRotationPolicyTest {

    @Test
    void choosesOldestBackupsForDeletionBeyondRetentionLimit() {
        BackupRotationPolicy policy = new BackupRotationPolicy(2);
        List<BackupRotationPolicy.BackupFile> deletions = policy.toDelete(List.of(
                new BackupRotationPolicy.BackupFile("new.zip", Instant.parse("2026-01-03T00:00:00Z")),
                new BackupRotationPolicy.BackupFile("old.zip", Instant.parse("2026-01-01T00:00:00Z")),
                new BackupRotationPolicy.BackupFile("mid.zip", Instant.parse("2026-01-02T00:00:00Z"))
        ));

        assertEquals(1, deletions.size());
        assertEquals("old.zip", deletions.get(0).name());
    }
}
