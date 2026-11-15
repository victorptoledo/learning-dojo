package dojo;

/**
 * The payment provider's SDK, version 2 from 2011. You cannot change it: it is a library.
 * Amounts are strings with two decimals ("12.50"), currencies are ISO codes, and the result
 * of a payment is a status code. After an accepted payment, {@link #lastReference()} gives
 * the provider's reference.
 */
public class LegacyPayClient {
    public static final int OK = 0;
    public static final int DECLINED = 1;
    public static final int BUSY = 2;

    private int counter;
    private String lastReference;

    public int submit(String account, String amount, String currencyCode) {
        counter++;
        lastReference = "LP-" + counter;
        return OK;
    }

    public String lastReference() {
        return lastReference;
    }
}
