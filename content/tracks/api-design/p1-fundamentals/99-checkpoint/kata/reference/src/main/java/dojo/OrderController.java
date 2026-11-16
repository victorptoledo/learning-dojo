package dojo;

import dojo.OrderExceptions.InvalidParameterException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** The orders collection, one order, and the cancellation of an order. */
@RestController
@RequestMapping("/orders")
public class OrderController {
    private static final int MAX_SIZE = 100;
    private static final Map<String, Comparator<Order>> SORTABLE = Map.of(
            "id", Comparator.comparingLong(Order::id),
            "createdAt", Comparator.comparing(Order::createdAt),
            "totalCents", Comparator.comparingLong(Order::totalCents));

    private final OrderStore store;

    public OrderController(OrderStore store) {
        this.store = store;
    }

    @GetMapping
    public ResponseEntity<OrderPage> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customer,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 1) {
            throw new InvalidParameterException("page", "page must be at least 1");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidParameterException("size", "size must be between 1 and " + MAX_SIZE);
        }
        OrderStatus wanted = status == null ? null : parseStatus(status);
        List<Order> matching = store.all().stream()
                .filter(order -> wanted == null || order.status() == wanted)
                .filter(order -> customer == null || order.customer().equals(customer))
                .sorted(comparator(sort))
                .toList();
        int totalPages = (matching.size() + size - 1) / size;
        List<Order> items = matching.stream().skip((long) (page - 1) * size).limit(size).toList();
        OrderPage body = new OrderPage(items, page, size, matching.size(), totalPages);
        return ResponseEntity.ok().header(HttpHeaders.LINK, links(page, totalPages)).body(body);
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable long id) {
        return store.find(id);
    }

    @PostMapping
    public ResponseEntity<Order> place(@Valid @RequestBody OrderRequest request) {
        Order order = store.place(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(order.id())
                .toUri();
        return ResponseEntity.created(location).body(order);
    }

    @PostMapping("/{id}/cancellation")
    public Order cancel(@PathVariable long id) {
        return store.cancel(id);
    }

    private static OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            String allowed = Arrays.stream(OrderStatus.values()).map(Enum::name).collect(Collectors.joining(", "));
            throw new InvalidParameterException("status", "status '" + status + "' is not one of " + allowed);
        }
    }

    /** "totalCents,-id": ascending total, then descending id. */
    private static Comparator<Order> comparator(String sort) {
        Comparator<Order> result = null;
        for (String key : sort.split(",")) {
            boolean descending = key.startsWith("-");
            String field = descending ? key.substring(1) : key;
            Comparator<Order> one = SORTABLE.get(field);
            if (one == null) {
                throw new InvalidParameterException("sort",
                        "sort field '" + field + "' is not one of createdAt, id, totalCents");
            }
            one = descending ? one.reversed() : one;
            result = result == null ? one : result.thenComparing(one);
        }
        return result.thenComparing(SORTABLE.get("id"));
    }

    private static String links(int page, int totalPages) {
        List<String> links = new ArrayList<>();
        int last = Math.max(totalPages, 1);
        links.add(link(1, "first"));
        if (page > 1) {
            links.add(link(Math.min(page - 1, last), "prev"));
        }
        if (page < totalPages) {
            links.add(link(page + 1, "next"));
        }
        links.add(link(last, "last"));
        return String.join(", ", links);
    }

    private static String link(int page, String rel) {
        String url = ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQueryParam("page", page)
                .toUriString();
        return "<" + url + ">; rel=\"" + rel + "\"";
    }
}
