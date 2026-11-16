package dojo.shop.sales;

/** A product as sales sees it: something with a name and a price. */
public record Product(String sku, String name, long priceCents, boolean discontinued) {
    public Product withPrice(long cents) {
        return new Product(sku, name, cents, discontinued);
    }
}
