import type { ReactNode } from "react";
import { TicketsProvider } from "./data.tsx";
import type { HelpDeskApi } from "./helpdesk-types.ts";
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
  const { pathname } = useLocation();
  const section = pathname === "/" ? "overview" : pathname === "/tickets" || ticketId(pathname) ? "tickets" : null;
  return (
    <>
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
      <main id="main" tabIndex={-1}>
        <Routes key={pathname} pathname={pathname} />
      </main>
    </>
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
