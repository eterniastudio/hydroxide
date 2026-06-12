package net.axther.hydroxide.modules.channels;

public record ChatChannel(String id, String displayName, double radius, String permission, boolean autoJoin) {
    public boolean local() {
        return radius > 0.0D;
    }
}
