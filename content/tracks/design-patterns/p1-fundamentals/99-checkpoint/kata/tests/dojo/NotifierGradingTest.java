package dojo;

import static dojo.GradingSupport.allClasses;
import static dojo.GradingSupport.code;
import static dojo.GradingSupport.instanceFieldTypes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NotifierGradingTest {
    /** A clock the test moves by hand. */
    static final class HandClock extends Clock {
        Instant now = Instant.parse("2026-05-01T09:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    static final class RecordingOperator extends LegacySmsGateway {
        final List<String> sent = new ArrayList<>();
        boolean up = true;

        @Override
        public boolean sendSms(String digits, String body) {
            sent.add(digits + ":" + body);
            return up && super.sendSms(digits, body);
        }
    }

    @Test
    void sms_numbers_are_cleaned_and_long_texts_cut() {
        var operator = new RecordingOperator();
        var sms = new SmsChannel(operator);
        sms.deliver(new Message("sms", "+44 20 7946 0000", "a".repeat(160)));
        sms.deliver(new Message("sms", "351912345678", "b".repeat(161)));
        assertEquals(List.of("442079460000:" + "a".repeat(160), "351912345678:" + "b".repeat(157) + "..."), operator.sent);
    }

    @Test
    void a_failed_sms_becomes_an_exception() {
        var operator = new RecordingOperator();
        operator.up = false;
        var error = assertThrows(DeliveryException.class,
                () -> new SmsChannel(operator).deliver(new Message("sms", "+1 555", "hi")));
        assertEquals("sms to +1 555 failed", error.getMessage());
    }

    @Test
    void email_splits_subject_and_body() {
        List<String> mails = new ArrayList<>();
        var email = new EmailChannel((address, subject, body) -> mails.add(address + "|" + subject + "|" + body));
        email.deliver(new Message("email", "a@x.org", "Only a subject"));
        email.deliver(new Message("email", "b@x.org", "Subject\nline one\nline two"));
        assertEquals(List.of("a@x.org|Only a subject|", "b@x.org|Subject|line one\nline two"), mails);
    }

    @Test
    void duplicates_are_dropped_only_inside_the_window() {
        var clock = new HandClock();
        List<Message> delivered = new ArrayList<>();
        var channel = new DeduplicatingChannel(delivered::add, clock, Duration.ofMinutes(10));
        var hello = new Message("push", "dev-1", "hello");
        channel.deliver(hello);
        clock.advance(Duration.ofMinutes(9));
        channel.deliver(hello);
        channel.deliver(new Message("push", "dev-2", "hello"));
        channel.deliver(new Message("push", "dev-1", "hello again"));
        clock.advance(Duration.ofMinutes(1));
        channel.deliver(hello);
        assertEquals(4, delivered.size());
        assertEquals(List.of(hello, new Message("push", "dev-2", "hello"), new Message("push", "dev-1", "hello again"), hello),
                delivered);
        assertThrows(IllegalArgumentException.class, () -> new DeduplicatingChannel(delivered::add, clock, Duration.ZERO));
    }

    @Test
    void a_failed_delivery_is_not_remembered() {
        var clock = new HandClock();
        int[] attempts = {0};
        var channel = new DeduplicatingChannel(message -> {
            if (attempts[0]++ == 0) throw new DeliveryException("down");
        }, clock, Duration.ofMinutes(10));
        var message = new Message("sms", "1", "x");
        assertThrows(DeliveryException.class, () -> channel.deliver(message));
        channel.deliver(message);
        channel.deliver(message);
        assertEquals(2, attempts[0], "the retry after a failure goes through, the duplicate after it does not");
    }

    @Test
    void new_channels_plug_in_without_changing_the_notifier() {
        List<Message> pushed = new ArrayList<>();
        var notifier = new Notifier(Map.of("push", pushed::add));
        notifier.send(new Message("push", "dev", "hi"));
        assertEquals(List.of(new Message("push", "dev", "hi")), pushed);
        assertEquals(Set.of("push"), notifier.channels());
        var error = assertThrows(IllegalArgumentException.class, () -> notifier.send(new Message("sms", "1", "x")));
        assertEquals("unknown channel: sms", error.getMessage());
    }

    @Test
    void the_standard_wiring_behaves_like_the_old_notifier() {
        var clock = new HandClock();
        var operator = new RecordingOperator();
        List<String> mails = new ArrayList<>();
        var notifier = Notifier.standard(operator, (a, s, b) -> mails.add(a + "|" + s), clock);
        assertEquals(Set.of("email", "sms"), notifier.channels());
        var sms = new Message("sms", "+351 91", "code 1");
        notifier.send(sms);
        notifier.send(sms);
        notifier.send(new Message("email", "a@x.org", "code 1"));
        notifier.send(new Message("email", "a@x.org", "code 1"));
        clock.advance(Duration.ofMinutes(10));
        notifier.send(sms);
        assertEquals(List.of("35191:code 1", "35191:code 1"), operator.sent);
        assertEquals(List.of("a@x.org|code 1"), mails);
    }

    @Test
    void routing_sending_and_suppressing_live_in_separate_classes() {
        assertTrue(Channel.class.isInterface(), "Channel must be an interface");
        for (Class<?> type : List.of(SmsChannel.class, EmailChannel.class, DeduplicatingChannel.class)) {
            assertTrue(Channel.class.isAssignableFrom(type), type.getSimpleName() + " must implement Channel");
        }
        assertEquals(List.of(Map.class), instanceFieldTypes(Notifier.class), "Notifier should hold only its channels");
        String notifier = code("dojo.Notifier");
        for (String forbidden : List.of("switch", ".equals(", "instanceof", "Instant", "sendSms(", ".send(")) {
            assertFalse(notifier.contains(forbidden), "Notifier should only look the channel up; found " + forbidden);
        }
        assertTrue(instanceFieldTypes(DeduplicatingChannel.class).contains(Channel.class),
                "DeduplicatingChannel should wrap another Channel");
        for (String name : allClasses()) {
            if (!name.equals("dojo.SmsChannel") && !name.equals("dojo.LegacySmsGateway")) {
                assertFalse(code(name).contains("sendSms("), "only SmsChannel may call the operator library: " + name);
            }
        }
    }
}
