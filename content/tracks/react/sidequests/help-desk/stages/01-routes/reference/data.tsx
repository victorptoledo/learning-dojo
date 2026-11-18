// The ticket list, loaded once and shared by every page.
import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import type { HelpDeskApi, Status, Ticket } from "./helpdesk-types.ts";

type TicketsState =
  | { status: "loading" }
  | { status: "error" }
  | { status: "ready"; tickets: readonly Ticket[] };

type TicketsValue = { state: TicketsState; retry: () => void; api: HelpDeskApi };

const TicketsContext = createContext<TicketsValue | null>(null);

export function TicketsProvider({ api, children }: { api: HelpDeskApi; children: ReactNode }): ReactNode {
  const [state, setState] = useState<TicketsState>({ status: "loading" });
  const [attempt, setAttempt] = useState(0);

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
    <TicketsContext.Provider value={{ state, retry: () => setAttempt((n) => n + 1), api }}>
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
