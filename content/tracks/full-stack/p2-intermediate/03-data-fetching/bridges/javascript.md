---
from: javascript
status: reviewed
concept: "Server state is not component state"
from_way: "useEffect + fetch + useState in every component, and a global store that copies API responses."
native_way: "A server-state cache (TanStack Query, SWR) with query keys, stale-while-revalidate, invalidation after mutations, and optimistic updates with rollback."
sensei_tip: "If you write setLoading(true) and setError(null) by hand in a third component, it is time for a server-state library."
---

| Hand-rolled | Server-state cache |
| --- | --- |
| `useEffect(() => fetch(...), [])` | `useQuery({ queryKey, queryFn })` |
| refetch after every write by hand | `invalidateQueries({ queryKey: ["cart"] })` |
| waiting for the server to update the like | `onMutate` optimistic update + rollback |
| duplicate requests from siblings | deduplicated by key |
