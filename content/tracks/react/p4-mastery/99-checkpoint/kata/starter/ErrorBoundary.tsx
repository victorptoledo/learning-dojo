import { Component, type ReactNode } from "react";

export type ErrorBoundaryProps = {
  fallback: (error: Error, reset: () => void) => ReactNode;
  /** Called once for every error the boundary catches, to report it. */
  onError?: (error: Error) => void;
  /** When it changes, the boundary resets by itself. */
  resetKey?: unknown;
  children: ReactNode;
};

type ErrorBoundaryState = { error: Error | null };

/** Renders fallback(error, reset) when a child throws while rendering. */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  override state: ErrorBoundaryState = { error: null };

  override render(): ReactNode {
    throw new Error("todo: write ErrorBoundary");
  }
}
