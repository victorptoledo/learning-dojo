package dojo;

public enum Outcome {
    /** A cell of the tenant's shard answered. */
    SERVED,
    /** Dropped on purpose to protect a shard that is short of cells. */
    SHED,
    /** No cell of the tenant's shard could answer. */
    UNAVAILABLE
}
