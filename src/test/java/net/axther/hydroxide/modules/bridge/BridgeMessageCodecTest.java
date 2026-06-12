package net.axther.hydroxide.modules.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BridgeMessageCodecTest {

    @Test
    void encodesAndDecodesEscapedJsonPayloads() {
        BridgeMessage message = new BridgeMessage("lobby", "chat", "Axther", "hello \"network\"");

        BridgeMessage decoded = BridgeMessageCodec.decode(BridgeMessageCodec.encode(message));

        assertEquals(message.serverId(), decoded.serverId());
        assertEquals(message.channel(), decoded.channel());
        assertEquals(message.sender(), decoded.sender());
        assertEquals(message.message(), decoded.message());
    }
}
