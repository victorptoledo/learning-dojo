import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { useState } from "react";
import { act, render, renderHook, screen, waitFor } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { SearchBox } from "../SearchBox.tsx";
import { useDebouncedValue } from "../useDebouncedValue.ts";
import { useSearch, type SearchFn } from "../useSearch.ts";

const DELAY = 30;

/** A search whose calls the test resolves or rejects by hand. */
function controlledSearch() {
  const calls: {
    query: string;
    signal: AbortSignal;
    resolve: (results: readonly string[]) => void;
    reject: (error: unknown) => void;
  }[] = [];
  const search: SearchFn = (query, signal) =>
    new Promise((resolve, reject) => {
      calls.push({ query, signal, resolve, reject });
    });
  return { calls, search };
}

async function settle(ms = DELAY * 3) {
  await act(async () => {
    await sleep(ms);
  });
}

describe("useDebouncedValue", () => {
  test("waits for the delay before returning a new value", async () => {
    const { result, rerender } = renderHook(({ value }) => useDebouncedValue(value, DELAY), {
      initialProps: { value: "a" },
    });
    rerender({ value: "b" });
    assert.equal(result.current, "a");
    await waitFor(() => assert.equal(result.current, "b"));
  });

  test("restarts the wait on every change and keeps only the last value", async () => {
    const seen: string[] = [];
    const { rerender } = renderHook(
      ({ value }) => {
        const debounced = useDebouncedValue(value, DELAY);
        if (seen.at(-1) !== debounced) {
          seen.push(debounced);
        }
        return debounced;
      },
      { initialProps: { value: "r" } },
    );
    for (const value of ["re", "rea", "reac", "react"]) {
      await act(async () => {
        await sleep(DELAY / 3);
      });
      rerender({ value });
    }
    await settle();
    assert.deepEqual(seen, ["r", "react"]);
  });
});

describe("useSearch", () => {
  test("a blank query is idle and never calls search", async () => {
    const { calls, search } = controlledSearch();
    const { result } = renderHook(() => useSearch("   ", search, DELAY));
    await settle();
    assert.deepEqual(result.current, { status: "idle" });
    assert.equal(calls.length, 0);
  });

  test("searches once for the trimmed, debounced query and reports the results", async () => {
    const { calls, search } = controlledSearch();
    const { result, rerender } = renderHook(({ query }) => useSearch(query, search, DELAY), {
      initialProps: { query: "" },
    });
    for (const query of ["r", "re", " rea "]) {
      rerender({ query });
    }
    await settle();
    assert.deepEqual(
      calls.map((call) => call.query),
      ["rea"],
    );
    assert.deepEqual(result.current, { status: "loading" });
    await act(async () => calls[0]?.resolve(["react", "reason"]));
    assert.deepEqual(result.current, { status: "success", results: ["react", "reason"] });
  });

  test("a newer query aborts the older search and its late result is ignored", async () => {
    const { calls, search } = controlledSearch();
    const { result, rerender } = renderHook(({ query }) => useSearch(query, search, DELAY), {
      initialProps: { query: "vue" },
    });
    await settle();
    rerender({ query: "react" });
    await settle();
    assert.equal(calls.length, 2);
    assert.equal(calls[0]?.signal.aborted, true);
    assert.equal(calls[1]?.signal.aborted, false);
    await act(async () => calls[1]?.resolve(["react"]));
    await act(async () => calls[0]?.resolve(["vue"]));
    assert.deepEqual(result.current, { status: "success", results: ["react"] });
  });

  test("a failed search reports its message", async () => {
    const { calls, search } = controlledSearch();
    const { result } = renderHook(() => useSearch("react", search, DELAY));
    await settle();
    await act(async () => calls[0]?.reject(new Error("Service unavailable")));
    assert.deepEqual(result.current, { status: "error", message: "Service unavailable" });
  });

  test("unmounting aborts the running search", async () => {
    const { calls, search } = controlledSearch();
    const { unmount } = renderHook(() => useSearch("react", search, DELAY));
    await settle();
    unmount();
    assert.equal(calls[0]?.signal.aborted, true);
  });

  test("a new search function on every render does not start new searches", async () => {
    const queries: string[] = [];
    const { rerender, result } = renderHook(
      ({ suffix }) =>
        useSearch(
          "react",
          async (query) => {
            queries.push(query);
            return [`${query}${suffix}`];
          },
          DELAY,
        ),
      { initialProps: { suffix: "!" } },
    );
    await settle();
    rerender({ suffix: "?" });
    rerender({ suffix: "#" });
    await settle();
    assert.deepEqual(queries, ["react"]);
    assert.deepEqual(result.current, { status: "success", results: ["react!"] });
  });
});

describe("SearchBox", () => {
  test("shows Searching…, then the results", async () => {
    const user = userEvent.setup();
    const { calls, search } = controlledSearch();
    render(<SearchBox search={search} delayMs={DELAY} />);
    await user.type(screen.getByLabelText("Search"), "hooks");
    const status = await screen.findByRole("status");
    assert.equal(status.textContent, "Searching…");
    await act(async () => calls[0]?.resolve(["useState", "useEffect"]));
    const items = screen.getAllByRole("listitem").map((item) => item.textContent);
    assert.deepEqual(items, ["useState", "useEffect"]);
    assert.equal(screen.queryByRole("status"), null);
  });

  test("shows No results and errors, and survives a re-rendering parent", async () => {
    const user = userEvent.setup();
    let calls = 0;
    function Parent() {
      const [clicks, setClicks] = useState(0);
      return (
        <>
          <button type="button" onClick={() => setClicks(clicks + 1)}>
            Re-render {clicks}
          </button>
          <SearchBox
            delayMs={DELAY}
            search={async (query) => {
              calls += 1;
              if (query === "boom") {
                throw new Error("Search is down");
              }
              return [];
            }}
          />
        </>
      );
    }
    render(<Parent />);
    await user.type(screen.getByLabelText("Search"), "zzz");
    assert.ok(await screen.findByText("No results"));
    await user.click(screen.getByRole("button", { name: /Re-render/ }));
    await user.click(screen.getByRole("button", { name: /Re-render/ }));
    await settle();
    assert.equal(calls, 1);
    await user.clear(screen.getByLabelText("Search"));
    await user.type(screen.getByLabelText("Search"), "boom");
    assert.equal((await screen.findByRole("alert")).textContent, "Search is down");
  });
});
