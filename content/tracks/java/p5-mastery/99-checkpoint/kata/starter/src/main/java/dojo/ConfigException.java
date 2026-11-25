package dojo;

import java.util.List;

/** Every problem found in the configuration at once. Already written. */
public class ConfigException extends RuntimeException {
    private final List<String> problems;

    public ConfigException(List<String> problems) {
        super("invalid configuration: " + String.join("; ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> problems() {
        return problems;
    }
}
