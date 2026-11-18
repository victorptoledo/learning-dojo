import type { ReactNode } from "react";
import { TicketsGate, label } from "./data.tsx";
import { Page } from "./Page.tsx";
import { Link } from "./router.tsx";

export function TicketPage({ id }: { id: string }): ReactNode {
  return (
    <TicketsGate>
      {(tickets) => {
        const ticket = tickets.find((candidate) => candidate.id === id);
        if (!ticket) {
          return (
            <Page title="Ticket not found">
              <p>
                <Link to="/tickets">All tickets</Link>
              </p>
            </Page>
          );
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
              <Link to="/tickets">All tickets</Link>
            </p>
          </Page>
        );
      }}
    </TicketsGate>
  );
}
