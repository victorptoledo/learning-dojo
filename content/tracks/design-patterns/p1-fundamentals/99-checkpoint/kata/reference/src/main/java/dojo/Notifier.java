package dojo;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Routes each message to the channel named in it. Channels arrive from outside. */
public class Notifier {
    private final Map<String, Channel> channels;

    public Notifier(Map<String, Channel> channels) {
        this.channels = Map.copyOf(channels);
    }

    /** The production wiring: SMS and email, each suppressing duplicates for ten minutes. */
    public static Notifier standard(LegacySmsGateway operator, Mailer mailer, Clock clock) {
        Duration window = Duration.ofMinutes(10);
        return new Notifier(Map.of(
                "sms", new DeduplicatingChannel(new SmsChannel(operator), clock, window),
                "email", new DeduplicatingChannel(new EmailChannel(mailer), clock, window)));
    }

    public void send(Message message) {
        Channel channel = channels.get(message.channel());
        if (channel == null) {
            throw new IllegalArgumentException("unknown channel: " + message.channel());
        }
        channel.deliver(message);
    }

    public Set<String> channels() {
        return new TreeSet<>(channels.keySet());
    }
}
