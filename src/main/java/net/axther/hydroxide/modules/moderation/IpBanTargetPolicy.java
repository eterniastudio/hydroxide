package net.axther.hydroxide.modules.moderation;

final class IpBanTargetPolicy {

    private IpBanTargetPolicy() {
    }

    static boolean shouldBanProfile(String input) {
        return !looksLikeIpLiteral(input);
    }

    static boolean looksLikeIpLiteral(String input) {
        return input.matches("\\d{1,3}(\\.\\d{1,3}){3}") || input.contains(":");
    }
}
