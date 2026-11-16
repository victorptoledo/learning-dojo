import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { render, screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { SignupForm, type SignupData } from "../SignupForm.tsx";
import { EMPTY, validateSignup } from "../signup.ts";

function deferred() {
  let resolve: () => void = () => {};
  let reject: (error: Error) => void = () => {};
  const promise = new Promise<void>((ok, fail) => {
    resolve = ok;
    reject = fail;
  });
  return { promise, resolve, reject };
}

async function fillValid(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Name"), "  Ada ");
  await user.type(screen.getByLabelText("Email"), "ada@example.com");
  await user.type(screen.getByLabelText("Password"), "engine42");
  await user.type(screen.getByLabelText("Confirm password"), "engine42");
  await user.click(screen.getByLabelText("I accept the terms"));
}

function describedError(label: string): string | null {
  const input = screen.getByLabelText(label);
  const id = input.getAttribute("aria-describedby");
  if (!id) {
    return null;
  }
  return document.getElementById(id)?.textContent ?? null;
}

describe("validateSignup", () => {
  test("reports every invalid field with its message", () => {
    assert.deepEqual(validateSignup(EMPTY), {
      name: "Enter your name",
      email: "Enter a valid email",
      password: "Use at least 8 characters with a digit",
      terms: "Accept the terms",
    });
  });

  test("checks email shape, password rules and the confirmation", () => {
    const base = {
      name: " Ada ",
      email: " ada@example.com ",
      password: "engine42",
      confirm: "engine42",
      terms: true,
    };
    assert.deepEqual(validateSignup(base), {});
    for (const email of ["ada", "ada@", "ada@example", "a da@example.com", "a@b@c.com"]) {
      assert.equal(validateSignup({ ...base, email }).email, "Enter a valid email", email);
    }
    for (const password of ["short1", "longenough"]) {
      const errors = validateSignup({ ...base, password, confirm: password });
      assert.equal(errors.password, "Use at least 8 characters with a digit", password);
    }
    assert.equal(validateSignup({ ...base, confirm: "engine43" }).confirm, "Passwords do not match");
  });
});

describe("SignupForm", () => {
  test("has labelled fields and no errors before the first submit", () => {
    render(<SignupForm onSubmit={async () => {}} />);
    for (const label of ["Name", "Email", "Password", "Confirm password", "I accept the terms"]) {
      assert.ok(screen.getByLabelText(label), label);
    }
    assert.equal(screen.getByLabelText("Email").getAttribute("type"), "email");
    assert.equal(screen.getByLabelText("Password").getAttribute("type"), "password");
    assert.equal(screen.queryByText("Enter your name"), null);
  });

  test("an invalid submit shows errors tied to the fields and does not call onSubmit", async () => {
    const user = userEvent.setup();
    let calls = 0;
    render(
      <SignupForm
        onSubmit={async () => {
          calls += 1;
        }}
      />,
    );
    await user.click(screen.getByRole("button", { name: "Create account" }));
    assert.equal(calls, 0);
    assert.equal(screen.getByLabelText("Name").getAttribute("aria-invalid"), "true");
    assert.equal(describedError("Name"), "Enter your name");
    assert.equal(describedError("Email"), "Enter a valid email");
    assert.equal(describedError("I accept the terms"), "Accept the terms");
  });

  test("focus moves to the first invalid field", async () => {
    const user = userEvent.setup();
    render(<SignupForm onSubmit={async () => {}} />);
    await user.type(screen.getByLabelText("Name"), "Ada");
    await user.click(screen.getByRole("button", { name: "Create account" }));
    assert.equal(document.activeElement, screen.getByLabelText("Email"));
  });

  test("after a failed submit, fixing a field removes its error as you type", async () => {
    const user = userEvent.setup();
    render(<SignupForm onSubmit={async () => {}} />);
    await user.click(screen.getByRole("button", { name: "Create account" }));
    assert.equal(describedError("Name"), "Enter your name");
    await user.type(screen.getByLabelText("Name"), "A");
    assert.equal(describedError("Name"), null);
    assert.notEqual(screen.getByLabelText("Name").getAttribute("aria-invalid"), "true");
    assert.equal(describedError("Email"), "Enter a valid email");
  });

  test("a mismatched confirmation is reported on the confirmation field", async () => {
    const user = userEvent.setup();
    render(<SignupForm onSubmit={async () => {}} />);
    await fillValid(user);
    await user.type(screen.getByLabelText("Confirm password"), "x");
    await user.click(screen.getByRole("button", { name: "Create account" }));
    assert.equal(describedError("Confirm password"), "Passwords do not match");
    assert.equal(document.activeElement, screen.getByLabelText("Confirm password"));
  });

  test("a valid submit sends trimmed data, shows the pending state and then welcomes", async () => {
    const user = userEvent.setup();
    const sent: SignupData[] = [];
    const call = deferred();
    render(
      <SignupForm
        onSubmit={(data) => {
          sent.push(data);
          return call.promise;
        }}
      />,
    );
    await fillValid(user);
    await user.click(screen.getByRole("button", { name: "Create account" }));
    assert.deepEqual(sent, [{ name: "Ada", email: "ada@example.com", password: "engine42" }]);
    const button = screen.getByRole("button", { name: "Creating account…" }) as HTMLButtonElement;
    assert.equal(button.disabled, true);
    call.resolve();
    const welcome = await screen.findByRole("status");
    assert.equal(welcome.textContent, "Welcome, Ada!");
    assert.equal(screen.queryByRole("button"), null);
  });

  test("a rejected submit shows the error and lets the user try again", async () => {
    const user = userEvent.setup();
    let attempts = 0;
    render(
      <SignupForm
        onSubmit={async () => {
          attempts += 1;
          if (attempts === 1) {
            throw new Error("Email already in use");
          }
        }}
      />,
    );
    await fillValid(user);
    await user.click(screen.getByRole("button", { name: "Create account" }));
    const alert = await screen.findByRole("alert");
    assert.equal(alert.textContent, "Email already in use");
    const button = screen.getByRole("button", { name: "Create account" }) as HTMLButtonElement;
    assert.equal(button.disabled, false);
    assert.equal((screen.getByLabelText("Email") as HTMLInputElement).value, "ada@example.com");
    await user.click(button);
    assert.equal((await screen.findByRole("status")).textContent, "Welcome, Ada!");
  });

  test("the page does not reload: the submit event is cancelled", async () => {
    const user = userEvent.setup();
    let defaultPrevented = false;
    render(<SignupForm onSubmit={async () => {}} />);
    // React handles events at its root, so look at the event once it has bubbled past it.
    const listener = (event: Event) => {
      defaultPrevented = event.defaultPrevented;
    };
    window.addEventListener("submit", listener);
    try {
      await user.click(screen.getByRole("button", { name: "Create account" }));
    } finally {
      window.removeEventListener("submit", listener);
    }
    assert.equal(defaultPrevented, true);
  });
});
