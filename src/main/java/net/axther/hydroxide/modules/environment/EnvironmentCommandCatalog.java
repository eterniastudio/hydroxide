package net.axther.hydroxide.modules.environment;

import java.util.List;

final class EnvironmentCommandCatalog {

    private static final List<String> COMMANDS = List.of(
            "time",
            "day",
            "night",
            "weather",
            "sun",
            "storm",
            "thunder",
            "ptime",
            "pweather"
    );

    private EnvironmentCommandCatalog() {
    }

    static List<String> commands() {
        return COMMANDS;
    }
}
