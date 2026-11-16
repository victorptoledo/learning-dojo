export type SignupValues = {
  name: string;
  email: string;
  password: string;
  confirm: string;
  terms: boolean;
};

export type Field = keyof SignupValues;

/** A message per invalid field; empty when the values are valid. */
export type SignupErrors = Partial<Record<Field, string>>;

/** The fields in the order they appear in the form (and get focus). */
export const FIELDS: readonly Field[] = ["name", "email", "password", "confirm", "terms"];

export const EMPTY: SignupValues = {
  name: "",
  email: "",
  password: "",
  confirm: "",
  terms: false,
};

/** Checks every field; see the table in README.md. */
export function validateSignup(_values: SignupValues): SignupErrors {
  throw new Error("todo: write validateSignup");
}
