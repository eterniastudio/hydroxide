package net.axther.hydroxide.modules.afk;

import java.util.List;

final class AfkCommandCatalog {

    private static final List<String> COMMANDS = List.of("afk", "afkcheck");

    private AfkCommandCatalog() {
    }

    static List<String> commands() {
        return COMMANDS;
    }
}
