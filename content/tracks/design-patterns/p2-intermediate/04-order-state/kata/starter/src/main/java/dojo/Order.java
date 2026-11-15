package dojo;

/**
 * An order and its life cycle: created, paid, shipped, delivered, or cancelled. The status is
 * a string, every method re-checks it, and the fields that only make sense in one status
 * (the tracking number, the refund) sit next to each other whether they apply or not.
 */
public class Order {
    private final String id;
    private final long totalCents;
    private String status = "CREATED";
    private String trackingNumber;
    private String cancelReason;
    private long refundedCents;

    public Order(String id, long totalCents) {
        this.id = id;
        this.totalCents = totalCents;
    }

    public void pay() {
        if (!status.equals("CREATED")) {
            throw new IllegalStateException("cannot pay an order that is " + status.toLowerCase());
        }
        status = "PAID";
    }

    public void ship(String tracking) {
        if (!status.equals("PAID")) {
            throw new IllegalStateException("cannot ship an order that is " + status.toLowerCase());
        }
        if (tracking == null || tracking.isBlank()) {
            throw new IllegalArgumentException("a shipment needs a tracking number");
        }
        trackingNumber = tracking;
        status = "SHIPPED";
    }

    public void deliver() {
        if (!status.equals("SHIPPED")) {
            throw new IllegalStateException("cannot deliver an order that is " + status.toLowerCase());
        }
        status = "DELIVERED";
    }

    public void cancel(String reason) {
        if (status.equals("CREATED")) {
            refundedCents = 0;
        } else if (status.equals("PAID")) {
            refundedCents = totalCents;
        } else {
            throw new IllegalStateException("cannot cancel an order that is " + status.toLowerCase());
        }
        cancelReason = reason;
        status = "CANCELLED";
    }

    public String status() {
        return status;
    }

    public String describe() {
        if (status.equals("CREATED")) {
            return id + ": created";
        } else if (status.equals("PAID")) {
            return id + ": paid " + money(totalCents);
        } else if (status.equals("SHIPPED")) {
            return id + ": shipped with " + trackingNumber;
        } else if (status.equals("DELIVERED")) {
            return id + ": delivered with " + trackingNumber;
        } else {
            return id + ": cancelled (" + cancelReason + "), refunded " + money(refundedCents);
        }
    }

    private static String money(long cents) {
        return cents / 100 + "." + String.format("%02d", cents % 100);
    }
}
