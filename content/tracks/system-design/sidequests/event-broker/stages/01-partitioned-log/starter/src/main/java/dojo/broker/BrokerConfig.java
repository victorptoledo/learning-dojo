package dojo.broker;

import java.time.Duration;

/**
 * The broker's settings: a group member that does not poll for `sessionTimeout` is dead
 * (stage 2), and a message gets `maxAttempts` handler calls before it is a dead letter
 * (stage 4).
 */
public record BrokerConfig(Duration sessionTimeout, int maxAttempts) {
}
