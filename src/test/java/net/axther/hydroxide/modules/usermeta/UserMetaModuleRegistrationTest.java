package net.axther.hydroxide.modules.usermeta;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMetaModuleRegistrationTest {

    @Test
    void hydroxideRegistersUserMetaModule() throws IOException {
        String source = Files.readString(Path.of("src/main/java/net/axther/hydroxide/Hydroxide.java"));

        assertTrue(source.contains("new UserMetaModule()"));
    }
}
