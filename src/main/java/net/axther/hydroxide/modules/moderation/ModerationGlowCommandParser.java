package net.axther.hydroxide.modules.moderation;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ModerationGlowCommandParser {

    private ModerationGlowCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Optional.empty(), Action.TOGGLE, Optional.empty()));
        }
        if (args.size() > 2) {
            return Optional.empty();
        }
        if (args.size() == 1) {
            Optional<State> state = State.from(args.getFirst());
            return state.map(value -> new Request(Optional.empty(), value.action(), value.color()))
                    .or(() -> Optional.of(new Request(Optional.of(args.getFirst()), Action.TOGGLE, Optional.empty())));
        }
        Optional<State> state = State.from(args.get(1));
        return state.map(value -> new Request(Optional.of(args.getFirst()), value.action(), value.color()));
    }

    static List<String> stateAndColorKeys() {
        return List.of("on", "off", "toggle", "red", "gold", "yellow", "green", "aqua", "blue", "pink", "white");
    }

    enum Action {
        ENABLE,
        DISABLE,
        TOGGLE,
        COLOR
    }

    record Request(Optional<String> targetName, Action action, Optional<NamedTextColor> color) {
    }

    private record State(Action action, Optional<NamedTextColor> color) {

        private static Optional<State> from(String input) {
            return switch (input.toLowerCase(Locale.ROOT).replace('-', '_')) {
                case "true", "on", "enable", "enabled" -> Optional.of(new State(Action.ENABLE, Optional.empty()));
                case "false", "off", "disable", "disabled", "clear", "none" -> Optional.of(new State(Action.DISABLE, Optional.empty()));
                case "toggle" -> Optional.of(new State(Action.TOGGLE, Optional.empty()));
                case "black" -> color(NamedTextColor.BLACK);
                case "dark_blue" -> color(NamedTextColor.DARK_BLUE);
                case "dark_green" -> color(NamedTextColor.DARK_GREEN);
                case "dark_aqua" -> color(NamedTextColor.DARK_AQUA);
                case "dark_red" -> color(NamedTextColor.DARK_RED);
                case "dark_purple", "purple" -> color(NamedTextColor.DARK_PURPLE);
                case "gold", "orange" -> color(NamedTextColor.GOLD);
                case "gray", "grey" -> color(NamedTextColor.GRAY);
                case "dark_gray", "dark_grey" -> color(NamedTextColor.DARK_GRAY);
                case "blue" -> color(NamedTextColor.BLUE);
                case "green" -> color(NamedTextColor.GREEN);
                case "aqua", "cyan" -> color(NamedTextColor.AQUA);
                case "red" -> color(NamedTextColor.RED);
                case "light_purple", "pink", "magenta" -> color(NamedTextColor.LIGHT_PURPLE);
                case "yellow" -> color(NamedTextColor.YELLOW);
                case "white" -> color(NamedTextColor.WHITE);
                default -> Optional.empty();
            };
        }

        private static Optional<State> color(NamedTextColor color) {
            return Optional.of(new State(Action.COLOR, Optional.of(color)));
        }
    }
}
