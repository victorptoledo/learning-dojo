import type { ReactNode } from "react";

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

/** WAI-ARIA tabs with automatic activation, a roving tabindex and keyboard support. */
export function Tabs(_props: TabsProps): ReactNode {
  throw new Error("todo: write Tabs");
}
