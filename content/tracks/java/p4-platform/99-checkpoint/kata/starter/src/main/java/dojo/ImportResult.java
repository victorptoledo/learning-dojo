package dojo;

import java.util.List;

/** What one import run did. Already written. */
public record ImportResult(int files, int entries, List<String> errors) {
    public ImportResult {
        errors = List.copyOf(errors);
    }
}
