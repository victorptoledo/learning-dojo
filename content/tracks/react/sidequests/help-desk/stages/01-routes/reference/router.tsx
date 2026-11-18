// A small router on the History API: the location lives in state, links push entries and
// popstate (Back, Forward) brings them back.
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type AnchorHTMLAttributes,
  type MouseEvent,
  type ReactNode,
} from "react";

export type Location = { pathname: string; search: string };
type Navigate = (to: string, options?: { replace?: boolean }) => void;

// Two contexts: links only need `navigate`, which never changes, so a location change does
// not re-render every link on the page.
const LocationContext = createContext<Location | null>(null);
const NavigateContext = createContext<Navigate | null>(null);

const current = (): Location => ({ pathname: window.location.pathname, search: window.location.search });

export function Router({ children }: { children: ReactNode }): ReactNode {
  const [location, setLocation] = useState(current);

  useEffect(() => {
    const onPopState = () => setLocation(current());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  const navigate = useCallback<Navigate>((to, options) => {
    if (options?.replace) {
      window.history.replaceState(null, "", to);
    } else {
      window.history.pushState(null, "", to);
    }
    setLocation(current());
  }, []);

  return (
    <NavigateContext.Provider value={navigate}>
      <LocationContext.Provider value={location}>{children}</LocationContext.Provider>
    </NavigateContext.Provider>
  );
}

export function useLocation(): Location {
  const location = useContext(LocationContext);
  if (!location) {
    throw new Error("useLocation must be used inside a Router");
  }
  return location;
}

export function useNavigate(): Navigate {
  const navigate = useContext(NavigateContext);
  if (!navigate) {
    throw new Error("useNavigate must be used inside a Router");
  }
  return navigate;
}

type LinkProps = { to: string } & Omit<AnchorHTMLAttributes<HTMLAnchorElement>, "href">;

/** A real link (it can be opened in a new tab) that navigates without reloading the page. */
export function Link({ to, onClick, children, ...rest }: LinkProps): ReactNode {
  const navigate = useNavigate();
  function click(event: MouseEvent<HTMLAnchorElement>) {
    onClick?.(event);
    const modified = event.metaKey || event.ctrlKey || event.shiftKey || event.altKey;
    if (event.defaultPrevented || event.button !== 0 || modified || rest.target) {
      return;
    }
    event.preventDefault();
    navigate(to);
  }
  return (
    <a href={to} onClick={click} {...rest}>
      {children}
    </a>
  );
}

/** The ticket id of a `/tickets/<id>` path, or null. */
export function ticketId(pathname: string): string | null {
  const match = /^\/tickets\/([^/]+)$/.exec(pathname);
  return match?.[1] ? decodeURIComponent(match[1]) : null;
}
