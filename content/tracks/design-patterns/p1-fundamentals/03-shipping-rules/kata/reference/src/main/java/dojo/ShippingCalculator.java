package dojo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Looks up the rule for a method and applies it. New methods arrive as data, not as branches. */
public class ShippingCalculator {
    private final Map<String, ShippingRule> rules;

    public ShippingCalculator() {
        this(ShippingRules.builtIn());
    }

    public ShippingCalculator(Map<String, ShippingRule> rules) {
        this.rules = Map.copyOf(rules);
    }

    public long quoteCents(String method, Parcel parcel) {
        ShippingRule rule = method == null ? null : rules.get(method);
        if (rule == null) {
            throw new IllegalArgumentException("unknown shipping method: " + method);
        }
        return rule.quoteCents(parcel);
    }

    public Set<String> methods() {
        return new TreeSet<>(rules.keySet());
    }

    /** A new calculator that also knows {@code method}; this one is unchanged. */
    public ShippingCalculator with(String method, ShippingRule rule) {
        Map<String, ShippingRule> more = new HashMap<>(rules);
        more.put(method, rule);
        return new ShippingCalculator(more);
    }

    /** The method with the lowest price for the parcel, skipping methods that refuse it. */
    public String cheapest(Parcel parcel) {
        return rules.entrySet().stream()
                .filter(entry -> accepts(entry.getValue(), parcel))
                .min(Comparator.comparingLong((Map.Entry<String, ShippingRule> entry) ->
                                entry.getValue().quoteCents(parcel))
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalArgumentException("no method can ship this parcel"));
    }

    private static boolean accepts(ShippingRule rule, Parcel parcel) {
        try {
            rule.quoteCents(parcel);
            return true;
        } catch (IllegalArgumentException refused) {
            return false;
        }
    }
}
