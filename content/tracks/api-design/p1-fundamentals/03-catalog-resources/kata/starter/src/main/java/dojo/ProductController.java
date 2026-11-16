package dojo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The catalog as resources: the collection /products and one /products/{id} per product. */
@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductStore store;

    public ProductController(ProductStore store) {
        this.store = store;
    }
}
