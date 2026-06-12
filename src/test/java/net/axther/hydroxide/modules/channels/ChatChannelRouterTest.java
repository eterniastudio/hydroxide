package net.axther.hydroxide.modules.channels;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatChannelRouterTest {

    @Test
    void localChannelUsesRadiusAndWorldWhileStaffRequiresPermission() {
        ChatChannelRouter router = new ChatChannelRouter(List.of(
                new ChatChannel("local", "Local", 100.0D, "", true),
                new ChatChannel("staff", "Staff", -1.0D, "hydroxide.channel.staff", false)
        ));

        ChatParticipant sender = new ChatParticipant("world", 0.0D, 64.0D, 0.0D, List.of());
        ChatParticipant near = new ChatParticipant("world", 50.0D, 64.0D, 0.0D, List.of());
        ChatParticipant far = new ChatParticipant("world", 120.0D, 64.0D, 0.0D, List.of());
        ChatParticipant staff = new ChatParticipant("world", 200.0D, 64.0D, 0.0D, List.of("hydroxide.channel.staff"));

        assertTrue(router.canReceive("local", sender, near));
        assertFalse(router.canReceive("local", sender, far));
        assertTrue(router.canReceive("staff", sender, staff));
        assertFalse(router.canReceive("staff", sender, near));
    }
}
