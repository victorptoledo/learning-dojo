package dojo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends notifications. Every channel is a branch of one switch, the SMS library's quirks are
 * handled inline, and duplicate suppression is tangled with sending, so adding a channel or
 * turning suppression off for one of them means editing this method.
 */
public class Notifier {
    private final LegacySmsGateway sms;
    private final Mailer mailer;
    private final Clock clock;
    private final Map<String, Instant> lastSent = new HashMap<>();

    public Notifier(LegacySmsGateway sms, Mailer mailer, Clock clock) {
        this.sms = sms;
        this.mailer = mailer;
        this.clock = clock;
    }

    public void send(Message message) {
        String key = message.channel() + "|" + message.to() + "|" + message.text();
        Instant now = clock.instant();
        Instant last = lastSent.get(key);
        if (last != null && Duration.between(last, now).compareTo(Duration.ofMinutes(10)) < 0) {
            return;
        }
        switch (message.channel()) {
            case "sms" -> {
                String digits = message.to().replace("+", "").replace(" ", "");
                String body = message.text();
                if (body.length() > 160) {
                    body = body.substring(0, 157) + "...";
                }
                if (!sms.sendSms(digits, body)) {
                    throw new DeliveryException("sms to " + message.to() + " failed");
                }
            }
            case "email" -> {
                String text = message.text();
                int newline = text.indexOf('\n');
                String subject = newline < 0 ? text : text.substring(0, newline);
                String body = newline < 0 ? "" : text.substring(newline + 1);
                mailer.send(message.to(), subject, body);
            }
            default -> throw new IllegalArgumentException("unknown channel: " + message.channel());
        }
        lastSent.put(key, now);
    }
}
