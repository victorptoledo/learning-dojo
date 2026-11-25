package dojo.legacy.pricing;

/**
 * The pricing engine from 2009. A library: do not change it. Every rounding step is integer
 * division, and the shop's invoices depend on it.
 */
public final class PriceEngine {
    /**
     * @param cat      "BK" books, "EL" electronics, "GR" grocery
     * @param unit     unit price in cents
     * @param qty      quantity
     * @param custType "M" member, "G" guest
     */
    public long calc(String cat, long unit, int qty, String custType) {
        if (qty < 1 || unit < 0) {
            throw new IllegalArgumentException("bad line");
        }
        if (!custType.equals("M") && !custType.equals("G")) {
            throw new IllegalArgumentException("bad customer type " + custType);
        }
        long t;
        switch (cat) {
            case "BK":
                t = unit * qty;
                if (custType.equals("M")) {
                    t = t * 95 / 100;
                }
                if (qty >= 10) {
                    t = t * 90 / 100;
                }
                return t;
            case "EL":
                t = unit * qty;
                if (qty >= 3) {
                    t = t * 97 / 100;
                }
                return t + 150L * qty;
            case "GR":
                t = unit * qty;
                if (t < 1000) {
                    t = t + 300;
                }
                if (custType.equals("M")) {
                    t = t * 90 / 100;
                }
                return t;
            default:
                throw new IllegalArgumentException("unknown category " + cat);
        }
    }
}
