package dojo.shortener;

import java.time.Duration;

/**
 * The token buckets of each client: writes (`post`) hold up to `writeBurst` tokens and get
 * one back every `writeRefill`; reads (`get`) hold `readBurst` and get one every `readRefill`.
 */
public record Limits(int writeBurst, Duration writeRefill, int readBurst, Duration readRefill) {
}
