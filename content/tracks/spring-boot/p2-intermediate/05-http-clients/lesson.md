# Calling Other Services

Sooner or later your service calls another one: a payment provider, a shipping API, an
internal service owned by another team. Remote calls are slow, can fail in many ways and can
hang forever. This lesson covers Spring's clients and the habits that keep a remote failure
from becoming your outage.

## RestClient

`RestClient` is Spring's synchronous HTTP client with a fluent API. Boot auto-configures a
`RestClient.Builder` bean (with Jackson, metrics and tracing); build one client per remote
service:

```java
@Configuration
class ShippingClientConfig {
    @Bean
    RestClient shippingClient(RestClient.Builder builder, ShippingProperties props) {
        return builder
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Bearer " + props.token())
                .build();
    }
}
```

```java
record Quote(String carrier, long cents, int days) {}

Quote quote = shippingClient.get()
        .uri("/quotes?from={from}&to={to}", from, to)
        .retrieve()
        .body(Quote.class);

ResponseEntity<Void> created = shippingClient.post()
        .uri("/shipments")
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ShipmentRequest(orderId, address))
        .retrieve()
        .toBodilessEntity();
```

- URI templates (`{from}`) are encoded for you; never concatenate user input into URLs.
- `body(Class)` maps JSON into a record; `ParameterizedTypeReference` handles `List<Quote>`.
- With virtual threads on (`spring.threads.virtual.enabled=true`), blocking calls like these
  scale well, so the synchronous client is usually all you need.

### Errors

By default `retrieve()` throws `HttpClientErrorException` for `4xx` and
`HttpServerErrorException` for `5xx`. Map them to what your domain means:

```java
Quote quote = shippingClient.get().uri("/quotes/{id}", id)
        .retrieve()
        .onStatus(status -> status.value() == 404, (request, response) -> {
            throw new QuoteNotFound(id);
        })
        .body(Quote.class);
```

Do not pass the remote error's body straight to your own clients: translate it, and log the
details.

## HTTP interfaces: a client as an interface

Like Spring Data repositories, you can **declare** a client and let Spring implement it:

```java
@HttpExchange("/quotes")
interface ShippingApi {
    @GetExchange
    Quote quote(@RequestParam String from, @RequestParam String to);

    @GetExchange("/{id}")
    Quote byId(@PathVariable String id);

    @PostExchange
    Quote create(@RequestBody QuoteRequest request);
}
```

Spring Boot 4 registers such interfaces as beans from configuration:

```java
@Configuration
@ImportHttpServices(group = "shipping", types = ShippingApi.class)
class HttpClientsConfig {}
```

```properties
spring.http.serviceclient.shipping.base-url=https://shipping.example.com
```

Then inject `ShippingApi` anywhere. In tests, it is just an interface: replace it with a fake
or a mock.

## Timeouts: always

A call without a timeout can block a thread (and a database connection, if you are in a
transaction) for minutes. Set both:

```properties
spring.http.clients.connect-timeout=2s
spring.http.clients.read-timeout=5s
```

- **Connect timeout**: how long to wait for the connection.
- **Read timeout**: how long to wait for the answer.

Choose them from what the caller can afford: if your endpoint must answer in 1 second, a
5-second read timeout downstream is already a bug.

## Retries, carefully

Retrying helps with **transient** failures (a dropped connection, a `503`), and hurts
everywhere else:

- Retry only **idempotent** operations (`GET`, or a `POST` carrying an idempotency key the
  server deduplicates). Retrying a payment `POST` without one can charge twice.
- Retry a **few** times with **backoff** (wait 100 ms, then 400 ms...), not immediately in a
  loop; a struggling service needs less traffic, not more.
- Never retry `4xx`: the request is wrong and will stay wrong.

Spring Framework 7 has resilience features built in: `@Retryable` (with backoff) and
`@ConcurrencyLimit`, enabled with `@EnableResilientMethods`. For circuit breakers, Resilience4j
is the usual choice.

## Testing clients

- Unit-test the code around the client by **faking the interface** (HTTP interfaces make this
  trivial).
- Test the HTTP mapping itself with `MockRestServiceServer` (bound to a `RestClient.Builder`)
  or a stub server such as WireMock, so you check URLs, headers and error handling without the
  real service.

## Summary

- Build one `RestClient` per remote service from the auto-configured builder; use URI templates
  and map JSON into records.
- Translate `4xx`/`5xx` into domain exceptions with `onStatus`.
- Declare clients as `@HttpExchange` interfaces when you can; they are easy to fake.
- Always set connect and read timeouts.
- Retry only idempotent, transient failures, a few times, with backoff.
