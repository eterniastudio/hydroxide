package net.axther.hydroxide.modules.builder;

import java.util.ArrayList;
import java.util.List;

public record BlockEditPlan(List<BlockChange> changes) {

    public BlockEditPlan {
        changes = List.copyOf(changes);
    }

    public List<List<BlockChange>> batches(int batchSize) {
        int size = Math.max(1, batchSize);
        List<List<BlockChange>> batches = new ArrayList<>();
        for (int index = 0; index < changes.size(); index += size) {
            batches.add(changes.subList(index, Math.min(changes.size(), index + size)));
        }
        return List.copyOf(batches);
    }

    public BlockEditPlan inverse() {
        return new BlockEditPlan(changes.stream().map(BlockChange::inverse).toList());
    }
}
