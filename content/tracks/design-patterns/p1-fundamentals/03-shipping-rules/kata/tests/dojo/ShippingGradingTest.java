package dojo;

import static dojo.GradingSupport.code;
import static dojo.GradingSupport.instanceFieldTypes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShippingGradingTest {
    private static final Parcel LIGHT_PT = new Parcel(1500, "PT", false);
    private static final Parcel HEAVY_ES = new Parcel(3200, "ES", true);
    private static final Parcel FREIGHT = new Parcel(45_000, "DE", false);

    @Test
    void prices_do_not_change() {
        var calculator = new ShippingCalculator();
        assertEquals(499, calculator.quoteCents("standard", LIGHT_PT));
        assertEquals(499 + 300 + 700 + 300, calculator.quoteCents("standard", HEAVY_ES));
        assertEquals(499, calculator.quoteCents("standard", new Parcel(2000, "PT", false)));
        assertEquals(499 + 150, calculator.quoteCents("standard", new Parcel(2001, "PT", false)));
        assertEquals((1299 + 4 * 250) * 2 + 300, calculator.quoteCents("express", HEAVY_ES));
        assertEquals(199, calculator.quoteCents("pickup", LIGHT_PT));
        assertEquals(4500 + 45 * 80, calculator.quoteCents("freight", FREIGHT));
        assertThrows(IllegalArgumentException.class, () -> calculator.quoteCents("pickup", HEAVY_ES));
        assertThrows(IllegalArgumentException.class, () -> calculator.quoteCents("freight", LIGHT_PT));
        var unknown = assertThrows(IllegalArgumentException.class, () -> calculator.quoteCents(null, LIGHT_PT));
        assertEquals("unknown shipping method: null", unknown.getMessage());
    }

    @Test
    void every_built_in_rule_works_on_its_own() {
        assertEquals(499 + 700, ShippingRules.standard().quoteCents(new Parcel(500, "FR", false)));
        assertEquals(1299 + 250, ShippingRules.express().quoteCents(new Parcel(1000, "PT", false)));
        assertEquals(199, ShippingRules.pickup().quoteCents(LIGHT_PT));
        assertEquals(4500 + 30 * 80, ShippingRules.freight().quoteCents(new Parcel(30_000, "PT", false)));
        assertEquals(Set.of("standard", "express", "pickup", "freight"), ShippingRules.builtIn().keySet());
    }

    @Test
    void shipping_rule_is_a_functional_interface() {
        assertTrue(ShippingRule.class.isInterface(), "ShippingRule must be an interface");
        List<Method> abstractMethods = Arrays.stream(ShippingRule.class.getMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .toList();
        assertEquals(1, abstractMethods.size(), "ShippingRule should have exactly one abstract method");
        assertEquals("quoteCents", abstractMethods.getFirst().getName());
    }

    @Test
    void a_new_method_is_added_without_touching_the_calculator() {
        ShippingRule drone = parcel -> parcel.grams() <= 2000 ? 999 : Long.MAX_VALUE;
        var calculator = new ShippingCalculator(Map.of("drone", drone, "standard", ShippingRules.standard()));
        assertEquals(999, calculator.quoteCents("drone", LIGHT_PT));
        assertEquals(Set.of("drone", "standard"), calculator.methods());
        assertThrows(IllegalArgumentException.class, () -> calculator.quoteCents("express", LIGHT_PT));
    }

    @Test
    void with_returns_a_new_calculator_and_leaves_the_old_one_alone() {
        var base = new ShippingCalculator();
        var more = base.with("bike", parcel -> 350);
        assertEquals(350, more.quoteCents("bike", LIGHT_PT));
        assertEquals(List.of("bike", "express", "freight", "pickup", "standard"), List.copyOf(more.methods()));
        assertFalse(base.methods().contains("bike"));
        assertThrows(IllegalArgumentException.class, () -> base.quoteCents("bike", LIGHT_PT));
    }

    @Test
    void cheapest_skips_methods_that_refuse_the_parcel() {
        var calculator = new ShippingCalculator();
        assertEquals("pickup", calculator.cheapest(LIGHT_PT));
        assertEquals("standard", calculator.cheapest(HEAVY_ES));
        assertEquals("standard", calculator.cheapest(FREIGHT));
        var picky = new ShippingCalculator(Map.of("pickup", ShippingRules.pickup()));
        assertThrows(IllegalArgumentException.class, () -> picky.cheapest(HEAVY_ES));
        var tie = new ShippingCalculator(Map.of("b", parcel -> 100, "a", parcel -> 100));
        assertEquals("a", tie.cheapest(LIGHT_PT), "ties go to the method name that sorts first");
    }

    @Test
    void the_calculator_has_no_branch_per_method() {
        String code = code("dojo.ShippingCalculator");
        assertFalse(code.contains(".equals("), "ShippingCalculator should look rules up, not compare method names");
        assertFalse(code.contains("switch"), "ShippingCalculator should look rules up, not switch on method names");
        assertFalse(code.contains("instanceof"), "no instanceof in ShippingCalculator");
        assertTrue(instanceFieldTypes(ShippingCalculator.class).stream().allMatch(Map.class::equals),
                "ShippingCalculator should hold only its map of rules");
    }
}
