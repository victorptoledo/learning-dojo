package dojo;

/** A slow remote lookup of customer names. Already written; the tests bring their own. */
public interface CustomerDirectory {
    String nameOf(String customerId);
}
