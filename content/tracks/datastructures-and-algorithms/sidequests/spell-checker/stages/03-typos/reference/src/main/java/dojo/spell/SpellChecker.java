package dojo.spell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The dictionary is a trie: one node per prefix, 26 children, and the frequency of the word that
 * ends there. Suggestions walk the trie with one row of the distance table per node.
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

    public List<String> suggest(String word, int maxDistance, int limit) {
        return search(word, maxDistance, limit, false);
    }

    public List<String> suggestTypos(String word, int maxDistance, int limit) {
        return search(word, maxDistance, limit, true);
    }

    public Optional<String> correct(String word) {
        String w = normalized(word);
        if (contains(w)) {
            return Optional.of(w);
        }
        return suggestTypos(w, 2, 1).stream().findFirst();
    }

    /** Optimal string alignment: Levenshtein plus swapping two neighbouring letters for 1. */
    public static int typoDistance(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("a word is null");
        }
        int[] older = new int[b.length() + 1];
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                current[j] = cell(a.charAt(i - 1), i > 1 ? a.charAt(i - 2) : 0, b, j, older, previous, current, i > 1);
            }
            int[] t = older;
            older = previous;
            previous = current;
            current = t;
        }
        return previous[b.length()];
    }

    private record Hit(String word, int distance, long frequency) {
    }

    /**
     * Walks the trie with one row of the distance table per node (the distances from the prefix
     * of the node to every prefix of the word) and stops going down a branch as soon as every
     * value of its row is above maxDistance: no word below can come back under it.
     */
    private List<String> search(String word, int maxDistance, int limit, boolean typos) {
        String w = normalized(word);
        if (maxDistance < 0 || limit < 0) {
            throw new IllegalArgumentException("negative maxDistance or limit");
        }
        int[] first = new int[w.length() + 1];
        for (int j = 0; j <= w.length(); j++) {
            first[j] = j;
        }
        var hits = new ArrayList<Hit>();
        for (int c = 0; c < 26; c++) {
            if (root.next[c] != null) {
                walk(root.next[c], (char) ('a' + c), (char) 0, w, null, first, maxDistance, typos, hits);
            }
        }
        hits.sort(Comparator.comparingInt(Hit::distance)
                .thenComparing(Comparator.comparingLong(Hit::frequency).reversed())
                .thenComparing(Hit::word));
        return hits.stream().limit(limit).map(Hit::word).toList();
    }

    private void walk(Node node, char c, char parentChar, String w, int[] older, int[] previous,
            int maxDistance, boolean typos, List<Hit> hits) {
        int[] row = new int[w.length() + 1];
        row[0] = previous[0] + 1;
        int best = row[0];
        for (int j = 1; j <= w.length(); j++) {
            row[j] = cell(c, parentChar, w, j, older, previous, row, typos && older != null);
            best = Math.min(best, row[j]);
        }
        if (node.word != null && row[w.length()] <= maxDistance) {
            hits.add(new Hit(node.word, row[w.length()], node.frequency));
        }
        if (best > maxDistance) {
            return;
        }
        for (int next = 0; next < 26; next++) {
            if (node.next[next] != null) {
                walk(node.next[next], (char) ('a' + next), c, w, previous, row, maxDistance, typos, hits);
            }
        }
    }

    /**
     * One cell of the distance table: the cost of turning a prefix ending in {@code c} (after
     * {@code before}) into the first {@code j} letters of {@code w}.
     */
    private static int cell(char c, char before, String w, int j, int[] older, int[] previous, int[] row,
            boolean swaps) {
        int value = Math.min(previous[j - 1] + (c == w.charAt(j - 1) ? 0 : 1), Math.min(previous[j], row[j - 1]) + 1);
        if (swaps && j > 1 && c == w.charAt(j - 2) && before == w.charAt(j - 1)) {
            value = Math.min(value, older[j - 2] + 1);
        }
        return value;
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
