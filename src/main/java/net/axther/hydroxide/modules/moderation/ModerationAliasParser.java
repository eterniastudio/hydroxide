package net.axther.hydroxide.modules.moderation;

import org.bukkit.GameMode;

import java.util.Locale;
import java.util.Optional;

final class ModerationAliasParser {

    private ModerationAliasParser() {
    }

    static Optional<GameMode> gameModeFromLabel(String label) {
        return switch (label.toLowerCase(Locale.ROOT)) {
            case "gmc" -> Optional.of(GameMode.CREATIVE);
            case "gms" -> Optional.of(GameMode.SURVIVAL);
            case "gma" -> Optional.of(GameMode.ADVENTURE);
            case "gmsp" -> Optional.of(GameMode.SPECTATOR);
            default -> Optional.empty();
        };
    }

    static Optional<String> speedTypeFromLabel(String label) {
        return switch (label.toLowerCase(Locale.ROOT)) {
            case "flyspeed" -> Optional.of("fly");
            case "walkspeed" -> Optional.of("walk");
            default -> Optional.empty();
        };
    }
}
