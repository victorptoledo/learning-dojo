package dojo;

import org.springframework.http.HttpStatus;

public class ShopException extends RuntimeException {
    private final HttpStatus status;

    private ShopException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static ShopException unknownProduct(String sku) {
        return new ShopException(HttpStatus.NOT_FOUND, "product " + sku + " not found");
    }

    public static ShopException orderNotFound(long id) {
        return new ShopException(HttpStatus.NOT_FOUND, "order " + id + " not found");
    }

    public static ShopException outOfStock(String sku, int wanted, int left) {
        return new ShopException(HttpStatus.CONFLICT, "only " + left + " of " + sku + " left, " + wanted + " wanted");
    }

    public static ShopException alreadyCancelled(Long id) {
        return new ShopException(HttpStatus.CONFLICT, "order " + id + " is already cancelled");
    }
}
