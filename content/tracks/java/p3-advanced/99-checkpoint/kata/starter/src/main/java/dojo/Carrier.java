package dojo;

/**
 * A remote carrier API. {@code quote} blocks (it is a slow network call), may throw, and may
 * return {@code null} when the carrier has no offer. Already written; the tests bring their
 * own carriers.
 */
public interface Carrier {
    String name();

    Quote quote(Shipment shipment) throws Exception;
}
