// Helpers shared by the contract tests of every stage: a fake HelpDeskApi and readers that see
// the app only through roles, names, visible text and the address bar.
import assert from "node:assert/strict";
import { afterEach } from "node:test";
import { act, render, screen, within } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { HelpDesk } from "../HelpDesk.tsx";
import {
  NotFoundError,
  type HelpDeskApi,
  type Message,
  type Status,
  type Ticket,
  type TicketDetail,
} from "../helpdesk-types.ts";

export type User = ReturnType<typeof userEvent.setup>;

/** Ticket n; a higher n was updated later. */
export function ticket(n: number, extra: Partial<Ticket> = {}): Ticket {
  return {
    id: `t${n}`,
    title: `Ticket ${n}`,
    status: "open",
    priority: "normal",
    requester: "Ana",
    updatedAt: new Date(Date.UTC(2026, 0, 1) + n * 60_000).toISOString(),
    ...extra,
  };
}

export function sampleTickets(): Ticket[] {
  return [
    ticket(1, { title: "Printer on fire", status: "closed", priority: "high", requester: "Bia" }),
    ticket(2, { title: "Cannot log in", status: "pending", requester: "Caio" }),
    ticket(3, { title: "VPN is slow", priority: "high" }),
    ticket(4, { title: "New laptop", status: "open", priority: "low", requester: "Dora" }),
  ];
}

type Pending<T> = { answer(value: T): Promise<void>; fail(error?: Error): Promise<void> };
export type ListCall = Pending<readonly Ticket[]>;
export type TicketCall = Pending<TicketDetail> & { id: string };
export type ReplyCall = Pending<Message> & { id: string; body: string };
export type StatusCall = Pending<Ticket> & { id: string; status: Status };

// Calls a test did not answer. React 19 ties every later transition to an async action that
// is still running, so one left hanging would freeze the navigation of the next tests: each
// test ends by failing whatever is still waiting.
const unanswered = new Set<() => void>();

afterEach(async () => {
  const left = [...unanswered];
  unanswered.clear();
  await act(async () => {
    for (const fail of left) {
      fail();
    }
  });
});

function pending<T>(push: (call: Pending<T>) => void): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const drop = () => reject(new Error("the test ended before this call was answered"));
    unanswered.add(drop);
    push({
      answer: (value) =>
        act(async () => {
          unanswered.delete(drop);
          resolve(value);
        }),
      fail: (error = new Error("HTTP 500")) =>
        act(async () => {
          unanswered.delete(drop);
          reject(error);
        }),
    });
  });
}

/**
 * A fake API over `tickets`. listTickets and getTicket answer at once, unless `manual` names
 * them: then each call waits in `listCalls` / `ticketCalls` until the test answers it.
 * reply and setStatus always wait.
 */
export function fakeApi(
  tickets: readonly Ticket[] = sampleTickets(),
  options: { manual?: ("list" | "ticket")[]; messages?: Record<string, readonly Message[]> } = {},
) {
  const listCalls: ListCall[] = [];
  const ticketCalls: TicketCall[] = [];
  const replyCalls: ReplyCall[] = [];
  const statusCalls: StatusCall[] = [];
  let listed = 0;
  let fetched: string[] = [];
  const detail = (id: string): TicketDetail | null => {
    const found = tickets.find((candidate) => candidate.id === id);
    return found ? { ...found, messages: options.messages?.[id] ?? [] } : null;
  };
  const api: HelpDeskApi = {
    listTickets() {
      listed += 1;
      if (options.manual?.includes("list")) {
        return pending((call) => listCalls.push(call));
      }
      return Promise.resolve(tickets.map((one) => ({ ...one })));
    },
    getTicket(id) {
      fetched = [...fetched, id];
      if (options.manual?.includes("ticket")) {
        return pending((call) => ticketCalls.push({ ...call, id }));
      }
      const found = detail(id);
      return found ? Promise.resolve(found) : Promise.reject(new NotFoundError(`no ticket ${id}`));
    },
    reply(id, body) {
      return pending((call) => replyCalls.push({ ...call, id, body }));
    },
    setStatus(id, status) {
      return pending((call) => statusCalls.push({ ...call, id, status }));
    },
  };
  return {
    api,
    listCalls,
    ticketCalls,
    replyCalls,
    statusCalls,
    detail,
    listedTimes: () => listed,
    fetchedIds: () => fetched,
  };
}

/** Lets pending promises, effects and transitions finish. */
export async function settle(): Promise<void> {
  for (let round = 0; round < 3; round++) {
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
  }
}

/** Opens the app on `url`, as when a link is opened in a new tab. */
export async function start(url: string, api: HelpDeskApi): Promise<User> {
  window.history.replaceState(null, "", url);
  document.title = "";
  const user = userEvent.setup();
  // An async act: a page that suspends while it mounts (stage 4) is retried when its data
  // arrives; under a plain render React 19 would not.
  await act(async () => {
    render(<HelpDesk api={api} />);
  });
  await settle();
  return user;
}

export async function follow(user: User, name: string | RegExp, scope?: HTMLElement): Promise<void> {
  const link = within(scope ?? document.body).getByRole("link", { name });
  // Inside an async act, so a page that suspends during the navigation (stage 4) is retried.
  await act(async () => {
    await user.click(link);
  });
  await settle();
}

/** Clicks inside an async act, so async work it starts (actions, transitions) can finish. */
export async function press(user: User, element: Element): Promise<void> {
  await act(async () => {
    await user.click(element);
  });
  await settle();
}

/** Picks an option of a select inside an async act, like `press`. */
export async function choose(user: User, select: Element, option: string): Promise<void> {
  await act(async () => {
    await user.selectOptions(select, option);
  });
  await settle();
}

export async function goBack(): Promise<void> {
  await act(async () => {
    window.history.back();
    await new Promise((resolve) => setTimeout(resolve, 20));
  });
  await settle();
}

export const heading = () => (screen.getByRole("heading", { level: 1 }).textContent ?? "").trim();
export const path = () => window.location.pathname + window.location.search;
export const nav = () => screen.getByRole("navigation", { name: "Main" });

/** The value of a term in the ticket's description list (<dt>Status</dt><dd>Open</dd>). */
export function field(term: string): string {
  const dt = screen.getByText(term, { selector: "dt" });
  return (dt.nextElementSibling?.textContent ?? "").trim();
}

/** The ticket titles of the Tickets list, in order. */
export function listedTitles(): string[] {
  const list = screen.queryByRole("list", { name: "Tickets" });
  if (!list) {
    return [];
  }
  return within(list)
    .queryAllByRole("link")
    .map((link) => (link.textContent ?? "").trim());
}

/** A short description of an element for failure messages (never the element itself). */
function nameOf(element: Element | null): string {
  if (!element || element === document.body) {
    return "nothing (the page itself)";
  }
  const label =
    element.getAttribute("aria-label") ??
    ("labels" in element ? (element as HTMLInputElement).labels?.[0]?.textContent : null) ??
    element.textContent ??
    "";
  return `<${element.tagName.toLowerCase()}> "${label.replace(/\s+/g, " ").trim().slice(0, 60)}"`;
}

// Compare elements with these, not with assert.equal: when it fails, assert would print the
// whole document, which takes minutes.
export function expectFocus(element: Element, context?: string): void {
  const active = document.activeElement;
  if (active !== element) {
    assert.fail(`${context ? `${context}: ` : ""}the focus should be on ${nameOf(element)}, but it is on ${nameOf(active)}`);
  }
}

export function expectAbsent(element: Element | null, context?: string): void {
  if (element !== null) {
    assert.fail(`${context ? `${context}: ` : ""}${nameOf(element)} should not be on the page`);
  }
}
