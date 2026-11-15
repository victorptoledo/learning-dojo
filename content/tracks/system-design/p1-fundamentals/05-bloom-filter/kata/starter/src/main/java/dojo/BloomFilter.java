package dojo;

/** A Bloom filter: no false negatives, a tunable rate of false positives. */
public final class BloomFilter {

    public BloomFilter(int bits, int hashes) {
        throw new UnsupportedOperationException("todo filter of " + bits + " bits and " + hashes + " hashes");
    }

    /** A filter sized for {@code expectedItems} at {@code falsePositiveRate}. */
    public static BloomFilter forCapacity(long expectedItems, double falsePositiveRate) {
        throw new UnsupportedOperationException("todo size for " + expectedItems);
    }

    public void add(String item) {
        throw new UnsupportedOperationException("todo add " + item);
    }

    /** false: never added. true: probably added. */
    public boolean mightContain(String item) {
        throw new UnsupportedOperationException("todo might contain " + item);
    }

    public int bitCount() {
        throw new UnsupportedOperationException("todo bit count");
    }

    public int hashCount() {
        throw new UnsupportedOperationException("todo hash count");
    }

    public int bitsSet() {
        throw new UnsupportedOperationException("todo bits set");
    }

    public long itemsAdded() {
        throw new UnsupportedOperationException("todo items added");
    }

    /** (1 - e^(-k n / m))^k for the items added so far. */
    public double expectedFalsePositiveRate() {
        throw new UnsupportedOperationException("todo expected rate");
    }
}
