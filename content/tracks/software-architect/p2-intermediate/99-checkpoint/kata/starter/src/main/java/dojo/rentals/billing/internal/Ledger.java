package dojo.rentals.billing.internal;

import java.util.ArrayList;
import java.util.List;

/** Billing's book of charges. Do not change it. */
public class Ledger {
    /** One posted line. */
    public record Entry(String id, String customerId, String reference, long amountCents) {}

    private final List<Entry> entries = new ArrayList<>();

    public String post(String customerId, String reference, long amountCents) {
        String id = "CH-" + (entries.size() + 1);
        entries.add(new Entry(id, customerId, reference, amountCents));
        return id;
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }
}
