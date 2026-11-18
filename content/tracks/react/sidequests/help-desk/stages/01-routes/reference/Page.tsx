import type { ReactNode } from "react";

/** Every page starts with its title as the one level 1 heading. */
export function Page({ title, children }: { title: string; children?: ReactNode }): ReactNode {
  return (
    <>
      <h1 tabIndex={-1}>{title}</h1>
      {children}
    </>
  );
}
