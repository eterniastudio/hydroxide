package net.axther.hydroxide.modules.environment;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentCommandCatalogTest {

    @Test
    void includesWorldAndPersonalEnvironmentCommands() {
        assertTrue(EnvironmentCommandCatalog.commands().containsAll(List.of(
                "time",
                "day",
                "night",
                "weather",
                "sun",
                "storm",
                "thunder",
                "ptime",
                "pweather"
        )));
    }
}
