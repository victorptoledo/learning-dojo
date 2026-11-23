package dojo.spell;

import java.util.List;

/** A dictionary that knows how far apart two words are. See README.md. */
public final class SpellChecker {

    public void add(String word) {
        add(word, 1);
    }

    public void add(String word, long frequency) {
        throw new UnsupportedOperationException("todo");
    }

    public boolean contains(String word) {
        throw new UnsupportedOperationException("todo");
    }

    public long frequency(String word) {
        throw new UnsupportedOperationException("todo");
    }

    public int size() {
        throw new UnsupportedOperationException("todo");
    }

    public List<Misspelling> check(String text) {
        throw new UnsupportedOperationException("todo");
    }

    public static int distance(String a, String b) {
        throw new UnsupportedOperationException("todo");
    }
}
