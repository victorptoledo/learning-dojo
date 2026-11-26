import {
  startTransition,
  Suspense,
  use,
  useCallback,
  useId,
  useOptimistic,
  useRef,
  useState,
  useTransition,
  type FormEvent,
  type ReactNode,
} from "react";
import { createResourceCache } from "./cache.ts";
import { ErrorBoundary } from "./ErrorBoundary.tsx";
import type { Post, PublicComment } from "./types.ts";

export type ThreadProps = {
  posts: readonly Post[];
  loadComments: (postId: string) => Promise<readonly PublicComment[]>;
  sendComment: (postId: string, body: string) => Promise<PublicComment>;
  /** Error monitoring: every unexpected error goes here. */
  report: (error: Error) => void;
};

function asError(error: unknown): Error {
  return error instanceof Error ? error : new Error(String(error));
}

/** The posts, the comments of the current post and the form for a new comment (see README.md). */
export function Thread({ posts, loadComments, sendComment, report }: ThreadProps): ReactNode {
  // One cache for the life of the page: the promises must survive renders, or use() would
  // suspend forever on a new promise each time.
  const [cache] = useState(() => createResourceCache(loadComments));
  const [postId, setPostId] = useState(posts[0]?.id ?? "");
  const [isSwitching, startSwitch] = useTransition();
  // Bumped by Try again: the page must render again to read a fresh promise from the cache.
  const [, setAttempt] = useState(0);
  // Comments the server stored while the page is open, per post, after the loaded ones.
  const [added, setAdded] = useState<Readonly<Record<string, readonly PublicComment[]>>>({});

  const keep = useCallback((post: string, comment: PublicComment) => {
    setAdded((current) => ({ ...current, [post]: [...(current[post] ?? []), comment] }));
  }, []);

  return (
    <div>
      <nav aria-label="Posts">
        {posts.map((post) => (
          <button
            key={post.id}
            type="button"
            aria-pressed={post.id === postId}
            onClick={() => startSwitch(() => setPostId(post.id))}
          >
            {post.title}
          </button>
        ))}
      </nav>
      <section aria-label="Discussion" aria-busy={isSwitching}>
        <ErrorBoundary
          resetKey={postId}
          onError={report}
          fallback={(_error, reset) => (
            <div role="alert">
              <p>Could not load comments</p>
              <button
                type="button"
                onClick={() => {
                  cache.evict(postId);
                  setAttempt((attempt) => attempt + 1);
                  reset();
                }}
              >
                Try again
              </button>
            </div>
          )}
        >
          <Suspense fallback={<p role="status">Loading comments…</p>}>
            <Comments
              postId={postId}
              loaded={cache.read(postId)}
              added={added[postId] ?? NONE}
              sendComment={sendComment}
              onStored={keep}
              report={report}
            />
          </Suspense>
        </ErrorBoundary>
      </section>
    </div>
  );
}

const NONE: readonly PublicComment[] = [];

type Shown = PublicComment & { sending?: true };

type CommentsProps = {
  postId: string;
  loaded: Promise<readonly PublicComment[]>;
  added: readonly PublicComment[];
  sendComment: ThreadProps["sendComment"];
  onStored: (postId: string, comment: PublicComment) => void;
  report: (error: Error) => void;
};

function Comments({ postId, loaded, added, sendComment, onStored, report }: CommentsProps) {
  const confirmed = [...use(loaded), ...added];
  const [shown, addSending] = useOptimistic<readonly Shown[], Shown>(confirmed, (current, comment) => [
    ...current,
    comment,
  ]);
  const [isSending, startSending] = useTransition();
  const [draft, setDraft] = useState("");
  const [invalid, setInvalid] = useState(false);
  const [failed, setFailed] = useState(false);
  const box = useRef<HTMLTextAreaElement>(null);
  const boxId = useId();
  const hintId = useId();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const body = draft.trim();
    setFailed(false);
    if (body === "") {
      setInvalid(true);
      box.current?.focus();
      return;
    }
    setInvalid(false);
    setDraft("");
    startSending(async () => {
      addSending({ id: "sending", author: "You", body, sending: true });
      try {
        const stored = await sendComment(postId, body);
        // After an await, updates must be wrapped again to belong to the action: then the
        // stored comment replaces the optimistic one in the same commit.
        startTransition(() => onStored(postId, stored));
      } catch (error) {
        report(asError(error));
        startTransition(() => {
          setFailed(true);
          setDraft(body);
        });
      }
    });
  }

  return (
    <>
      {shown.length === 0 ? (
        <p>No comments yet</p>
      ) : (
        <ul aria-label="Comments">
          {shown.map((comment) => (
            <li key={comment.id}>
              <strong>{comment.author}</strong>: {comment.body}
              {comment.sending && " (sending)"}
            </li>
          ))}
        </ul>
      )}
      <form aria-label="New comment" onSubmit={submit} noValidate>
        <label htmlFor={boxId}>Comment</label>
        <textarea
          id={boxId}
          ref={box}
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          aria-invalid={invalid ? true : undefined}
          aria-describedby={invalid ? hintId : undefined}
        />
        {invalid && <p id={hintId}>Write a comment first</p>}
        {failed && <p role="alert">Could not post your comment. Try again.</p>}
        <button type="submit" disabled={isSending}>
          Post comment
        </button>
      </form>
    </>
  );
}
