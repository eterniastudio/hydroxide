package net.axther.hydroxide.modules.jail;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JailSentenceTest {

    @Test
    void calculatesRemainingSecondsAndExpiry() {
        Instant now = Instant.parse("2026-06-11T12:00:00Z");
        JailSentence sentence = new JailSentence(
                UUID.randomUUID(),
                "cell-a",
                UUID.randomUUID(),
                "Testing",
                now.plusSeconds(30),
                null
        );

        assertEquals(30, sentence.remainingSeconds(now));
        assertFalse(sentence.expired(now));
        assertTrue(sentence.expired(now.plusSeconds(31)));
    }
}
