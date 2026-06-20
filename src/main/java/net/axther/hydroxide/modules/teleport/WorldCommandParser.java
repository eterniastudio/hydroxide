package net.axther.hydroxide.modules.teleport;

import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class WorldCommandParser {

    private WorldCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        List<String> values = new ArrayList<>();
        boolean silent = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else {
                values.add(arg);
            }
        }
        if (values.isEmpty() || values.size() > 2) {
            return Optional.empty();
        }
        return Optional.of(new Request(
                selector(values.getFirst()),
                values.size() == 2 ? Optional.of(values.get(1)) : Optional.empty(),
                silent
        ));
    }

    static WorldSelector selector(String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        Optional<World.Environment> environment = switch (normalized) {
            case "normal", "overworld", "0", "1" -> Optional.of(World.Environment.NORMAL);
            case "nether", "the_nether", "2" -> Optional.of(World.Environment.NETHER);
            case "end", "the_end", "3" -> Optional.of(World.Environment.THE_END);
            default -> Optional.empty();
        };
        return new WorldSelector(input, environment);
    }

    record Request(WorldSelector selector, Optional<String> targetName, boolean silent) {
    }

    record WorldSelector(String input, Optional<World.Environment> environment) {
    }
}
