// A small form validation library: rules for one field, a typed schema for a form, and rules
// that compare fields. `K` is the names of the form's fields.

export type Rule<K extends string = string> =
  | { kind: "required" }
  | { kind: "minLength"; min: number }
  | { kind: "maxLength"; max: number }
  | { kind: "pattern"; regex: RegExp; message: string }
  | { kind: "number"; min?: number; max?: number }
  | { kind: "oneOf"; options: readonly string[] }
  | { kind: "sameAs"; field: K }
  | { kind: "custom"; check: (value: string, form: Record<K, string>) => string | null };

const NUMBER = /^-?\d+(\.\d+)?$/;

export function assertNever(value: never): never {
  throw new Error(`unexpected value: ${JSON.stringify(value)}`);
}

/** The trimmed text, `null` for an empty value, or `undefined` when it is not text at all. */
function readText(value: unknown): string | null | undefined {
  if (value === undefined || value === null) {
    return null;
  }
  if (typeof value !== "string") {
    return undefined;
  }
  const text = value.trim();
  return text === "" ? null : text;
}

function checkNumber(rule: { min?: number; max?: number }, text: string): string | null {
  if (!NUMBER.test(text)) {
    return "must be a number";
  }
  const number = Number(text);
  if (rule.min !== undefined && number < rule.min) {
    return `must be at least ${rule.min}`;
  }
  if (rule.max !== undefined && number > rule.max) {
    return `must be at most ${rule.max}`;
  }
  return null;
}

/** The message of a rule on a value that is there, or `null` when the value follows it. */
function checkRule<K extends string>(rule: Rule<K>, text: string, form: Record<K, string>): string | null {
  switch (rule.kind) {
    case "required":
      return null;
    case "minLength":
      return [...text].length < rule.min ? `must be at least ${rule.min} characters` : null;
    case "maxLength":
      return [...text].length > rule.max ? `must be at most ${rule.max} characters` : null;
    case "pattern":
      rule.regex.lastIndex = 0;
      return rule.regex.test(text) ? null : rule.message;
    case "number":
      return checkNumber(rule, text);
    case "oneOf":
      return rule.options.includes(text) ? null : `must be one of: ${rule.options.join(", ")}`;
    case "sameAs":
      return text === (form[rule.field] ?? "") ? null : `must match ${rule.field}`;
    case "custom":
      return rule.check(text, form);
    default:
      return assertNever(rule);
  }
}

/** The messages of every rule the value breaks; `form` is what `sameAs` and `custom` see. */
export function checkField<K extends string = string>(
  rules: readonly Rule<K>[],
  value: unknown,
  form: Record<K, string> = {} as Record<K, string>,
): string[] {
  const text = readText(value);
  if (text === undefined) {
    return ["must be text"];
  }
  if (text === null) {
    return rules.some((rule) => rule.kind === "required") ? ["is required"] : [];
  }
  const messages: string[] = [];
  for (const rule of rules) {
    const message = checkRule(rule, text, form);
    if (message !== null) {
      messages.push(message);
    }
  }
  return messages;
}

/** Field names and their rules. */
export type Schema = Record<string, readonly Rule[]>;

export type FieldError<K extends string> = { field: K; messages: string[] };

export type ValidationResult<K extends string> =
  | { ok: true; values: Record<K, string> }
  | { ok: false; errors: FieldError<K>[] };

/**
 * The schema itself, typed with its field names and literal rule kinds. `NoInfer` makes the
 * names come from the keys only, so a `sameAs` naming another field is an error at that rule.
 */
export function defineSchema<K extends string>(
  schema: Record<K, readonly Rule<NoInfer<K>>[]>,
): Record<K, readonly Rule<K>[]> {
  return schema;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function validate<K extends string>(
  schema: Record<K, readonly Rule<NoInfer<K>>[]>,
  input: unknown,
): ValidationResult<K> {
  const form = isRecord(input) ? input : {};
  const fields = Object.keys(schema) as K[];
  const raw = (field: K): unknown => (Object.hasOwn(form, field) ? form[field] : undefined);
  const values = {} as Record<K, string>;
  for (const field of fields) {
    const value = raw(field);
    values[field] = typeof value === "string" ? value.trim() : "";
  }
  const errors: FieldError<K>[] = [];
  for (const field of fields) {
    const messages = checkField(schema[field], raw(field), values);
    if (messages.length > 0) {
      errors.push({ field, messages });
    }
  }
  return errors.length === 0 ? { ok: true, values } : { ok: false, errors };
}
