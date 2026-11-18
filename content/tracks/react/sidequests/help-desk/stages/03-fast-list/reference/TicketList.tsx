import { memo, useDeferredValue, useMemo, useState, type ReactNode } from "react";
import { STATUSES, TicketsGate, byUpdate, label } from "./data.tsx";
import type { Status, Ticket } from "./helpdesk-types.ts";
import { Page } from "./Page.tsx";
import { trackRender } from "./renderLog.ts";
import { Link, useLocation, useNavigate } from "./router.tsx";

const PAGE_SIZE = 100;

type Filters = { q: string; status: Status | null };

function readFilters(search: string): Filters {
  const params = new URLSearchParams(search);
  const status = params.get("status");
  return {
    q: params.get("q") ?? "",
    status: STATUSES.includes(status as Status) ? (status as Status) : null,
  };
}

function urlOf(filters: Filters): string {
  const params = new URLSearchParams();
  if (filters.q !== "") {
    params.set("q", filters.q);
  }
  if (filters.status) {
    params.set("status", filters.status);
  }
  const query = params.toString();
  return query === "" ? "/tickets" : `/tickets?${query}`;
}

/** The tickets, filtered by the search text and the status kept in the URL. */
export function TicketListPage(): ReactNode {
  const { search } = useLocation();
  const navigate = useNavigate();
  const filters = readFilters(search);
  // Filters are details of the same page: they replace the history entry instead of adding one.
  const update = (change: Partial<Filters>) => navigate(urlOf({ ...filters, ...change }), { replace: true });

  return (
    <Page title="Tickets">
      <label>
        Search tickets{" "}
        <input type="search" value={filters.q} onChange={(event) => update({ q: event.target.value })} />
      </label>
      <div role="group" aria-label="Status">
        <button type="button" aria-pressed={filters.status === null} onClick={() => update({ status: null })}>
          All
        </button>
        {STATUSES.map((status) => (
          <button
            key={status}
            type="button"
            aria-pressed={filters.status === status}
            onClick={() => update({ status })}
          >
            {label(status)}
          </button>
        ))}
      </div>
      <TicketsGate>{(tickets) => <Results tickets={tickets} filters={filters} />}</TicketsGate>
    </Page>
  );
}

function Results({ tickets, filters }: { tickets: readonly Ticket[]; filters: Filters }): ReactNode {
  const sorted = useMemo(() => byUpdate(tickets), [tickets]);
  // Typing stays responsive: the list catches up with the text in a background render.
  const q = useDeferredValue(filters.q.trim().toLowerCase());
  const matches = useMemo(
    () =>
      sorted.filter(
        (ticket) =>
          (filters.status === null || ticket.status === filters.status) &&
          (q === "" || ticket.title.toLowerCase().includes(q) || ticket.requester.toLowerCase().includes(q)),
      ),
    [sorted, q, filters.status],
  );
  const key = `${q}|${filters.status ?? ""}`;
  const [shown, setShown] = useState({ key, limit: PAGE_SIZE });
  if (shown.key !== key) {
    // New filters: back to the first page (a state adjustment during render, not an effect).
    setShown({ key, limit: PAGE_SIZE });
  }
  const limit = shown.key === key ? shown.limit : PAGE_SIZE;
  const rows = matches.slice(0, limit);

  if (matches.length === 0) {
    return <p>No tickets match</p>;
  }
  return (
    <>
      <p>{`Showing ${rows.length} of ${matches.length} tickets`}</p>
      <ul aria-label="Tickets">
        {rows.map((ticket) => (
          <TicketRow key={ticket.id} ticket={ticket} />
        ))}
      </ul>
      {rows.length < matches.length ? (
        <button type="button" onClick={() => setShown({ key, limit: limit + PAGE_SIZE })}>
          Show more
        </button>
      ) : null}
    </>
  );
}

const TicketRow = memo(function TicketRow({ ticket }: { ticket: Ticket }): ReactNode {
  trackRender(`row:${ticket.id}`);
  return (
    <li>
      <Link to={`/tickets/${encodeURIComponent(ticket.id)}`}>{ticket.title}</Link>{" "}
      <span>{`${label(ticket.status)} · ${label(ticket.priority)} · ${ticket.requester}`}</span>
    </li>
  );
});
