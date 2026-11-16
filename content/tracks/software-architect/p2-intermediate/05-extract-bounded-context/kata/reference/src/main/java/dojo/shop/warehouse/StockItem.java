package dojo.shop.warehouse;

/** A product as the warehouse sees it: something with a weight, a place and a count. */
public record StockItem(String sku, int weightGrams, String bin, int onHand) {
    public StockItem withOnHand(int count) {
        return new StockItem(sku, weightGrams, bin, count);
    }
}
