package dojo.tasks;

/** Where a task is in its life. `DEAD` is used from stage 2. */
public enum Status {
    READY,
    LEASED,
    DONE,
    DEAD
}
