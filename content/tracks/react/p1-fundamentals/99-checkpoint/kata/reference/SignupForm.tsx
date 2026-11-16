import { useId, useRef, useState, type FormEvent } from "react";
import { EMPTY, FIELDS, validateSignup, type Field, type SignupErrors } from "./signup.ts";

export type SignupData = {
  name: string;
  email: string;
  password: string;
};

export type SignupFormProps = {
  /** Creates the account; rejects with an Error whose message is shown to the user. */
  onSubmit: (data: SignupData) => Promise<void>;
};

const TEXT_FIELDS: readonly { field: Exclude<Field, "terms">; label: string; type: string }[] = [
  { field: "name", label: "Name", type: "text" },
  { field: "email", label: "Email", type: "email" },
  { field: "password", label: "Password", type: "password" },
  { field: "confirm", label: "Confirm password", type: "password" },
];

/** The sign-up form: controlled fields, accessible errors, focus, pending and result. */
export function SignupForm({ onSubmit }: SignupFormProps) {
  const id = useId();
  const [values, setValues] = useState(EMPTY);
  const [submitted, setSubmitted] = useState(false);
  const [pending, setPending] = useState(false);
  const [failure, setFailure] = useState<string | null>(null);
  const [welcome, setWelcome] = useState<string | null>(null);
  const refs = useRef<Partial<Record<Field, HTMLInputElement | null>>>({});

  // Errors are derived: none before the first submit, then they follow the values.
  const errors: SignupErrors = submitted ? validateSignup(values) : {};

  function update<K extends Field>(field: K, value: (typeof EMPTY)[K]) {
    setValues((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitted(true);
    const found = validateSignup(values);
    const first = FIELDS.find((field) => found[field] !== undefined);
    if (first) {
      refs.current[first]?.focus();
      return;
    }
    setPending(true);
    setFailure(null);
    try {
      await onSubmit({
        name: values.name.trim(),
        email: values.email.trim(),
        password: values.password,
      });
      setWelcome(values.name.trim());
    } catch (error) {
      setFailure(error instanceof Error ? error.message : "Something went wrong");
    } finally {
      setPending(false);
    }
  }

  if (welcome !== null) {
    return <p role="status">Welcome, {welcome}!</p>;
  }

  function errorProps(field: Field) {
    const message = errors[field];
    return message === undefined
      ? {}
      : { "aria-invalid": true, "aria-describedby": `${id}-${field}-error` };
  }

  function errorText(field: Field) {
    const message = errors[field];
    return message === undefined ? null : <p id={`${id}-${field}-error`}>{message}</p>;
  }

  return (
    <form noValidate onSubmit={handleSubmit}>
      {TEXT_FIELDS.map(({ field, label, type }) => (
        <div key={field}>
          <label htmlFor={`${id}-${field}`}>{label}</label>
          <input
            id={`${id}-${field}`}
            type={type}
            ref={(node) => {
              refs.current[field] = node;
            }}
            value={values[field]}
            onChange={(event) => update(field, event.target.value)}
            {...errorProps(field)}
          />
          {errorText(field)}
        </div>
      ))}
      <div>
        <input
          id={`${id}-terms`}
          type="checkbox"
          ref={(node) => {
            refs.current.terms = node;
          }}
          checked={values.terms}
          onChange={(event) => update("terms", event.target.checked)}
          {...errorProps("terms")}
        />
        <label htmlFor={`${id}-terms`}>I accept the terms</label>
        {errorText("terms")}
      </div>
      {failure && <p role="alert">{failure}</p>}
      <button type="submit" disabled={pending}>
        {pending ? "Creating account…" : "Create account"}
      </button>
    </form>
  );
}
