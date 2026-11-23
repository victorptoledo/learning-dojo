package dojo.shortener;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.Optional;
import java.util.regex.Pattern;

/** Creates short codes and resolves them. See README.md. */
public final class Shortener {
    static final String DIGITS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Pattern ALIAS = Pattern.compile("[A-Za-z0-9_-]{3,32}");
    private static final int MAX_URL = 2048;

    private final LinkStore store;
    private final IdAllocator ids;
    private final ShortenerConfig config;
    private long nextId;
    private long blockEnd; // exclusive; nextId == blockEnd means the block is used up

    public Shortener(LinkStore store, IdAllocator ids, Clock clock, ShortenerConfig config) {
        this.store = store;
        this.ids = ids;
        this.config = config;
    }

    public String shorten(String url) {
        validateUrl(url);
        while (true) {
            String code = encode(takeId(), config.codeLength());
            if (store.putIfAbsent(code, url)) {
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
        return alias;
    }

    public Optional<String> resolve(String code) {
        return store.get(code);
    }

    private long takeId() {
        if (nextId == blockEnd) {
            nextId = ids.reserve(config.blockSize());
            blockEnd = nextId + config.blockSize();
        }
        return nextId++;
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
