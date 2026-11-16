import type { ReactNode } from "react";
import type { ResourceCache } from "./resource.ts";

export type User = {
  id: string;
  name: string;
  bio: string;
};

/** Reads the user with use() and shows it in an article. */
export function UserCard(_props: { userPromise: Promise<User> }): ReactNode {
  throw new Error("todo: write UserCard");
}

export type ProfilePageProps = {
  ids: readonly string[];
  cache: ResourceCache<User>;
};

/** User buttons and the profile, with Suspense, an error boundary and transitions. */
export function ProfilePage(_props: ProfilePageProps): ReactNode {
  throw new Error("todo: write ProfilePage");
}
