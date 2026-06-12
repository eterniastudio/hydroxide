package net.axther.hydroxide.modules.nickname;

import net.axther.hydroxide.text.TextFormatter;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NicknameServiceTest {

    private final TextFormatter formatter = new TextFormatter();

    @Test
    void sanitizesNicknameForProfileNameRules() {
        assertEquals("CoolName", NicknameService.sanitizeProfileName("&#44CCFFCool Name!"));
        assertEquals("Player", NicknameService.sanitizeProfileName("<gradient:#fff:#000>***</gradient>"));
        assertEquals("abcdefghijklmnop", NicknameService.sanitizeProfileName("abcdefghijklmnopqrstuvwxyz"));
    }

    @Test
    void detectsFormattingPermissionRequirements() {
        assertFalse(NicknameService.requiresColorPermission("PlainName"));
        assertTrue(NicknameService.requiresColorPermission("&aGreen"));
        assertTrue(NicknameService.requiresColorPermission("&#44CCFFHex"));
        assertTrue(NicknameService.requiresHexPermission("&#44CCFFHex"));
        assertTrue(NicknameService.requiresGradientPermission("<gradient:#44CCFF:#FFB000>Hydro</gradient>"));
        assertTrue(NicknameService.requiresRainbowPermission("<rainbow>Hydro</rainbow>"));
    }

    @Test
    void cachesAndResolvesRealNamesFromFormattedNicknames() {
        NicknameService service = new NicknameService(formatter);
        UUID playerId = UUID.randomUUID();

        service.cacheNickname(playerId, "Axther", "&#44CCFFHydro");

        assertEquals(Optional.of("Axther"), service.realName("hydro"));
        assertEquals(Optional.of("Axther"), service.realName("&#44CCFFHydro"));
        assertEquals(Optional.of("&#44CCFFHydro"), service.rawNickname(playerId));
    }

    @Test
    void validatesLengthAllowedCharactersAndBlacklistAgainstPlainNickname() {
        NicknameService.NicknamePolicy policy = new NicknameService.NicknamePolicy(
                8,
                "^[A-Za-z0-9_ .-]+$",
                java.util.List.of("badword")
        );

        assertTrue(NicknameService.validateNickname("&aHydro", policy).valid());
        assertFalse(NicknameService.validateNickname("&aHydroxideCore", policy).valid());
        assertFalse(NicknameService.validateNickname("Bad|Name", policy).valid());
        assertFalse(NicknameService.validateNickname("nicebadword", policy).valid());
    }
}
