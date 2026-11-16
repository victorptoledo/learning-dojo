export type Tone = "neutral" | "success" | "warning";

export type BadgeProps = {
  label: string;
  tone?: Tone;
};

/** A small label: `<span className="badge badge-<tone>">`, tone "neutral" by default. */
export function Badge({ label, tone = "neutral" }: BadgeProps) {
  return <span className={`badge badge-${tone}`}>{label}</span>;
}
