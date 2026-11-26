import assert from "node:assert/strict";
import { after, afterEach, before, describe, test } from "node:test";
import { act, render, screen, within } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { ErrorBoundary } from "../ErrorBoundary.tsx";
import { checkNewComment, toPublicComments } from "../server.ts";
import { Thread } from "../Thread.tsx";
import type { CommentRow, Post, PublicComment } from "../types.ts";

// React logs every error an error boundary catches; keep the test output readable.
const originalError = console.error;
before(() => {
  console.error = () => {};
});
after(() => {
  console.error = originalError;
});

const POSTS: readonly Post[] = [
  { id: "p1", title: "Why keys matter" },
  { id: "p2", title: "Suspense in practice" },
];

const COMMENTS: Record<string, readonly PublicComment[]> = {
  p1: [
    { id: "c1", author: "Ana", body: "Finally it clicks" },
    { id: "c2", author: "Bo", body: "Index keys bit me last week" },
  ],
  p2: [{ id: "c3", author: "Cy", body: "Transitions are underrated" }],
};

type User = ReturnType<typeof userEvent.setup>;
type Pending<T> = { answer(value: T): Promise<void>; fail(error?: Error): Promise<void> };

// Calls a test did not answer. React 19 ties later transitions to an async action that is still
// running, so one left hanging would freeze the next tests: each test fails what is left.
const unanswered = new Set<() => void>();
afterEach(async () => {
  const left = [...unanswered];
  unanswered.clear();
  await act(async () => {
    for (const drop of left) {
      drop();
    }
  });
});

function pending<T>(push: (call: Pending<T>) => void): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const drop = () => reject(new Error("the test ended before this call was answered"));
    unanswered.add(drop);
    push({
      answer: (value) =>
        act(async () => {
          unanswered.delete(drop);
          resolve(value);
        }),
      fail: (error = new Error("HTTP 500")) =>
        act(async () => {
          unanswered.delete(drop);
          reject(error);
        }),
    });
  });
}

/** A fake server: every call waits until the test answers or fails it. */
function fakeServer() {
  const loads: (Pending<readonly PublicComment[]> & { postId: string })[] = [];
  const sends: (Pending<PublicComment> & { postId: string; body: string })[] = [];
  const reported: Error[] = [];
  return {
    loads,
    sends,
    reported,
    loadComments: (postId: string) => pending<readonly PublicComment[]>((call) => loads.push({ ...call, postId })),
    sendComment: (postId: string, body: string) =>
      pending<PublicComment>((call) => sends.push({ ...call, postId, body })),
    report: (error: Error) => {
      reported.push(error);
    },
  };
}

/** Lets pending promises, effects and transitions finish. */
async function settle(): Promise<void> {
  for (let round = 0; round < 3; round++) {
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
  }
}

/** Renders the thread inside an awaited act: in React 19 a tree that suspends needs it. */
async function start(server = fakeServer()) {
  const user = userEvent.setup();
  await act(async () => {
    render(
      <Thread posts={POSTS} loadComments={server.loadComments} sendComment={server.sendComment} report={server.report} />,
    );
  });
  return { server, user };
}

/** Starts and answers the first load with the comments of p1. */
async function started() {
  const { server, user } = await start();
  await server.loads[0]?.answer(COMMENTS.p1 ?? []);
  await settle();
  return { server, user };
}

async function press(user: User, element: Element): Promise<void> {
  await act(async () => {
    await user.click(element);
  });
  await settle();
}

function comments(): string[] {
  const list = screen.queryByRole("list", { name: "Comments" });
  if (!list) {
    return [];
  }
  return within(list)
    .getAllByRole("listitem")
    .map((item) => (item.textContent ?? "").replace(/\s+/g, " ").trim());
}

const discussion = () => screen.getByRole("region", { name: "Discussion" });
const postButton = (title: string) =>
  within(screen.getByRole("navigation", { name: "Posts" })).getByRole("button", { name: title });
const box = () => screen.getByRole("textbox", { name: "Comment" }) as HTMLTextAreaElement;
const postComment = () => screen.getByRole("button", { name: "Post comment" }) as HTMLButtonElement;

async function write(user: User, text: string): Promise<void> {
  await user.clear(box());
  if (text !== "") {
    await user.type(box(), text);
  }
  await press(user, postComment());
}

function nameOf(element: Element | null): string {
  if (!element || element === document.body) {
    return "nothing (the page itself)";
  }
  return `<${element.tagName.toLowerCase()}> "${(element.getAttribute("aria-label") ?? element.textContent ?? "").trim().slice(0, 60)}"`;
}

// Compare elements with these, not assert.equal: a failing assert.equal prints the whole document.
function expectAbsent(element: Element | null, context?: string): void {
  if (element !== null) {
    assert.fail(`${context ? `${context}: ` : ""}${nameOf(element)} should not be on the page`);
  }
}

function expectFocus(element: Element): void {
  if (document.activeElement !== element) {
    assert.fail(`the focus should be on ${nameOf(element)}, but it is on ${nameOf(document.activeElement)}`);
  }
}

function row(id: string, extra: Partial<CommentRow> = {}): CommentRow {
  return {
    id,
    postId: "p1",
    authorId: `u-${id}`,
    authorName: `Name ${id}`,
    authorEmail: `${id}@example.com`,
    body: `Body ${id}`,
    postedAt: "2026-03-01T10:00:00Z",
    deletedAt: null,
    ...extra,
  };
}

function form(fields: Record<string, string>): FormData {
  const data = new FormData();
  for (const [name, value] of Object.entries(fields)) {
    data.set(name, value);
  }
  return data;
}

const ANA = { userId: "u7", name: "Ana" };
const POST_IDS = new Set(["p1", "p2"]);

describe("the server side", () => {
  test("checkNewComment refuses anonymous users, unknown posts, and empty or long bodies", () => {
    const ok = form({ postId: "p1", body: "Hello" });
    assert.deepEqual(checkNewComment(null, ok, POST_IDS), { ok: false, error: "Sign in to comment" });
    assert.deepEqual(checkNewComment(ANA, form({ postId: "p9", body: "Hello" }), POST_IDS), {
      ok: false,
      error: "Unknown post",
    });
    assert.deepEqual(checkNewComment(ANA, form({ body: "Hello" }), POST_IDS), { ok: false, error: "Unknown post" });
    assert.deepEqual(checkNewComment(ANA, form({ postId: "p1", body: "   " }), POST_IDS), {
      ok: false,
      error: "Write a comment first",
    });
    assert.deepEqual(checkNewComment(ANA, form({ postId: "p1" }), POST_IDS), {
      ok: false,
      error: "Write a comment first",
    });
    assert.equal(checkNewComment(ANA, form({ postId: "p1", body: ` ${"x".repeat(280)} ` }), POST_IDS).ok, true);
    assert.deepEqual(checkNewComment(ANA, form({ postId: "p1", body: "x".repeat(281) }), POST_IDS), {
      ok: false,
      error: "Comments are at most 280 characters",
    });
  });

  test("checkNewComment takes the author from the session, never from the form", () => {
    const forged = form({ postId: "p2", body: "Hi", authorId: "admin", authorName: "The Admin" });
    assert.deepEqual(checkNewComment(ANA, forged, POST_IDS), {
      ok: true,
      comment: { postId: "p2", authorId: "u7", authorName: "Ana", body: "Hi" },
    });
  });

  test("toPublicComments sends only id, author and body, oldest first, without deleted comments", () => {
    const rows = [
      row("late", { postedAt: "2026-03-01T23:45:00Z" }),
      row("gone", { postedAt: "2026-03-01T08:00:00Z", deletedAt: "2026-03-01T09:00:00Z" }),
      row("early", { postedAt: "2026-03-02T00:30:00+01:00" }),
    ];
    assert.deepEqual(toPublicComments(rows), [
      { id: "early", author: "Name early", body: "Body early" },
      { id: "late", author: "Name late", body: "Body late" },
    ]);
    assert.equal(rows[0]?.id, "late", "the rows are not reordered in place");
  });
});

describe("ErrorBoundary", () => {
  function Bomb(): never {
    throw new Error("boom");
  }

  test("shows the fallback and reports the error once", async () => {
    const reported: Error[] = [];
    await act(async () => {
      render(
        <ErrorBoundary fallback={(error) => <p>Failed: {error.message}</p>} onError={(error) => reported.push(error)}>
          <Bomb />
        </ErrorBoundary>,
      );
    });
    assert.ok(screen.getByText("Failed: boom"));
    assert.deepEqual(
      reported.map((error) => error.message),
      ["boom"],
    );
  });
});

describe("the thread", () => {
  test("shows a loading status, then the comments of the first post", async () => {
    const { server } = await start();
    assert.ok(screen.getByRole("status"));
    assert.equal(screen.getByRole("status").textContent, "Loading comments…");
    assert.deepEqual(
      server.loads.map((call) => call.postId),
      ["p1"],
    );
    await server.loads[0]?.answer(COMMENTS.p1 ?? []);
    await settle();
    assert.deepEqual(comments(), ["Ana: Finally it clicks", "Bo: Index keys bit me last week"]);
    assert.equal(postButton("Why keys matter").getAttribute("aria-pressed"), "true");
    expectAbsent(screen.queryByRole("status"));
  });

  test("switching posts keeps the old comments on screen, marked busy, until the new ones arrive", async () => {
    const { server, user } = await started();
    await press(user, postButton("Suspense in practice"));
    assert.deepEqual(comments(), ["Ana: Finally it clicks", "Bo: Index keys bit me last week"]);
    assert.equal(discussion().getAttribute("aria-busy"), "true");
    expectAbsent(screen.queryByRole("status"), "no loading fallback during the switch");
    await server.loads[1]?.answer(COMMENTS.p2 ?? []);
    await settle();
    assert.deepEqual(comments(), ["Cy: Transitions are underrated"]);
    assert.equal(discussion().getAttribute("aria-busy"), "false");
    assert.equal(postButton("Suspense in practice").getAttribute("aria-pressed"), "true");
  });

  test("a post that was loaded once is not loaded again", async () => {
    const { server, user } = await started();
    await press(user, postButton("Suspense in practice"));
    await server.loads[1]?.answer([]);
    await settle();
    assert.ok(screen.getByText("No comments yet"));
    await press(user, postButton("Why keys matter"));
    assert.deepEqual(comments(), ["Ana: Finally it clicks", "Bo: Index keys bit me last week"]);
    assert.equal(server.loads.length, 2);
  });

  test("a failed load is contained, reported once, and Try again loads it again", async () => {
    const { server, user } = await started();
    await press(user, postButton("Suspense in practice"));
    await server.loads[1]?.fail(new Error("HTTP 503"));
    await settle();
    const alert = screen.getByRole("alert");
    assert.ok(within(alert).getByText("Could not load comments"));
    assert.deepEqual(
      server.reported.map((error) => error.message),
      ["HTTP 503"],
    );
    assert.ok(postButton("Why keys matter"), "the posts stay usable");

    await press(user, within(alert).getByRole("button", { name: "Try again" }));
    assert.equal(server.loads.length, 3);
    assert.equal(server.loads[2]?.postId, "p2");
    await server.loads[2]?.answer(COMMENTS.p2 ?? []);
    await settle();
    assert.deepEqual(comments(), ["Cy: Transitions are underrated"]);
    expectAbsent(screen.queryByRole("alert"));
  });

  test("after a failed load, another post still opens", async () => {
    const { server, user } = await started();
    await press(user, postButton("Suspense in practice"));
    await server.loads[1]?.fail();
    await settle();
    await press(user, postButton("Why keys matter"));
    expectAbsent(screen.queryByRole("alert"));
    assert.deepEqual(comments(), ["Ana: Finally it clicks", "Bo: Index keys bit me last week"]);
  });

  test("an empty comment is not sent: the field says why and keeps the focus", async () => {
    const { server, user } = await started();
    await write(user, "   ");
    assert.equal(server.sends.length, 0);
    assert.equal(box().getAttribute("aria-invalid"), "true");
    const described = box().getAttribute("aria-describedby") ?? "";
    assert.equal(document.getElementById(described)?.textContent?.trim(), "Write a comment first");
    expectFocus(box());
  });

  test("a comment shows at once, marked as sending, and the box is emptied", async () => {
    const { server, user } = await started();
    await write(user, "  Great read  ");
    assert.equal(server.sends.length, 1);
    assert.equal(server.sends[0]?.postId, "p1");
    assert.equal(server.sends[0]?.body, "Great read");
    assert.deepEqual(comments(), [
      "Ana: Finally it clicks",
      "Bo: Index keys bit me last week",
      "You: Great read (sending)",
    ]);
    assert.equal(box().value, "");
    assert.equal(postComment().disabled, true, "one comment at a time");
  });

  test("once stored, the comment is the one the server answered, and it stays after switching posts", async () => {
    const { server, user } = await started();
    await write(user, "Great read");
    await server.sends[0]?.answer({ id: "c9", author: "Ana", body: "Great read" });
    await settle();
    assert.deepEqual(comments(), ["Ana: Finally it clicks", "Bo: Index keys bit me last week", "Ana: Great read"]);
    assert.equal(postComment().disabled, false);

    await press(user, postButton("Suspense in practice"));
    await server.loads[1]?.answer(COMMENTS.p2 ?? []);
    await settle();
    await press(user, postButton("Why keys matter"));
    assert.deepEqual(comments(), ["Ana: Finally it clicks", "Bo: Index keys bit me last week", "Ana: Great read"]);
    assert.equal(server.loads.length, 2);
  });

  test("a failed comment disappears, is reported, says so, and gives the text back", async () => {
    const { server, user } = await started();
    await write(user, "Great read");
    await server.sends[0]?.fail(new Error("HTTP 500"));
    await settle();
    assert.deepEqual(comments(), ["Ana: Finally it clicks", "Bo: Index keys bit me last week"]);
    const form = screen.getByRole("form", { name: "New comment" });
    assert.equal((within(form).getByRole("alert").textContent ?? "").trim(), "Could not post your comment. Try again.");
    assert.equal(box().value, "Great read");
    assert.equal(postComment().disabled, false);
    assert.deepEqual(
      server.reported.map((error) => error.message),
      ["HTTP 500"],
    );
  });
});
