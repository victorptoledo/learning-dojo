package dojo;

/**
 * One way of pricing a parcel: the strategy. A rule that cannot ship a parcel throws
 * {@link IllegalArgumentException}.
 */
@FunctionalInterface
public interface ShippingRule {
    long quoteCents(Parcel parcel);
}
