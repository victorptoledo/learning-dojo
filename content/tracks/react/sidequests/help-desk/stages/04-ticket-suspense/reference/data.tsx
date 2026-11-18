// The ticket list, loaded once and shared by every page, and one promise per ticket detail.
import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import type { HelpDeskApi, Status, Ticket, TicketDetail } from "./helpdesk-types.ts";

type TicketsState =
  | { status: "loading" }
  | { status: "error" }
  | { status: "ready"; tickets: readonly Ticket[] };

type TicketsValue = {
  state: TicketsState;
  retry: () => void;
  api: HelpDeskApi;
  /** The same promise for an id every time (what `use` needs), until it is forgotten. */
  ticket: (id: string) => Promise<TicketDetail>;
  forgetTicket: (id: string) => void;
};

const TicketsContext = createContext<TicketsValue | null>(null);

export function TicketsProvider({ api, children }: { api: HelpDeskApi; children: ReactNode }): ReactNode {
  const [state, setState] = useState<TicketsState>({ status: "loading" });
  const [attempt, setAttempt] = useState(0);
  const details = useRef(new Map<string, Promise<TicketDetail>>());

  const ticket = useCallback(
    (id: string) => {
      let promise = details.current.get(id);
      if (!promise) {
        promise = api.getTicket(id);
        details.current.set(id, promise);
      }
      return promise;
    },
    [api],
  );
  const forgetTicket = useCallback((id: string) => {
    details.current.delete(id);
  }, []);

  useEffect(() => {
    let live = true;
    setState({ status: "loading" });
    api.listTickets().then(
      (tickets) => live && setState({ status: "ready", tickets }),
      () => live && setState({ status: "error" }),
    );
    return () => {
      live = false;
    };
  }, [api, attempt]);

  return (
    <TicketsContext.Provider value={{ state, retry: () => setAttempt((n) => n + 1), api, ticket, forgetTicket }}>
      {children}
    </TicketsContext.Provider>
  );
}

export function useTickets(): TicketsValue {
  const value = useContext(TicketsContext);
  if (!value) {
    throw new Error("useTickets must be used inside a TicketsProvider");
  }
  return value;
}

/** Shows the loading and error states of the list, and `children` once it is there. */
export function TicketsGate({ children }: { children: (tickets: readonly Ticket[]) => ReactNode }): ReactNode {
  const { state, retry } = useTickets();
  if (state.status === "loading") {
    return <p>Loading tickets</p>;
  }
  if (state.status === "error") {
    return (
      <div role="alert">
        <p>Could not load the tickets.</p>
        <button type="button" onClick={retry}>
          Try again
        </button>
      </div>
    );
  }
  return children(state.tickets);
}

export const STATUSES: readonly Status[] = ["open", "pending", "closed"];

export const label = (word: string) => word.charAt(0).toUpperCase() + word.slice(1);

/** Newest first. */
export function byUpdate(tickets: readonly Ticket[]): Ticket[] {
  return [...tickets].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt) || a.id.localeCompare(b.id));
}
