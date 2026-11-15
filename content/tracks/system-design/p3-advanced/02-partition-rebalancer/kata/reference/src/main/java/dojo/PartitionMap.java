package dojo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Fixed partitions assigned to nodes, rebalanced with as few moves as possible. */
public final class PartitionMap {
    private final int partitions;
    private final String[] owner;
    private final TreeMap<String, TreeSet<Integer>> owned = new TreeMap<>();

    public PartitionMap(int partitions) {
        if (partitions < 1) {
            throw new IllegalArgumentException("at least one partition");
        }
        this.partitions = partitions;
        this.owner = new String[partitions];
    }

    /** The partition of a key: it never changes. */
    public int partitionFor(String key) {
        return (int) Math.floorMod(Hashing.hash64(key), (long) partitions);
    }

    /** Adds a node and returns the partitions moved to it. */
    public List<Move> addNode(String node) {
        if (owned.containsKey(node)) {
            throw new IllegalArgumentException(node + " is already in the cluster");
        }
        List<Move> moves = new ArrayList<>();
        if (owned.isEmpty()) {
            TreeSet<Integer> all = new TreeSet<>();
            for (int p = 0; p < partitions; p++) {
                owner[p] = node;
                all.add(p);
            }
            owned.put(node, all);
            return moves;
        }
        int target = partitions / (owned.size() + 1);
        TreeSet<Integer> mine = new TreeSet<>();
        for (int i = 0; i < target; i++) {
            String donor = mostLoaded();
            int partition = owned.get(donor).pollLast();
            owner[partition] = node;
            mine.add(partition);
            moves.add(new Move(partition, donor, node));
        }
        owned.put(node, mine);
        return moves;
    }

    /** Removes a node and returns where its partitions went. */
    public List<Move> removeNode(String node) {
        if (!owned.containsKey(node)) {
            throw new IllegalArgumentException(node + " is not in the cluster");
        }
        if (owned.size() == 1) {
            throw new IllegalStateException("cannot remove the last node");
        }
        TreeSet<Integer> leaving = owned.remove(node);
        List<Move> moves = new ArrayList<>();
        for (int partition : leaving) {
            String receiver = leastLoaded();
            owned.get(receiver).add(partition);
            owner[partition] = receiver;
            moves.add(new Move(partition, node, receiver));
        }
        return moves;
    }

    public String ownerOf(int partition) {
        if (partition < 0 || partition >= partitions) {
            throw new IllegalArgumentException("no partition " + partition);
        }
        if (owned.isEmpty()) {
            throw new IllegalStateException("the cluster has no nodes");
        }
        return owner[partition];
    }

    public String nodeFor(String key) {
        return ownerOf(partitionFor(key));
    }

    /** Every node, sorted, with its partitions in ascending order. */
    public Map<String, List<Integer>> assignment() {
        Map<String, List<Integer>> result = new LinkedHashMap<>();
        owned.forEach((node, parts) -> result.put(node, List.copyOf(parts)));
        return result;
    }

    private String mostLoaded() {
        String best = null;
        for (var entry : owned.entrySet()) {
            if (best == null || entry.getValue().size() > owned.get(best).size()) {
                best = entry.getKey();
            }
        }
        return best;
    }

    private String leastLoaded() {
        String best = null;
        for (var entry : owned.entrySet()) {
            if (best == null || entry.getValue().size() < owned.get(best).size()) {
                best = entry.getKey();
            }
        }
        return best;
    }
}
