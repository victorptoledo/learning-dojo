package dojo.shop.pricing.migration;

import dojo.shop.pricing.api.Category;
import dojo.shop.pricing.api.PriceRequest;
import dojo.shop.pricing.api.Pricing;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The strangler: routes each category to the legacy engine, to a shadow run of both, or to the
 * modern implementation, and lets a category cut over only after a clean shadow run.
 */
public final class StranglerPricing implements Pricing {
    static final int CLEAN_RUNS_BEFORE_CUTOVER = 3;

    private final Pricing legacy;
    private final Pricing modern;
    private final Map<Category, Mode> modes = new EnumMap<>(Category.class);
    private final Map<Category, ShadowRun> runs = new EnumMap<>(Category.class);
    private final List<Mismatch> mismatches = new ArrayList<>();

    public StranglerPricing(Pricing legacy, Pricing modern) {
        this.legacy = legacy;
        this.modern = modern;
    }

    public Mode mode(Category category) {
        return modes.getOrDefault(category, Mode.LEGACY);
    }

    public void route(Category category, Mode mode) {
        if (mode == Mode.MODERN && !readyToCutOver(category)) {
            throw new IllegalStateException(category + " needs a clean shadow run first");
        }
        if (mode == Mode.SHADOW && mode(category) != Mode.SHADOW) {
            runs.put(category, new ShadowRun());
        }
        modes.put(category, mode);
    }

    public List<Mismatch> mismatches() {
        return List.copyOf(mismatches);
    }

    @Override
    public long priceCents(PriceRequest request) {
        return switch (mode(request.category())) {
            case LEGACY -> legacy.priceCents(request);
            case SHADOW -> shadow(request);
            case MODERN -> modern.priceCents(request);
        };
    }

    private long shadow(PriceRequest request) {
        long expected = legacy.priceCents(request);
        String actual = modernResult(request);
        ShadowRun run = runs.get(request.category());
        run.compared++;
        if (!actual.equals(Long.toString(expected))) {
            run.clean = false;
            mismatches.add(new Mismatch(request, expected, actual));
        }
        return expected;
    }

    /** The modern answer as text; a modern failure must never fail the customer's request. */
    private String modernResult(PriceRequest request) {
        try {
            return Long.toString(modern.priceCents(request));
        } catch (RuntimeException e) {
            return "error: " + e.getMessage();
        }
    }

    private boolean readyToCutOver(Category category) {
        ShadowRun run = runs.get(category);
        return mode(category) == Mode.SHADOW && run.clean && run.compared >= CLEAN_RUNS_BEFORE_CUTOVER;
    }

    private static final class ShadowRun {
        private int compared;
        private boolean clean = true;
    }
}
