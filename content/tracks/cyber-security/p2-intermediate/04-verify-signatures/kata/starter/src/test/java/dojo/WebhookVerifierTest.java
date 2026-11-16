package dojo;

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

class WebhookVerifierTest {
    private static final byte[] SECRET = "whsec-test-secret".getBytes(StandardCharsets.UTF_8);
    private static final Instant NOW = Instant.ofEpochSecond(1_794_700_000L);
    private static final byte[] BODY = "{\"id\":\"evt_1\",\"amount\":4200}".getBytes(StandardCharsets.UTF_8);

    private static String sign(long t, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
        mac.update((t + ".").getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }

    private static WebhookVerifier verifier() {
        return new WebhookVerifier(List.of(SECRET), Duration.ofMinutes(5), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void acceptsAFreshCorrectlySignedWebhook() throws Exception {
        long t = NOW.getEpochSecond();
        assertTrue(verifier().verify(BODY, "t=" + t + ",v1=" + sign(t, BODY)));
    }

    @Test
    void rejectsAReplayFromAnHourAgo() throws Exception {
        long t = NOW.getEpochSecond() - 3600;
        assertFalse(verifier().verify(BODY, "t=" + t + ",v1=" + sign(t, BODY)));
    }
}
