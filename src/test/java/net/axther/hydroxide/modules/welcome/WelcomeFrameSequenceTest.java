package net.axther.hydroxide.modules.welcome;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WelcomeFrameSequenceTest {

    @Test
    void returnsFramesByTickAndLoopsWhenConfigured() {
        WelcomeFrameSequence sequence = new WelcomeFrameSequence(List.of(
                new WelcomeFrameSequence.Frame("W", "One"),
                new WelcomeFrameSequence.Frame("We", "Two"),
                new WelcomeFrameSequence.Frame("Welcome", "Three")
        ), 5, true);

        assertEquals("W", sequence.frameAtTick(0).title());
        assertEquals("We", sequence.frameAtTick(7).title());
        assertEquals("Welcome", sequence.frameAtTick(14).title());
        assertEquals("W", sequence.frameAtTick(15).title());
    }
}
