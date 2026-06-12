package net.axther.hydroxide.modules.channels;

import java.util.List;

public record ChatParticipant(String world, double x, double y, double z, List<String> permissions) {
    public boolean hasPermission(String permission) {
        return permission == null || permission.isBlank() || permissions.contains(permission);
    }
}
