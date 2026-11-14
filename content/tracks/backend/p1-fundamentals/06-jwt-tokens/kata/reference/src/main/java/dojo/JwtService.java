package dojo;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Issues and verifies HS256 JSON Web Tokens: base64url(header) + "." + base64url(payload) + "." +
 * base64url(HMAC-SHA256 of the first two parts). The header is {"alg":"HS256","typ":"JWT"} and
 * the payload {"sub":..., "exp": seconds since the epoch, "roles": [...]}.
 */
public final class JwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final byte[] secret;
    private final Clock clock;

    public JwtService(byte[] secret, Clock clock) {
        this.secret = secret.clone();
        this.clock = clock;
    }

    public String issue(String subject, List<String> roles, Duration timeToLive) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", subject);
        claims.put("exp", clock.instant().plus(timeToLive).getEpochSecond());
        claims.put("roles", List.copyOf(roles));
        String signingInput = encode(HEADER) + "." + encode(MiniJson.write(claims));
        return signingInput + "." + ENCODER.encodeToString(sign(signingInput));
    }

    public Claims verify(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.", -1);
        if (parts.length != 3) {
            throw new InvalidTokenException("malformed");
        }
        Map<String, Object> header = json(parts[0]);
        if (!"HS256".equals(header.get("alg"))) {
            throw new InvalidTokenException("unsupported algorithm");
        }
        byte[] signature;
        try {
            signature = DECODER.decode(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("malformed");
        }
        if (!MessageDigest.isEqual(sign(parts[0] + "." + parts[1]), signature)) {
            throw new InvalidTokenException("bad signature");
        }
        Map<String, Object> payload = json(parts[1]);
        if (!(payload.get("sub") instanceof String subject) || !(payload.get("exp") instanceof Long exp)) {
            throw new InvalidTokenException("malformed");
        }
        Instant expiresAt = Instant.ofEpochSecond(exp);
        if (!clock.instant().isBefore(expiresAt)) {
            throw new InvalidTokenException("expired");
        }
        @SuppressWarnings("unchecked")
        List<String> roles = payload.get("roles") instanceof List<?> list ? (List<String>) list : List.of();
        return new Claims(subject, expiresAt, roles);
    }

    private static String encode(String json) {
        return ENCODER.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, Object> json(String part) {
        try {
            return MiniJson.read(new String(DECODER.decode(part), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("malformed");
        }
    }

    private byte[] sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
