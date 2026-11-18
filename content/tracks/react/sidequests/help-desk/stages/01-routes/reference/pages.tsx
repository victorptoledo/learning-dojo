import type { ReactNode } from "react";
import { STATUSES, TicketsGate, label } from "./data.tsx";
import { Page } from "./Page.tsx";
import { Link } from "./router.tsx";

export function OverviewPage(): ReactNode {
  return (
    <Page title="Overview">
      <TicketsGate>
        {(tickets) => (
          <ul aria-label="Tickets by status">
            {STATUSES.map((status) => (
              <li key={status}>{`${label(status)}: ${tickets.filter((ticket) => ticket.status === status).length}`}</li>
            ))}
          </ul>
        )}
      </TicketsGate>
    </Page>
  );
}

export function NotFoundPage(): ReactNode {
  return (
    <Page title="Page not found">
      <p>
        <Link to="/">Go to the overview</Link>
      </p>
    </Page>
  );
}
