import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, api } from '../api/client';
import { StatusBadge } from '../components/StatusBadge';
import { formatDateTime, formatMoney, humanise } from '../format';
import { useAsync } from '../hooks/useAsync';
import { describeTransition } from '../disputes/timeline';

export function DisputeDetailPage() {
  const { id } = useParams();
  const disputeId = Number(id);
  const { data: dispute, error, loading, reload } = useAsync(() => api.getDispute(disputeId), [disputeId]);
  const [actionError, setActionError] = useState<string | null>(null);
  const [withdrawing, setWithdrawing] = useState(false);

  if (loading && !dispute) return <p>Loading dispute…</p>;
  if (error || !dispute) {
    return (
      <section>
        <p role="alert" className="error">{error ?? 'Dispute not found'}</p>
        <Link to="/disputes">Back to dispute history</Link>
      </section>
    );
  }

  const canWithdraw = dispute.status === 'SUBMITTED' || dispute.status === 'UNDER_REVIEW';

  async function withdraw() {
    if (!window.confirm('Withdraw this dispute? You can raise a new one later if needed.')) return;
    setWithdrawing(true);
    setActionError(null);
    try {
      await api.withdrawDispute(disputeId);
      reload();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Could not withdraw the dispute.');
    } finally {
      setWithdrawing(false);
    }
  }

  return (
    <section>
      <Link to="/disputes">← Back to dispute history</Link>
      <h1>Dispute for {dispute.transaction.merchantName}</h1>
      <p>
        <StatusBadge status={dispute.status} />
      </p>

      <dl className="details">
        <dt>Transaction</dt>
        <dd>
          <Link to={`/transactions/${dispute.transaction.id}`}>{dispute.transaction.reference}</Link>
        </dd>
        <dt>Amount</dt>
        <dd className="numeric">{formatMoney(dispute.transaction.amount, dispute.transaction.currency)}</dd>
        <dt>Reason</dt>
        <dd>{humanise(dispute.reason)}</dd>
        <dt>Raised</dt>
        <dd>{formatDateTime(dispute.createdAt)}</dd>
        <dt>Last updated</dt>
        <dd>{formatDateTime(dispute.updatedAt)}</dd>
        {dispute.closedAt && (
          <>
            <dt>Closed</dt>
            <dd>{formatDateTime(dispute.closedAt)}</dd>
          </>
        )}
      </dl>

      <h2>Your description</h2>
      <p className="description">{dispute.description}</p>

      <h2>Timeline</h2>
      <ol className="timeline">
        {(dispute.events ?? []).map((event) => (
          <li key={`${event.createdAt}-${event.toStatus}`}>
            <div>
              <strong>{describeTransition(event.fromStatus, event.toStatus)}</strong>
              <span className="muted small"> · {formatDateTime(event.createdAt)}</span>
            </div>
            {event.note && <div className="small">{event.note}</div>}
          </li>
        ))}
      </ol>

      {actionError && <p role="alert" className="error">{actionError}</p>}

      {canWithdraw && (
        <button type="button" className="danger" onClick={withdraw} disabled={withdrawing}>
          {withdrawing ? 'Withdrawing…' : 'Withdraw dispute'}
        </button>
      )}
    </section>
  );
}
