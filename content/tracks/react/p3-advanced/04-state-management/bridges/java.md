---
from: java
status: draft
concept: "Caches, sessions and application state"
from_way: "A Spring app separates request-scoped data, the HTTP session, caches in front of the database, and application-wide beans; nobody stores DB rows in a singleton map by hand."
native_way: "A React app separates local state, URL state, a query cache for server data, and a small amount of shared client state in context or a store."
sensei_tip: "TanStack Query is your second-level cache in front of the API; a Zustand store is a small application-scoped bean; the URL is the request. Put each piece where its lifetime matches."
---

| Java / Spring | React |
| --- | --- |
| Request parameters | URL path and search params |
| `@SessionScope` bean | Context or store for the signed-in user |
| Second-level cache (Caffeine) | Query cache (TanStack Query) |
| Application-scoped bean | A store (Zustand, Redux Toolkit) |
| Local variables | Local component state |
| Event bus listeners | Store subscriptions with selectors |
