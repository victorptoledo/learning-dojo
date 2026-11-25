package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.fitness.ArchitectureRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class FitnessGradingTest {
    private static final Map<String, Set<String>> SHOP = GradingGraph.scan("dojo.shop");

    private static List<String> uses(Predicate<String> from, Predicate<String> forbidden) {
        return SHOP.entrySet().stream()
                .filter(entry -> from.test(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream().filter(forbidden).map(to -> entry.getKey() + " -> " + to))
                .toList();
    }

    private static Set<String> baseline() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("src", "test", "resources", "fitness-baseline.txt"));
        return new TreeSet<>(lines.stream().map(String::strip).filter(line -> !line.isEmpty()).toList());
    }

    // ---- the shop's graph, checked independently of your rules -------------------------------

    @Test
    void checkoutKnowsOnlyThePricingPort() {
        assertTrue(SHOP.containsKey("dojo.shop.checkout.Checkout"), "the scan found no shop classes");
        assertEquals(List.of(), uses(name -> name.startsWith("dojo.shop.checkout."),
                to -> to.startsWith("dojo.legacy.")
                        || to.startsWith("dojo.shop.pricing.") && !to.startsWith("dojo.shop.pricing.api.")));
    }

    @Test
    void theModernPricingKnowsNeitherTheLegacyNorTheStrangler() {
        assertTrue(SHOP.containsKey("dojo.shop.pricing.modern.ModernPricing"), "expected dojo.shop.pricing.modern.ModernPricing");
        assertEquals(List.of(), uses(name -> name.startsWith("dojo.shop.pricing.modern."),
                to -> to.startsWith("dojo.legacy.") || to.startsWith("dojo.shop.pricing.legacy.")
                        || to.startsWith("dojo.shop.pricing.migration.")));
    }

    @Test
    void theStranglerAndThePortDoNotKnowTheEngines() {
        assertEquals(List.of(), uses(
                name -> name.startsWith("dojo.shop.pricing.migration.") || name.startsWith("dojo.shop.pricing.api."),
                to -> to.startsWith("dojo.legacy.") || to.startsWith("dojo.shop.pricing.legacy.")
                        || to.startsWith("dojo.shop.pricing.modern.")));
    }

    // ---- your rules -------------------------------------------------------------------------

    @Test
    void theLegacyRuleAllowsOnlyTheAdapterAndTheRoot() {
        Map<String, Set<String>> graph = Map.of(
                "dojo.shop.pricing.legacy.LegacyPricing", Set.of("dojo.legacy.pricing.PriceEngine", "java.lang.String"),
                "dojo.shop.App", Set.of("dojo.legacy.pricing.PriceEngine"),
                "dojo.shop.Wiring", Set.of("dojo.legacy.pricing.PriceEngine"),
                "dojo.shop.Shop", Set.of("dojo.shop.pricing.migration.StranglerPricing"),
                "dojo.shop.Apps.Loader", Set.of("dojo.legacy.pricing.PriceEngine"),
                "dojo.shop.cart.Cart", Set.of("dojo.legacy.pricing.PriceEngine", "dojo.legacy.pricing.Codes"),
                "dojo.shop.app.Main", Set.of("dojo.legacy.pricing.PriceEngine"));
        assertEquals(new TreeSet<>(List.of(
                "legacy: dojo.shop.Apps.Loader -> dojo.legacy.pricing.PriceEngine",
                "legacy: dojo.shop.app.Main -> dojo.legacy.pricing.PriceEngine",
                "legacy: dojo.shop.cart.Cart -> dojo.legacy.pricing.Codes",
                "legacy: dojo.shop.cart.Cart -> dojo.legacy.pricing.PriceEngine")),
                ArchitectureRules.violations(graph),
                "only the classes of dojo.shop itself are the root; dojo.shop.app is not");
    }

    @Test
    void theModernRuleKeepsTheNewCodeClean() {
        Map<String, Set<String>> graph = Map.of(
                "dojo.shop.pricing.modern.Books", Set.of("dojo.shop.pricing.legacy.LegacyPricing", "dojo.shop.pricing.api.Pricing"),
                "dojo.shop.pricing.modern.Rules", Set.of("dojo.shop.pricing.migration.Mode"),
                "dojo.shop.pricing.migration.StranglerPricing", Set.of("dojo.shop.pricing.api.Pricing"));
        assertEquals(new TreeSet<>(List.of(
                "modern: dojo.shop.pricing.modern.Books -> dojo.shop.pricing.legacy.LegacyPricing",
                "modern: dojo.shop.pricing.modern.Rules -> dojo.shop.pricing.migration.Mode")),
                ArchitectureRules.violations(graph));
    }

    @Test
    void theApiRuleKeepsCallersOnThePort() {
        Map<String, Set<String>> graph = Map.of(
                "dojo.shop.checkout.Checkout", Set.of("dojo.shop.pricing.api.Pricing", "dojo.shop.pricing.modern.ModernPricing"),
                "dojo.shop.orders.Orders", Set.of("dojo.shop.pricing.migration.StranglerPricing"),
                "dojo.shop.pricing.migration.StranglerPricing", Set.of("dojo.shop.pricing.api.Pricing"),
                "dojo.shop.App", Set.of("dojo.shop.pricing.modern.ModernPricing"));
        assertEquals(new TreeSet<>(List.of(
                "api: dojo.shop.checkout.Checkout -> dojo.shop.pricing.modern.ModernPricing",
                "api: dojo.shop.orders.Orders -> dojo.shop.pricing.migration.StranglerPricing")),
                ArchitectureRules.violations(graph));
    }

    @Test
    void aCleanGraphHasNoViolations() {
        assertEquals(Set.of(), ArchitectureRules.violations(Map.of(
                "dojo.shop.checkout.Checkout", Set.of("dojo.shop.pricing.api.Pricing", "java.util.List"))));
    }

    // ---- the ratchet ------------------------------------------------------------------------

    @Test
    void theBaselineMatchesTheShopExactly() throws IOException {
        Set<String> current = ArchitectureRules.violations(SHOP);
        Set<String> added = new TreeSet<>(current);
        added.removeAll(baseline());
        Set<String> fixed = new TreeSet<>(baseline());
        fixed.removeAll(current);
        assertEquals(Set.of(), added, "new violations");
        assertEquals(Set.of(), fixed, "fixed violations still in fitness-baseline.txt");
    }

    @Test
    void checkoutIsNoLongerInTheBaseline() throws IOException {
        assertEquals(List.of(), baseline().stream().filter(line -> line.contains("dojo.shop.checkout.")).toList());
    }
}
