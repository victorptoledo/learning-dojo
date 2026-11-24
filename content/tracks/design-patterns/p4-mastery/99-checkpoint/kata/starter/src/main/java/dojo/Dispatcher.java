package dojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends notifications by email, SMS and push. Three teams added a layer each "to make it
 * extensible": a global config, a strategy per channel, a factory for the strategies and a
 * provider for the factory. It works, and DispatcherTest pins what it does.
 */
public class Dispatcher {

    /** Global settings, one instance for the whole application. */
    public static class Config {
        private static Config instance;
        public int quietStart = 22;
        public int quietEnd = 7;
        public int smsMax = 160;
        public int pushMax = 40;

        public static Config getInstance() {
            if (instance == null) {
                instance = new Config();
            }
            return instance;
        }
    }

    public interface NotificationStrategy {
        String format(String user, String text);
    }

    public static class EmailStrategy implements NotificationStrategy {
        @Override
        public String format(String user, String text) {
            return "EMAIL " + user + ": " + text;
        }
    }

    public static class SmsStrategy implements NotificationStrategy {
        @Override
        public String format(String user, String text) {
            int max = Config.getInstance().smsMax;
            if (text.length() > max) {
                text = text.substring(0, max - 3) + "...";
            }
            return "SMS " + user + ": " + text;
        }
    }

    public static class PushStrategy implements NotificationStrategy {
        @Override
        public String format(String user, String text) {
            int max = Config.getInstance().pushMax;
            if (text.length() > max) {
                text = text.substring(0, max);
            }
            return "PUSH " + user + ": " + text;
        }
    }

    public static class NotificationStrategyFactory {
        public NotificationStrategy create(String type) {
            if (type.equals("email")) {
                return new EmailStrategy();
            } else if (type.equals("sms")) {
                return new SmsStrategy();
            } else if (type.equals("push")) {
                return new PushStrategy();
            }
            return null;
        }
    }

    public static class NotificationStrategyFactoryProvider {
        private static NotificationStrategyFactoryProvider instance;

        public static NotificationStrategyFactoryProvider getInstance() {
            if (instance == null) {
                instance = new NotificationStrategyFactoryProvider();
            }
            return instance;
        }

        public NotificationStrategyFactory factory() {
            return new NotificationStrategyFactory();
        }
    }

    private List<String> sent = new ArrayList<>();
    private List<String> held = new ArrayList<>();
    private List<String> heldTypes = new ArrayList<>();
    private long cost = 0;

    /** Sends now, or holds an SMS or push during quiet hours. Returns whether it was sent. */
    public boolean send(String type, String user, String text, int hour) {
        NotificationStrategy strategy = NotificationStrategyFactoryProvider.getInstance().factory().create(type);
        if (strategy == null) {
            throw new IllegalArgumentException("unknown channel: " + type);
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("text is required");
        }
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour must be 0-23");
        }
        String message = strategy.format(user, text);
        if (type.equals("sms")) {
            if (hour >= Config.getInstance().quietStart || hour < Config.getInstance().quietEnd) {
                held.add(message);
                heldTypes.add("sms");
                return false;
            }
            sent.add(message);
            cost = cost + 5;
            return true;
        } else if (type.equals("push")) {
            if (hour >= Config.getInstance().quietStart || hour < Config.getInstance().quietEnd) {
                held.add(message);
                heldTypes.add("push");
                return false;
            }
            sent.add(message);
            cost = cost + 1;
            return true;
        } else {
            sent.add(message);
            return true;
        }
    }

    /** Outside quiet hours, sends everything held, in order. Returns how many were sent. */
    public int releaseHeld(int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour must be 0-23");
        }
        if (hour >= Config.getInstance().quietStart || hour < Config.getInstance().quietEnd) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < held.size(); i++) {
            sent.add(held.get(i));
            if (heldTypes.get(i).equals("sms")) {
                cost = cost + 5;
            } else if (heldTypes.get(i).equals("push")) {
                cost = cost + 1;
            }
            count++;
        }
        held.clear();
        heldTypes.clear();
        return count;
    }

    public List<String> sent() {
        return new ArrayList<>(sent);
    }

    public List<String> held() {
        return new ArrayList<>(held);
    }

    public long costCents() {
        return cost;
    }
}
