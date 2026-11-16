import type { ReactNode } from "react";

export type SignupData = {
  name: string;
  email: string;
  password: string;
};

export type SignupFormProps = {
  /** Creates the account; rejects with an Error whose message is shown to the user. */
  onSubmit: (data: SignupData) => Promise<void>;
};

/** The sign-up form: controlled fields, accessible errors, focus, pending and result. */
export function SignupForm(_props: SignupFormProps): ReactNode {
  throw new Error("todo: write SignupForm");
}
