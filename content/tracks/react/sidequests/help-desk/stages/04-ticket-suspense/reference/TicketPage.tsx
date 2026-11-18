import { Component, use, useState, type ReactNode } from "react";
import { label, useTickets } from "./data.tsx";
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

function TicketDetails({ id }: { id: string }): ReactNode {
  const { ticket: load } = useTickets();
  const ticket = use(load(id));
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
      <h2>Messages</h2>
      {ticket.messages.length === 0 ? (
        <p>No messages yet</p>
      ) : (
        <ol aria-label="Messages">
          {ticket.messages.map((message) => (
            <li key={message.id}>
              <p>
                <strong>{message.author}</strong>
              </p>
              <p>{message.body}</p>
            </li>
          ))}
        </ol>
      )}
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
