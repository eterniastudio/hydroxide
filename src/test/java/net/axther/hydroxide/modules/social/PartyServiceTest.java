package net.axther.hydroxide.modules.social;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartyServiceTest {

    @Test
    void acceptsInvitesAndBlocksFriendlyFireWithinParty() {
        PartyService service = new PartyService();
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();

        service.invite(leader, member);
        service.accept(member);

        assertTrue(service.sameParty(leader, member));
        assertFalse(service.friendlyFireAllowed(leader, member));
        service.setFriendlyFire(leader, true);
        assertTrue(service.friendlyFireAllowed(leader, member));
    }
}
