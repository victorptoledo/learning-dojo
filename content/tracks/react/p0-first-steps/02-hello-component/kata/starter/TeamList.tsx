import type { ReactNode } from "react";
import type { User } from "./UserCard.tsx";

export type TeamListProps = {
  title: string;
  users: readonly User[];
};

/** A section with the title, the member count and one UserCard per user, sorted by name. */
export function TeamList(_props: TeamListProps): ReactNode {
  throw new Error("todo: write TeamList");
}
