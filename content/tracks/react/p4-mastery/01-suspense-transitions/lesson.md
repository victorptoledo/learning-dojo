# Suspense, use, Transitions and Error Boundaries

Async UI has three questions: what to show while something loads, what to show when it
fails, and what to keep on screen while the next thing loads. React answers them with
**Suspense**, **error boundaries** and **transitions**.

## Suspense

`<Suspense fallback={...}>` shows its fallback while any component inside it is **suspended**
(waiting), then reveals its children all at once:

```tsx
<Suspense fallback={<ProfileSkeleton />}>
  <ProfileCard id={id} />
  <Suspense fallback={<PostsSkeleton />}>
    <Posts id={id} />
  </Suspense>
</Suspense>
```

Boundaries decide **what reveals together**. Nested boundaries let the outer content appear
first and the inner content stream in later. Components suspend when they read data that is
not ready (with `use` or a Suspense-enabled library), or when `lazy` code is still loading.

## use

`use(promise)` reads the value of a promise. If it is pending, the component suspends; if it
is rejected, the error goes to the nearest error boundary:

```tsx
import { use } from "react";

function ProfileCard({ userPromise }: { userPromise: Promise<User> }) {
  const user = use(userPromise);
  return <h2>{user.name}</h2>;
}
```

The promise must be **stable**: created by a loader, a framework, a cache or a parent, and the
same object on every render. A promise created inside the component's render is new every time,
so it would never resolve from React's point of view. A small cache keyed by id is enough:

```ts
const cache = new Map<string, Promise<User>>();
export function fetchUser(id: string): Promise<User> {
  let promise = cache.get(id);
  if (!promise) {
    promise = api.user(id);
    cache.set(id, promise);
  }
  return promise;
}
```

Unlike other hooks, `use` may be called inside conditions and loops. `use(Context)` also reads a
context, conditionally if needed.

## Error boundaries

An **error boundary** catches errors thrown while rendering its children (including rejected
promises read with `use`) and shows a fallback instead of unmounting the whole app. It is still
a class component, because it needs `getDerivedStateFromError`:

```tsx
class ErrorBoundary extends Component<Props, { error: Error | null }> {
  override state = { error: null as Error | null };

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  override componentDidCatch(error: Error, info: ErrorInfo) {
    reportError(error, info.componentStack);       // logging
  }

  override render() {
    if (this.state.error) {
      return this.props.fallback(this.state.error, () => this.setState({ error: null }));
    }
    return this.props.children;
  }
}
```

Many apps use the `react-error-boundary` package instead of writing the class. Error boundaries
do **not** catch errors in event handlers or async code outside rendering; handle those with
`try`/`catch` and state. To **retry**, reset the boundary and make sure the failed request will
be made again (evict it from the cache, and let the component that reads the cache render
again); otherwise the same rejected promise fails again.
Changing a boundary's `key` also resets it.

## Transitions

A **transition** marks an update as non-urgent. React keeps showing the current UI while it
renders the new one in the background, and the update can be interrupted by more urgent input:

```tsx
const [isPending, startTransition] = useTransition();

function selectUser(id: string) {
  startTransition(() => {
    setUserId(id);
  });
}

<div aria-busy={isPending} style={{ opacity: isPending ? 0.6 : 1 }}>
  <Suspense fallback={<Spinner />}>
    <ProfileCard userPromise={fetchUser(userId)} />
  </Suspense>
</div>
```

Without the transition, switching users would replace the profile with the spinner. With it,
React **keeps the old profile** visible (already revealed content is not hidden again) and
`isPending` lets you dim it, until the new one is ready. Actions passed to `<form action>` and
`useActionState` run in transitions too, and in React 19 the function passed to
`startTransition` can be `async`.

`useDeferredValue(value)` is the value-based version: it gives a copy of `value` that lags
behind during heavy renders, so an input stays responsive while a big list renders with the
previous query.

## Optimistic updates

`useOptimistic(state, update)` shows the expected result **immediately** while an action is in
flight, and falls back to the real state when it settles:

```tsx
const [optimisticLikes, addLike] = useOptimistic(likes, (current, n: number) => current + n);
async function like() {
  addLike(1);
  await api.like(postId);   // when this transition ends, real state takes over
}
```

## Designing loading states

- Prefer **skeletons** with the shape of the content to spinners, to avoid layout shift.
- Do not wrap every component: place boundaries where a partial reveal makes sense.
- Contain failures: an error in a sidebar should not blank the page.
- Avoid flicker: keep old content during transitions instead of showing fallbacks again.

## Key points

- Suspense shows a fallback while children wait and reveals them together; nest boundaries to
  stream.
- `use(promise)` suspends on a stable promise from a cache or loader; never create it in render.
- Error boundaries catch render errors; retry by resetting them and evicting the failed request.
- Transitions keep current content on screen and mark updates as interruptible;
  `useDeferredValue` and `useOptimistic` complete the toolbox.

## Further reading

- react.dev, `<Suspense>`: <https://react.dev/reference/react/Suspense>
- react.dev, `use`: <https://react.dev/reference/react/use>
- react.dev, `useTransition`: <https://react.dev/reference/react/useTransition>
- react.dev, `useDeferredValue`: <https://react.dev/reference/react/useDeferredValue>
- react.dev, `useOptimistic`: <https://react.dev/reference/react/useOptimistic>
- react.dev, error boundaries (`Component`): <https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary>
