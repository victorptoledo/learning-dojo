package dojo;

/** A parcel to ship: its weight in grams, the destination country code and whether it is fragile. */
public record Parcel(int grams, String country, boolean fragile) {
    public Parcel {
        if (grams <= 0) throw new IllegalArgumentException("a parcel weighs something");
    }
}
