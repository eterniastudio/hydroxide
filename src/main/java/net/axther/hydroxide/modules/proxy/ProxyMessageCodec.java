package net.axther.hydroxide.modules.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class ProxyMessageCodec {

    private ProxyMessageCodec() {
    }

    public static byte[] encode(ProxyMessage message) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeUTF(message.subchannel());
            output.writeUTF(message.target());
            output.writeUTF(message.payload());
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode proxy message", exception);
        }
    }

    public static ProxyMessage decode(byte[] payload) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            return new ProxyMessage(input.readUTF(), input.readUTF(), input.readUTF());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to decode proxy message", exception);
        }
    }
}
