package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LinterGradingTest {
    static final String CLEAN = """
            openapi: 3.2.0
            info:
              title: Orders
              version: 2.0.0
              contact: { name: Orders team, email: orders-team@example.com }
            security:
              - bearer: [orders:read]
            paths:
              /orders:
                get:
                  operationId: listOrders
                  parameters:
                    - { name: cursor, in: query, schema: { type: string } }
                  responses:
                    200: { description: A page of orders }
                    default:
                      description: An error
                      content:
                        application/problem+json: { schema: { type: object } }
                post:
                  operationId: placeOrder
                  requestBody:
                    content:
                      application/json: { schema: { $ref: '#/components/schemas/Order' } }
                  responses:
                    '201': { description: Placed }
                    '422':
                      description: Rejected
                      content:
                        application/problem+json: { schema: { type: object } }
              /orders/{orderId}/line-items/{lineId}:
                parameters:
                  - { name: orderId, in: path, required: true, schema: { type: string } }
                get:
                  operationId: getLineItem
                  parameters:
                    - { name: lineId, in: path, required: true, schema: { type: string } }
                  responses:
                    2XX: { description: The line item }
                    '404': { description: No such line item }
              /health:
                get:
                  operationId: health
                  security: []
                  responses:
                    '204': { description: Healthy }
            components:
              schemas:
                Order:
                  type: object
                  properties:
                    id: { type: string }
                    createdAt: { type: string, format: date-time }
                    totalCents: { type: integer }
            """;

    static List<Finding> lint(String yaml) {
        return Linter.lint(yaml);
    }

    static Finding finding(String rule, String pointer, String message) {
        return new Finding(rule, pointer, message);
    }

    @Test
    void a_document_that_follows_the_guide_has_no_findings() {
        assertEquals(List.of(), lint(CLEAN));
    }

    @Test
    void openapi_must_be_3_1_or_3_2() {
        assertEquals(List.of(finding("openapi-version", "/openapi", "openapi must be 3.1.x or 3.2.x, got 3.0.3")),
                lint(CLEAN.replace("openapi: 3.2.0", "openapi: 3.0.3")));
        assertEquals(List.of(), lint(CLEAN.replace("openapi: 3.2.0", "openapi: 3.1.1")));
    }

    @Test
    void the_owner_must_be_reachable() {
        assertEquals(List.of(finding("info-contact", "/info", "info.contact.email is required")),
                lint(CLEAN.replace("contact: { name: Orders team, email: orders-team@example.com }",
                        "contact: { name: Orders team }")));
    }

    @Test
    void paths_are_lowercase_kebab_case_without_a_trailing_slash() {
        assertEquals(List.of(
                        finding("path-kebab-case", "/paths/~1orders~1{orderId}~1lineItems~1{lineId}",
                                "segment lineItems is not lowercase kebab-case")),
                lint(CLEAN.replace("/line-items/", "/lineItems/")));
        assertEquals(List.of(finding("path-kebab-case", "/paths/~1health~1", "path ends with a slash")),
                lint(CLEAN.replace("/health:", "/health/:")));
    }

    @Test
    void paths_do_not_contain_verbs() {
        assertEquals(List.of(finding("path-no-verbs", "/paths/~1get-orders",
                        "segment get-orders starts with the verb get: let the HTTP method say what happens")),
                lint(CLEAN.replace("  /orders:", "  /get-orders:")));
    }

    @Test
    void every_template_variable_is_a_declared_required_path_parameter() {
        String missing = CLEAN.replace("    - { name: lineId, in: path, required: true, schema: { type: string } }\n",
                "    - { name: lineId, in: path, schema: { type: string } }\n");
        assertEquals(List.of(finding("path-params-defined", "/paths/~1orders~1{orderId}~1line-items~1{lineId}/get",
                        "path parameter lineId is not declared as a required path parameter")),
                lint(missing));
    }

    @Test
    void operation_ids_exist_and_are_unique() {
        assertEquals(List.of(finding("operation-id", "/paths/~1orders/post", "duplicate operationId listOrders")),
                lint(CLEAN.replace("operationId: placeOrder", "operationId: listOrders")));
        assertEquals(List.of(finding("operation-id", "/paths/~1health/get", "missing operationId")),
                lint(CLEAN.replace("operationId: health", "summary: Health check")));
    }

    @Test
    void every_operation_has_a_success_response() {
        assertEquals(List.of(finding("success-response", "/paths/~1health/get/responses", "no 2xx response")),
                lint(CLEAN.replace("'204': { description: Healthy }", "'503': { description: Unhealthy }")));
    }

    @Test
    void error_responses_with_a_body_are_problem_details() {
        assertEquals(List.of(finding("problem-json", "/paths/~1orders/post/responses/422",
                        "error responses must use application/problem+json")),
                lint(CLEAN.replace("description: Rejected\n          content:\n            application/problem+json:",
                        "description: Rejected\n          content:\n            application/json:")));
    }

    @Test
    void a_get_has_no_request_body() {
        String withBody = CLEAN.replace("""
                      operationId: health
                """, """
                      operationId: health
                      requestBody:
                        content:
                          application/json: { schema: { type: object } }
                """);
        assertEquals(List.of(finding("no-get-body", "/paths/~1health/get/requestBody",
                        "a GET must not have a request body")),
                lint(withBody));
    }

    @Test
    void operations_need_security_unless_they_say_they_are_public() {
        String noRootSecurity = CLEAN.replace("""
                security:
                  - bearer: [orders:read]
                """, "");
        String message = "no security requirement: declare one, or security: [] for a public operation";
        assertEquals(List.of(
                        finding("security-defined", "/paths/~1orders/get", message),
                        finding("security-defined", "/paths/~1orders/post", message),
                        finding("security-defined", "/paths/~1orders~1{orderId}~1line-items~1{lineId}/get", message)),
                lint(noRootSecurity));
    }

    @Test
    void schema_properties_are_camel_case() {
        assertEquals(List.of(finding("schema-camel-case", "/components/schemas/Order/properties/created_at",
                        "property created_at is not camelCase")),
                lint(CLEAN.replace("createdAt:", "created_at:")));
    }

    @Test
    void a_legacy_document_gets_every_finding_sorted_by_pointer_then_rule() {
        String legacy = """
                openapi: 3.0.3
                info:
                  title: Legacy orders
                  version: '1'
                paths:
                  /getOrders:
                    post:
                      operationId: listOrders
                      responses:
                        '200': { description: ok }
                  /orders/{id}/:
                    get:
                      operationId: listOrders
                      requestBody:
                        content:
                          application/json: { schema: { type: object } }
                      responses:
                        404:
                          description: missing
                          content:
                            application/json: { schema: { type: object } }
                  /order_items:
                    get:
                      operationId: listItems
                      security: []
                      responses:
                        '204': { description: none }
                components:
                  schemas:
                    Order:
                      properties:
                        id: { type: string }
                        created_at: { type: string }
                        TotalCents: { type: integer }
                """;
        String noSecurity = "no security requirement: declare one, or security: [] for a public operation";
        String order = "/paths/~1orders~1{id}~1";
        assertEquals(List.of(
                finding("schema-camel-case", "/components/schemas/Order/properties/TotalCents",
                        "property TotalCents is not camelCase"),
                finding("schema-camel-case", "/components/schemas/Order/properties/created_at",
                        "property created_at is not camelCase"),
                finding("info-contact", "/info", "info.contact.email is required"),
                finding("openapi-version", "/openapi", "openapi must be 3.1.x or 3.2.x, got 3.0.3"),
                finding("path-kebab-case", "/paths/~1getOrders", "segment getOrders is not lowercase kebab-case"),
                finding("path-no-verbs", "/paths/~1getOrders",
                        "segment getOrders starts with the verb get: let the HTTP method say what happens"),
                finding("security-defined", "/paths/~1getOrders/post", noSecurity),
                finding("path-kebab-case", "/paths/~1order_items", "segment order_items is not lowercase kebab-case"),
                finding("path-kebab-case", order, "path ends with a slash"),
                finding("operation-id", order + "/get", "duplicate operationId listOrders"),
                finding("path-params-defined", order + "/get",
                        "path parameter id is not declared as a required path parameter"),
                finding("security-defined", order + "/get", noSecurity),
                finding("no-get-body", order + "/get/requestBody", "a GET must not have a request body"),
                finding("success-response", order + "/get/responses", "no 2xx response"),
                finding("problem-json", order + "/get/responses/404", "error responses must use application/problem+json")),
                lint(legacy));
    }
}
