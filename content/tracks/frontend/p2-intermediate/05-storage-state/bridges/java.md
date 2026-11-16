---
from: java
status: draft
concept: "State and persistence"
from_way: "HTTP sessions on the server, JPA for persistence and a cache like Caffeine."
native_way: "The browser keeps state per origin in memory, the URL, Web Storage, IndexedDB and cookies; the server stays the source of truth."
sensei_tip: "Think of localStorage as a small, untrusted, user-editable cache, and IndexedDB as an embedded database like H2 that the user can delete."
---

| Java | In the browser |
| --- | --- |
| `HttpSession` attribute | `sessionStorage` (per tab) or an HttpOnly cookie |
| `Preferences` API | `localStorage` |
| Embedded H2 database | IndexedDB |
| `@RequestParam` filters | URL query parameters |
