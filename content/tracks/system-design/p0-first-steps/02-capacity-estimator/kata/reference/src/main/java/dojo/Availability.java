package dojo;

import java.time.Duration;

/** Availability of compositions. Availabilities are fractions: 0.999 means 99.9 %. */
public final class Availability {
    private static final long SECONDS_PER_YEAR = 365L * 86_400;

    private Availability() {}

    /** All parts are needed: the availabilities multiply. */
    public static double serial(double... parts) {
        if (parts.length == 0) {
            throw new IllegalArgumentException("at least one part is needed");
        }
        double result = 1.0;
        for (double part : parts) {
            result *= requireFraction(part);
        }
        return result;
    }

    /** Any one of {@code copies} independent copies is enough. */
    public static double parallel(double each, int copies) {
        requireFraction(each);
        if (copies < 1) {
            throw new IllegalArgumentException("at least one copy is needed");
        }
        return 1.0 - Math.pow(1.0 - each, copies);
    }

    /** Downtime in a 365-day year, rounded to whole seconds. */
    public static Duration downtimePerYear(double availability) {
        requireFraction(availability);
        return Duration.ofSeconds(Math.round((1.0 - availability) * SECONDS_PER_YEAR));
    }

    private static double requireFraction(double value) {
        if (!(value >= 0 && value <= 1)) {
            throw new IllegalArgumentException("an availability is a fraction between 0 and 1: " + value);
        }
        return value;
    }
}
