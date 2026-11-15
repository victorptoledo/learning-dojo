package dojo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Hands out expiring leases with strictly increasing fencing tokens. */
public final class LeaseManager {
    private final Duration leaseDuration;
    private final Clock clock;
    private Lease live;
    private long lastToken;

    public LeaseManager(Duration leaseDuration, Clock clock) {
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("a lease must last a positive time");
        }
        this.leaseDuration = leaseDuration;
        this.clock = clock;
    }

    public synchronized Optional<Lease> tryAcquire(String owner) {
        Instant now = clock.instant();
        if (isLive(live, now)) {
            return Optional.empty();
        }
        live = new Lease(owner, ++lastToken, now.plus(leaseDuration));
        return Optional.of(live);
    }

    public synchronized Optional<Lease> renew(Lease lease) {
        Instant now = clock.instant();
        if (!isCurrent(lease, now)) {
            return Optional.empty();
        }
        live = new Lease(lease.owner(), lease.token(), now.plus(leaseDuration));
        return Optional.of(live);
    }

    public synchronized boolean release(Lease lease) {
        if (!isCurrent(lease, clock.instant())) {
            return false;
        }
        live = null;
        return true;
    }

    public synchronized Optional<Lease> current() {
        return isLive(live, clock.instant()) ? Optional.of(live) : Optional.empty();
    }

    /** True only for the live lease, and only until {@code margin} before it expires. */
    public synchronized boolean isSafeToAct(Lease lease, Duration margin) {
        Instant now = clock.instant();
        return isCurrent(lease, now) && now.isBefore(live.expiresAt().minus(margin));
    }

    private boolean isCurrent(Lease lease, Instant now) {
        return isLive(live, now) && live.token() == lease.token();
    }

    private static boolean isLive(Lease lease, Instant now) {
        return lease != null && now.isBefore(lease.expiresAt());
    }
}
