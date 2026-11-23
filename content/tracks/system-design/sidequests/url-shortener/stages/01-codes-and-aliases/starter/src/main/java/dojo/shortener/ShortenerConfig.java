package dojo.shortener;

import java.time.Duration;

/**
 * The numbers of the design. `blockSize` ids are reserved per round trip to the ticket
 * server and codes are padded to `codeLength`; the cache settings are used from stage 2.
 */
public record ShortenerConfig(
        int blockSize, int codeLength, int cacheCapacity, Duration cacheTtl, Duration negativeTtl) {
}
