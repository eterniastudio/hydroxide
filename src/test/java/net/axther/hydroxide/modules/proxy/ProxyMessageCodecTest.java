package net.axther.hydroxide.modules.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProxyMessageCodecTest {

    @Test
    void roundTripsGlobalAlertPayloads() {
        ProxyMessage message = new ProxyMessage("GlobalAlert", "staff", "Axther banned Example");

        ProxyMessage decoded = ProxyMessageCodec.decode(ProxyMessageCodec.encode(message));

        assertEquals(message.subchannel(), decoded.subchannel());
        assertEquals(message.target(), decoded.target());
        assertEquals(message.payload(), decoded.payload());
    }
}
