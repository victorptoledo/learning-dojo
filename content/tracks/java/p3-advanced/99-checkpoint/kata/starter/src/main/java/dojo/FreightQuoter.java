package dojo;

import java.time.Duration;
import java.util.List;

/** Asks every carrier for a quote at the same time and picks the best one. */
public final class FreightQuoter {
    /** The order being quoted, bound while a carrier is called, so the carrier can log it. */
    public static final ScopedValue<String> ORDER_ID = ScopedValue.newInstance();

    public FreightQuoter(List<Carrier> carriers, int maxCallsPerCarrier, Duration timeout) {
        // TODO: validate, and keep what each call needs
    }

    /** One result per carrier, in carrier order, all asked at the same time within one deadline. */
    public QuoteBoard quote(Shipment shipment, Choice choice) {
        throw new UnsupportedOperationException("write FreightQuoter.quote");
    }

    /** Quotes every shipment at the same time; one board per shipment, in order. */
    public List<QuoteBoard> quoteAll(List<Shipment> shipments, Choice choice) {
        throw new UnsupportedOperationException("write FreightQuoter.quoteAll");
    }

    public Stats stats() {
        throw new UnsupportedOperationException("write FreightQuoter.stats");
    }

    /** "2 quoted, 1 failed, 1 timed out; best: Rapido at 1250 cents in 2 days". */
    public static String summary(QuoteBoard board) {
        throw new UnsupportedOperationException("write FreightQuoter.summary");
    }
}
