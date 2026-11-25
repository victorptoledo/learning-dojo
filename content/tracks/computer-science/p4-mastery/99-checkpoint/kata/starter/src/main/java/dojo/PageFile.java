package dojo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * A file of fixed-size pages: the disk. Given, and complete. It counts every page it reads and
 * writes, which is how the tests measure your I/O.
 */
public final class PageFile implements AutoCloseable {
    public static final int PAGE_SIZE = 4096;

    private final FileChannel channel;
    private long reads;
    private long writes;

    private PageFile(FileChannel channel) {
        this.channel = channel;
    }

    /** Opens the file, creating it empty if it does not exist. */
    public static PageFile open(Path path) {
        try {
            return new PageFile(FileChannel.open(path, StandardOpenOption.CREATE,
                    StandardOpenOption.READ, StandardOpenOption.WRITE));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** How many whole pages the file holds. */
    public int pageCount() {
        try {
            return Math.toIntExact(channel.size() / PAGE_SIZE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Reads page {@code pageId} into all of {@code into}, which must hold exactly one page. */
    public void read(int pageId, ByteBuffer into) {
        if (pageId < 0 || pageId >= pageCount()) {
            throw new IllegalArgumentException("page " + pageId + " is not in the file");
        }
        ByteBuffer view = whole(into);
        try {
            while (view.hasRemaining()) {
                if (channel.read(view, (long) pageId * PAGE_SIZE + view.position()) < 0) {
                    throw new IOException("unexpected end of file in page " + pageId);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        reads++;
    }

    /** Writes all of {@code from} as page {@code pageId}; a page past the end grows the file. */
    public void write(int pageId, ByteBuffer from) {
        if (pageId < 0) {
            throw new IllegalArgumentException("page " + pageId + " does not exist");
        }
        ByteBuffer view = whole(from);
        try {
            while (view.hasRemaining()) {
                channel.write(view, (long) pageId * PAGE_SIZE + view.position());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        writes++;
    }

    /** Forces the written pages to the device (fsync). */
    public void sync() {
        try {
            channel.force(true);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public long reads() {
        return reads;
    }

    public long writes() {
        return writes;
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ByteBuffer whole(ByteBuffer buffer) {
        if (buffer.capacity() != PAGE_SIZE) {
            throw new IllegalArgumentException("a page buffer holds exactly " + PAGE_SIZE + " bytes");
        }
        return buffer.duplicate().clear();
    }
}
