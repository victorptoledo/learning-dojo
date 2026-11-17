import { checkField, type Rule } from "../validator.ts";
import type { Equal, Expect } from "./expect.ts";

// test: every kind of rule compiles with its fields
export const all: Rule[] = [
  { kind: "required" },
  { kind: "minLength", min: 2 },
  { kind: "maxLength", max: 40 },
  { kind: "pattern", regex: /^\S+@\S+$/, message: "must be an email" },
  { kind: "number" },
  { kind: "number", min: 0 },
  { kind: "number", max: 10 },
  { kind: "number", min: 0, max: 10 },
  { kind: "oneOf", options: ["S", "M", "L"] },
];

// test: a readonly list of options is accepted
const sizes = ["S", "M"] as const;
export const size: Rule = { kind: "oneOf", options: sizes };

// test: checkField takes a readonly list of rules and returns the messages
const frozen: readonly Rule[] = [{ kind: "required" }];
export const messages = checkField(frozen, "x");
export type Messages = Expect<Equal<typeof messages, string[]>>;

// test: a kind that is not in the table does not compile
// @ts-expect-error: "email" is not a kind of rule
export const unknownKind: Rule = { kind: "email" };

// test: minLength needs min
// @ts-expect-error: min is missing
export const noMin: Rule = { kind: "minLength" };

// test: maxLength needs a number
// @ts-expect-error: max must be a number
export const textMax: Rule = { kind: "maxLength", max: "10" };

// test: pattern needs its message
// @ts-expect-error: message is missing
export const noMessage: Rule = { kind: "pattern", regex: /a/ };

// test: pattern takes a RegExp, not a string
// @ts-expect-error: regex must be a RegExp
export const textRegex: Rule = { kind: "pattern", regex: "^a$", message: "a" };

// test: oneOf options are text
// @ts-expect-error: options must be strings
export const numberOptions: Rule = { kind: "oneOf", options: [1, 2] };

// test: a field of another kind of rule does not compile
// @ts-expect-error: required has no min
export const requiredWithMin: Rule = { kind: "required", min: 1 };

// test: the union can be narrowed on kind
export function boundOf(rule: Rule): number | undefined {
  if (rule.kind === "minLength") {
    return rule.min;
  }
  if (rule.kind === "maxLength") {
    return rule.max;
  }
  return undefined;
}
