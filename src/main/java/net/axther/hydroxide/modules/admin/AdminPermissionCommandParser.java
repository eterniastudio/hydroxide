package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;

final class AdminPermissionCommandParser {

    private AdminPermissionCommandParser() {
    }

    static Optional<CheckPermRequest> parseCheckPerm(List<String> args) {
        if (args.size() > 1) {
            return Optional.empty();
        }
        if (args.isEmpty() || args.getFirst().isBlank()) {
            return Optional.of(new CheckPermRequest(Optional.empty()));
        }
        return Optional.of(new CheckPermRequest(Optional.of(args.getFirst().trim().toLowerCase(java.util.Locale.ROOT))));
    }

    static Optional<HasPermissionRequest> parseHasPermission(List<String> args) {
        if (args.size() != 2 || args.getFirst().isBlank() || args.get(1).isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new HasPermissionRequest(args.getFirst(), args.get(1).trim().toLowerCase(java.util.Locale.ROOT)));
    }

    record CheckPermRequest(Optional<String> keyword) {
    }

    record HasPermissionRequest(String playerName, String permission) {
    }
}
