package dojo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * The tenants (the companies that bill their customers through this API) and their API keys,
 * stored as SHA-256 hashes. Written for you. Two tenants are seeded for trying the API by
 * hand; the tests register their own.
 */
@Component
public class Tenants {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, String> tenantByKeyHash = new ConcurrentHashMap<>();

    public Tenants() {
        tenantByKeyHash.put(hash("bk_test_northwind_try_me"), "northwind");
        tenantByKeyHash.put(hash("bk_test_contoso_try_me"), "contoso");
    }

    /** Registers a tenant and returns its new API key. */
    public String register(String tenantId) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String key = "bk_test_" + HexFormat.of().formatHex(bytes);
        tenantByKeyHash.put(hash(key), tenantId);
        return key;
    }

    /** The tenant this key belongs to; empty for a missing or unknown key. */
    public Optional<String> authenticate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tenantByKeyHash.get(hash(apiKey)));
    }

    private static String hash(String apiKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
