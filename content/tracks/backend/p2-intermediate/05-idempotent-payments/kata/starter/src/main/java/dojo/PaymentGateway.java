package dojo;

/** The card processor. Every call to charge moves real money. */
public interface PaymentGateway {
    /** Charges the account and returns the charge id, or throws DeclinedException. */
    String charge(String account, long amountCents);
}
