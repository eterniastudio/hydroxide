package net.axther.hydroxide.modules.backup;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public final class BackupRotationPolicy {

    private final int maxBackups;

    public BackupRotationPolicy(int maxBackups) {
        this.maxBackups = Math.max(1, maxBackups);
    }

    public List<BackupFile> toDelete(List<BackupFile> backups) {
        int deleteCount = Math.max(0, backups.size() - maxBackups);
        return backups.stream()
                .sorted(Comparator.comparing(BackupFile::createdAt))
                .limit(deleteCount)
                .toList();
    }

    public record BackupFile(String name, Instant createdAt) {
    }
}
