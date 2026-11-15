package dojo;

import java.util.Map;

/** The built-in rules, each usable and testable on its own, and the factory that names them. */
public final class ShippingRules {
    private ShippingRules() {}

    public static ShippingRule standard() {
        return parcel -> {
            long price = 499 + Math.max(0, startedKilos(parcel.grams() - 2000)) * 150;
            if (abroad(parcel)) price += 700;
            return price + fragileSurcharge(parcel);
        };
    }

    public static ShippingRule express() {
        return parcel -> {
            long price = 1299 + startedKilos(parcel.grams()) * 250;
            if (abroad(parcel)) price *= 2;
            return price + fragileSurcharge(parcel);
        };
    }

    public static ShippingRule pickup() {
        return parcel -> {
            if (parcel.grams() > 10_000) throw new IllegalArgumentException("pickup points take parcels up to 10 kg");
            if (abroad(parcel)) throw new IllegalArgumentException("pickup points exist only in PT");
            return 199;
        };
    }

    public static ShippingRule freight() {
        return parcel -> {
            if (parcel.grams() < 30_000) throw new IllegalArgumentException("freight starts at 30 kg");
            return 4500 + startedKilos(parcel.grams()) * 80;
        };
    }

    /** The rules the shop offers today, by method name. */
    public static Map<String, ShippingRule> builtIn() {
        return Map.of("standard", standard(), "express", express(), "pickup", pickup(), "freight", freight());
    }

    private static long startedKilos(int grams) {
        return grams <= 0 ? 0 : (grams + 999) / 1000;
    }

    private static boolean abroad(Parcel parcel) {
        return !parcel.country().equals("PT");
    }

    private static long fragileSurcharge(Parcel parcel) {
        return parcel.fragile() ? 300 : 0;
    }
}
