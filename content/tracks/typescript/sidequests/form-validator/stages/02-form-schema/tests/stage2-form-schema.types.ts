import { defineSchema, validate, type FieldError, type Rule, type Schema, type ValidationResult } from "../validator.ts";
import type { Equal, Expect } from "./expect.ts";

const signup = defineSchema({
  name: [{ kind: "required" }, { kind: "minLength", min: 2 }],
  age: [{ kind: "number", min: 18 }],
});
const result = validate(signup, {});

// test: defineSchema keeps the field names without as const
export type Fields = Expect<Equal<keyof typeof signup, "name" | "age">>;

// test: a schema is an object of readonly rule lists
const rules: readonly Rule[] = [{ kind: "required" }];
export const schema: Schema = { name: rules, age: [{ kind: "number" }] };

// test: defineSchema checks the rules
// @ts-expect-error: minLength needs min
defineSchema({ name: [{ kind: "minLength" }] });

// test: FieldError and ValidationResult have the fields of the README
export type ErrorShape = Expect<Equal<FieldError<"a">, { field: "a"; messages: string[] }>>;
export const passed: ValidationResult<"a"> = { ok: true, values: { a: "x" } };
export const failed: ValidationResult<"a"> = { ok: false, errors: [{ field: "a", messages: ["m"] }] };

// test: validate returns a ValidationResult of the schema's field names
export type ResultType = Expect<Equal<typeof result, ValidationResult<"name" | "age">>>;

// test: after checking ok, values has the fields of the schema
export const name: string | undefined = result.ok ? result.values.name : undefined;
export const age: string | undefined = result.ok ? result.values.age : undefined;

// test: values has no field outside the schema
// @ts-expect-error: phone is not a field of signup
export const phone = result.ok ? result.values.phone : undefined;

// test: values cannot be read before checking ok
// @ts-expect-error: the result may be a failure
export const early = result.values;

// test: the errors name the schema's fields
export type ErrorField = Expect<
  Equal<Extract<typeof result, { ok: false }>["errors"][number]["field"], "name" | "age">
>;

// test: a schema written inline keeps its names too
const inline = validate({ city: [{ kind: "required" }] }, null);
export type InlineFields = Expect<Equal<typeof inline, ValidationResult<"city">>>;

// test: the input can be anything
export const fromAnything = [validate(signup, 42), validate(signup, "x"), validate(signup, undefined)];
