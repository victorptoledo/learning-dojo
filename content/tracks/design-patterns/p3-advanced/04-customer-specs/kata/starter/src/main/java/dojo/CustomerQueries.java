package dojo;

/**
 * The same questions for the production database, as SQL. The conditions are written a second
 * time, and values are glued into the text: a country called "x' OR '1'='1" reads every row.
 */
public final class CustomerQueries {
    private CustomerQueries() {}

    public static String activeInCountry(String country) {
        return "SELECT * FROM customers WHERE active = TRUE AND country = '" + country + "'";
    }

    public static String loyalInCountry(String country, int minOrders) {
        return "SELECT * FROM customers WHERE active = TRUE AND country = '" + country + "' AND orders >= " + minOrders;
    }

    public static String dormant() {
        return "SELECT * FROM customers WHERE active = FALSE OR orders < 1";
    }
}
