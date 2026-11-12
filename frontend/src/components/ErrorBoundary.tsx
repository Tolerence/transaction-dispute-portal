import { Component, type ErrorInfo, type ReactNode } from 'react';
import { reportClientError } from '../telemetry/report';

interface Props {
  children: ReactNode;
}

interface State {
  failed: boolean;
}

/**
 * Catches render errors so one broken page does not blank the whole app. The error is reported, and the user
 * gets a message with a way to reload.
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { failed: false };

  static getDerivedStateFromError(): State {
    return { failed: true };
  }

  componentDidCatch(error: Error, _info: ErrorInfo): void {
    reportClientError('error-boundary', error);
  }

  render(): ReactNode {
    if (!this.state.failed) {
      return this.props.children;
    }
    return (
      <div role="alert" style={{ padding: '2rem' }}>
        <h1>Something went wrong</h1>
        <p>The error has been reported. Reload the page to continue.</p>
        <button type="button" onClick={() => window.location.reload()}>
          Reload
        </button>
      </div>
    );
  }
}
