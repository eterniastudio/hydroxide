package net.axther.hydroxide.commands;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompletionUtilsTest {

    @Test
    void returnsSortedPrefixMatches() {
        assertEquals(List.of("delete", "disable"), CompletionUtils.matching("d", List.of("set", "disable", "delete")));
    }

    @Test
    void suggestsInclusiveIntegerRanges() {
        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), CompletionUtils.integerRange("", 1, 10));
        assertEquals(List.of("1", "10"), CompletionUtils.integerRange("1", 1, 10));
        assertEquals(List.of("3"), CompletionUtils.integerRange("3", 1, 10));
    }
}
