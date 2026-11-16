package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KvStoreGradingTest {
    @TempDir
    Path dir;

    private Path log() {
        return dir.resolve(KvStore.LOG);
    }

    /** The frame format of the README, written independently of the solution. */
    static byte[] frame(List<Mutation> batch) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(batch.size());
        for (Mutation m : batch) {
            out.writeByte(m.value() == null ? 2 : 1);
            byte[] key = m.key().getBytes(StandardCharsets.UTF_8);
            out.writeInt(key.length);
            out.write(key);
            if (m.value() != null) {
                byte[] value = m.value().getBytes(StandardCharsets.UTF_8);
                out.writeInt(value.length);
                out.write(value);
            }
        }
        byte[] body = bytes.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(body);
        return ByteBuffer.allocate(8 + body.length).putInt(body.length).putInt((int) crc.getValue()).put(body).array();
    }

    static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) out.writeBytes(p);
        return out.toByteArray();
    }

    private void writeThree() throws IOException {
        try (KvStore store = KvStore.open(dir)) {
            store.put("a", "1");
            store.put("b", "2");
            store.put("c", "3");
        }
    }

    // ---- the basics ------------------------------------------------------------------------

    @Test
    void put_get_overwrite_delete_and_entries() throws IOException {
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(0, store.size());
            store.put("b", "2");
            store.put("a", "1");
            store.put("b", "two");
            store.delete("missing");
            store.put("c", "3");
            store.delete("c");
            assertEquals(Optional.of("two"), store.get("b"));
            assertEquals(Optional.empty(), store.get("c"));
            assertEquals(2, store.size());
            assertEquals(new TreeMap<>(Map.of("a", "1", "b", "two")), store.entries());
            assertEquals(List.of("a", "b"), new ArrayList<>(store.entries().keySet()));
        }
    }

    @Test
    void entries_is_a_copy() throws IOException {
        try (KvStore store = KvStore.open(dir)) {
            store.put("k", "v");
            store.entries().clear();
            assertEquals(1, store.size());
        }
    }

    @Test
    void unicode_and_empty_values_survive_a_restart() throws IOException {
        try (KvStore store = KvStore.open(dir)) {
            store.put("città", "café ☕ 😀");
            store.put("empty", "");
        }
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(Optional.of("café ☕ 😀"), store.get("città"));
            assertEquals(Optional.of(""), store.get("empty"));
        }
    }

    @Test
    void open_creates_the_directory() throws IOException {
        Path nested = dir.resolve("a/b/c");
        try (KvStore store = KvStore.open(nested)) {
            store.put("x", "y");
        }
        assertTrue(Files.exists(nested.resolve(KvStore.LOG)));
    }

    @Test
    void invalid_input_and_closed_stores_are_rejected() throws IOException {
        KvStore store = KvStore.open(dir);
        assertThrows(IllegalArgumentException.class, () -> store.put("", "v"));
        assertThrows(IllegalArgumentException.class, () -> store.put(null, "v"));
        assertThrows(IllegalArgumentException.class, () -> store.put("k", null));
        assertThrows(IllegalArgumentException.class, () -> store.delete(""));
        assertThrows(IllegalArgumentException.class, () -> store.apply(List.of(Mutation.put("ok", "1"), Mutation.delete(""))));
        assertEquals(Optional.empty(), store.get("ok"), "a rejected batch must not be partly applied");
        store.close();
        store.close();
        assertThrows(IllegalStateException.class, () -> store.get("k"));
        assertThrows(IllegalStateException.class, () -> store.put("k", "v"));
        assertThrows(IllegalStateException.class, store::checkpoint);
    }

    // ---- the log ---------------------------------------------------------------------------

    @Test
    void each_write_is_in_the_log_before_put_returns() throws IOException {
        try (KvStore store = KvStore.open(dir)) {
            store.put("k", "v");
            assertArrayEquals(frame(List.of(Mutation.put("k", "v"))), Files.readAllBytes(log()));
            store.delete("k");
            assertArrayEquals(
                    concat(frame(List.of(Mutation.put("k", "v"))), frame(List.of(Mutation.delete("k")))),
                    Files.readAllBytes(log()));
        }
    }

    @Test
    void a_batch_is_one_frame() throws IOException {
        List<Mutation> batch = List.of(Mutation.put("x", "1"), Mutation.put("y", "2"), Mutation.delete("x"));
        try (KvStore store = KvStore.open(dir)) {
            store.apply(batch);
            assertEquals(Optional.empty(), store.get("x"));
            assertEquals(Optional.of("2"), store.get("y"));
            store.apply(List.of());
        }
        assertArrayEquals(frame(batch), Files.readAllBytes(log()));
    }

    @Test
    void a_handwritten_log_is_recovered() throws IOException {
        Files.write(log(), concat(
                frame(List.of(Mutation.put("one", "1"), Mutation.put("two", "2"))),
                frame(List.of(Mutation.delete("one"))),
                frame(List.of(Mutation.put("three", "3")))));
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(new TreeMap<>(Map.of("two", "2", "three", "3")), store.entries());
            assertEquals(new RecoveryInfo(3, 0), store.lastRecovery());
        }
    }

    // ---- crashes ---------------------------------------------------------------------------

    @Test
    void a_torn_last_write_is_discarded_and_the_log_truncated() throws IOException {
        writeThree();
        byte[] full = Files.readAllBytes(log());
        int lastFrame = frame(List.of(Mutation.put("c", "3"))).length;
        Files.write(log(), Arrays.copyOf(full, full.length - 3));
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(new TreeMap<>(Map.of("a", "1", "b", "2")), store.entries());
            assertEquals(new RecoveryInfo(2, lastFrame - 3), store.lastRecovery());
            assertEquals(full.length - lastFrame, Files.size(log()));
            store.put("d", "4");
        }
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(new TreeMap<>(Map.of("a", "1", "b", "2", "d", "4")), store.entries());
            assertEquals(new RecoveryInfo(3, 0), store.lastRecovery());
        }
    }

    @Test
    void a_frame_cut_inside_its_header_is_discarded() throws IOException {
        writeThree();
        byte[] full = Files.readAllBytes(log());
        int lastFrame = frame(List.of(Mutation.put("c", "3"))).length;
        Files.write(log(), Arrays.copyOf(full, full.length - lastFrame + 5));
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(2, store.size());
            assertEquals(new RecoveryInfo(2, 5), store.lastRecovery());
        }
    }

    @Test
    void garbage_after_the_last_frame_is_discarded() throws IOException {
        writeThree();
        long size = Files.size(log());
        Files.write(log(), concat(Files.readAllBytes(log()), new byte[] {0, 0, 0, 9, 1, 2, 3}));
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(3, store.size());
            assertEquals(new RecoveryInfo(3, 7), store.lastRecovery());
        }
        assertEquals(size, Files.size(log()));
    }

    @Test
    void a_flipped_bit_fails_the_checksum() throws IOException {
        writeThree();
        byte[] bytes = Files.readAllBytes(log());
        bytes[bytes.length - 1] ^= 0x01; // the last byte of the value "3"
        Files.write(log(), bytes);
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(Optional.empty(), store.get("c"));
            assertEquals(2, store.size());
        }
    }

    @Test
    void recovery_stops_at_the_first_damaged_frame() throws IOException {
        writeThree();
        byte[] bytes = Files.readAllBytes(log());
        int first = frame(List.of(Mutation.put("a", "1"))).length;
        bytes[first + 12] ^= 0x40; // inside the second frame's payload
        Files.write(log(), bytes);
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(new TreeMap<>(Map.of("a", "1")), store.entries());
            assertEquals(new RecoveryInfo(1, bytes.length - first), store.lastRecovery());
        }
    }

    @Test
    void a_torn_batch_is_all_or_nothing() throws IOException {
        try (KvStore store = KvStore.open(dir)) {
            store.put("before", "ok");
            store.apply(List.of(Mutation.put("x", "1"), Mutation.put("y", "2"), Mutation.put("z", "3")));
        }
        byte[] full = Files.readAllBytes(log());
        Files.write(log(), Arrays.copyOf(full, full.length - 4));
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(new TreeMap<>(Map.of("before", "ok")), store.entries());
        }
    }

    @Test
    void a_frame_with_an_impossible_length_is_damage_not_a_crash() throws IOException {
        Files.write(log(), concat(frame(List.of(Mutation.put("a", "1"))), new byte[] {0x7F, -1, -1, -1, 0, 0, 0, 0}));
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(1, store.size());
            assertEquals(new RecoveryInfo(1, 8), store.lastRecovery());
        }
    }

    // ---- checkpoints -----------------------------------------------------------------------

    @Test
    void checkpoint_writes_a_snapshot_and_empties_the_log() throws IOException {
        try (KvStore store = KvStore.open(dir)) {
            for (int i = 0; i < 50; i++) store.put("k" + i, "v" + i);
            store.delete("k7");
            store.checkpoint();
            assertEquals(0, Files.size(log()));
            assertTrue(Files.exists(dir.resolve(KvStore.SNAPSHOT)));
            assertFalse(Files.exists(dir.resolve(KvStore.SNAPSHOT_TMP)));
            assertEquals(49, store.size());
            store.put("after", "x");
            store.delete("k0");
        }
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(49, store.size());
            assertEquals(Optional.of("x"), store.get("after"));
            assertEquals(Optional.empty(), store.get("k0"));
            assertEquals(Optional.of("v49"), store.get("k49"));
            assertEquals(new RecoveryInfo(2, 0), store.lastRecovery());
        }
    }

    @Test
    void the_snapshot_is_one_frame_of_puts_in_key_order() throws IOException {
        try (KvStore store = KvStore.open(dir)) {
            store.put("b", "2");
            store.put("a", "1");
            store.checkpoint();
        }
        assertArrayEquals(frame(List.of(Mutation.put("a", "1"), Mutation.put("b", "2"))),
                Files.readAllBytes(dir.resolve(KvStore.SNAPSHOT)));
    }

    @Test
    void a_crash_before_the_rename_leaves_a_stale_temporary_file() throws IOException {
        writeThree();
        Files.write(dir.resolve(KvStore.SNAPSHOT_TMP), new byte[] {1, 2, 3});
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(3, store.size());
        }
        assertFalse(Files.exists(dir.resolve(KvStore.SNAPSHOT_TMP)));
    }

    @Test
    void a_crash_after_the_rename_but_before_emptying_the_log_is_harmless() throws IOException {
        try (KvStore store = KvStore.open(dir)) {
            store.put("a", "1");
            store.put("b", "2");
            store.delete("a");
            byte[] logBefore = Files.readAllBytes(log());
            store.checkpoint();
            Files.write(log(), logBefore); // as if the truncation never happened
        }
        try (KvStore store = KvStore.open(dir)) {
            assertEquals(new TreeMap<>(Map.of("b", "2")), store.entries());
        }
    }

    @Test
    void a_corrupt_snapshot_is_an_error() throws IOException {
        try (KvStore store = KvStore.open(dir)) {
            store.put("a", "1");
            store.checkpoint();
        }
        byte[] snapshot = Files.readAllBytes(dir.resolve(KvStore.SNAPSHOT));
        snapshot[snapshot.length - 1] ^= 1;
        Files.write(dir.resolve(KvStore.SNAPSHOT), snapshot);
        assertThrows(IOException.class, () -> KvStore.open(dir));
    }

    @Test
    void a_large_batch_and_a_large_recovery_are_fast() {
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            List<Mutation> batch = new ArrayList<>();
            for (int i = 0; i < 100_000; i++) batch.add(Mutation.put("key-" + i, "value-" + i));
            try (KvStore store = KvStore.open(dir)) {
                store.apply(batch);
                for (int i = 0; i < 200; i++) store.put("small-" + i, "x");
            }
            try (KvStore store = KvStore.open(dir)) {
                assertEquals(100_200, store.size());
                assertEquals(new RecoveryInfo(201, 0), store.lastRecovery());
                store.checkpoint();
            }
            try (KvStore store = KvStore.open(dir)) {
                assertEquals(Optional.of("value-99999"), store.get("key-99999"));
            }
        });
    }
}
