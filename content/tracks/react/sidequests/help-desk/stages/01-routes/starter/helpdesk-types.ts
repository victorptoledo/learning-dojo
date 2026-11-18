// The contract between the app and the help desk API. The tests pass their own `HelpDeskApi`,
// so keep these types as they are; everything else is yours.

export type Status = "open" | "pending" | "closed";
export type Priority = "low" | "normal" | "high";

export type Ticket = {
  id: string;
  title: string;
  status: Status;
  priority: Priority;
  requester: string;
  /** ISO 8601, e.g. "2026-11-02T14:05:00Z". */
  updatedAt: string;
};

export type Message = { id: string; author: string; body: string; sentAt: string };

export type TicketDetail = Ticket & { messages: readonly Message[] };

/** What `getTicket` rejects with when there is no ticket with that id. */
export class NotFoundError extends Error {
  override name = "NotFoundError";
}

export type HelpDeskApi = {
  /** Every ticket, in no particular order. */
  listTickets(): Promise<readonly Ticket[]>;
  /** Used from stage 4. Rejects with a NotFoundError for an unknown id. */
  getTicket(id: string): Promise<TicketDetail>;
  /** Used from stage 5: sends a reply and answers the message as stored. */
  reply(id: string, body: string): Promise<Message>;
  /** Used from stage 5: changes the status and answers the ticket as stored. */
  setStatus(id: string, status: Status): Promise<Ticket>;
};
