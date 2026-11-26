// The server side of the thread. In a framework these functions would sit behind 'use server'
// and in a Server Component; here they are plain functions the tests call directly.
import type { CheckResult, CommentRow, PublicComment, Session } from "./types.ts";

export const MAX_COMMENT_LENGTH = 280;

/** Checks a submitted "new comment" form before anything is stored (see README.md). */
export function checkNewComment(
  _session: Session | null,
  _form: FormData,
  _postIds: ReadonlySet<string>,
): CheckResult {
  throw new Error("todo: write checkNewComment");
}

/** The rows a Server Component may pass to the browser, oldest first (see README.md). */
export function toPublicComments(_rows: readonly CommentRow[]): PublicComment[] {
  throw new Error("todo: write toPublicComments");
}
