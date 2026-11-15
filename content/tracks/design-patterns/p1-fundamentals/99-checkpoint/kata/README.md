# Notification Hub

`Notifier.send` delivers SMS and email notifications from one `switch`. The SMS branch deals
with the operator library's quirks inline, the email branch splits subject and body, and
duplicate suppression (the same message within ten minutes is dropped) is mixed into the same
method. Adding a push channel, or turning suppression off for email only, means editing it.

Use everything from this phase: **Strategy** (each channel is a `Channel`), **Adapter** (the
SMS library and the mailer behind `Channel`), **Decorator** (suppression wraps any channel)
and a **factory** method that wires the production setup. `LegacySmsGateway` and `Mailer` are
libraries: do not change them.

## The target design

| Type | What it does |
| --- | --- |
| `@FunctionalInterface interface Channel` | `void deliver(Message message)`; throws `DeliveryException` on failure |
| `SmsChannel implements Channel` | `SmsChannel(LegacySmsGateway operator)`. Removes `+` and spaces from the number; a text over 160 characters becomes its first 157 plus `"..."`; a `false` from the library throws `DeliveryException("sms to " + to + " failed")` (the original `to`). |
| `EmailChannel implements Channel` | `EmailChannel(Mailer mailer)`. The text up to the first `\n` is the subject and the rest the body; with no newline, the body is `""`. |
| `DeduplicatingChannel implements Channel` | `DeduplicatingChannel(Channel inner, Clock clock, Duration window)`. Drops a message **equal** to one it delivered less than `window` ago; at exactly `window` it sends again. Only successful deliveries are remembered. A zero or negative window throws `IllegalArgumentException`. |
| `Notifier` | `Notifier(Map<String, Channel> channels)`; `send(message)` delivers through `channels.get(message.channel())`, an unknown channel throws `IllegalArgumentException("unknown channel: " + name)`; `Set<String> channels()` sorted |
| `static Notifier standard(LegacySmsGateway, Mailer, Clock)` | `"sms"` and `"email"`, each wrapped in a `DeduplicatingChannel` with a ten-minute window |

## What the grading tests check

- SMS cleaning and cutting, failures as exceptions, and the email subject split.
- Suppression inside and outside the window, per message, with a clock the test moves.
- A failed delivery is not remembered, so it can be retried at once.
- A new channel (a lambda) works without changing `Notifier`; `standard(...)` behaves like
  the old class.
- `Notifier` holds only its map and has no `switch`, `equals`, `instanceof` or time logic;
  only `SmsChannel` calls `sendSms`.

The visible tests describe the target design, so they do not compile until `Channel` and its
implementations exist.
