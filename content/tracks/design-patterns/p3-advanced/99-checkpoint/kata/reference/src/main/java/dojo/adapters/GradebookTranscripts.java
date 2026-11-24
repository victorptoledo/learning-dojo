package dojo.adapters;

import dojo.domain.Grade;
import dojo.domain.Transcripts;
import dojo.legacy.LegacyGradebook;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Anti-corruption layer: turns the gradebook's strings into grades, skipping what is broken. */
public final class GradebookTranscripts implements Transcripts {
    private final LegacyGradebook gradebook;

    public GradebookTranscripts(LegacyGradebook gradebook) {
        this.gradebook = gradebook;
    }

    @Override
    public List<Grade> of(String studentId) {
        List<Grade> grades = new ArrayList<>();
        for (String entry : gradebook.transcript(studentId).split(";")) {
            String[] parts = entry.split(":");
            if (parts.length == 2 && !parts[0].isBlank()) {
                points(parts[1].trim()).ifPresent(points -> grades.add(new Grade(parts[0].trim(), points)));
            }
        }
        return List.copyOf(grades);
    }

    /** A to F, with an optional + or - worth 0.3; A+ stays 4.0 and F is always 0.0. */
    static Optional<Double> points(String letter) {
        if (letter.isEmpty() || letter.length() > 2) {
            return Optional.empty();
        }
        double base = switch (letter.charAt(0)) {
            case 'A' -> 4.0;
            case 'B' -> 3.0;
            case 'C' -> 2.0;
            case 'D' -> 1.0;
            case 'F' -> 0.0;
            default -> -1.0;
        };
        if (base < 0) {
            return Optional.empty();
        }
        if (letter.length() == 1) {
            return Optional.of(base);
        }
        return switch (letter.charAt(1)) {
            case '+' -> Optional.of(base == 0.0 ? 0.0 : Math.min(4.0, base + 0.3));
            case '-' -> Optional.of(base == 0.0 ? 0.0 : base - 0.3);
            default -> Optional.empty();
        };
    }
}
