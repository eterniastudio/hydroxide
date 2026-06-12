package net.axther.hydroxide.modules.chatfilter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatFilterEngineTest {

    @Test
    void replacesBlockedRegexMatchesAndTracksStrikes() {
        ChatFilterEngine engine = new ChatFilterEngine(new ChatFilterEngine.Policy(
                List.of("bad\\w*"),
                ChatFilterEngine.FilterMode.REPLACE_ASTERISKS,
                List.of("cookie"),
                0.7D,
                1000L
        ));

        ChatFilterEngine.Result result = engine.moderate(UUID.randomUUID(), "that badword slipped", 10_000L);

        assertTrue(result.flagged());
        assertEquals("that *** slipped", result.message());
        assertEquals(1, result.strikes());
    }

    @Test
    void throttlesSpamAndNormalizesExcessiveCaps() {
        UUID playerId = UUID.randomUUID();
        ChatFilterEngine engine = new ChatFilterEngine(ChatFilterEngine.Policy.defaults());

        engine.moderate(playerId, "hello", 1000L);
        ChatFilterEngine.Result spam = engine.moderate(playerId, "HELLO AGAIN FRIEND", 1200L);

        assertTrue(spam.flagged());
        assertEquals("hello again friend", spam.message());
        assertTrue(spam.rules().contains("spam"));
        assertTrue(spam.rules().contains("caps"));
    }
}
