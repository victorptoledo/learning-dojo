import { useId, useRef, useState, type KeyboardEvent, type ReactNode } from "react";

export type Tab = {
  id: string;
  title: string;
  content: ReactNode;
  disabled?: boolean;
};

export type TabsProps = {
  /** The accessible name of the tab list. */
  label: string;
  tabs: readonly Tab[];
  defaultTabId?: string;
  onChange?: (id: string) => void;
};

/** The index of the next enabled tab from `from` in direction `step`, wrapping around. */
function nextEnabled(tabs: readonly Tab[], from: number, step: 1 | -1): number {
  for (let i = 1; i <= tabs.length; i++) {
    const index = (from + step * i + tabs.length * i) % tabs.length;
    if (!tabs[index]?.disabled) {
      return index;
    }
  }
  return from;
}

/** WAI-ARIA tabs with automatic activation, a roving tabindex and keyboard support. */
export function Tabs({ label, tabs, defaultTabId, onChange }: TabsProps) {
  const base = useId();
  const [selectedId, setSelectedId] = useState(
    () => defaultTabId ?? tabs.find((tab) => !tab.disabled)?.id ?? null,
  );
  const refs = useRef(new Map<string, HTMLButtonElement>());

  const tabId = (id: string) => `${base}-tab-${id}`;
  const panelId = (id: string) => `${base}-panel-${id}`;

  function select(id: string) {
    if (id === selectedId) {
      return;
    }
    setSelectedId(id);
    onChange?.(id);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const current = tabs.findIndex((tab) => tab.id === selectedId);
    let target: number | null = null;
    switch (event.key) {
      case "ArrowRight":
        target = nextEnabled(tabs, current, 1);
        break;
      case "ArrowLeft":
        target = nextEnabled(tabs, current, -1);
        break;
      case "Home":
        target = nextEnabled(tabs, -1, 1);
        break;
      case "End":
        target = nextEnabled(tabs, tabs.length, -1);
        break;
    }
    if (target === null) {
      return;
    }
    event.preventDefault();
    const tab = tabs[target];
    if (tab && !tab.disabled) {
      select(tab.id);
      refs.current.get(tab.id)?.focus();
    }
  }

  const selected = tabs.find((tab) => tab.id === selectedId);

  return (
    <div>
      <div role="tablist" aria-label={label} onKeyDown={handleKeyDown}>
        {tabs.map((tab) => {
          const isSelected = tab.id === selectedId;
          return (
            <button
              key={tab.id}
              ref={(node) => {
                if (node) {
                  refs.current.set(tab.id, node);
                } else {
                  refs.current.delete(tab.id);
                }
              }}
              id={tabId(tab.id)}
              type="button"
              role="tab"
              aria-selected={isSelected}
              aria-controls={panelId(tab.id)}
              aria-disabled={tab.disabled ? true : undefined}
              tabIndex={isSelected ? 0 : -1}
              onClick={() => {
                if (!tab.disabled) {
                  select(tab.id);
                }
              }}
            >
              {tab.title}
            </button>
          );
        })}
      </div>
      {selected && (
        <div
          role="tabpanel"
          id={panelId(selected.id)}
          aria-labelledby={tabId(selected.id)}
          tabIndex={0}
        >
          {selected.content}
        </div>
      )}
    </div>
  );
}
