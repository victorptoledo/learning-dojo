package dojo.wallet;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A UTC clock the tests set by hand. */
final class MutableClock extends Clock {
    private volatile Instant now = Instant.parse("2099-01-01T00:00:00Z");

    void set(String instant) {
        now = Instant.parse(instant);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}
