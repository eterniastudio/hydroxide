package net.axther.hydroxide.modules.bridge;

public record BridgeMessage(String serverId, String channel, String sender, String message) {
}
