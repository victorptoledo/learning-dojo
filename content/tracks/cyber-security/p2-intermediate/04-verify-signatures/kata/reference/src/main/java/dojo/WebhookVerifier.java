package dojo;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Verifies webhooks signed with HMAC-SHA256 over "t.body", with a replay window. */
public final class WebhookVerifier {
    private static final int HEX_LENGTH = 64;

    private final List<byte[]> secrets;
    private final Duration tolerance;
    private final Clock clock;

    public WebhookVerifier(List<byte[]> secrets, Duration tolerance, Clock clock) {
        this.secrets = secrets.stream().map(byte[]::clone).toList();
        this.tolerance = tolerance;
        this.clock = clock;
    }

    /** True only for a fresh webhook whose v1 signature matches one of the secrets. */
    public boolean verify(byte[] rawBody, String signatureHeader) {
        try {
            String timestamp = null;
            List<byte[]> candidates = new ArrayList<>();
            for (String part : signatureHeader.split(",", -1)) {
                int eq = part.indexOf('=');
                if (eq < 0) {
                    return false;
                }
                String key = part.substring(0, eq).trim();
                String value = part.substring(eq + 1).trim();
                if (key.equals("t")) {
                    if (timestamp != null) {
                        return false;
                    }
                    timestamp = value;
                } else if (key.equals("v1") && value.length() == HEX_LENGTH) {
                    candidates.add(HexFormat.of().parseHex(value));
                }
            }
            if (timestamp == null || candidates.isEmpty() || !timestamp.matches("[0-9]{1,12}")) {
                return false;
            }
            long age = clock.instant().getEpochSecond() - Long.parseLong(timestamp);
            if (Math.abs(age) > tolerance.toSeconds()) {
                return false;
            }
            boolean valid = false;
            for (byte[] secret : secrets) {
                byte[] expected = hmac(secret, timestamp, rawBody);
                for (byte[] candidate : candidates) {
                    valid |= MessageDigest.isEqual(expected, candidate);
                }
            }
            return valid;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] hmac(byte[] secret, String timestamp, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            return mac.doFinal(body);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
