import type { ReactNode } from "react";

export type Tone = "neutral" | "success" | "warning";

export type BadgeProps = {
  label: string;
  tone?: Tone;
};

/** A small label: `<span className="badge badge-<tone>">`, tone "neutral" by default. */
export function Badge(_props: BadgeProps): ReactNode {
  throw new Error("todo: write Badge");
}
