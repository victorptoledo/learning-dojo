package dojo;

/**
 * Prices a parcel for a shipping method. Every new carrier means another branch here, and the
 * rules of one method cannot be used or tested on their own.
 */
public class ShippingCalculator {
    public long quoteCents(String method, Parcel parcel) {
        if (method == null) {
            throw new IllegalArgumentException("unknown shipping method: null");
        }
        if (method.equals("standard")) {
            long price = 499;
            if (parcel.grams() > 2000) {
                long extraKilos = (parcel.grams() - 2000 + 999) / 1000;
                price += extraKilos * 150;
            }
            if (!parcel.country().equals("PT")) {
                price += 700;
            }
            if (parcel.fragile()) {
                price += 300;
            }
            return price;
        } else if (method.equals("express")) {
            long kilos = (parcel.grams() + 999) / 1000;
            long price = 1299 + kilos * 250;
            if (!parcel.country().equals("PT")) {
                price = price * 2;
            }
            if (parcel.fragile()) {
                price += 300;
            }
            return price;
        } else if (method.equals("pickup")) {
            if (parcel.grams() > 10_000) {
                throw new IllegalArgumentException("pickup points take parcels up to 10 kg");
            }
            if (!parcel.country().equals("PT")) {
                throw new IllegalArgumentException("pickup points exist only in PT");
            }
            return 199;
        } else if (method.equals("freight")) {
            if (parcel.grams() < 30_000) {
                throw new IllegalArgumentException("freight starts at 30 kg");
            }
            long kilos = (parcel.grams() + 999) / 1000;
            return 4500 + kilos * 80;
        }
        throw new IllegalArgumentException("unknown shipping method: " + method);
    }
}
