package dojo;

import java.util.ArrayList;
import java.util.List;

/** Sends notifications now, or holds the ones that wait for the end of quiet hours. */
public final class Dispatcher {
    private static final int QUIET_FROM = 22;
    private static final int QUIET_UNTIL = 7;

    private record Message(Channel channel, String text) {}

    private final List<String> sent = new ArrayList<>();
    private final List<Message> held = new ArrayList<>();
    private long costCents;

    /** Sends now, or holds an SMS or push during quiet hours. Returns whether it was sent. */
    public boolean send(String channelName, String user, String text, int hour) {
        Channel channel = Channel.parse(channelName);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        checkHour(hour);
        Message message = new Message(channel, channel.format(user, text));
        if (channel.waitsForQuietHours() && isQuiet(hour)) {
            held.add(message);
            return false;
        }
        deliver(message);
        return true;
    }

    /** Outside quiet hours, sends everything held, in order. Returns how many were sent. */
    public int releaseHeld(int hour) {
        checkHour(hour);
        if (isQuiet(hour)) {
            return 0;
        }
        int released = held.size();
        held.forEach(this::deliver);
        held.clear();
        return released;
    }

    public List<String> sent() {
        return List.copyOf(sent);
    }

    public List<String> held() {
        return held.stream().map(Message::text).toList();
    }

    public long costCents() {
        return costCents;
    }

    private void deliver(Message message) {
        sent.add(message.text());
        costCents += message.channel().costCents();
    }

    private static boolean isQuiet(int hour) {
        return hour >= QUIET_FROM || hour < QUIET_UNTIL;
    }

    private static void checkHour(int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour must be 0-23");
        }
    }
}
