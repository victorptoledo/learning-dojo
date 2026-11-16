package dojo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.CRC32;

/** A crash-safe key-value store: a write-ahead log of checksummed frames plus atomic snapshots. */
public final class KvStore implements AutoCloseable {
    public static final String LOG = "wal.log";
    public static final String SNAPSHOT = "data.snapshot";
    public static final String SNAPSHOT_TMP = "data.snapshot.tmp";

    private static final byte PUT = 1;
    private static final byte DELETE = 2;

    private final Path dir;
    private final TreeMap<String, String> data = new TreeMap<>();
    private final RecoveryInfo recovery;
    private FileChannel log;
    private boolean closed;

    private KvStore(Path dir) throws IOException {
        this.dir = dir;
        Files.createDirectories(dir);
        Files.deleteIfExists(dir.resolve(SNAPSHOT_TMP));
        Path snapshot = dir.resolve(SNAPSHOT);
        if (Files.exists(snapshot)) {
            byte[] bytes = Files.readAllBytes(snapshot);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            List<Mutation> all = readFrame(buffer);
            if (all == null || buffer.hasRemaining()) throw new IOException("corrupt snapshot " + snapshot);
            all.forEach(this::applyInMemory);
        }
        Path logPath = dir.resolve(LOG);
        log = FileChannel.open(logPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(logPath));
        int batches = 0;
        int validEnd = 0;
        List<Mutation> batch;
        while ((batch = readFrame(buffer)) != null) {
            batch.forEach(this::applyInMemory);
            batches++;
            validEnd = buffer.position();
        }
        long discarded = buffer.limit() - validEnd;
        if (discarded > 0) {
            log.truncate(validEnd);
            log.force(true);
        }
        log.position(validEnd);
        recovery = new RecoveryInfo(batches, discarded);
    }

    /** Opens (creating if needed) the store in {@code dir}, recovering from its files. */
    public static KvStore open(Path dir) throws IOException {
        return new KvStore(dir);
    }

    public Optional<String> get(String key) {
        checkOpen();
        return Optional.ofNullable(data.get(key));
    }

    public void put(String key, String value) throws IOException {
        apply(List.of(Mutation.put(key, value)));
    }

    public void delete(String key) throws IOException {
        apply(List.of(Mutation.delete(key)));
    }

    /** Applies all mutations atomically: after a crash, either all of them or none are present. */
    public void apply(List<Mutation> batch) throws IOException {
        checkOpen();
        if (batch.isEmpty()) return;
        for (Mutation m : batch) validate(m);
        ByteBuffer frame = ByteBuffer.wrap(frame(batch));
        while (frame.hasRemaining()) log.write(frame);
        log.force(false);
        batch.forEach(this::applyInMemory);
    }

    public int size() {
        checkOpen();
        return data.size();
    }

    /** A sorted copy of all entries. */
    public SortedMap<String, String> entries() {
        checkOpen();
        return new TreeMap<>(data);
    }

    public RecoveryInfo lastRecovery() {
        return recovery;
    }

    /** Writes all data to a new snapshot atomically, then empties the log. */
    public void checkpoint() throws IOException {
        checkOpen();
        List<Mutation> all = new ArrayList<>(data.size());
        for (Map.Entry<String, String> e : data.entrySet()) all.add(Mutation.put(e.getKey(), e.getValue()));
        Path tmp = dir.resolve(SNAPSHOT_TMP);
        try (FileChannel out = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            ByteBuffer frame = ByteBuffer.wrap(frame(all));
            while (frame.hasRemaining()) out.write(frame);
            out.force(true);
        }
        Files.move(tmp, dir.resolve(SNAPSHOT), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        log.truncate(0);
        log.position(0);
        log.force(true);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            log.close();
        }
    }

    // ---- frames ---------------------------------------------------------------------------

    private static byte[] frame(List<Mutation> batch) throws IOException {
        ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
        DataOutputStream payload = new DataOutputStream(payloadBytes);
        payload.writeInt(batch.size());
        for (Mutation m : batch) {
            payload.writeByte(m.value() == null ? DELETE : PUT);
            writeString(payload, m.key());
            if (m.value() != null) writeString(payload, m.value());
        }
        byte[] body = payloadBytes.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(body);
        ByteBuffer out = ByteBuffer.allocate(8 + body.length);
        out.putInt(body.length).putInt((int) crc.getValue()).put(body);
        return out.array();
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    /** The next frame's mutations, or null (with the position unchanged) if it is missing or damaged. */
    private static List<Mutation> readFrame(ByteBuffer buffer) {
        int start = buffer.position();
        List<Mutation> result;
        try {
            result = parseFrame(buffer);
        } catch (RuntimeException e) {
            result = null;
        }
        if (result == null) buffer.position(start);
        return result;
    }

    private static List<Mutation> parseFrame(ByteBuffer buffer) {
        if (buffer.remaining() < 8) return null;
        int length = buffer.getInt();
        int checksum = buffer.getInt();
        if (length < 4 || length > buffer.remaining()) return null;
        byte[] body = new byte[length];
        buffer.get(body);
        CRC32 crc = new CRC32();
        crc.update(body);
        if ((int) crc.getValue() != checksum) return null;
        ByteBuffer p = ByteBuffer.wrap(body);
        int count = p.getInt();
        List<Mutation> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            byte op = p.get();
            String key = readString(p);
            if (op == PUT) out.add(Mutation.put(key, readString(p)));
            else if (op == DELETE) out.add(Mutation.delete(key));
            else return null;
        }
        return p.hasRemaining() ? null : out;
    }

    private static String readString(ByteBuffer p) {
        int n = p.getInt();
        if (n < 0 || n > p.remaining()) throw new IllegalArgumentException("bad length");
        byte[] bytes = new byte[n];
        p.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // ---- helpers --------------------------------------------------------------------------

    private void applyInMemory(Mutation m) {
        if (m.value() == null) data.remove(m.key());
        else data.put(m.key(), m.value());
    }

    private static void validate(Mutation m) {
        if (m == null || m.key() == null || m.key().isEmpty()) {
            throw new IllegalArgumentException("keys must be non-empty");
        }
    }

    private void checkOpen() {
        if (closed) throw new IllegalStateException("the store is closed");
    }
}
