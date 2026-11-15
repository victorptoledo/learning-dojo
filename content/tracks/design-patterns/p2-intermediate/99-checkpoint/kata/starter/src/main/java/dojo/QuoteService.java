package dojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Prices quotes. Seven positional parameters (two of them booleans that are easy to swap), the
 * pricing rules in one method in a fixed order, and the audit log and the sales mail called
 * directly at the end: a new rule or a new party interested in quotes means editing this class.
 */
public class QuoteService {
    private final AuditLog audit;
    private final SalesTeamMail mail;

    public QuoteService(AuditLog audit, SalesTeamMail mail) {
        this.audit = audit;
        this.mail = mail;
    }

    public Quote quote(String customer, String product, int quantity, long unitCents,
                       boolean vip, String coupon, boolean express) {
        if (customer == null || customer.isBlank()) throw new IllegalArgumentException("customer is required");
        if (product == null || product.isBlank()) throw new IllegalArgumentException("product is required");
        if (quantity < 1) throw new IllegalArgumentException("quantity must be at least 1");
        if (unitCents < 0) throw new IllegalArgumentException("unit price cannot be negative");

        long cents = quantity * unitCents;
        List<String> adjustments = new ArrayList<>();
        if (quantity >= 10) {
            cents -= cents * 5 / 100;
            adjustments.add("bulk -5%");
        }
        if (vip) {
            cents -= cents * 10 / 100;
            adjustments.add("vip -10%");
        }
        if ("SAVE5".equals(coupon)) {
            cents = Math.max(0, cents - 500);
            adjustments.add("coupon SAVE5");
        }
        if (express) {
            cents += 1500;
            adjustments.add("express +15.00");
        }
        cents += (cents * 23 + 50) / 100;
        adjustments.add("tax 23%");

        Quote quote = new Quote(customer, cents, adjustments);
        audit.record(customer + " " + product + " x" + quantity + " = " + cents);
        if (cents > 100_000) {
            mail.send("Big quote for " + customer + ": " + cents);
        }
        return quote;
    }
}
