package net.axther.hydroxide.modules.announcement;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnouncementCommandParserTest {

    @Test
    void parsesActionbarAudienceDurationAndMessage() {
        AnnouncementCommandParser.TimedMessage request = AnnouncementCommandParser.actionbar(
                List.of("all", "-s:5", "<green>Restart soon")
        ).orElseThrow();

        assertTrue(request.target().all());
        assertEquals(Duration.ofSeconds(5), request.duration());
        assertEquals("<green>Restart soon", request.message());
    }

    @Test
    void parsesBossbarPlayerDurationAndMessage() {
        AnnouncementCommandParser.TimedMessage request = AnnouncementCommandParser.bossbar(
                List.of("Steve", "-sec:8", "<red>Combat event")
        ).orElseThrow();

        assertEquals("Steve", request.target().name());
        assertEquals(Duration.ofSeconds(8), request.duration());
        assertEquals("<red>Combat event", request.message());
    }

    @Test
    void parsesTitleWithSubtitleAndTimingFlags() {
        AnnouncementCommandParser.TitleMessage request = AnnouncementCommandParser.title(
                List.of("all", "-in:5", "-keep:40", "-out:10", "<gold>Welcome", "\\n", "<gray>Enjoy")
        ).orElseThrow();

        assertTrue(request.target().all());
        assertEquals("<gold>Welcome", request.title());
        assertEquals("<gray>Enjoy", request.subtitle());
        assertEquals(5, request.fadeInTicks());
        assertEquals(40, request.stayTicks());
        assertEquals(10, request.fadeOutTicks());
    }

    @Test
    void parsesTellRawTargetAndFormattedMessage() {
        AnnouncementCommandParser.FormattedMessage request = AnnouncementCommandParser.tellRaw(
                List.of("@a", "<hover:show_text:'<gray>Details'><green>Click me</hover>")
        ).orElseThrow();

        assertTrue(request.target().all());
        assertEquals("<hover:show_text:'<gray>Details'><green>Click me</hover>", request.message());
    }

    @Test
    void rejectsMissingTargetOrMessage() {
        assertTrue(AnnouncementCommandParser.actionbar(List.of()).isEmpty());
        assertTrue(AnnouncementCommandParser.actionbar(List.of("all")).isEmpty());
        assertTrue(AnnouncementCommandParser.bossbar(List.of("Steve", "-sec:5")).isEmpty());
        assertTrue(AnnouncementCommandParser.title(List.of("all", "-keep:20")).isEmpty());
        assertTrue(AnnouncementCommandParser.tellRaw(List.of("all")).isEmpty());
    }
}
