package dojo;

import java.util.List;

/** A trie of searched words with frequencies, prefix counts and suggestions. */
public final class Autocomplete {

    public void add(String word) {
        throw new UnsupportedOperationException("todo add");
    }

    public void add(String word, long times) {
        throw new UnsupportedOperationException("todo add " + times + " times");
    }

    public long frequency(String word) {
        throw new UnsupportedOperationException("todo frequency");
    }

    public boolean contains(String word) {
        throw new UnsupportedOperationException("todo contains");
    }

    /** The number of distinct words. */
    public int size() {
        throw new UnsupportedOperationException("todo size");
    }

    /** Distinct words starting with the prefix, in O(prefix length). */
    public int countWithPrefix(String prefix) {
        throw new UnsupportedOperationException("todo countWithPrefix");
    }

    /** At most k words with the prefix: most frequent first, ties alphabetical. */
    public List<String> suggest(String prefix, int k) {
        throw new UnsupportedOperationException("todo suggest " + k);
    }

    /** Removes the word and prunes empty branches; false if it was absent. */
    public boolean remove(String word) {
        throw new UnsupportedOperationException("todo remove");
    }

    /** The number of trie nodes, not counting the root. */
    public int nodeCount() {
        throw new UnsupportedOperationException("todo nodeCount");
    }
}
