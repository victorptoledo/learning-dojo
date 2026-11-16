package dojo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

/** A crash-safe key-value store: a write-ahead log of checksummed frames plus atomic snapshots. */
public final class KvStore implements AutoCloseable {
    public static final String LOG = "wal.log";
    public static final String SNAPSHOT = "data.snapshot";
    public static final String SNAPSHOT_TMP = "data.snapshot.tmp";

    private KvStore() {}

    /** Opens (creating if needed) the store in {@code dir}, recovering from its files. */
    public static KvStore open(Path dir) throws IOException {
        throw new UnsupportedOperationException("todo: open " + dir);
    }

    public Optional<String> get(String key) {
        throw new UnsupportedOperationException("todo: get " + key);
    }

    public void put(String key, String value) throws IOException {
        throw new UnsupportedOperationException("todo: put " + key);
    }

    public void delete(String key) throws IOException {
        throw new UnsupportedOperationException("todo: delete " + key);
    }

    /** Applies all mutations atomically: after a crash, either all of them or none are present. */
    public void apply(List<Mutation> batch) throws IOException {
        throw new UnsupportedOperationException("todo: apply " + batch.size() + " mutations");
    }

    public int size() {
        throw new UnsupportedOperationException("todo: size");
    }

    /** A sorted copy of all entries. */
    public SortedMap<String, String> entries() {
        throw new UnsupportedOperationException("todo: entries");
    }

    public RecoveryInfo lastRecovery() {
        throw new UnsupportedOperationException("todo: lastRecovery");
    }

    /** Writes all data to a new snapshot atomically, then empties the log. */
    public void checkpoint() throws IOException {
        throw new UnsupportedOperationException("todo: checkpoint");
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("todo: close");
    }
}
