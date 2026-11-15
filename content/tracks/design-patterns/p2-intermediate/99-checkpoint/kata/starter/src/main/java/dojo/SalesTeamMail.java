package dojo;

import java.util.ArrayList;
import java.util.List;

/** Tells the sales team about big quotes. */
public class SalesTeamMail {
    private final List<String> outbox = new ArrayList<>();

    public void send(String text) {
        outbox.add(text);
    }

    public List<String> outbox() {
        return List.copyOf(outbox);
    }
}
