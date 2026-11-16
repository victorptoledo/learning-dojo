import { Badge } from "./Badge.tsx";

export type User = {
  id: string;
  name: string;
  email?: string;
  active: boolean;
  skills: readonly string[];
};

export type UserCardProps = {
  user: User;
};

/** An article with the name, an Active/Inactive badge, the email link and the skills. */
export function UserCard({ user }: UserCardProps) {
  return (
    <article aria-label={user.name}>
      <h2>{user.name}</h2>
      {user.active ? <Badge label="Active" tone="success" /> : <Badge label="Inactive" />}
      {user.email && <a href={`mailto:${user.email}`}>{user.email}</a>}
      {user.skills.length > 0 ? (
        <ul>
          {user.skills.map((skill) => (
            <li key={skill}>{skill}</li>
          ))}
        </ul>
      ) : (
        <p>No skills yet</p>
      )}
    </article>
  );
}
