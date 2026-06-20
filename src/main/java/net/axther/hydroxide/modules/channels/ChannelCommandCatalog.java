package net.axther.hydroxide.modules.channels;

import java.util.List;

final class ChannelCommandCatalog {

    private static final List<QuickCommand> QUICK_COMMANDS = List.of(
            new QuickCommand("g", "global", "hydroxide.command.channel", List.of()),
            new QuickCommand("l", "local", "hydroxide.command.channel", List.of()),
            new QuickCommand("sc", "staff", "hydroxide.channel.staff", List.of()),
            new QuickCommand("staffmsg", "staff", "hydroxide.channel.staff", List.of("staffchat", "schat")),
            new QuickCommand("trade", "trade", "hydroxide.channel.trade", List.of())
    );

    private ChannelCommandCatalog() {
    }

    static List<QuickCommand> quickCommands() {
        return QUICK_COMMANDS;
    }

    record QuickCommand(String command, String channelId, String permission, List<String> aliases) {
    }
}
