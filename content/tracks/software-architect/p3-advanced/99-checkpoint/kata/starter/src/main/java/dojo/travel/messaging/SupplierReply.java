package dojo.travel.messaging;

import dojo.travel.domain.Leg;

/**
 * A supplier's answer to a {@code RESERVE} command, delivered at least once: the same message
 * (same {@code messageId}) can arrive twice. {@code detail} is the confirmation code when
 * {@code reserved} is true and the reason otherwise. Given: do not change.
 */
public record SupplierReply(String messageId, String tripId, Leg leg, boolean reserved, String detail) {
    public static SupplierReply reserved(String messageId, String tripId, Leg leg, String confirmation) {
        return new SupplierReply(messageId, tripId, leg, true, confirmation);
    }

    public static SupplierReply refused(String messageId, String tripId, Leg leg, String reason) {
        return new SupplierReply(messageId, tripId, leg, false, reason);
    }
}
