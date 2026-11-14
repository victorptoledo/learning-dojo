package dojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** The /products endpoints of the README. */
@RestController
public class ProductController {
    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @PostMapping("/products")
    public ResponseEntity<Product> add(@Valid @RequestBody ProductRequest request, UriComponentsBuilder uri) {
        Product product = products.add(request);
        return ResponseEntity.created(uri.path("/products/{sku}").build(product.sku())).body(product);
    }

    @GetMapping("/products/{sku}")
    public Product get(@PathVariable String sku) {
        return products.get(sku);
    }

    @GetMapping("/products")
    public ProductPage list(@RequestParam(required = false) String category,
                            @RequestParam(defaultValue = "0") @Min(0) int page,
                            @RequestParam(defaultValue = "10") @Min(1) int size) {
        return products.list(category, page, size);
    }

    @PostMapping("/products/{sku}/discount")
    public PriceQuote discount(@PathVariable String sku, @Valid @RequestBody DiscountRequest request) {
        return products.discount(sku, request.percent());
    }
}
