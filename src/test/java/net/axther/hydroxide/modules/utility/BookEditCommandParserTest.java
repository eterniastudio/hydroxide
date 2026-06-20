package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookEditCommandParserTest {

    @Test
    void parsesTitleAuthorAndAddPageActions() {
        BookEditCommandParser.Request title = BookEditCommandParser.parse(List.of("title", "Server", "Guide")).orElseThrow();
        BookEditCommandParser.Request author = BookEditCommandParser.parse(List.of("author", "Admin")).orElseThrow();
        BookEditCommandParser.Request addPage = BookEditCommandParser.parse(List.of("addpage", "Welcome")).orElseThrow();

        assertEquals(BookEditCommandParser.Action.TITLE, title.action());
        assertEquals(1, title.valueStartIndex());
        assertEquals(BookEditCommandParser.Action.AUTHOR, author.action());
        assertEquals(BookEditCommandParser.Action.ADD_PAGE, addPage.action());
    }

    @Test
    void parsesPageActionWithOneBasedPageNumber() {
        BookEditCommandParser.Request request = BookEditCommandParser.parse(List.of("page", "2", "Rules")).orElseThrow();

        assertEquals(BookEditCommandParser.Action.PAGE, request.action());
        assertEquals(2, request.page().orElseThrow());
        assertEquals(2, request.valueStartIndex());
    }

    @Test
    void parsesUnlockWithoutValue() {
        BookEditCommandParser.Request request = BookEditCommandParser.parse(List.of("unlock")).orElseThrow();

        assertEquals(BookEditCommandParser.Action.UNLOCK, request.action());
        assertTrue(request.page().isEmpty());
    }

    @Test
    void rejectsMissingValuesInvalidPagesAndUnknownActions() {
        assertTrue(BookEditCommandParser.parse(List.of()).isEmpty());
        assertTrue(BookEditCommandParser.parse(List.of("title")).isEmpty());
        assertTrue(BookEditCommandParser.parse(List.of("author")).isEmpty());
        assertTrue(BookEditCommandParser.parse(List.of("addpage")).isEmpty());
        assertTrue(BookEditCommandParser.parse(List.of("page", "0", "Rules")).isEmpty());
        assertTrue(BookEditCommandParser.parse(List.of("page", "two", "Rules")).isEmpty());
        assertTrue(BookEditCommandParser.parse(List.of("unlock", "extra")).isEmpty());
        assertTrue(BookEditCommandParser.parse(List.of("unknown", "value")).isEmpty());
    }
}
