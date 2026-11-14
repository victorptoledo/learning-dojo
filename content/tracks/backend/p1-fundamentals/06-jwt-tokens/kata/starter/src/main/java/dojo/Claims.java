package dojo;

import java.time.Instant;
import java.util.List;

/** What a token says: who ({@code sub}), until when ({@code exp}) and with which roles. */
public record Claims(String subject, Instant expiresAt, List<String> roles) {
    public Claims {
        roles = List.copyOf(roles);
    }
}
