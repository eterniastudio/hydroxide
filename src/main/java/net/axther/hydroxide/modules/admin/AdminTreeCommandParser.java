package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;

final class AdminTreeCommandParser {

    private AdminTreeCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        return parse(args, null);
    }

    static Optional<Request> parse(List<String> args, String defaultTreeTypeName) {
        if (args.size() > 2) {
            return Optional.empty();
        }
        Optional<String> treeTypeName = Optional.empty();
        Optional<String> targetName = Optional.empty();
        for (String arg : args) {
            if (arg.regionMatches(true, 0, "-p:", 0, 3)) {
                if (targetName.isPresent()) {
                    return Optional.empty();
                }
                String value = arg.substring(3).trim();
                if (value.isBlank()) {
                    return Optional.empty();
                }
                targetName = Optional.of(value);
            } else if (arg.startsWith("-")) {
                return Optional.empty();
            } else {
                if (treeTypeName.isPresent()) {
                    return Optional.empty();
                }
                treeTypeName = Optional.of(arg);
            }
        }
        if (treeTypeName.isEmpty() && defaultTreeTypeName != null && !defaultTreeTypeName.isBlank()) {
            treeTypeName = Optional.of(defaultTreeTypeName);
        }
        return Optional.of(new Request(treeTypeName, targetName));
    }

    record Request(Optional<String> treeTypeName, Optional<String> targetName) {
    }
}
