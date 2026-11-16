package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LinterTest {
    static final String CLEAN = """
            openapi: 3.1.1
            info:
              title: Orders
              version: 1.4.0
              contact: { email: orders-team@example.com }
            security:
              - bearer: [orders:read]
            paths:
              /orders/{orderId}:
                get:
                  operationId: getOrder
                  parameters:
                    - { name: orderId, in: path, required: true, schema: { type: string } }
                  responses:
                    '200': { description: The order }
                    '404':
                      description: No such order
                      content:
                        application/problem+json: { schema: { type: object } }
            """;

    @Test
    void a_document_that_follows_the_guide_has_no_findings() {
        assertEquals(List.of(), Linter.lint(CLEAN));
    }

    @Test
    void an_operation_without_an_operation_id_is_reported() {
        List<Finding> findings = Linter.lint(CLEAN.replace("operationId: getOrder", "summary: Get one order"));
        assertEquals(List.of(new Finding("operation-id", "/paths/~1orders~1{orderId}/get", "missing operationId")),
                findings);
    }
}
