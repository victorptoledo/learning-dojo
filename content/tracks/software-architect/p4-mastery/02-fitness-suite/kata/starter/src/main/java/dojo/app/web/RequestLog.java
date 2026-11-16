package dojo.app.web;

import java.util.ArrayList;
import java.util.List;

public final class RequestLog {
    private static final List<String> LINES = new ArrayList<>();

    private RequestLog() {}

    public static void note(String line) {
        LINES.add(line);
    }
}
