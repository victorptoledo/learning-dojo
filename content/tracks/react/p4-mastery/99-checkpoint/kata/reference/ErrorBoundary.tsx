import { Component, type ErrorInfo, type ReactNode } from "react";

export type ErrorBoundaryProps = {
  fallback: (error: Error, reset: () => void) => ReactNode;
  /** Called once for every error the boundary catches, to report it. */
  onError?: (error: Error) => void;
  /** When it changes, the boundary resets by itself. */
  resetKey?: unknown;
  children: ReactNode;
};

type ErrorBoundaryState = { error: Error | null; resetKey?: unknown };

function asError(error: unknown): Error {
  return error instanceof Error ? error : new Error(String(error));
}

/** Renders fallback(error, reset) when a child throws while rendering. */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  override state: ErrorBoundaryState = { error: null, resetKey: this.props.resetKey };

  static getDerivedStateFromError(error: unknown): Partial<ErrorBoundaryState> {
    return { error: asError(error) };
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

  // getDerivedStateFromError runs during render and may run more than once; reporting is a
  // side effect, so it belongs in the commit phase.
  override componentDidCatch(error: unknown, _info: ErrorInfo): void {
    this.props.onError?.(asError(error));
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
