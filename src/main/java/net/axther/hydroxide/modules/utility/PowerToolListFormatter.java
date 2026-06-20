package net.axther.hydroxide.modules.utility;

import java.util.Locale;
import java.util.Map;

final class PowerToolListFormatter {

    private PowerToolListFormatter() {
    }

    static Map<String, Object> placeholders(AttachedCommand command) {
        return Map.of(
                "click", command.click().name().toLowerCase(Locale.ROOT),
                "executor", command.executor().name().toLowerCase(Locale.ROOT),
                "uses", command.usesRemaining() < 0 ? "infinite" : command.usesRemaining(),
                "command", command.command()
        );
    }
}
