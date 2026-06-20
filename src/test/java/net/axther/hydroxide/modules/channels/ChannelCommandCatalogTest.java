package net.axther.hydroxide.modules.channels;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelCommandCatalogTest {

    @Test
    void declaresCmiStyleStaffMessageCommandForStaffChannel() {
        Map<String, ChannelCommandCatalog.QuickCommand> commands = ChannelCommandCatalog.quickCommands().stream()
                .collect(Collectors.toMap(ChannelCommandCatalog.QuickCommand::command, Function.identity()));

        ChannelCommandCatalog.QuickCommand staff = commands.get("staffmsg");

        assertEquals("staff", staff.channelId());
        assertEquals("hydroxide.channel.staff", staff.permission());
        assertTrue(staff.aliases().contains("staffchat"));
        assertTrue(staff.aliases().contains("schat"));
        assertTrue(commands.containsKey("sc"));
    }
}
