package dojo.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The dictionary is a trie: one node per prefix, 26 children, and the frequency of the word that
 * ends there.
 */
public final class SpellChecker {

    private static final class Node {
        final Node[] next = new Node[26];
        long frequency;
        String word;
    }

    private final Node root = new Node();
    private int size;

    public void add(String word) {
        add(word, 1);
    }

    public void add(String word, long frequency) {
        String w = normalized(word);
        if (frequency < 1) {
            throw new IllegalArgumentException("frequency must be at least 1: " + frequency);
        }
        Node node = root;
        for (int i = 0; i < w.length(); i++) {
            int c = w.charAt(i) - 'a';
            if (node.next[c] == null) {
                node.next[c] = new Node();
            }
            node = node.next[c];
        }
        if (node.word == null) {
            node.word = w;
            size++;
        }
        node.frequency += frequency;
    }

    public boolean contains(String word) {
        return frequency(word) > 0;
    }

    public long frequency(String word) {
        if (word == null) {
            throw new IllegalArgumentException("the word is null");
        }
        Node node = find(word.toLowerCase(Locale.ROOT));
        return node == null ? 0 : node.frequency;
    }

    public int size() {
        return size;
    }

    public List<Misspelling> check(String text) {
        if (text == null) {
            throw new IllegalArgumentException("the text is null");
        }
        var result = new ArrayList<Misspelling>();
        int i = 0;
        while (i < text.length()) {
            if (!isLetter(text.charAt(i))) {
                i++;
                continue;
            }
            int start = i;
            while (i < text.length() && isLetter(text.charAt(i))) {
                i++;
            }
            String word = text.substring(start, i);
            if (!contains(word)) {
                result.add(new Misspelling(word, start));
            }
        }
        return result;
    }

    /** Levenshtein distance with two rows: O(|a|·|b|) time, O(min(|a|, |b|)) memory. */
    public static int distance(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("a word is null");
        }
        if (a.length() < b.length()) {
            String t = a;
            a = b;
            b = t;
        }
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            char c = a.charAt(i - 1);
            for (int j = 1; j <= b.length(); j++) {
                int substitute = previous[j - 1] + (c == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitute, Math.min(previous[j], current[j - 1]) + 1);
            }
            int[] t = previous;
            previous = current;
            current = t;
        }
        return previous[b.length()];
    }

    private Node find(String word) {
        Node node = root;
        for (int i = 0; i < word.length() && node != null; i++) {
            int c = word.charAt(i) - 'a';
            node = c >= 0 && c < 26 ? node.next[c] : null;
        }
        return node;
    }

    private static boolean isLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    /** The word in lower case; it must be one or more letters a-z. */
    private static String normalized(String word) {
        if (word == null || word.isEmpty()) {
            throw new IllegalArgumentException("empty word");
        }
        String w = word.toLowerCase(Locale.ROOT);
        for (int i = 0; i < w.length(); i++) {
            if (w.charAt(i) < 'a' || w.charAt(i) > 'z') {
                throw new IllegalArgumentException("not a word of letters a-z: " + word);
            }
        }
        return w;
    }
}
