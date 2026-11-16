import { Suspense, use, useState, useTransition } from "react";
import { ErrorBoundary } from "./ErrorBoundary.tsx";
import type { ResourceCache } from "./resource.ts";

export type User = {
  id: string;
  name: string;
  bio: string;
};

/** Reads the user with use() and shows it in an article. */
export function UserCard({ userPromise }: { userPromise: Promise<User> }) {
  const user = use(userPromise);
  return (
    <article aria-label={user.name}>
      <h2>{user.name}</h2>
      <p>{user.bio}</p>
    </article>
  );
}

export type ProfilePageProps = {
  ids: readonly string[];
  cache: ResourceCache<User>;
};

/** User buttons and the profile, with Suspense, an error boundary and transitions. */
export function ProfilePage({ ids, cache }: ProfilePageProps) {
  const [userId, setUserId] = useState(ids[0] ?? "");
  const [isPending, startTransition] = useTransition();
  // Bumped by Try again: the page must render again to read a fresh promise from the cache.
  const [, setAttempt] = useState(0);

  return (
    <div>
      <nav aria-label="Users">
        {ids.map((id) => (
          <button
            key={id}
            type="button"
            aria-pressed={id === userId}
            onClick={() => startTransition(() => setUserId(id))}
          >
            {id}
          </button>
        ))}
      </nav>
      <section aria-label="Profile" aria-busy={isPending}>
        <ErrorBoundary
          resetKey={userId}
          fallback={(error, reset) => (
            <div role="alert">
              <p>
                Could not load {userId}: {error.message}
              </p>
              <button
                type="button"
                onClick={() => {
                  cache.evict(userId);
                  setAttempt((attempt) => attempt + 1);
                  reset();
                }}
              >
                Try again
              </button>
            </div>
          )}
        >
          <Suspense fallback={<p role="status">Loading profile…</p>}>
            <UserCard userPromise={cache.read(userId)} />
          </Suspense>
        </ErrorBoundary>
      </section>
    </div>
  );
}
