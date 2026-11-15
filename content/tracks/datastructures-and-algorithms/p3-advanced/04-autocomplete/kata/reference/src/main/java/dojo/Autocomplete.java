package dojo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** A trie of searched words with frequencies, prefix counts and suggestions. */
public final class Autocomplete {
    private static final class Node {
        final Map<Character, Node> children = new HashMap<>();
        long frequency;
        int words;
    }

    private record Hit(String word, long frequency) {}

    /** Best first: higher frequency, then alphabetical. */
    private static final Comparator<Hit> BEST_FIRST =
            Comparator.comparingLong(Hit::frequency).reversed().thenComparing(Hit::word);

    private final Node root = new Node();
    private int nodes;

    public void add(String word) {
        add(word, 1);
    }

    public void add(String word, long times) {
        requireWord(word);
        if (times < 1) throw new IllegalArgumentException("times must be at least 1");
        boolean isNew = frequency(word) == 0;
        Node node = root;
        if (isNew) node.words++;
        for (int i = 0; i < word.length(); i++) {
            Node child = node.children.get(word.charAt(i));
            if (child == null) {
                child = new Node();
                node.children.put(word.charAt(i), child);
                nodes++;
            }
            node = child;
            if (isNew) node.words++;
        }
        node.frequency += times;
    }

    public long frequency(String word) {
        Node node = walk(word);
        return node == null ? 0 : node.frequency;
    }

    public boolean contains(String word) {
        return frequency(word) > 0;
    }

    public int size() {
        return root.words;
    }

    public int countWithPrefix(String prefix) {
        if (prefix == null) throw new IllegalArgumentException("null prefix");
        Node node = walk(prefix);
        return node == null ? 0 : node.words;
    }

    public List<String> suggest(String prefix, int k) {
        if (prefix == null) throw new IllegalArgumentException("null prefix");
        if (k < 0) throw new IllegalArgumentException("k cannot be negative");
        Node start = walk(prefix);
        if (start == null || k == 0) return List.of();
        // keep the k best seen so far; the worst of them is at the head
        PriorityQueue<Hit> best = new PriorityQueue<>(BEST_FIRST.reversed());
        Deque<Node> nodeStack = new ArrayDeque<>();
        Deque<String> wordStack = new ArrayDeque<>();
        nodeStack.push(start);
        wordStack.push(prefix);
        while (!nodeStack.isEmpty()) {
            Node node = nodeStack.pop();
            String word = wordStack.pop();
            if (node.frequency > 0) {
                best.add(new Hit(word, node.frequency));
                if (best.size() > k) best.poll();
            }
            for (var entry : node.children.entrySet()) {
                nodeStack.push(entry.getValue());
                wordStack.push(word + entry.getKey());
            }
        }
        List<Hit> hits = new ArrayList<>(best);
        hits.sort(BEST_FIRST);
        return hits.stream().map(Hit::word).toList();
    }

    public boolean remove(String word) {
        requireWord(word);
        if (!contains(word)) return false;
        Node node = root;
        node.words--;
        for (int i = 0; i < word.length(); i++) {
            Node child = node.children.get(word.charAt(i));
            if (--child.words == 0) {
                node.children.remove(word.charAt(i));
                nodes -= count(child);
                return true;
            }
            node = child;
        }
        node.frequency = 0;
        return true;
    }

    public int nodeCount() {
        return nodes;
    }

    private static int count(Node node) {
        int total = 0;
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(node);
        while (!stack.isEmpty()) {
            Node n = stack.pop();
            total++;
            n.children.values().forEach(stack::push);
        }
        return total;
    }

    private Node walk(String text) {
        Node node = root;
        for (int i = 0; i < text.length() && node != null; i++) {
            node = node.children.get(text.charAt(i));
        }
        return node;
    }

    private static void requireWord(String word) {
        if (word == null || word.isEmpty()) throw new IllegalArgumentException("a word is required");
    }
}
