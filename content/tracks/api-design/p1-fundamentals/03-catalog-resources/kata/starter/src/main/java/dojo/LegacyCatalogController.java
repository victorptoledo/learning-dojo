package dojo;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The catalog API as it was first written: one POST per action, a verb in every URL, and
 * always 200 with an "ok" flag. Replace it with ProductController and delete this class.
 */
@RestController
public class LegacyCatalogController {
    private final ProductStore store;

    public LegacyCatalogController(ProductStore store) {
        this.store = store;
    }

    @PostMapping("/getProducts")
    public Map<String, Object> getProducts() {
        return ok("products", store.all());
    }

    @PostMapping("/getProduct")
    public Map<String, Object> getProduct(@RequestBody Map<String, Long> body) {
        return store.find(body.get("id"))
                .map(product -> ok("product", product))
                .orElseGet(() -> failed("no such product"));
    }

    @PostMapping("/createProduct")
    public Map<String, Object> createProduct(@RequestBody ProductRequest request) {
        return ok("product", store.create(request));
    }

    @PostMapping("/deleteProduct")
    public Map<String, Object> deleteProduct(@RequestBody Map<String, Long> body) {
        return store.delete(body.get("id")) ? ok("deleted", true) : failed("no such product");
    }

    private static Map<String, Object> ok(String key, Object value) {
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("ok", true);
        answer.put(key, value);
        return answer;
    }

    private static Map<String, Object> failed(String message) {
        return Map.of("ok", false, "error", message);
    }
}
