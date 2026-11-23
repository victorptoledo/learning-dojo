package dojo.raft;

import java.util.Map;

/**
 * The state machine after applying every entry up to `index` (whose term is `term`): the
 * key-value data and the last sequence applied for each client. Used from stage 5; before
 * that it stays `Snapshot.EMPTY`.
 */
public record Snapshot(long index, long term, Map<String, String> data, Map<String, Long> clients) {

    public static final Snapshot EMPTY = new Snapshot(0, 0, Map.of(), Map.of());
}
