package dojo.shortener;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Creates short codes and resolves them through a read cache. See README.md. */
public final class Shortener {
    static final String DIGITS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Pattern ALIAS = Pattern.compile("[A-Za-z0-9_-]{3,32}");
    private static final int MAX_URL = 2048;

    /** A cached answer: the URL, or empty for a code the store does not have. */
    private record Entry(Optional<String> url, Instant expiresAt) {}

    private final LinkStore store;
    private final IdAllocator ids;
    private final Clock clock;
    private final ShortenerConfig config;
    private final Object idLock = new Object();
    private long nextId;
    private long blockEnd; // exclusive; nextId == blockEnd means the block is used up

    // The cache has its own lock, never held during a round trip to the store.
    private final LinkedHashMap<String, Entry> cache = new LinkedHashMap<>(16, 0.75f, true);
    private long hits;
    private long misses;
    private long evictions;

    public Shortener(LinkStore store, IdAllocator ids, Clock clock, ShortenerConfig config) {
        this.store = store;
        this.ids = ids;
        this.clock = clock;
        this.config = config;
    }

    public String shorten(String url) {
        validateUrl(url);
        while (true) {
            String code = encode(takeId(), config.codeLength());
            if (store.putIfAbsent(code, url)) {
                remember(code, Optional.of(url), config.cacheTtl());
                return code;
            }
        }
    }

    public String shorten(String url, String alias) {
        validateUrl(url);
        if (alias == null || !ALIAS.matcher(alias).matches()) {
            throw new IllegalArgumentException("an alias has 3 to 32 letters, digits, '-' or '_': " + alias);
        }
        if (!store.putIfAbsent(alias, url)) {
            throw new IllegalStateException("the alias is taken: " + alias);
        }
        remember(alias, Optional.of(url), config.cacheTtl());
        return alias;
    }

    public Optional<String> resolve(String code) {
        Instant now = clock.instant();
        synchronized (cache) {
            Entry entry = cache.get(code);
            if (entry != null && now.isBefore(entry.expiresAt())) {
                hits++;
                return entry.url();
            }
            if (entry != null) {
                cache.remove(code);
            }
            misses++;
        }
        Optional<String> url = store.get(code);
        remember(code, url, url.isPresent() ? config.cacheTtl() : config.negativeTtl());
        return url;
    }

    public boolean delete(String code) {
        boolean deleted = store.delete(code);
        synchronized (cache) {
            cache.remove(code);
        }
        return deleted;
    }

    public CacheStats stats() {
        synchronized (cache) {
            return new CacheStats(hits, misses, evictions);
        }
    }

    private void remember(String code, Optional<String> url, Duration ttl) {
        Instant expiresAt = clock.instant().plus(ttl);
        synchronized (cache) {
            cache.put(code, new Entry(url, expiresAt));
            while (cache.size() > config.cacheCapacity()) {
                var eldest = cache.entrySet().iterator();
                eldest.next();
                eldest.remove();
                evictions++;
            }
        }
    }

    private long takeId() {
        synchronized (idLock) {
            if (nextId == blockEnd) {
                nextId = ids.reserve(config.blockSize());
                blockEnd = nextId + config.blockSize();
            }
            return nextId++;
        }
    }

    static String encode(long id, int length) {
        var code = new StringBuilder();
        do {
            code.append(DIGITS.charAt((int) (id % 62)));
            id /= 62;
        } while (id > 0);
        while (code.length() < length) {
            code.append('0');
        }
        return code.reverse().toString();
    }

    static void validateUrl(String url) {
        if (url == null || url.length() > MAX_URL) {
            throw new IllegalArgumentException("a URL has at most " + MAX_URL + " characters");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("not a URL: " + e.getMessage());
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("only http and https URLs: " + url);
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("a URL needs a host: " + url);
        }
    }
}
