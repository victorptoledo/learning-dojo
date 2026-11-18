import type { ReactNode } from "react";
import { TicketsGate, byUpdate, label } from "./data.tsx";
import { Page } from "./Page.tsx";
import { Link } from "./router.tsx";

export function TicketListPage(): ReactNode {
  return (
    <Page title="Tickets">
      <TicketsGate>
        {(tickets) => (
          <ul aria-label="Tickets">
            {byUpdate(tickets).map((ticket) => (
              <li key={ticket.id}>
                <Link to={`/tickets/${encodeURIComponent(ticket.id)}`}>{ticket.title}</Link>{" "}
                <span>{`${label(ticket.status)} · ${label(ticket.priority)} · ${ticket.requester}`}</span>
              </li>
            ))}
          </ul>
        )}
      </TicketsGate>
    </Page>
  );
}
