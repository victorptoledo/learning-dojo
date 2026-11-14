package dojo;

import org.springframework.http.HttpStatus;

/** Every refusal of the catalog, with the status it answers. */
public class CatalogException extends RuntimeException {
    private final HttpStatus status;

    private CatalogException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static CatalogException notFound(String sku) {
        return new CatalogException(HttpStatus.NOT_FOUND, "product " + sku + " not found");
    }

    public static CatalogException duplicate(String sku) {
        return new CatalogException(HttpStatus.CONFLICT, "product " + sku + " already exists");
    }

    public static CatalogException pageTooLarge(int size, int max) {
        return new CatalogException(HttpStatus.BAD_REQUEST, "page size " + size + " is larger than " + max);
    }

    public static CatalogException discountTooHigh(int percent, int max) {
        return new CatalogException(HttpStatus.UNPROCESSABLE_CONTENT,
                "discount " + percent + "% is higher than the maximum of " + max + "%");
    }
}
