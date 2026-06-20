package net.axther.hydroxide.modules.mail;

import java.util.List;

final class MailCommandCatalog {

    private static final String COMMAND = "mail";
    private static final List<String> ACTIONS = List.of("read", "send", "sendtemp", "delete", "clear", "sendall");

    private MailCommandCatalog() {
    }

    static String command() {
        return COMMAND;
    }

    static List<String> actions() {
        return ACTIONS;
    }
}
