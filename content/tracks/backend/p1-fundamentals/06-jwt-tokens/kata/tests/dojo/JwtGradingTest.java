package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class JwtGradingTest {
    private static final byte[] SECRET = "dojo-test-secret-with-at-least-32-bytes!".getBytes(StandardCharsets.UTF_8);
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private static JwtService at(Instant instant) {
        return new JwtService(SECRET, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static String decode(String part) {
        return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
    }

    private static String reason(JwtService service, String token) {
        return assertThrows(InvalidTokenException.class, () -> service.verify(token)).reason();
    }

    @Test
    void a_token_has_three_base64url_parts_with_the_standard_header() {
        String token = at(NOW).issue("ada", List.of("USER"), Duration.ofMinutes(15));
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
        assertEquals(Map.of("alg", "HS256", "typ", "JWT"), MiniJson.read(decode(parts[0])));
        for (String part : parts) {
            org.junit.jupiter.api.Assertions.assertFalse(part.contains("=") || part.contains("+") || part.contains("/"),
                    "base64url without padding: " + part);
        }
    }

    @Test
    void the_payload_is_readable_by_anyone() {
        String token = at(NOW).issue("ada", List.of("USER", "ADMIN"), Duration.ofMinutes(15));
        Map<String, Object> claims = MiniJson.read(decode(token.split("\\.")[1]));
        assertEquals("ada", claims.get("sub"));
        assertEquals(NOW.plusSeconds(900).getEpochSecond(), claims.get("exp"));
        assertEquals(List.of("USER", "ADMIN"), claims.get("roles"));
    }

    @Test
    void the_signature_is_hmac_sha256_of_header_dot_payload() throws Exception {
        String token = at(NOW).issue("ada", List.of(), Duration.ofMinutes(1));
        int lastDot = token.lastIndexOf('.');
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
        byte[] expected = mac.doFinal(token.substring(0, lastDot).getBytes(StandardCharsets.US_ASCII));
        assertArrayEquals(expected, Base64.getUrlDecoder().decode(token.substring(lastDot + 1)));
    }

    @Test
    void a_valid_token_gives_back_its_claims() {
        String token = at(NOW).issue("linus", List.of("USER"), Duration.ofMinutes(15));
        assertEquals(new Claims("linus", NOW.plusSeconds(900), List.of("USER")), at(NOW.plusSeconds(60)).verify(token));
    }

    @Test
    void an_expired_token_is_refused() {
        String token = at(NOW).issue("ada", List.of(), Duration.ofMinutes(15));
        assertEquals("expired", reason(at(NOW.plusSeconds(900)), token));
        assertEquals("expired", reason(at(NOW.plusSeconds(3600)), token));
        at(NOW.plusSeconds(899)).verify(token);
    }

    @Test
    void a_tampered_payload_is_refused() {
        String token = at(NOW).issue("ada", List.of("USER"), Duration.ofMinutes(15));
        String[] parts = token.split("\\.");
        String forged = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MiniJson.write(new java.util.LinkedHashMap<>(Map.of("sub", "ada", "exp", NOW.plusSeconds(900).getEpochSecond(), "roles", List.of("ADMIN"))))
                        .getBytes(StandardCharsets.UTF_8));
        assertEquals("bad signature", reason(at(NOW), parts[0] + "." + forged + "." + parts[2]));
    }

    @Test
    void a_token_signed_with_another_key_is_refused() {
        var other = new JwtService("another-secret-that-is-also-long-enough".getBytes(StandardCharsets.UTF_8),
                Clock.fixed(NOW, ZoneOffset.UTC));
        assertEquals("bad signature", reason(at(NOW), other.issue("ada", List.of(), Duration.ofMinutes(5))));
    }

    @Test
    void the_none_algorithm_is_refused() {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(("{\"sub\":\"root\",\"exp\":" + NOW.plusSeconds(600).getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8));
        assertEquals("unsupported algorithm", reason(at(NOW), header + "." + payload + "."));
    }

    @Test
    void malformed_tokens_are_refused() {
        assertEquals("malformed", reason(at(NOW), "not-a-token"));
        assertEquals("malformed", reason(at(NOW), "a.b"));
        assertEquals("malformed", reason(at(NOW), "!!!.???.***"));
        assertEquals("malformed", reason(at(NOW), null));
    }
}
