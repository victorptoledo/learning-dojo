package dojo;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/** Hands out expiring leases with strictly increasing fencing tokens. */
public final class LeaseManager {

    public LeaseManager(Duration leaseDuration, Clock clock) {
        throw new UnsupportedOperationException("todo leases of " + leaseDuration);
    }

    public Optional<Lease> tryAcquire(String owner) {
        throw new UnsupportedOperationException("todo acquire for " + owner);
    }

    public Optional<Lease> renew(Lease lease) {
        throw new UnsupportedOperationException("todo renew " + lease);
    }

    public boolean release(Lease lease) {
        throw new UnsupportedOperationException("todo release " + lease);
    }

    public Optional<Lease> current() {
        throw new UnsupportedOperationException("todo current");
    }

    /** True only for the live lease, and only until {@code margin} before it expires. */
    public boolean isSafeToAct(Lease lease, Duration margin) {
        throw new UnsupportedOperationException("todo safe to act " + lease);
    }
}
