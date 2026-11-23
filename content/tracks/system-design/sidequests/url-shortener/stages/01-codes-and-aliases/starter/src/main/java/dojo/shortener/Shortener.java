package dojo.shortener;

import java.time.Clock;
import java.util.Optional;

/** Creates short codes and resolves them. See README.md. */
public final class Shortener {

    public Shortener(LinkStore store, IdAllocator ids, Clock clock, ShortenerConfig config) {
    }

    public String shorten(String url) {
        throw new UnsupportedOperationException("todo: shorten");
    }

    public String shorten(String url, String alias) {
        throw new UnsupportedOperationException("todo: shorten with an alias");
    }

    public Optional<String> resolve(String code) {
        throw new UnsupportedOperationException("todo: resolve");
    }
}
