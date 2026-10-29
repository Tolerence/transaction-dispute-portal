import type { DisputeStatus, TransactionStatus } from '../api/types';
import { humanise } from '../format';

type Status = DisputeStatus | TransactionStatus;

export function StatusBadge({ status }: { status: Status }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{humanise(status)}</span>;
}
