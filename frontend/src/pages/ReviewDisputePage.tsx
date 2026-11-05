import { FormEvent, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, api } from '../api/client';
import type { Dispute, ReviewDecision } from '../api/types';
import { DetailSkeleton } from '../components/Skeleton';
import { StatusBadge } from '../components/StatusBadge';
import { REVIEW_NOTE_MAX, validateReviewNote } from '../disputes/validation';
import { formatDateTime, formatMoney, humanise } from '../format';
import { useAsync } from '../hooks/useAsync';

export function ReviewDisputePage() {
  const { id } = useParams();
  const disputeId = Number(id);
  const { data: dispute, error, loading, reload } = useAsync(
    () => api.getReviewDispute(disputeId),
    [disputeId],
  );
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (loading && !dispute) return <DetailSkeleton />;
  if (error || !dispute) {
    return (
      <section>
        <p role="alert" className="error">{error ?? 'Dispute not found'}</p>
        <Link to="/review">Back to queue</Link>
      </section>
    );
  }

  async function startReview() {
    setBusy(true);
    setActionError(null);
    try {
      await api.startReview(disputeId);
      reload();
    } catch (err) {
      setActionError(describe(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section>
      <Link className="back-link" to="/review">← Back to queue</Link>
      <h1>Dispute for {dispute.transaction.merchantName}</h1>
      <p>
        <StatusBadge status={dispute.status} />
      </p>

      <dl className="details">
        <dt>Customer</dt>
        <dd className="mono">{dispute.customerId}</dd>
        <dt>Transaction</dt>
        <dd className="mono">{dispute.transaction.reference}</dd>
        <dt>Amount</dt>
        <dd className="numeric">{formatMoney(dispute.transaction.amount, dispute.transaction.currency)}</dd>
        <dt>Booked</dt>
        <dd>{formatDateTime(dispute.transaction.bookedAt)}</dd>
        <dt>Reason</dt>
        <dd>{humanise(dispute.reason)}</dd>
        <dt>Raised</dt>
        <dd>{formatDateTime(dispute.createdAt)}</dd>
      </dl>

      <h2>Customer's description</h2>
      <p className="description">{dispute.description}</p>

      <h2>Timeline</h2>
      <ol className="timeline">
        {(dispute.events ?? []).map((event) => (
          <li key={`${event.createdAt}-${event.toStatus}-${event.actor}`}>
            <div>
              <strong>{event.fromStatus ? `${humanise(event.fromStatus)} → ${humanise(event.toStatus)}` : 'Raised'}</strong>
              <span className="muted small"> · {event.actor} · {formatDateTime(event.createdAt)}</span>
            </div>
            {event.note && <div className="small">{event.note}</div>}
          </li>
        ))}
      </ol>

      {actionError && <p role="alert" className="error">{actionError}</p>}

      {dispute.status === 'SUBMITTED' && (
        <button type="button" onClick={startReview} disabled={busy}>
          {busy ? 'Starting…' : 'Start review'}
        </button>
      )}

      {dispute.status === 'UNDER_REVIEW' && (
        <DecisionForm dispute={dispute} onDecided={reload} />
      )}
    </section>
  );
}

function DecisionForm({ dispute, onDecided }: { dispute: Dispute; onDecided: () => void }) {
  const [decision, setDecision] = useState<ReviewDecision>('ACCEPT');
  const [note, setNote] = useState('');
  const [noteError, setNoteError] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validation = validateReviewNote(note);
    setNoteError(validation);
    setServerError(null);
    if (validation) return;

    setSubmitting(true);
    try {
      await api.decideDispute(dispute.id, { decision, note: note.trim() });
      onDecided();
    } catch (err) {
      setServerError(describe(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="card dispute-form" onSubmit={onSubmit} noValidate>
      <h2>Decision</h2>
      <p className="muted">
        The note is shown to the customer in their dispute timeline. Write it as a plain, factual explanation.
        Decisions are final.
      </p>

      <label>
        Outcome
        <select value={decision} onChange={(e) => setDecision(e.target.value as ReviewDecision)}>
          <option value="ACCEPT">Accept: refund or correct the customer</option>
          <option value="REJECT">Reject: transaction stands</option>
        </select>
      </label>

      <label>
        Note to the customer
        <textarea
          rows={4}
          maxLength={REVIEW_NOTE_MAX + 100}
          value={note}
          onChange={(e) => setNote(e.target.value)}
          aria-invalid={Boolean(noteError)}
        />
        <span className="muted small">{note.trim().length} / {REVIEW_NOTE_MAX}</span>
        {noteError && <span className="field-error">{noteError}</span>}
      </label>

      {serverError && <p role="alert" className="error">{serverError}</p>}

      <button type="submit" disabled={submitting}>
        {submitting ? 'Saving…' : 'Confirm decision'}
      </button>
    </form>
  );
}

function describe(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 403) return 'You are not allowed to review this dispute.';
    if (err.status === 409) return 'This dispute was changed by someone else. Refresh and try again.';
    return err.message;
  }
  return 'Something went wrong. Please try again.';
}
