package dojo.rentals.billing.api;

import java.util.List;

/** What billing publishes for other contexts. */
public interface Billing {
    ChargeReceipt charge(ChargeRequest request);

    List<ChargeReceipt> charges();
}
