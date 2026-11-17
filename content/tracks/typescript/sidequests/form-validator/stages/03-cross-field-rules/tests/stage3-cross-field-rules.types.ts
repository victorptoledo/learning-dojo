import { checkField, defineSchema, validate, type Rule, type ValidationResult } from "../validator.ts";
import type { Equal, Expect } from "./expect.ts";

// test: sameAs and custom compile in a schema
export const signup = defineSchema({
  password: [{ kind: "required" }],
  confirm: [{ kind: "sameAs", field: "password" }],
  nickname: [{ kind: "custom", check: (value, form) => (value === form.password ? "taken" : null) }],
});
export type SignupFields = Expect<Equal<keyof typeof signup, "password" | "confirm" | "nickname">>;

// test: sameAs only names fields of the same schema
// @ts-expect-error: "pasword" is not a field of the schema
defineSchema({ password: [{ kind: "required" }], confirm: [{ kind: "sameAs", field: "pasword" }] });

// test: sameAs in an inline schema only names its fields too
// @ts-expect-error: "b" is not a field of the schema
validate({ a: [{ kind: "sameAs", field: "b" }] }, {});

// test: custom sees the value as text and the form with its field names
defineSchema({
  password: [],
  hint: [
    {
      kind: "custom",
      check: (value, form) => {
        const own: string = value;
        const other: string = form.password;
        return own === other ? "same" : null;
      },
    },
  ],
});

// test: custom cannot read a field outside the schema
// @ts-expect-error: nope is not a field of the schema
defineSchema({ password: [], hint: [{ kind: "custom", check: (_value, form) => form.nope }] });

// test: custom returns a message or null
// @ts-expect-error: check must return string | null
defineSchema({ hint: [{ kind: "custom", check: () => 42 }] });

// test: Rule without arguments still works for a single field
const loose: Rule[] = [{ kind: "required" }, { kind: "sameAs", field: "anything" }];
export const looseMessages: string[] = checkField(loose, "x");

// test: Rule takes the field names
export const typed: Rule<"a" | "b"> = { kind: "sameAs", field: "b" };
// @ts-expect-error: "c" is not one of the names
export const wrongName: Rule<"a" | "b"> = { kind: "sameAs", field: "c" };

// test: validate still knows the field names
const result = validate(signup, {});
export type ResultType = Expect<Equal<typeof result, ValidationResult<"password" | "confirm" | "nickname">>>;
