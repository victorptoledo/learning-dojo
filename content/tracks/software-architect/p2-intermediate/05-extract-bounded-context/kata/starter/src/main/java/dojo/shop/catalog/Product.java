package dojo.shop.catalog;

/** The legacy product: one class for everybody. After the split it is only an import format. */
public class Product {
    private final String sku;
    private String name;
    private String description;
    private long priceCents;
    private boolean discontinued;
    private int weightGrams;
    private String binLocation;
    private int stockOnHand;

    public Product(String sku, String name, String description, long priceCents, boolean discontinued,
                   int weightGrams, String binLocation, int stockOnHand) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.priceCents = priceCents;
        this.discontinued = discontinued;
        this.weightGrams = weightGrams;
        this.binLocation = binLocation;
        this.stockOnHand = stockOnHand;
    }

    public String sku() {
        return sku;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public long priceCents() {
        return priceCents;
    }

    public void setPriceCents(long priceCents) {
        this.priceCents = priceCents;
    }

    public boolean discontinued() {
        return discontinued;
    }

    public int weightGrams() {
        return weightGrams;
    }

    public String binLocation() {
        return binLocation;
    }

    public int stockOnHand() {
        return stockOnHand;
    }

    public void setStockOnHand(int stockOnHand) {
        this.stockOnHand = stockOnHand;
    }
}
