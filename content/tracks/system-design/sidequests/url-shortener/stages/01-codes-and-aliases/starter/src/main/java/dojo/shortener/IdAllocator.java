package dojo.shortener;

/**
 * The ticket server. Each call is a round trip and hands out ids no other caller ever gets.
 * Implementations are safe to call from many threads.
 */
public interface IdAllocator {

    /** Reserves `count` ids for the caller and returns the first: the block is first..first+count-1. */
    long reserve(int count);
}
