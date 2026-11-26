// The server side of the thread. In a framework these functions would sit behind 'use server'
// and in a Server Component; here they are plain functions the tests call directly.
import type { CheckResult, CommentRow, PublicComment, Session } from "./types.ts";

export const MAX_COMMENT_LENGTH = 280;

/** Checks a submitted "new comment" form before anything is stored (see README.md). */
export function checkNewComment(
  session: Session | null,
  form: FormData,
  postIds: ReadonlySet<string>,
): CheckResult {
  // A server function is a public endpoint: anyone can call it with any form, so every check
  // happens here, whatever the page already checked.
  if (session === null) {
    return { ok: false, error: "Sign in to comment" };
  }
  const postId = form.get("postId");
  if (typeof postId !== "string" || !postIds.has(postId)) {
    return { ok: false, error: "Unknown post" };
  }
  const raw = form.get("body");
  const body = typeof raw === "string" ? raw.trim() : "";
  if (body === "") {
    return { ok: false, error: "Write a comment first" };
  }
  if (body.length > MAX_COMMENT_LENGTH) {
    return { ok: false, error: `Comments are at most ${MAX_COMMENT_LENGTH} characters` };
  }
  // The author comes from the session, never from fields the client sent.
  return { ok: true, comment: { postId, authorId: session.userId, authorName: session.name, body } };
}

/** The rows a Server Component may pass to the browser, oldest first (see README.md). */
export function toPublicComments(rows: readonly CommentRow[]): PublicComment[] {
  return rows
    .filter((row) => row.deletedAt === null)
    .toSorted((a, b) => Date.parse(a.postedAt) - Date.parse(b.postedAt))
    // Pick the public fields one by one: spreading the row would send the e-mail along.
    .map((row) => ({ id: row.id, author: row.authorName, body: row.body }));
}
