import assert from "node:assert/strict";
import { after, before, describe, test } from "node:test";
import type { ReactElement } from "react";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { ErrorBoundary } from "../ErrorBoundary.tsx";
import { ProfilePage, type User } from "../ProfilePage.tsx";
import { createResourceCache } from "../resource.ts";

// React logs every error an error boundary catches; keep the test output readable.
const originalError = console.error;
before(() => {
  console.error = () => {};
});
after(() => {
  console.error = originalError;
});

const USERS: Record<string, User> = {
  ada: { id: "ada", name: "Ada Lovelace", bio: "First programmer" },
  linus: { id: "linus", name: "Linus Torvalds", bio: "Kernel hacker" },
};

/** A loader whose calls the test settles by hand. */
function controlledLoader() {
  const calls: { key: string; resolve: () => void; reject: (error: Error) => void }[] = [];
  const load = (key: string) =>
    new Promise<User>((resolve, reject) => {
      calls.push({
        key,
        resolve: () => resolve(USERS[key] ?? { id: key, name: key, bio: "" }),
        reject,
      });
    });
  return { calls, load };
}

/** Rendering that suspends must happen inside an awaited act() in React 19. */
async function renderPage(page: ReactElement) {
  await act(async () => {
    render(page);
  });
}

/** A click whose update suspends, inside an awaited act() as React 19 requires. */
async function press(element: HTMLElement) {
  await act(async () => {
    fireEvent.click(element);
  });
}

function Bomb({ message }: { message: string }): never {
  throw new Error(message);
}

describe("createResourceCache", () => {
  test("evict makes the next read load again", async () => {
    const keys: string[] = [];
    const cache = createResourceCache(async (key) => {
      keys.push(key);
      return key.length;
    });
    const first = cache.read("abc");
    cache.evict("abc");
    const second = cache.read("abc");
    assert.notEqual(first, second);
    assert.equal(await second, 3);
    assert.deepEqual(keys, ["abc", "abc"]);
    cache.read("de");
    assert.deepEqual(keys, ["abc", "abc", "de"]);
  });
});

describe("ErrorBoundary", () => {
  test("renders the children until one throws, then the fallback with the error", () => {
    const { rerender } = render(
      <ErrorBoundary fallback={(error) => <p role="alert">{error.message}</p>}>
        <p>All good</p>
      </ErrorBoundary>,
    );
    assert.ok(screen.getByText("All good"));
    rerender(
      <ErrorBoundary fallback={(error) => <p role="alert">{error.message}</p>}>
        <Bomb message="Kaboom" />
      </ErrorBoundary>,
    );
    assert.equal(screen.getByRole("alert").textContent, "Kaboom");
  });

  test("reset renders the children again", async () => {
    const user = userEvent.setup();
    let explode = true;
    function Maybe() {
      if (explode) {
        throw new Error("Not yet");
      }
      return <p>Recovered</p>;
    }
    render(
      <ErrorBoundary
        fallback={(error, reset) => (
          <button type="button" onClick={reset}>
            {error.message}, retry
          </button>
        )}
      >
        <Maybe />
      </ErrorBoundary>,
    );
    explode = false;
    await user.click(screen.getByRole("button", { name: "Not yet, retry" }));
    assert.ok(screen.getByText("Recovered"));
  });
});

describe("ProfilePage", () => {
  test("shows the fallback, then the card of the first user", async () => {
    const { calls, load } = controlledLoader();
    await renderPage(<ProfilePage ids={["ada", "linus"]} cache={createResourceCache(load)} />);
    assert.equal(screen.getByRole("status").textContent, "Loading profile…");
    assert.equal(
      screen.getByRole("button", { name: "ada" }).getAttribute("aria-pressed"),
      "true",
    );
    await act(async () => calls[0]?.resolve());
    // React 19 throttles revealing Suspense content a little: wait for it.
    const card = await screen.findByRole("article", { name: "Ada Lovelace" });
    assert.equal(within(card).getByRole("heading").textContent, "Ada Lovelace");
    assert.ok(within(card).getByText("First programmer"));
    assert.equal(screen.queryByRole("status"), null);
  });

  test("switching users keeps the old card visible while the new one loads", async () => {
    const { calls, load } = controlledLoader();
    await renderPage(<ProfilePage ids={["ada", "linus"]} cache={createResourceCache(load)} />);
    await act(async () => calls[0]?.resolve());
    await screen.findByRole("article", { name: "Ada Lovelace" });
    const section = screen.getByRole("region", { name: "Profile" });
    assert.equal(section.getAttribute("aria-busy"), "false");
    await press(screen.getByRole("button", { name: "linus" }));
    assert.ok(screen.getByRole("article", { name: "Ada Lovelace" }), "the old card stays");
    assert.equal(screen.queryByRole("status"), null, "no fallback during the transition");
    assert.equal(section.getAttribute("aria-busy"), "true");
    await act(async () => calls[1]?.resolve());
    assert.ok(await screen.findByRole("article", { name: "Linus Torvalds" }));
    assert.equal(section.getAttribute("aria-busy"), "false");
    assert.equal(
      screen.getByRole("button", { name: "linus" }).getAttribute("aria-pressed"),
      "true",
    );
  });

  test("a failed load shows an alert, and Try again loads that user again", async () => {
    const { calls, load } = controlledLoader();
    await renderPage(<ProfilePage ids={["ada"]} cache={createResourceCache(load)} />);
    await act(async () => calls[0]?.reject(new Error("HTTP 500")));
    const alert = await screen.findByRole("alert");
    assert.ok(within(alert).getByText("Could not load ada: HTTP 500"));
    await press(within(alert).getByRole("button", { name: "Try again" }));
    assert.equal(calls.length, 2, "the user is requested again");
    await act(async () => calls[1]?.resolve());
    assert.ok(await screen.findByRole("article", { name: "Ada Lovelace" }));
    assert.equal(screen.queryByRole("alert"), null);
  });

  test("a failure does not stay on screen after switching to another user", async () => {
    const { calls, load } = controlledLoader();
    await renderPage(<ProfilePage ids={["ada", "linus"]} cache={createResourceCache(load)} />);
    await act(async () => calls[0]?.reject(new Error("Gone")));
    assert.ok(await screen.findByRole("alert"));
    await press(screen.getByRole("button", { name: "linus" }));
    await act(async () => calls.find((call) => call.key === "linus")?.resolve());
    assert.ok(await screen.findByRole("article", { name: "Linus Torvalds" }));
    assert.equal(screen.queryByRole("alert"), null);
  });

  test("a user loaded before is shown again without a new request", async () => {
    const { calls, load } = controlledLoader();
    await renderPage(<ProfilePage ids={["ada", "linus"]} cache={createResourceCache(load)} />);
    await act(async () => calls[0]?.resolve());
    await screen.findByRole("article", { name: "Ada Lovelace" });
    await press(screen.getByRole("button", { name: "linus" }));
    await act(async () => calls[1]?.resolve());
    await screen.findByRole("article", { name: "Linus Torvalds" });
    await press(screen.getByRole("button", { name: "ada" }));
    assert.ok(await screen.findByRole("article", { name: "Ada Lovelace" }));
    assert.equal(calls.length, 2);
  });
});
