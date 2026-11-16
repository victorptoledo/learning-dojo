---
from: java
status: draft
concept: "CompletableFuture with cancellation, and a cache"
from_way: "You call a client (RestClient, WebClient) and get a value or a CompletableFuture; caching is @Cacheable; cancellation is future.cancel(true)."
native_way: "fetch returns a promise; an AbortController cancels it in the effect cleanup; a library such as TanStack Query plays the role of @Cacheable with expiry."
sensei_tip: "Treat useQuery like @Cacheable with a TTL and an evict on writes: the queryKey is the cache key, staleTime is the TTL, invalidateQueries is @CacheEvict."
---

| Java | React |
| --- | --- |
| `CompletableFuture<User>` | `Promise<User>` |
| `future.cancel(true)` | `controller.abort()` in the cleanup |
| `response.getStatusCode().is2xxSuccessful()` | `response.ok` |
| `@Cacheable("users")` | `useQuery({ queryKey: ["user", id] })` |
| `@CacheEvict` | `queryClient.invalidateQueries(...)` |
| `CompletableFuture.allOf` | `Promise.all` |
