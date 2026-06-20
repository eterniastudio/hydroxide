package net.axther.hydroxide.modules.builder;

import java.util.List;
import java.util.Optional;

final class BlockStateCycler {

    private BlockStateCycler() {
    }

    static <T> Optional<T> cycle(List<T> values, T current, BlockCyclingCommandParser.Direction direction) {
        if (values.size() < 2) {
            return Optional.empty();
        }
        int index = values.indexOf(current);
        if (index < 0) {
            return Optional.empty();
        }
        int next = direction == BlockCyclingCommandParser.Direction.FORWARD
                ? (index + 1) % values.size()
                : (index - 1 + values.size()) % values.size();
        return Optional.of(values.get(next));
    }
}
