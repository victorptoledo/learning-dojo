package dojo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * The partners and their API keys. Written for you. Only a SHA-256 hash of each key is kept,
 * as a key store should; {@link #authenticate} hashes what the client sent and looks it up.
 * Two partners are seeded for trying the API by hand; the tests register their own.
 */
@Component
public class Partners {
    /** A partner, the scopes its key grants and the secret its webhooks are signed with. */
    public record Partner(String id, Set<String> scopes, String webhookSecret) {
        public boolean has(String scope) {
            return scopes.contains(scope);
        }
    }

    /** What a partner is given once, when it registers: its key and its webhook secret. */
    public record Credentials(String apiKey, String webhookSecret) {}

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Partner> byKeyHash = new ConcurrentHashMap<>();

    public Partners() {
        add("dk_test_acme_try_me_with_curl", new Partner("acme", Set.of("exports:read", "exports:write"),
                "whsec_" + Base64.getEncoder().encodeToString("acme webhook secret!".getBytes(StandardCharsets.UTF_8))));
        add("dk_test_globex_read_only_key", new Partner("globex", Set.of("exports:read"),
                "whsec_" + Base64.getEncoder().encodeToString("globex webhook secret".getBytes(StandardCharsets.UTF_8))));
    }

    /** Registers a partner with these scopes and returns its new key and webhook secret. */
    public Credentials register(String partnerId, String... scopes) {
        byte[] keyBytes = new byte[16];
        byte[] secretBytes = new byte[24];
        RANDOM.nextBytes(keyBytes);
        RANDOM.nextBytes(secretBytes);
        String apiKey = "dk_test_" + HexFormat.of().formatHex(keyBytes);
        String secret = "whsec_" + Base64.getEncoder().encodeToString(secretBytes);
        add(apiKey, new Partner(partnerId, Set.of(scopes), secret));
        return new Credentials(apiKey, secret);
    }

    /** The partner this key belongs to; empty for a missing or unknown key. */
    public Optional<Partner> authenticate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byKeyHash.get(hash(apiKey)));
    }

    private void add(String apiKey, Partner partner) {
        byKeyHash.put(hash(apiKey), partner);
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
