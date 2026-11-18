import { Suspense, type ReactNode } from "react";
import { TicketsProvider } from "./data.tsx";
import type { HelpDeskApi } from "./helpdesk-types.ts";
import { Announcer } from "./Page.tsx";
import { NotFoundPage, OverviewPage } from "./pages.tsx";
import { Link, Router, ticketId, useLocation } from "./router.tsx";
import { TicketListPage } from "./TicketList.tsx";
import { TicketPage } from "./TicketPage.tsx";

/** The whole app: navigation, routes and the shared ticket list. */
export function HelpDesk({ api }: { api: HelpDeskApi }): ReactNode {
  return (
    <Router>
      <TicketsProvider api={api}>
        <Layout />
      </TicketsProvider>
    </Router>
  );
}

function Layout(): ReactNode {
  const { pathname, pending } = useLocation();
  const section = pathname === "/" ? "overview" : pathname === "/tickets" || ticketId(pathname) ? "tickets" : null;
  return (
    <Announcer>
      <a
        href="#main"
        onClick={(event) => {
          event.preventDefault();
          document.getElementById("main")?.focus();
        }}
      >
        Skip to content
      </a>
      <nav aria-label="Main">
        <ul>
          <li>
            <Link to="/" aria-current={section === "overview" ? "page" : undefined}>
              Overview
            </Link>
          </li>
          <li>
            <Link to="/tickets" aria-current={section === "tickets" ? "page" : undefined}>
              Tickets
            </Link>
          </li>
        </ul>
      </nav>
      <main id="main" tabIndex={-1} aria-busy={pending ? true : undefined}>
        <Suspense fallback={<p>Loading ticket</p>}>
          <Routes key={pathname} pathname={pathname} />
        </Suspense>
      </main>
    </Announcer>
  );
}

function Routes({ pathname }: { pathname: string }): ReactNode {
  if (pathname === "/") {
    return <OverviewPage />;
  }
  if (pathname === "/tickets") {
    return <TicketListPage />;
  }
  const id = ticketId(pathname);
  if (id !== null) {
    return <TicketPage id={id} />;
  }
  return <NotFoundPage />;
}
