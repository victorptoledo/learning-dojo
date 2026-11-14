# Events, Async Work, Scheduling and Caching

Some work does not belong in the request that triggers it: sending the confirmation email,
updating a search index, recalculating a report every night. And some work should not be done
twice when the answer has not changed. Spring has a small, consistent toolbox for each case.

## Application events: decouple the parts

The order service should not know every module that cares about a new order. It **publishes an
event**; whoever is interested **listens**:

```java
public record OrderPlaced(long orderId, String customerEmail) {}

@Service
class OrderService {
    private final ApplicationEventPublisher events;
    ...
    @Transactional
    public Order place(Cart cart) {
        Order order = orders.save(Order.from(cart));
        events.publishEvent(new OrderPlaced(order.getId(), cart.email()));
        return order;
    }
}

@Component
class ConfirmationMail {
    @EventListener
    void on(OrderPlaced event) {
        mailer.send(event.customerEmail(), "Order " + event.orderId() + " received");
    }
}
```

Any object can be an event; records are the natural choice. By default listeners run
**synchronously**, in the same thread and the same transaction as the publisher: if the
listener throws, the order rolls back too.

### After the commit

An email for an order that is then rolled back is a bug. `@TransactionalEventListener` runs the
listener only **after the transaction commits** (by default):

```java
@TransactionalEventListener
void on(OrderPlaced event) { mailer.send(...); }
```

For work that must survive crashes (the process dies right after the commit), store the event
in the same transaction (the **outbox** pattern) and process it afterwards; Spring Modulith's
event publication registry does that for you.

## Async work: @Async

```java
@Configuration
@EnableAsync
class AsyncConfig {}

@Service
class ReportService {
    @Async
    public CompletableFuture<Report> build(long customerId) {
        return CompletableFuture.completedFuture(slowReport(customerId));
    }
}
```

An `@Async` method returns immediately; the work runs on another thread. Return `void` or a
`CompletableFuture`. Like `@Transactional`, it works through a proxy: **calls from the same
class are not async**.

With `spring.threads.virtual.enabled=true`, Boot runs `@Async` tasks, scheduled tasks and request
handling on **virtual threads**, so blocking work in them is cheap. Exceptions thrown in a `void`
async method do not reach the caller; log them, or return a future.

## Scheduling: @Scheduled

```java
@Configuration
@EnableScheduling
class SchedulingConfig {}

@Component
class Cleanup {
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Lisbon")   // every day at 03:00
    void purgeExpiredCarts() { ... }

    @Scheduled(fixedDelayString = "PT5M")                      // 5 minutes after the last run ended
    void syncPrices() { ... }
}
```

`fixedRate` starts every N regardless of how long a run takes; `fixedDelay` waits N after the
previous run **finished**, so runs never overlap. With several instances of the service, every
instance runs the job: use a lock (ShedLock, or a database row) or a real job scheduler when it
must run once per cluster.

## Caching: @Cacheable

```java
@Configuration
@EnableCaching
class CacheConfig {}

@Service
class PriceService {
    @Cacheable("prices")
    public Price priceOf(String sku) {          // runs once per sku; later calls hit the cache
        return supplier.fetchPrice(sku);
    }

    @CacheEvict(value = "prices", key = "#sku")
    public void priceChanged(String sku) {}

    @CachePut(value = "prices", key = "#result.sku()")
    public Price update(PriceUpdate update) { ... }
}
```

- The **key** is built from the parameters (`sku` here); write `key = "#..."` (SpEL) for more
  control.
- Boot uses a simple in-memory map by default. In production pick a real cache: **Caffeine** in
  memory with size limits and expiry, or **Redis** shared by all instances.
- Caching is a proxy too: self-invocation skips it.

Hard questions to answer before caching anything: **how stale may the data be?**, **what
invalidates it?**, and **how big can the cache grow?** A cache with no expiry and no eviction is a
memory leak with good intentions.

## Choosing the tool

| Need | Use |
| --- | --- |
| Other parts react to something that happened | an application event |
| ...but only if the transaction committed | `@TransactionalEventListener` |
| Do slow work without making the caller wait | `@Async` (or a message queue for durability) |
| Run something on a timetable | `@Scheduled` (with a lock in a cluster) |
| Avoid recomputing the same answer | `@Cacheable` + an expiry and eviction plan |

## Summary

- Publish records as events with `ApplicationEventPublisher`; listen with `@EventListener`, or
  `@TransactionalEventListener` to act only after commit.
- `@Async` runs methods on another thread (virtual threads when enabled); proxies mean no
  self-invocation.
- `@Scheduled` with `cron`, `fixedRate` or `fixedDelay`; guard cluster-wide jobs with a lock.
- `@Cacheable`, `@CacheEvict` and `@CachePut`, backed by Caffeine or Redis, with a clear
  staleness and eviction plan.
