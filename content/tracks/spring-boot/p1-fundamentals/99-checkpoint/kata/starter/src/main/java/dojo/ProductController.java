package dojo;

import org.springframework.web.bind.annotation.RestController;

/** The /products endpoints of the README. */
@RestController
public class ProductController {
    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }
}
