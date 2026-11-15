package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * These tests describe the design you are refactoring towards, so they do not compile until
 * Channel and its implementations exist (see README.md).
 */
class NotifierTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-01T09:00:00Z"), ZoneOffset.UTC);

    @Test
    void the_sms_channel_adapts_the_operator_library() {
        List<String> sent = new ArrayList<>();
        var operator = new LegacySmsGateway() {
            @Override
            public boolean sendSms(String digits, String body) {
                sent.add(digits + ":" + body);
                return true;
            }
        };
        new SmsChannel(operator).deliver(new Message("sms", "+351 912 345 678", "Your code is 1234"));
        assertEquals(List.of("351912345678:Your code is 1234"), sent);
    }

    @Test
    void the_notifier_routes_by_channel() {
        List<String> mails = new ArrayList<>();
        Mailer mailer = (address, subject, body) -> mails.add(address + "|" + subject + "|" + body);
        var notifier = new Notifier(Map.of("email", new EmailChannel(mailer)));

        notifier.send(new Message("email", "ana@example.com", "Order shipped\nIt arrives Friday."));
        assertEquals(List.of("ana@example.com|Order shipped|It arrives Friday."), mails);
        assertThrows(IllegalArgumentException.class, () -> notifier.send(new Message("fax", "x", "y")));
    }

    @Test
    void duplicates_are_dropped_by_a_decorator() {
        List<Message> delivered = new ArrayList<>();
        Channel channel = new DeduplicatingChannel(delivered::add, CLOCK, Duration.ofMinutes(10));
        var message = new Message("push", "device-1", "Hello");
        channel.deliver(message);
        channel.deliver(message);
        assertEquals(List.of(message), delivered);
    }
}
