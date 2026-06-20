package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpBanTargetPolicyTest {

    @Test
    void ipLiteralsDoNotRequireAProfileBan() {
        assertFalse(IpBanTargetPolicy.shouldBanProfile("192.0.2.10"));
        assertFalse(IpBanTargetPolicy.shouldBanProfile("2001:db8::1"));
    }

    @Test
    void playerNameTargetsRequireAProfileBanForCmiCompatibility() {
        assertTrue(IpBanTargetPolicy.shouldBanProfile("Steve"));
        assertTrue(IpBanTargetPolicy.shouldBanProfile("Axther_"));
    }
}
