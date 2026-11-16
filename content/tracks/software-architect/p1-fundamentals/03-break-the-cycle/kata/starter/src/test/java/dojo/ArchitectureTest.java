package dojo;

import static org.junit.jupiter.api.Assertions.assertFalse;

import dojo.fitness.ClassGraph;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    @Test
    void billingDoesNotUseOrders() {
        Map<String, Set<String>> packages = ClassGraph.packageDependencies(ClassGraph.scan("dojo.shop"));
        System.out.println(packages);
        assertFalse(packages.get("dojo.shop.billing").contains("dojo.shop.orders"),
                "billing still uses orders: " + packages);
    }
}
