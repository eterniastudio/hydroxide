package net.axther.hydroxide.modules.economy;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChequeTokenSignerTest {

    @Test
    void acceptsUntamperedChequeTokens() {
        ChequeTokenSigner signer = new ChequeTokenSigner("server-secret");
        UUID id = UUID.randomUUID();
        double amount = 125.25;

        String signature = signer.sign(id, amount);

        assertTrue(signer.valid(id, amount, signature));
    }

    @Test
    void rejectsChangedAmountIdOrSignature() {
        ChequeTokenSigner signer = new ChequeTokenSigner("server-secret");
        UUID id = UUID.randomUUID();
        double amount = 125.25;
        String signature = signer.sign(id, amount);

        assertFalse(signer.valid(id, amount + 1.0, signature));
        assertFalse(signer.valid(UUID.randomUUID(), amount, signature));
        assertFalse(signer.valid(id, amount, signature.substring(1) + "x"));
    }
}
