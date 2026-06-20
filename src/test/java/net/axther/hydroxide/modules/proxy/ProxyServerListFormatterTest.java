package net.axther.hydroxide.modules.proxy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyServerListFormatterTest {

    @Test
    void sortsConfiguredServersAndDropsBlankNames() {
        ProxyServerListFormatter.Snapshot snapshot = ProxyServerListFormatter.snapshot(List.of(
                "survival",
                "Lobby",
                " ",
                "creative"
        ), "");

        assertEquals(3, snapshot.totalCount());
        assertEquals(3, snapshot.shownCount());
        assertEquals(List.of("creative", "Lobby", "survival"), snapshot.servers());
    }

    @Test
    void filtersConfiguredServersCaseInsensitively() {
        ProxyServerListFormatter.Snapshot snapshot = ProxyServerListFormatter.snapshot(List.of(
                "lobby",
                "minigames",
                "survival"
        ), "iv");

        assertEquals(3, snapshot.totalCount());
        assertEquals(1, snapshot.shownCount());
        assertEquals(List.of("survival"), snapshot.servers());
    }

    @Test
    void handlesEmptyConfiguredServers() {
        ProxyServerListFormatter.Snapshot snapshot = ProxyServerListFormatter.snapshot(List.of(), "");

        assertTrue(snapshot.servers().isEmpty());
        assertEquals(0, snapshot.totalCount());
        assertEquals(0, snapshot.shownCount());
    }
}
