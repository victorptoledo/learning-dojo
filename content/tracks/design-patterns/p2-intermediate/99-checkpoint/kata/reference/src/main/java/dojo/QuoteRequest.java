package dojo;

/** What is being quoted. Built with {@link #builder()}, so no argument can be passed in the wrong slot. */
public record QuoteRequest(String customer, String product, int quantity, long unitCents,
                           boolean vip, String coupon, boolean express) {
    public QuoteRequest {
        if (customer == null || customer.isBlank()) throw new IllegalArgumentException("customer is required");
        if (product == null || product.isBlank()) throw new IllegalArgumentException("product is required");
        if (quantity < 1) throw new IllegalArgumentException("quantity must be at least 1");
        if (unitCents < 0) throw new IllegalArgumentException("unit price cannot be negative");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String customer;
        private String product;
        private int quantity = 1;
        private long unitCents;
        private boolean vip;
        private String coupon;
        private boolean express;

        private Builder() {}

        public Builder customer(String customer) {
            this.customer = customer;
            return this;
        }

        public Builder product(String product) {
            this.product = product;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder unitCents(long unitCents) {
            this.unitCents = unitCents;
            return this;
        }

        public Builder vip(boolean vip) {
            this.vip = vip;
            return this;
        }

        public Builder coupon(String coupon) {
            this.coupon = coupon;
            return this;
        }

        public Builder express(boolean express) {
            this.express = express;
            return this;
        }

        public QuoteRequest build() {
            return new QuoteRequest(customer, product, quantity, unitCents, vip, coupon, express);
        }
    }
}
