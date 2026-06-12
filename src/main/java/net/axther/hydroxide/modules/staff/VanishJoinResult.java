package net.axther.hydroxide.modules.staff;

public record VanishJoinResult(
        boolean vanished,
        boolean changed,
        VanishReason reason
) {
}
