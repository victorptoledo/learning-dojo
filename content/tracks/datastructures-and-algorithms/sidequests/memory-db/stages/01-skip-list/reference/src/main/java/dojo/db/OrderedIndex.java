package dojo.db;

import java.util.Comparator;
import java.util.Random;

/**
 * An indexable skip list: sorted linked lists stacked in levels, each node in a level with
 * probability 1/2 of the level below, so a search drops through about log2(n) levels with about
 * two steps each. Every forward pointer also stores its span (how many nodes of the bottom level it
 * skips), kept up to date for the rank questions of the next stage.
 */
public final class OrderedIndex<K, V> {

    private static final int MAX_LEVEL = 32;

    private static final class Node<K, V> {
        final K key;
        V value;
        final Node<K, V>[] next;
        final int[] span;

        @SuppressWarnings("unchecked")
        Node(K key, V value, int levels) {
            this.key = key;
            this.value = value;
            this.next = (Node<K, V>[]) new Node[levels];
            this.span = new int[levels];
        }
    }

    private final Comparator<? super K> order;
    private final Node<K, V> head = new Node<>(null, null, MAX_LEVEL);
    private final Random random = new Random(0x5EED);
    private int levels = 1;
    private int size;

    public OrderedIndex(Comparator<? super K> order) {
        if (order == null) {
            throw new IllegalArgumentException("no comparator");
        }
        this.order = order;
    }

    public V put(K key, V value) {
        requireKey(key);
        @SuppressWarnings("unchecked")
        Node<K, V>[] update = (Node<K, V>[]) new Node[MAX_LEVEL];
        int[] rank = new int[MAX_LEVEL];
        Node<K, V> x = head;
        for (int i = levels - 1; i >= 0; i--) {
            rank[i] = i == levels - 1 ? 0 : rank[i + 1];
            while (x.next[i] != null && order.compare(x.next[i].key, key) < 0) {
                rank[i] += x.span[i];
                x = x.next[i];
            }
            update[i] = x;
        }
        Node<K, V> found = x.next[0];
        if (found != null && order.compare(found.key, key) == 0) {
            V old = found.value;
            found.value = value;
            return old;
        }
        int height = randomHeight();
        if (height > levels) {
            for (int i = levels; i < height; i++) {
                rank[i] = 0;
                update[i] = head;
                head.span[i] = size;
            }
            levels = height;
        }
        var node = new Node<>(key, value, height);
        for (int i = 0; i < height; i++) {
            node.next[i] = update[i].next[i];
            update[i].next[i] = node;
            node.span[i] = update[i].span[i] - (rank[0] - rank[i]);
            update[i].span[i] = rank[0] - rank[i] + 1;
        }
        for (int i = height; i < levels; i++) {
            update[i].span[i]++;
        }
        size++;
        return null;
    }

    public V get(K key) {
        requireKey(key);
        Node<K, V> x = lastBefore(key).next[0];
        return x != null && order.compare(x.key, key) == 0 ? x.value : null;
    }

    public V remove(K key) {
        requireKey(key);
        @SuppressWarnings("unchecked")
        Node<K, V>[] update = (Node<K, V>[]) new Node[MAX_LEVEL];
        Node<K, V> x = head;
        for (int i = levels - 1; i >= 0; i--) {
            while (x.next[i] != null && order.compare(x.next[i].key, key) < 0) {
                x = x.next[i];
            }
            update[i] = x;
        }
        Node<K, V> gone = x.next[0];
        if (gone == null || order.compare(gone.key, key) != 0) {
            return null;
        }
        for (int i = 0; i < levels; i++) {
            if (update[i].next[i] == gone) {
                update[i].span[i] += gone.span[i] - 1;
                update[i].next[i] = gone.next[i];
            } else {
                update[i].span[i]--;
            }
        }
        while (levels > 1 && head.next[levels - 1] == null) {
            levels--;
        }
        size--;
        return gone.value;
    }

    public int size() {
        return size;
    }

    public K first() {
        return head.next[0] == null ? null : head.next[0].key;
    }

    public K last() {
        Node<K, V> x = head;
        for (int i = levels - 1; i >= 0; i--) {
            while (x.next[i] != null) {
                x = x.next[i];
            }
        }
        return x == head ? null : x.key;
    }

    public K floor(K key) {
        requireKey(key);
        Node<K, V> before = lastBefore(key);
        Node<K, V> x = before.next[0];
        if (x != null && order.compare(x.key, key) == 0) {
            return x.key;
        }
        return before == head ? null : before.key;
    }

    public K ceiling(K key) {
        requireKey(key);
        Node<K, V> x = lastBefore(key).next[0];
        return x == null ? null : x.key;
    }

    /** The last node whose key is below {@code key}, or the head. */
    private Node<K, V> lastBefore(K key) {
        Node<K, V> x = head;
        for (int i = levels - 1; i >= 0; i--) {
            while (x.next[i] != null && order.compare(x.next[i].key, key) < 0) {
                x = x.next[i];
            }
        }
        return x;
    }

    private int randomHeight() {
        int height = 1;
        while (height < MAX_LEVEL && random.nextBoolean()) {
            height++;
        }
        return height;
    }

    private static void requireKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("null key");
        }
    }
}
