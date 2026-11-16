import { UserCard, type User } from "./UserCard.tsx";

export type TeamListProps = {
  title: string;
  users: readonly User[];
};

/** A section with the title, the member count and one UserCard per user, sorted by name. */
export function TeamList({ title, users }: TeamListProps) {
  const sorted = users.toSorted((a, b) => a.name.localeCompare(b.name));
  const count = users.length === 1 ? "1 member" : `${users.length} members`;
  return (
    <section aria-label={title}>
      <h1>{title}</h1>
      <p>{count}</p>
      {sorted.length === 0 ? (
        <p>No members yet</p>
      ) : (
        <ul>
          {sorted.map((user) => (
            <li key={user.id}>
              <UserCard user={user} />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
