import {
  Component,
  startTransition,
  use,
  useId,
  useOptimistic,
  useRef,
  useState,
  useTransition,
  type FormEvent,
  type ReactNode,
} from "react";
import { STATUSES, label, useTickets } from "./data.tsx";
import type { Message, Status, TicketDetail } from "./helpdesk-types.ts";
import { Page } from "./Page.tsx";
import { Link } from "./router.tsx";

/** A ticket read with `use`: the Suspense boundary around the routes shows the loading state. */
export function TicketPage({ id }: { id: string }): ReactNode {
  const { forgetTicket } = useTickets();
  const [attempt, setAttempt] = useState(0);
  return (
    <TicketBoundary
      key={attempt}
      onRetry={() => {
        forgetTicket(id);
        setAttempt((n) => n + 1);
      }}
    >
      <TicketDetails id={id} />
    </TicketBoundary>
  );
}

type Shown = Message & { sending?: boolean };

function TicketDetails({ id }: { id: string }): ReactNode {
  const { ticket: load, api, rememberTicket, patchTicket } = useTickets();
  const [ticket, setTicket] = useState<TicketDetail>(use(load(id)));
  const latest = useRef(ticket);
  latest.current = ticket;

  const [statusFailed, setStatusFailed] = useState(false);
  const [draft, setDraft] = useState("");
  const [replyError, setReplyError] = useState<string | null>(null);
  const [replyFailed, setReplyFailed] = useState(false);
  const [sending, startSending] = useTransition();
  const [messages, addSending] = useOptimistic<readonly Shown[], Shown>(ticket.messages, (current, message) => [
    ...current,
    message,
  ]);
  const replyBox = useRef<HTMLTextAreaElement>(null);
  const ids = useId();

  function keep(next: TicketDetail) {
    latest.current = next;
    setTicket(next);
    rememberTicket(next);
  }

  async function changeStatus(status: Status) {
    const previous = latest.current.status;
    setStatusFailed(false);
    keep({ ...latest.current, status });
    patchTicket(id, { status });
    try {
      const saved = await api.setStatus(id, status);
      keep({ ...latest.current, ...saved, messages: latest.current.messages });
      patchTicket(id, saved);
    } catch {
      keep({ ...latest.current, status: previous });
      patchTicket(id, { status: previous });
      setStatusFailed(true);
    }
  }

  function sendReply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const body = draft.trim();
    if (body === "") {
      setReplyError("Write a reply first");
      replyBox.current?.focus();
      return;
    }
    setReplyError(null);
    setReplyFailed(false);
    setDraft("");
    startSending(async () => {
      addSending({ id: "sending", author: "You", body, sentAt: "", sending: true });
      try {
        const message = await api.reply(id, body);
        startTransition(() => keep({ ...latest.current, messages: [...latest.current.messages, message] }));
      } catch {
        startTransition(() => {
          setReplyFailed(true);
          setDraft(body);
        });
      }
    });
  }

  return (
    <Page title={ticket.title}>
      <dl>
        <dt>Status</dt>
        <dd>{label(ticket.status)}</dd>
        <dt>Priority</dt>
        <dd>{label(ticket.priority)}</dd>
        <dt>Requester</dt>
        <dd>{ticket.requester}</dd>
      </dl>
      <p>
        <label htmlFor={`${ids}-status`}>Change status</label>{" "}
        <select
          id={`${ids}-status`}
          value={ticket.status}
          onChange={(event) => void changeStatus(event.target.value as Status)}
        >
          {STATUSES.map((status) => (
            <option key={status} value={status}>
              {label(status)}
            </option>
          ))}
        </select>
      </p>
      {statusFailed ? <p role="alert">Could not change the status.</p> : null}

      <h2>Messages</h2>
      {messages.length === 0 ? (
        <p>No messages yet</p>
      ) : (
        <ol aria-label="Messages">
          {messages.map((message) => (
            <li key={message.id}>
              <p>
                <strong>{message.author}</strong>
              </p>
              <p>{message.body}</p>
              {message.sending ? <p>Sending…</p> : null}
            </li>
          ))}
        </ol>
      )}

      <form aria-label="Reply" onSubmit={sendReply} noValidate>
        <label htmlFor={`${ids}-reply`}>Your reply</label>
        <textarea
          id={`${ids}-reply`}
          ref={replyBox}
          value={draft}
          aria-invalid={replyError ? true : undefined}
          aria-describedby={replyError ? `${ids}-reply-error` : undefined}
          onChange={(event) => setDraft(event.target.value)}
        />
        {replyError ? <p id={`${ids}-reply-error`}>{replyError}</p> : null}
        <button type="submit" disabled={sending}>
          Send reply
        </button>
      </form>
      {replyFailed ? <p role="alert">Your reply was not sent.</p> : null}

      <p>
        <Link to="/tickets">All tickets</Link>
      </p>
    </Page>
  );
}

type BoundaryProps = { children: ReactNode; onRetry: () => void };

class TicketBoundary extends Component<BoundaryProps, { error: unknown }> {
  override state: { error: unknown } = { error: null };

  static getDerivedStateFromError(error: unknown) {
    return { error };
  }

  override render(): ReactNode {
    const { error } = this.state;
    if (error === null) {
      return this.props.children;
    }
    if (error instanceof Error && error.name === "NotFoundError") {
      return (
        <Page title="Ticket not found">
          <p>
            <Link to="/tickets">All tickets</Link>
          </p>
        </Page>
      );
    }
    return (
      <Page title="Ticket">
        <div role="alert">
          <p>Could not load this ticket.</p>
          <button type="button" onClick={this.props.onRetry}>
            Try again
          </button>
        </div>
      </Page>
    );
  }
}
