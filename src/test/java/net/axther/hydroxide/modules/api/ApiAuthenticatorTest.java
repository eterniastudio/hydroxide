package net.axther.hydroxide.modules.api;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiAuthenticatorTest {

    @Test
    void acceptsBearerOrApiKeyTokensOnlyWhenConfigured() {
        ApiAuthenticator authenticator = new ApiAuthenticator(Set.of("alpha", "bravo"));

        assertTrue(authenticator.authorized("Bearer alpha"));
        assertTrue(authenticator.authorized("ApiKey bravo"));
        assertFalse(authenticator.authorized("Bearer wrong"));
        assertFalse(authenticator.authorized(""));
    }
}
