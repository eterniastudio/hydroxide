package net.axther.hydroxide.modules.builder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class UndoHistory {

    private final int maxSnapshots;
    private final Duration expiry;
    private final Deque<Entry> undo = new ArrayDeque<>();
    private final Deque<Entry> redo = new ArrayDeque<>();

    public UndoHistory(int maxSnapshots, Duration expiry) {
        this.maxSnapshots = Math.max(1, maxSnapshots);
        this.expiry = expiry;
    }

    public void record(BlockEditPlan plan, Instant now) {
        undo.addLast(new Entry(plan, now));
        redo.clear();
        trim(undo);
    }

    public Optional<BlockEditPlan> undo(Instant now) {
        prune(now);
        Entry entry = undo.pollLast();
        if (entry == null) {
            return Optional.empty();
        }
        redo.addLast(new Entry(entry.plan(), now));
        return Optional.of(entry.plan());
    }

    public Optional<BlockEditPlan> redo(Instant now) {
        prune(now);
        Entry entry = redo.pollLast();
        if (entry == null) {
            return Optional.empty();
        }
        undo.addLast(new Entry(entry.plan(), now));
        trim(undo);
        return Optional.of(entry.plan());
    }

    private void trim(Deque<Entry> entries) {
        while (entries.size() > maxSnapshots) {
            entries.removeFirst();
        }
    }

    private void prune(Instant now) {
        undo.removeIf(entry -> expired(entry, now));
        redo.removeIf(entry -> expired(entry, now));
    }

    private boolean expired(Entry entry, Instant now) {
        return expiry != null && entry.createdAt().plus(expiry).isBefore(now);
    }

    private record Entry(BlockEditPlan plan, Instant createdAt) {
    }
}
