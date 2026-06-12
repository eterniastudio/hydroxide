package net.axther.hydroxide.modules.proxy;

public record ProxyMessage(String subchannel, String target, String payload) {
}
