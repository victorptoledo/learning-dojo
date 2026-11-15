package dojo;

/** The old API, kept for its callers: builds a request and wires the audit log and the sales mail as listeners. */
public class QuoteService {
    private final QuoteEngine engine = new QuoteEngine(PricingSteps.standard());

    public QuoteService(AuditLog audit, SalesTeamMail mail) {
        engine.subscribe((request, quote) -> audit.record(
                request.customer() + " " + request.product() + " x" + request.quantity() + " = " + quote.cents()));
        engine.subscribe((request, quote) -> {
            if (quote.cents() > 100_000) {
                mail.send("Big quote for " + request.customer() + ": " + quote.cents());
            }
        });
    }

    public Quote quote(String customer, String product, int quantity, long unitCents,
                       boolean vip, String coupon, boolean express) {
        return engine.quote(QuoteRequest.builder()
                .customer(customer).product(product).quantity(quantity).unitCents(unitCents)
                .vip(vip).coupon(coupon).express(express)
                .build());
    }
}
