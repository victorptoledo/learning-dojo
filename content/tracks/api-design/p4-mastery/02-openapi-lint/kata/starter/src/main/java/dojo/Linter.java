package dojo;

import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/** Checks an OpenAPI document against the company's API style guide (see README.md). */
public final class Linter {
    private Linter() {}

    /** Every finding in the document, sorted by pointer, then rule, then message. */
    public static List<Finding> lint(String openApiYaml) {
        Map<String, Object> document = new Yaml().load(openApiYaml);
        return List.of();
    }
}
