package dojo.cluster;

/** The database behind the cache: slow, and what the cache is there to protect. */
public interface Origin {

    /** The current value of the key; may throw when the database fails. */
    String load(String key);
}
