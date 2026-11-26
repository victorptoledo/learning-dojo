// Given: the shapes shared by the server code and the page. Do not change this file.

/** What the page may know about a comment: it crosses from the server to the browser. */
export type PublicComment = {
  readonly id: string;
  readonly author: string;
  readonly body: string;
};

export type Post = {
  readonly id: string;
  readonly title: string;
};

/** The signed-in user, as the server knows it from the session cookie. */
export type Session = {
  readonly userId: string;
  readonly name: string;
};

/** A row of the comments table: never send it to the browser as it is. */
export type CommentRow = {
  readonly id: string;
  readonly postId: string;
  readonly authorId: string;
  readonly authorName: string;
  readonly authorEmail: string;
  readonly body: string;
  /** ISO 8601. */
  readonly postedAt: string;
  /** ISO 8601, or null while the comment is visible. */
  readonly deletedAt: string | null;
};

/** A comment the server function has checked and may store. */
export type NewComment = {
  readonly postId: string;
  readonly authorId: string;
  readonly authorName: string;
  readonly body: string;
};

export type CheckResult = { ok: true; comment: NewComment } | { ok: false; error: string };
