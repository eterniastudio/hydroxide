package net.axther.hydroxide.modules.admin;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;

final class AdminSolveExpression {

    private static final Map<String, DoubleUnaryOperator> FUNCTIONS = Map.ofEntries(
            Map.entry("abs", Math::abs),
            Map.entry("acos", Math::acos),
            Map.entry("asin", Math::asin),
            Map.entry("atan", Math::atan),
            Map.entry("ceil", Math::ceil),
            Map.entry("cos", Math::cos),
            Map.entry("floor", Math::floor),
            Map.entry("ln", Math::log),
            Map.entry("log", Math::log10),
            Map.entry("round", value -> (double) Math.round(value)),
            Map.entry("sin", Math::sin),
            Map.entry("sqrt", Math::sqrt),
            Map.entry("tan", Math::tan)
    );

    private AdminSolveExpression() {
    }

    static Optional<Double> evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            return Optional.empty();
        }
        try {
            Parser parser = new Parser(expression);
            double value = parser.parse();
            if (!Double.isFinite(value)) {
                return Optional.empty();
            }
            return Optional.of(value);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static final class Parser {

        private final String input;
        private int position;

        private Parser(String input) {
            this.input = input;
        }

        private double parse() {
            double value = expression();
            skipWhitespace();
            if (position != input.length()) {
                throw new IllegalArgumentException("Unexpected trailing input");
            }
            return value;
        }

        private double expression() {
            double value = term();
            while (true) {
                skipWhitespace();
                if (consume('+')) {
                    value += term();
                } else if (consume('-')) {
                    value -= term();
                } else {
                    return value;
                }
            }
        }

        private double term() {
            double value = power();
            while (true) {
                skipWhitespace();
                if (consume('*')) {
                    value *= power();
                } else if (consume('/')) {
                    value /= power();
                } else if (consume('%')) {
                    value %= power();
                } else {
                    return value;
                }
            }
        }

        private double power() {
            double value = unary();
            skipWhitespace();
            if (consume('^')) {
                value = Math.pow(value, power());
            }
            return value;
        }

        private double unary() {
            skipWhitespace();
            if (consume('+')) {
                return unary();
            }
            if (consume('-')) {
                return -unary();
            }
            return primary();
        }

        private double primary() {
            skipWhitespace();
            if (consume('(')) {
                double value = expression();
                if (!consume(')')) {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }
                return value;
            }
            if (peekDigitOrDot()) {
                return number();
            }
            if (peekLetter()) {
                String name = identifier();
                if (consume('(')) {
                    DoubleUnaryOperator function = FUNCTIONS.get(name);
                    if (function == null) {
                        throw new IllegalArgumentException("Unknown function");
                    }
                    double argument = expression();
                    if (!consume(')')) {
                        throw new IllegalArgumentException("Missing function parenthesis");
                    }
                    return function.applyAsDouble(argument);
                }
                return constant(name);
            }
            throw new IllegalArgumentException("Expected value");
        }

        private double number() {
            int start = position;
            while (position < input.length()
                    && (Character.isDigit(input.charAt(position)) || input.charAt(position) == '.')) {
                position++;
            }
            if (position < input.length() && (input.charAt(position) == 'e' || input.charAt(position) == 'E')) {
                position++;
                if (position < input.length() && (input.charAt(position) == '+' || input.charAt(position) == '-')) {
                    position++;
                }
                while (position < input.length() && Character.isDigit(input.charAt(position))) {
                    position++;
                }
            }
            try {
                return Double.parseDouble(input.substring(start, position));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid number", exception);
            }
        }

        private String identifier() {
            int start = position;
            while (position < input.length() && Character.isLetter(input.charAt(position))) {
                position++;
            }
            return input.substring(start, position).toLowerCase(Locale.ROOT);
        }

        private double constant(String name) {
            return switch (name) {
                case "pi" -> Math.PI;
                case "e" -> Math.E;
                default -> throw new IllegalArgumentException("Unknown constant");
            };
        }

        private boolean consume(char expected) {
            skipWhitespace();
            if (position < input.length() && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }

        private boolean peekDigitOrDot() {
            return position < input.length()
                    && (Character.isDigit(input.charAt(position)) || input.charAt(position) == '.');
        }

        private boolean peekLetter() {
            return position < input.length() && Character.isLetter(input.charAt(position));
        }
    }
}
