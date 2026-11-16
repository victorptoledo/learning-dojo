package dojo;

import dojo.FeedExceptions.InvalidCursorException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Where a page ended: the sort key of its last event, plus the filter it was issued for.
 * Clients see it only as an opaque base64url string.
 */
public record Cursor(Instant occurredAt, long id, String type) {
    private static final String VERSION = "v1";

    /** True if the event comes after this cursor in newest-first order. */
    boolean isBefore(Event event) {
        int byTime = event.occurredAt().compareTo(occurredAt);
        return byTime < 0 || (byTime == 0 && event.id() < id);
    }

    static Cursor after(Event last, String type) {
        return new Cursor(last.occurredAt(), last.id(), type);
    }

    String encode() {
        String raw = String.join("|", VERSION, occurredAt.toString(), Long.toString(id), type == null ? "" : type);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static Cursor decode(String encoded, String expectedType) {
        Cursor cursor;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", -1);
            if (parts.length != 4 || !parts[0].equals(VERSION)) {
                throw new IllegalArgumentException("unknown cursor format");
            }
            cursor = new Cursor(Instant.parse(parts[1]), Long.parseLong(parts[2]),
                    parts[3].isEmpty() ? null : parts[3]);
        } catch (RuntimeException e) {
            throw new InvalidCursorException("the cursor is not one this API issued");
        }
        if (!Objects.equals(cursor.type(), expectedType)) {
            throw new InvalidCursorException("the cursor was issued for a different filter");
        }
        return cursor;
    }
}
