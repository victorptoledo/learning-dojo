package dojo;

/**
 * A way to reach a user. The type code became a sealed type: each channel owns its format,
 * its price and whether it waits for quiet hours, and the compiler knows all three.
 */
public sealed interface Channel {
    String format(String user, String text);

    long costCents();

    boolean waitsForQuietHours();

    static Channel parse(String name) {
        return switch (name) {
            case "email" -> new Email();
            case "sms" -> new Sms();
            case "push" -> new Push();
            case null, default -> throw new IllegalArgumentException("unknown channel: " + name);
        };
    }

    record Email() implements Channel {
        @Override
        public String format(String user, String text) {
            return "EMAIL " + user + ": " + text;
        }

        @Override
        public long costCents() {
            return 0;
        }

        @Override
        public boolean waitsForQuietHours() {
            return false;
        }
    }

    record Sms() implements Channel {
        static final int MAX_LENGTH = 160;

        @Override
        public String format(String user, String text) {
            String body = text.length() <= MAX_LENGTH ? text : text.substring(0, MAX_LENGTH - 3) + "...";
            return "SMS " + user + ": " + body;
        }

        @Override
        public long costCents() {
            return 5;
        }

        @Override
        public boolean waitsForQuietHours() {
            return true;
        }
    }

    record Push() implements Channel {
        static final int TITLE_LENGTH = 40;

        @Override
        public String format(String user, String text) {
            return "PUSH " + user + ": " + text.substring(0, Math.min(TITLE_LENGTH, text.length()));
        }

        @Override
        public long costCents() {
            return 1;
        }

        @Override
        public boolean waitsForQuietHours() {
            return true;
        }
    }
}
