import type { ReactNode } from "react";

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
export function UserCard(_props: UserCardProps): ReactNode {
  throw new Error("todo: write UserCard");
}
