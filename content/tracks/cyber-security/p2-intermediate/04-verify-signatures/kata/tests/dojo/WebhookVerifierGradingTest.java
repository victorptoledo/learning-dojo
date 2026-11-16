package dojo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class WebhookVerifierGradingTest {
    private static final byte[] OLD = "whsec-old".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NEW = "whsec-new".getBytes(StandardCharsets.UTF_8);
    private static final Instant NOW = Instant.ofEpochSecond(1_794_700_000L);
    private static final long T = NOW.getEpochSecond();
    private static final byte[] BODY = "{\"id\":\"evt_9\", \"amount\": 100}".getBytes(StandardCharsets.UTF_8);

    private static String sign(byte[] secret, String t, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            mac.update((t + ".").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static WebhookVerifier verifier(byte[]... secrets) {
        return new WebhookVerifier(List.of(secrets), Duration.ofSeconds(300), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void theSignatureCoversTheRawBytesAndTheTimestamp() {
        var v = verifier(NEW);
        String sig = sign(NEW, "" + T, BODY);
        assertTrue(v.verify(BODY, "t=" + T + ",v1=" + sig));
        byte[] reformatted = "{\"id\":\"evt_9\",\"amount\":100}".getBytes(StandardCharsets.UTF_8);
        assertFalse(v.verify(reformatted, "t=" + T + ",v1=" + sig));
        assertFalse(v.verify(BODY, "t=" + (T + 1) + ",v1=" + sig));
        byte[] binary = {0, (byte) 0xff, (byte) 0xc3, 0x28, 10};
        assertTrue(v.verify(binary, "t=" + T + ",v1=" + sign(NEW, "" + T, binary)));
    }

    @Test
    void theToleranceWorksBothWaysInclusive() {
        var v = verifier(NEW);
        for (long t : new long[] {T - 300, T + 300, T - 299}) {
            assertTrue(v.verify(BODY, "t=" + t + ",v1=" + sign(NEW, "" + t, BODY)), "t=" + t);
        }
        for (long t : new long[] {T - 301, T + 301, 0}) {
            assertFalse(v.verify(BODY, "t=" + t + ",v1=" + sign(NEW, "" + t, BODY)), "t=" + t);
        }
    }

    @Test
    void anyConfiguredSecretAndAnyV1MayMatch() {
        var v = verifier(OLD, NEW);
        assertTrue(v.verify(BODY, "t=" + T + ",v1=" + sign(OLD, "" + T, BODY)));
        assertTrue(v.verify(BODY, "t=" + T + ",v1=" + sign(NEW, "" + T, BODY)));
        String wrong = sign("other".getBytes(StandardCharsets.UTF_8), "" + T, BODY);
        assertTrue(v.verify(BODY, " t=" + T + " , v1=" + wrong + ", v1=" + sign(NEW, "" + T, BODY) + ",v2=abc"));
        assertFalse(verifier(OLD).verify(BODY, "t=" + T + ",v1=" + sign(NEW, "" + T, BODY)));
        assertTrue(v.verify(BODY, "t=" + T + ",v1=" + sign(NEW, "" + T, BODY).toUpperCase()));
    }

    @Test
    void malformedHeadersAreRejectedWithoutThrowing() {
        var v = verifier(NEW);
        String sig = sign(NEW, "" + T, BODY);
        String[] bad = {
            "",
            "v1=" + sig,
            "t=" + T,
            "t=" + T + ",v2=" + sig,
            "t=" + T + ",t=" + T + ",v1=" + sig,
            "t=abc,v1=" + sig,
            "t=" + T + ".5,v1=" + sig,
            "t=" + T + ",v1=" + sig.substring(2),
            "t=" + T + ",v1=" + sig + "00",
            "t=" + T + ",v1=zz" + sig.substring(2),
            "t=" + T + ",v1",
            "t=" + T + ",,v1=" + sig.substring(1) + "x",
            "t=99999999999999999999999,v1=" + sig,
        };
        for (String header : bad) {
            assertDoesNotThrow(() -> v.verify(BODY, header), header);
            assertFalse(v.verify(BODY, header), header);
        }
    }

    @Test
    void comparesInConstantTimeNotWithEquals() throws Exception {
        String source = new String(
                java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/dojo/WebhookVerifier.java")),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("MessageDigest.isEqual"), "use MessageDigest.isEqual for the tag comparison");
    }
}
