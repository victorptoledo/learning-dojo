package dojo.broker;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** A stable 64-bit hash: the same text gives the same value on every machine. */
public final class Hashing {
    private Hashing() {}

    /** The first 8 bytes of the MD5 digest of the UTF-8 text. */
    public static long hash64(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(text.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is always available", e);
        }
    }
}
