package dojo;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Two stable 64-bit hashes of a text, from its MD5 digest. */
public final class Hashing {
    private Hashing() {}

    /** {h1, h2}: the first and the last 8 bytes of the MD5 digest of the UTF-8 text. */
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
