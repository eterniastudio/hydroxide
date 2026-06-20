package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClearEnderCommandParserTest {

    @Test
    void defaultsToSenderAndParsesSilentFlag() {
        ClearEnderCommandParser.Request self = ClearEnderCommandParser.parse(List.of()).orElseThrow();
        ClearEnderCommandParser.Request silentSelf = ClearEnderCommandParser.parse(List.of("-s")).orElseThrow();

        assertTrue(self.targetName().isEmpty());
        assertTrue(!self.silent());
        assertTrue(silentSelf.targetName().isEmpty());
        assertTrue(silentSelf.silent());
    }

    @Test
    void parsesTargetAndSilentFlag() {
        ClearEnderCommandParser.Request request = ClearEnderCommandParser.parse(List.of("Alex", "-s")).orElseThrow();

        assertEquals("Alex", request.targetName().orElseThrow());
        assertTrue(request.silent());
    }

    @Test
    void rejectsUnknownFlagsAndExtraTargets() {
        assertTrue(ClearEnderCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(ClearEnderCommandParser.parse(List.of("Alex", "Blake")).isEmpty());
        assertTrue(ClearEnderCommandParser.parse(List.of("Alex", "-s", "Blake")).isEmpty());
    }
}
