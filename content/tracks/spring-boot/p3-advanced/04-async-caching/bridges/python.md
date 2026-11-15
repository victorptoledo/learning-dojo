---
from: python
status: draft
concept: "Background work in Spring"
from_way: "Celery or background tasks, signals, and functools.lru_cache or Redis caches."
native_way: "Application events, @Async methods, @TransactionalEventListener and the cache abstraction with @Cacheable."
sensei_tip: "Django's post_save signal fires before the transaction commits unless you use on_commit; Spring's @TransactionalEventListener(phase = AFTER_COMMIT) is on_commit."
---

| Python | Spring Boot 4 |
| --- | --- |
| transaction.on_commit(...) | @TransactionalEventListener |
| BackgroundTasks / Celery | @Async or a message broker |
| @lru_cache | @Cacheable |
| cache.delete(key) | @CacheEvict |
