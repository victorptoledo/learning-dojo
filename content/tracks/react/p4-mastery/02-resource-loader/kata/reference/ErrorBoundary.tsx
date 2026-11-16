import { Component, type ReactNode } from "react";

export type ErrorBoundaryProps = {
  fallback: (error: Error, reset: () => void) => ReactNode;
  /** When it changes, the boundary resets by itself. */
  resetKey?: unknown;
  children: ReactNode;
};

type ErrorBoundaryState = { error: Error | null; resetKey: unknown };

/** Renders fallback(error, reset) when a child throws while rendering. */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  override state: ErrorBoundaryState = { error: null, resetKey: this.props.resetKey };

  static getDerivedStateFromError(error: unknown): Partial<ErrorBoundaryState> {
    return { error: error instanceof Error ? error : new Error(String(error)) };
  }

  static getDerivedStateFromProps(
    props: ErrorBoundaryProps,
    state: ErrorBoundaryState,
  ): Partial<ErrorBoundaryState> | null {
    if (!Object.is(props.resetKey, state.resetKey)) {
      return { error: null, resetKey: props.resetKey };
    }
    return null;
  }

  reset = () => {
    this.setState({ error: null });
  };

  override render(): ReactNode {
    if (this.state.error) {
      return this.props.fallback(this.state.error, this.reset);
    }
    return this.props.children;
  }
}
