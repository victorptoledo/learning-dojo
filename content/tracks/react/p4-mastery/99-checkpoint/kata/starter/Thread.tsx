import type { ReactNode } from "react";
import type { Post, PublicComment } from "./types.ts";

export type ThreadProps = {
  posts: readonly Post[];
  loadComments: (postId: string) => Promise<readonly PublicComment[]>;
  sendComment: (postId: string, body: string) => Promise<PublicComment>;
  /** Error monitoring: every unexpected error goes here. */
  report: (error: Error) => void;
};

/** The posts, the comments of the current post and the form for a new comment (see README.md). */
export function Thread(_props: ThreadProps): ReactNode {
  throw new Error("todo: write Thread");
}
