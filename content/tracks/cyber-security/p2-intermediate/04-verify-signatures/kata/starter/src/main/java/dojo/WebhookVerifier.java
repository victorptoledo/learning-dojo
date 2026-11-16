package dojo;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Verifies webhooks signed with HMAC-SHA256 over "t.body". Today it trusts too much: fix it. */
public final class WebhookVerifier {
    private final List<byte[]> secrets;
    private final Duration tolerance;
    private final Clock clock;

    public WebhookVerifier(List<byte[]> secrets, Duration tolerance, Clock clock) {
        this.secrets = List.copyOf(secrets);
        this.tolerance = tolerance;
        this.clock = clock;
    }

    /** True only for a fresh webhook whose v1 signature matches one of the secrets. */
    public boolean verify(byte[] rawBody, String signatureHeader) {
        String timestamp = null;
        String signature = null;
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.split("=");
            if (pair[0].equals("t")) {
                timestamp = pair[1];
            } else if (pair[0].equals("v1")) {
                signature = pair[1];
            }
        }
        String expected = hmacHex(secrets.get(0), timestamp + "." + new String(rawBody, StandardCharsets.UTF_8));
        return expected.equals(signature);
    }

    private static String hmacHex(byte[] secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
