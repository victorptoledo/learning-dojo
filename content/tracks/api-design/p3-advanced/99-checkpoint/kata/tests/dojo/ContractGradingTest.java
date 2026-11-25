package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/** The OpenAPI contract in src/main/resources/openapi.yaml describes what the API does. */
class ContractGradingTest {
    static final Path CONTRACT = Path.of("src", "main", "resources", "openapi.yaml");

    final Map<String, Object> root = load();

    @SuppressWarnings("unchecked")
    static Map<String, Object> load() {
        try {
            return (Map<String, Object>) new Yaml().load(Files.readString(CONTRACT));
        } catch (IOException e) {
            return fail("cannot read " + CONTRACT);
        }
    }

    /** The node itself, or what its local {@code $ref} points at. */
    Object deref(Object node) {
        for (int hops = 0; node instanceof Map<?, ?> map && map.get("$ref") instanceof String ref; hops++) {
            if (!ref.startsWith("#/") || hops > 10) {
                return fail("only local references are expected: " + ref);
            }
            Object target = root;
            for (String token : ref.substring(2).split("/")) {
                String key = token.replace("~1", "/").replace("~0", "~");
                if (!(target instanceof Map<?, ?> parent) || !parent.containsKey(key)) {
                    return fail("the reference " + ref + " points at nothing");
                }
                target = parent.get(key);
            }
            node = target;
        }
        return node;
    }

    Map<?, ?> map(Object node, String what) {
        Object value = deref(node);
        if (!(value instanceof Map<?, ?> map)) {
            return fail(what + " is missing");
        }
        return map;
    }

    Map<?, ?> at(Map<?, ?> parent, String key, String what) {
        return map(parent.get(key), what);
    }

    Map<?, ?> operation(String path, String method) {
        Map<?, ?> paths = at(root, "paths", "paths");
        return at(at(paths, path, path), method, method.toUpperCase() + " " + path);
    }

    void assertScope(String path, String method, String scope) {
        String where = method.toUpperCase() + " " + path;
        Object security = operation(path, method).get("security");
        assertInstanceOf(List.class, security, where + " must declare its own security requirement");
        boolean found = ((List<?>) security).stream()
                .map(requirement -> map(requirement, where + " security requirement"))
                .anyMatch(requirement -> requirement.get("partnerKey") instanceof List<?> scopes && scopes.contains(scope));
        assertTrue(found, where + " must require partnerKey with the scope " + scope);
    }

    void assertProblemResponses(String path, String method) {
        String where = method.toUpperCase() + " " + path;
        Map<?, ?> responses = at(operation(path, method), "responses", where + " responses");
        for (String code : List.of("401", "403", "429")) {
            Map<?, ?> response = at(responses, code, where + " response " + code);
            Map<?, ?> content = at(response, "content", where + " response " + code + " content");
            assertTrue(content.containsKey("application/problem+json"),
                    where + " response " + code + " must be application/problem+json");
        }
        Map<?, ?> headers = at(at(responses, "429", where + " 429"), "headers", where + " 429 headers");
        for (String name : List.of("Retry-After", "RateLimit", "RateLimit-Policy")) {
            assertTrue(headers.containsKey(name), where + " 429 must declare the header " + name);
        }
    }

    @Test
    void the_contract_keeps_what_was_reviewed() {
        assertTrue(String.valueOf(root.get("openapi")).startsWith("3.1"), "openapi 3.1.x");
        Map<?, ?> created = at(at(operation("/exports", "post"), "responses", "POST responses"), "202", "202");
        assertTrue(at(created, "headers", "202 headers").containsKey("Location"), "202 keeps its Location");
        at(at(operation("/exports/{exportId}", "get"), "responses", "GET responses"), "404", "404");
        Map<?, ?> schemes = at(at(root, "components", "components"), "securitySchemes", "securitySchemes");
        Map<?, ?> partnerKey = at(schemes, "partnerKey", "the partnerKey security scheme");
        assertEquals("bearer", String.valueOf(partnerKey.get("scheme")).toLowerCase());
    }

    @Test
    void each_operation_requires_its_scope() {
        assertScope("/exports", "post", "exports:write");
        assertScope("/exports/{exportId}", "get", "exports:read");
    }

    @Test
    void each_operation_documents_401_403_and_429_as_problems() {
        assertProblemResponses("/exports", "post");
        assertProblemResponses("/exports/{exportId}", "get");
    }

    @Test
    void the_202_tells_clients_when_to_poll() {
        Map<?, ?> created = at(at(operation("/exports", "post"), "responses", "POST responses"), "202", "202");
        assertTrue(at(created, "headers", "202 headers").containsKey("Retry-After"),
                "the 202 must declare the Retry-After header");
    }

    @Test
    void the_webhook_is_part_of_the_contract() {
        Map<?, ?> webhook = at(at(at(root, "webhooks", "the webhooks section"), "exportFinished",
                "the exportFinished webhook"), "post", "the exportFinished webhook's post");
        Object parameters = webhook.get("parameters");
        assertInstanceOf(List.class, parameters, "the webhook must declare its header parameters");
        for (String name : List.of("webhook-id", "webhook-timestamp", "webhook-signature")) {
            boolean declared = ((List<?>) parameters).stream()
                    .map(parameter -> map(parameter, "a webhook parameter"))
                    .anyMatch(parameter -> name.equalsIgnoreCase(String.valueOf(parameter.get("name")))
                            && "header".equals(parameter.get("in"))
                            && Boolean.TRUE.equals(parameter.get("required")));
            assertTrue(declared, "the webhook must declare the required header " + name);
        }
        Map<?, ?> content = at(at(webhook, "requestBody", "the webhook requestBody"), "content", "its content");
        assertTrue(content.containsKey("application/json"), "the webhook body is application/json");
    }
}
