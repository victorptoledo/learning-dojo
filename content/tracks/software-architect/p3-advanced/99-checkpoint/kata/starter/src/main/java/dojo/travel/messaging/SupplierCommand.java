package dojo.travel.messaging;

import dojo.travel.domain.Leg;

/** A command to one supplier: reserve or release the trip's leg. Given: do not change. */
public record SupplierCommand(String tripId, Leg leg, Action action) {
    public enum Action {
        RESERVE,
        RELEASE
    }

    public static SupplierCommand reserve(String tripId, Leg leg) {
        return new SupplierCommand(tripId, leg, Action.RESERVE);
    }

    public static SupplierCommand release(String tripId, Leg leg) {
        return new SupplierCommand(tripId, leg, Action.RELEASE);
    }
}
