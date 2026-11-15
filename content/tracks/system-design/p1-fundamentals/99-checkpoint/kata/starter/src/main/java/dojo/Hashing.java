package dojo;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Stable hashes from the MD5 digest of a text. */
public final class Hashing {
    private Hashing() {}

    /** The first 8 bytes of the digest. */
    public static long hash64(String text) {
        return hash128(text)[0];
    }

    /** {h1, h2}: the first and the last 8 bytes of the digest. */
    public static long[] hash128(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(text.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.wrap(digest);
            return new long[] {buffer.getLong(), buffer.getLong()};
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is always available", e);
        }
    }
}
