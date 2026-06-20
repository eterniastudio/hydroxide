package net.axther.hydroxide.modules.economy;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

final class ChequeTokenSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    ChequeTokenSigner(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Cheque signing secret cannot be blank.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    String sign(UUID id, double amount) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload(id, amount).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign cheque token.", exception);
        }
    }

    boolean valid(UUID id, double amount, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(sign(id, amount).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private String payload(UUID id, double amount) {
        return id + ":" + ChequeAmountParser.canonical(amount);
    }
}
