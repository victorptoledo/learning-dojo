package dojo.shortener;

import java.util.Optional;

/**
 * The database of links, shared by every shortener. Each call is a network round trip, so
 * count them. Implementations are safe to call from many threads.
 */
public interface LinkStore {

    /** Saves the link unless the code exists; `false` means the code was already taken. */
    boolean putIfAbsent(String code, String url);

    /** The long URL of the code, if it exists. */
    Optional<String> get(String code);

    /** Removes the code; `false` when it did not exist. Used from stage 2. */
    boolean delete(String code);
}
