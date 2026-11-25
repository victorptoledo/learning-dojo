package dojo;

import java.util.List;

/** The cell map the control plane publishes: a version that only goes up, the cells, the shard size. */
public record CellMap(long version, List<String> cells, int shardSize) {}
