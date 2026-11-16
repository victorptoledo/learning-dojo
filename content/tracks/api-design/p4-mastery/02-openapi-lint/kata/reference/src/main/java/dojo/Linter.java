package dojo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;

/** Checks an OpenAPI document against the company's API style guide (see README.md). */
public final class Linter {
    static final List<String> METHODS = List.of("get", "put", "post", "delete", "options", "head", "patch", "trace");
    static final Set<String> VERBS = Set.of("get", "list", "fetch", "create", "add", "update", "delete", "remove");
    static final Pattern VERSION = Pattern.compile("3\\.[12]\\.\\d+");
    static final Pattern KEBAB = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    static final Pattern FIRST_WORD = Pattern.compile("^[a-z]+");
    static final Pattern CAMEL =Pattern.compile("[a-z][a-zA-Z0-9]*");
    static final Pattern TEMPLATE = Pattern.compile("\\{([^}]+)}");
    static final Pattern SUCCESS = Pattern.compile("2(\\d\\d|XX)");
    static final String PROBLEM_JSON = "application/problem+json";

    private Linter() {}

    /** Every finding in the document, sorted by pointer, then rule, then message. */
    public static List<Finding> lint(String openApiYaml) {
        Map<String, Object> document = new Yaml().load(openApiYaml);
        List<Finding> findings = new ArrayList<>();
        checkRoot(document, findings);
        Set<String> operationIds = new HashSet<>();
        map(document.get("paths")).forEach((path, item) -> checkPath(document, path, map(item), operationIds, findings));
        checkSchemas(map(map(document.get("components")).get("schemas")), findings);
        findings.sort(Comparator.comparing(Finding::pointer).thenComparing(Finding::rule).thenComparing(Finding::message));
        return findings;
    }

    private static void checkRoot(Map<String, Object> document, List<Finding> findings) {
        Object version = document.get("openapi");
        if (version == null || !VERSION.matcher(String.valueOf(version)).matches()) {
            findings.add(new Finding("openapi-version", "/openapi",
                    "openapi must be 3.1.x or 3.2.x, got " + (version == null ? "nothing" : version)));
        }
        Object email = map(map(document.get("info")).get("contact")).get("email");
        if (email == null || String.valueOf(email).isBlank()) {
            findings.add(new Finding("info-contact", "/info", "info.contact.email is required"));
        }
    }

    private static void checkPath(Map<String, Object> document, String path, Map<String, Object> item,
            Set<String> operationIds, List<Finding> findings) {
        String pointer = "/paths/" + escape(path);
        if (path.length() > 1 && path.endsWith("/")) {
            findings.add(new Finding("path-kebab-case", pointer, "path ends with a slash"));
        }
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || segment.startsWith("{")) {
                continue;
            }
            if (!KEBAB.matcher(segment).matches()) {
                findings.add(new Finding("path-kebab-case", pointer, "segment " + segment + " is not lowercase kebab-case"));
            }
            Matcher word = FIRST_WORD.matcher(segment);
            if (word.find() && VERBS.contains(word.group())) {
                findings.add(new Finding("path-no-verbs", pointer, "segment " + segment + " starts with the verb "
                        + word.group() + ": let the HTTP method say what happens"));
            }
        }
        List<Object> pathParameters = list(item.get("parameters"));
        for (String method : METHODS) {
            if (item.containsKey(method)) {
                checkOperation(document, path, method, map(item.get(method)), pathParameters, operationIds, findings);
            }
        }
    }

    private static void checkOperation(Map<String, Object> document, String path, String method,
            Map<String, Object> operation, List<Object> pathParameters, Set<String> operationIds,
            List<Finding> findings) {
        String pointer = "/paths/" + escape(path) + "/" + method;

        Object operationId = operation.get("operationId");
        if (operationId == null) {
            findings.add(new Finding("operation-id", pointer, "missing operationId"));
        } else if (!operationIds.add(String.valueOf(operationId))) {
            findings.add(new Finding("operation-id", pointer, "duplicate operationId " + operationId));
        }

        List<Object> parameters = new ArrayList<>(pathParameters);
        parameters.addAll(list(operation.get("parameters")));
        Matcher template = TEMPLATE.matcher(path);
        while (template.find()) {
            String name = template.group(1);
            boolean declared = parameters.stream().map(Linter::map).anyMatch(parameter ->
                    name.equals(parameter.get("name")) && "path".equals(parameter.get("in"))
                            && Boolean.TRUE.equals(parameter.get("required")));
            if (!declared) {
                findings.add(new Finding("path-params-defined", pointer,
                        "path parameter " + name + " is not declared as a required path parameter"));
            }
        }

        if (method.equals("get") && operation.containsKey("requestBody")) {
            findings.add(new Finding("no-get-body", pointer + "/requestBody", "a GET must not have a request body"));
        }

        if (!operation.containsKey("security") && list(document.get("security")).isEmpty()) {
            findings.add(new Finding("security-defined", pointer,
                    "no security requirement: declare one, or security: [] for a public operation"));
        }

        Map<String, Object> responses = map(operation.get("responses"));
        if (responses.keySet().stream().noneMatch(code -> SUCCESS.matcher(code).matches())) {
            findings.add(new Finding("success-response", pointer + "/responses", "no 2xx response"));
        }
        responses.forEach((code, response) -> {
            boolean error = code.startsWith("4") || code.startsWith("5") || code.equals("default");
            Map<String, Object> content = map(map(response).get("content"));
            if (error && !content.isEmpty() && !content.containsKey(PROBLEM_JSON)) {
                findings.add(new Finding("problem-json", pointer + "/responses/" + escape(code),
                        "error responses must use " + PROBLEM_JSON));
            }
        });
    }

    private static void checkSchemas(Map<String, Object> schemas, List<Finding> findings) {
        schemas.forEach((name, schema) -> map(map(schema).get("properties")).keySet().forEach(property -> {
            if (!CAMEL.matcher(property).matches()) {
                findings.add(new Finding("schema-camel-case",
                        "/components/schemas/" + escape(name) + "/properties/" + escape(property),
                        "property " + property + " is not camelCase"));
            }
        }));
    }

    /** RFC 6901: "~" becomes "~0" and "/" becomes "~1". */
    static String escape(String token) {
        return token.replace("~", "~0").replace("/", "~1");
    }

    /** A YAML mapping with its keys as strings (YAML reads 200 as a number), or an empty one. */
    static Map<String, Object> map(Object value) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (value instanceof Map<?, ?> yaml) {
            yaml.forEach((key, entry) -> result.put(String.valueOf(key), entry));
        }
        return result;
    }

    static List<Object> list(Object value) {
        return value instanceof List<?> yaml ? new ArrayList<>(yaml) : List.of();
    }
}
