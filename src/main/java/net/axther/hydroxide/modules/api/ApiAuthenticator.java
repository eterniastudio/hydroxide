package net.axther.hydroxide.modules.api;

import java.util.Set;

public final class ApiAuthenticator {

    private final Set<String> tokens;

    public ApiAuthenticator(Set<String> tokens) {
        this.tokens = Set.copyOf(tokens);
    }

    public boolean authorized(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return false;
        }
        String token = authorizationHeader;
        if (authorizationHeader.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            token = authorizationHeader.substring("Bearer ".length());
        } else if (authorizationHeader.regionMatches(true, 0, "ApiKey ", 0, "ApiKey ".length())) {
            token = authorizationHeader.substring("ApiKey ".length());
        }
        return !token.isBlank() && tokens.contains(token.trim());
    }
}
