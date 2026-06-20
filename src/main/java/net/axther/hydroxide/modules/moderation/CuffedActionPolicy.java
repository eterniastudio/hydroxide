package net.axther.hydroxide.modules.moderation;

import java.util.Locale;
import java.util.Set;

final class CuffedActionPolicy {

    private static final Set<String> RECOVERY_COMMANDS = Set.of("cuff", "handcuff", "hydroxide");

    private CuffedActionPolicy() {
    }

    static boolean shouldCancel(boolean cuffed, Action action) {
        return cuffed && action != null;
    }

    static boolean shouldCancelCommand(boolean cuffed, String message) {
        if (!cuffed) {
            return false;
        }
        String command = message == null ? "" : message.stripLeading();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        String root = command.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return !RECOVERY_COMMANDS.contains(root);
    }

    enum Action {
        MOVE,
        INTERACT,
        INVENTORY,
        DROP,
        CHAT,
        BLOCK_BREAK,
        BLOCK_PLACE
    }
}
