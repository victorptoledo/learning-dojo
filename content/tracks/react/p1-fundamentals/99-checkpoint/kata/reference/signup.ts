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

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Checks every field; see the table in README.md. */
export function validateSignup(values: SignupValues): SignupErrors {
  const errors: SignupErrors = {};
  if (values.name.trim() === "") {
    errors.name = "Enter your name";
  }
  if (!EMAIL.test(values.email.trim())) {
    errors.email = "Enter a valid email";
  }
  if (values.password.length < 8 || !/\d/.test(values.password)) {
    errors.password = "Use at least 8 characters with a digit";
  }
  if (values.confirm !== values.password) {
    errors.confirm = "Passwords do not match";
  }
  if (!values.terms) {
    errors.terms = "Accept the terms";
  }
  return errors;
}
