package dojo;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Issues and verifies HS256 JSON Web Tokens: base64url(header) + "." + base64url(payload) + "." +
 * base64url(HMAC-SHA256 of the first two parts). The header is {"alg":"HS256","typ":"JWT"} and
 * the payload {"sub":..., "exp": seconds since the epoch, "roles": [...]}.
 */
public final class JwtService {
    private final byte[] secret;
    private final Clock clock;

    public JwtService(byte[] secret, Clock clock) {
        this.secret = secret.clone();
        this.clock = clock;
    }

    public String issue(String subject, List<String> roles, Duration timeToLive) {
        throw new UnsupportedOperationException("todo issue a token for " + subject);
    }

    public Claims verify(String token) {
        throw new UnsupportedOperationException("todo verify " + token);
    }
}
