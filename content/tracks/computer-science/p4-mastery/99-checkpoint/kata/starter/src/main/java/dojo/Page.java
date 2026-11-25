package dojo;

import java.nio.ByteBuffer;

/**
 * A page held in a buffer pool frame. Given, and complete. Read and write {@link #data()} with
 * absolute methods ({@code getLong(offset)}, {@code putInt(offset, value)}); its byte order is
 * big-endian, Java's default.
 */
public final class Page {
    private final int id;
    private final ByteBuffer data;

    Page(int id, ByteBuffer data) {
        if (data.capacity() != PageFile.PAGE_SIZE) {
            throw new IllegalArgumentException("a page holds exactly " + PageFile.PAGE_SIZE + " bytes");
        }
        this.id = id;
        this.data = data;
    }

    public int id() {
        return id;
    }

    public ByteBuffer data() {
        return data;
    }
}
