package dojo;

import java.time.Duration;

/** Availability of compositions. Availabilities are fractions: 0.999 means 99.9 %. */
public final class Availability {
    private Availability() {}

    /** All parts are needed: the availabilities multiply. */
    public static double serial(double... parts) {
        throw new UnsupportedOperationException("todo serial");
    }

    /** Any one of {@code copies} independent copies is enough. */
    public static double parallel(double each, int copies) {
        throw new UnsupportedOperationException("todo parallel");
    }

    /** Downtime in a 365-day year, rounded to whole seconds. */
    public static Duration downtimePerYear(double availability) {
        throw new UnsupportedOperationException("todo downtime");
    }
}
