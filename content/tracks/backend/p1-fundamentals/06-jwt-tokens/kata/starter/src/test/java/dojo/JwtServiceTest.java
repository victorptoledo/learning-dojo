package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    @Test
    void verifies_its_own_tokens() {
        var clock = Clock.fixed(Instant.parse("2026-11-14T10:00:00Z"), ZoneOffset.UTC);
        var jwt = new JwtService("a-secret-of-thirty-two-bytes-or-more".getBytes(StandardCharsets.UTF_8), clock);
        assertEquals("ada", jwt.verify(jwt.issue("ada", List.of("USER"), Duration.ofMinutes(5))).subject());
    }
}
