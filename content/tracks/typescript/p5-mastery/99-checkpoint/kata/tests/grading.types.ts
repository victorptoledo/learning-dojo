import type { FlagEntry, FlagsError, FlagValue, FlagValues, Listener } from "@dojo/flags";
import { createClient, defineFlags } from "@dojo/flags";
import type { Equal, Expect } from "./expect.ts";

const flags = defineFlags({
  darkMode: { kind: "boolean", default: false },
  maxUploads: { kind: "number", default: 3, min: 1, max: 10 },
  checkout: { kind: "variant", variants: ["classic", "one-page", "wizard"], default: "classic" },
});
type Flags = typeof flags;
const client = createClient(flags);

// test: values are derived from the definitions
export type Checkout = Expect<Equal<FlagValue<Flags["checkout"]>, "classic" | "one-page" | "wizard">>;
export type Values = Expect<
  Equal<
    FlagValues<Flags>,
    { readonly darkMode: boolean; readonly maxUploads: number; readonly checkout: "classic" | "one-page" | "wizard" }
  >
>;

// test: a variant default must be one of the variants
defineFlags({
  // @ts-expect-error: "xl" is not a variant
  size: { kind: "variant", variants: ["s", "m"], default: "xl" },
});

// test: reads are typed by key
const checkout = client.get("checkout");
export type Get = Expect<Equal<typeof checkout, "classic" | "one-page" | "wizard">>;
// @ts-expect-error: not a flag
client.get("nope");
const snapshot = client.snapshot();
// @ts-expect-error: a snapshot is read-only
snapshot.darkMode = true;

// test: entries narrow the value by the key
export type Entries = Expect<
  Equal<
    FlagEntry<Flags>,
    | readonly ["darkMode", boolean]
    | readonly ["maxUploads", number]
    | readonly ["checkout", "classic" | "one-page" | "wizard"]
  >
>;
for (const [key, value] of client.entries()) {
  if (key === "maxUploads") {
    const n: number = value;
    void n;
  }
}

// test: listeners are checked soundly
// @ts-expect-error: a listener for one variant cannot listen to all three
client.subscribe("checkout", (value: "wizard") => void value);
client.subscribe("checkout", (value: string) => void value);
export type ListenerType = Expect<Equal<Listener<number>, (value: number, previous: number) => void>>;

// test: errors carry read-only issues
export type Issues = Expect<Equal<FlagsError["issues"], readonly string[]>>;

// test: internals are not part of the package
// @ts-expect-error: not exported by package.json
export type Internal = typeof import("@dojo/flags/client.ts");
