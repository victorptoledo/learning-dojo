package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KvStoreTest {
    @TempDir
    Path dir;

    @Test
    void data_survives_reopening() throws Exception {
        try (KvStore store = KvStore.open(dir)) {
            store.put("lang", "java");
            store.put("year", "2026");
            store.delete("year");
        }
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(Optional.of("java"), store.get("lang"));
            assertEquals(Optional.empty(), store.get("year"));
            assertEquals(new RecoveryInfo(3, 0), store.lastRecovery());
        }
    }
}
