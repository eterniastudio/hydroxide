package net.axther.hydroxide.modules.builder;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndoHistoryTest {

    @Test
    void storesUndoAndRedoSnapshotsWithLimits() {
        UndoHistory history = new UndoHistory(1, Duration.ofMinutes(5));
        BlockEditPlan first = plan(Material.STONE);
        BlockEditPlan second = plan(Material.DIRT);

        history.record(first, Instant.now());
        history.record(second, Instant.now());

        assertEquals(second, history.undo(Instant.now()).orElseThrow());
        assertTrue(history.undo(Instant.now()).isEmpty());
        assertEquals(second, history.redo(Instant.now()).orElseThrow());
    }

    private BlockEditPlan plan(Material material) {
        return new BlockEditPlan(List.of(new BlockChange(new BlockVector3i(0, 0, 0), Material.AIR, material)));
    }
}
