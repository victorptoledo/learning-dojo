package dojo.rentals.billing.internal;

import dojo.rentals.billing.api.Billing;
import dojo.rentals.billing.api.ChargeReceipt;
import dojo.rentals.billing.api.ChargeRequest;
import java.util.List;

/** Billing's implementation of its api, over its own ledger. */
public class BillingService implements Billing {
    private final Ledger ledger = new Ledger();

    @Override
    public ChargeReceipt charge(ChargeRequest request) {
        String id = ledger.post(request.customerId(), request.reference(), request.amountCents());
        return new ChargeReceipt(id, request.customerId(), request.reference(), request.amountCents());
    }

    @Override
    public List<ChargeReceipt> charges() {
        return ledger.entries().stream()
                .map(entry -> new ChargeReceipt(entry.id(), entry.customerId(), entry.reference(), entry.amountCents()))
                .toList();
    }
}
