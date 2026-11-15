package dojo;

/** Critical work (checkout, login) is kept; sheddable work (prefetch, batch) goes first. */
public enum Priority {
    CRITICAL,
    SHEDDABLE
}
