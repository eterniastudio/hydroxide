package net.axther.hydroxide.modules.motd;

import java.util.List;

final class ServerInfoCommandCatalog {

    private static final List<String> COMMANDS = List.of("motd", "info", "rules", "ctext", "editctext", "helpop", "list", "ping", "gc", "tps",
            "servertime", "setmotd", "status", "maxplayers");

    private ServerInfoCommandCatalog() {
    }

    static List<String> commands() {
        return COMMANDS;
    }
}
