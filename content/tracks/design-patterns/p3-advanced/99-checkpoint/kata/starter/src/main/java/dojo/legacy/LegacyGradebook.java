package dojo.legacy;

import java.util.HashMap;
import java.util.Map;

/**
 * The registrar's gradebook library, from 2006. Do not change it: other systems use it.
 * A transcript is one string such as {@code "MATH101:B+;PHYS201:C"}, or {@code ""} when the
 * student has no grades. Old rows may hold empty or broken entries.
 */
public final class LegacyGradebook {
    private final Map<String, String> rows = new HashMap<>();

    public void put(String studentId, String transcript) {
        rows.put(studentId, transcript);
    }

    public String transcript(String studentId) {
        return rows.getOrDefault(studentId, "");
    }
}
