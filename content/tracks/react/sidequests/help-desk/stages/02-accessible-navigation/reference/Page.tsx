import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { useLocation } from "./router.tsx";

const AnnounceContext = createContext<(text: string) => void>(() => {});

/** A live region that screen readers read when the page changes. */
export function Announcer({ children }: { children: ReactNode }): ReactNode {
  const [text, setText] = useState("");
  return (
    <AnnounceContext.Provider value={setText}>
      {children}
      <p role="status">{text}</p>
    </AnnounceContext.Provider>
  );
}

/**
 * Every page starts with its title as the one level 1 heading. It names the browser tab and,
 * after a navigation, takes the focus and is announced, as a page load would be.
 */
export function Page({ title, children }: { title: string; children?: ReactNode }): ReactNode {
  const { navigations } = useLocation();
  const announce = useContext(AnnounceContext);
  const heading = useRef<HTMLHeadingElement>(null);
  const navigated = useRef(navigations > 0);

  useEffect(() => {
    document.title = `${title} · Help desk`;
  }, [title]);

  useEffect(() => {
    if (navigated.current) {
      heading.current?.focus();
      announce(`Navigated to ${title}`);
    }
    // Once, when the page appears.
  }, []);

  return (
    <>
      <h1 tabIndex={-1} ref={heading}>
        {title}
      </h1>
      {children}
    </>
  );
}
