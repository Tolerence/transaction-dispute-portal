import { FormEvent, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError, api } from '../api/client';
import type { DisputeReason } from '../api/types';
import { StatusBadge } from '../components/StatusBadge';
import { DESCRIPTION_MAX, DISPUTE_REASONS, DisputeFormErrors, validateDispute } from '../disputes/validation';
import { formatDateTime, formatMoney, humanise } from '../format';
import { useAsync } from '../hooks/useAsync';

export function TransactionDetailPage() {
  const { id } = useParams();
  const transactionId = Number(id);
  const { data: tx, error, loading } = useAsync(() => api.getTransaction(transactionId), [transactionId]);

  if (loading && !tx) return <p>Loading transaction…</p>;
  if (error || !tx) {
    return (
      <section>
        <p role="alert" className="error">{error ?? 'Transaction not found'}</p>
        <Link to="/transactions">Back to transactions</Link>
      </section>
    );
  }

  return (
    <section>
      <Link to="/transactions">← Back to transactions</Link>
      <h1>{tx.merchantName}</h1>

      <dl className="details">
        <dt>Amount</dt>
        <dd className="numeric">{formatMoney(tx.amount, tx.currency)}</dd>
        <dt>Booked</dt>
        <dd>{formatDateTime(tx.bookedAt)}</dd>
        <dt>Reference</dt>
        <dd className="mono">{tx.reference}</dd>
        <dt>Status</dt>
        <dd>
          <StatusBadge status={tx.status} />
        </dd>
      </dl>

      {tx.disputable ? (
        <DisputeForm transactionId={tx.id} />
      ) : (
        <p className="notice">
          This transaction cannot be disputed: {tx.ineligibilityReason?.toLowerCase()}.{' '}
          <Link to="/disputes">View your dispute history</Link>
        </p>
      )}
    </section>
  );
}

function DisputeForm({ transactionId }: { transactionId: number }) {
  const navigate = useNavigate();
  const [reason, setReason] = useState<DisputeReason | ''>('');
  const [description, setDescription] = useState('');
  const [errors, setErrors] = useState<DisputeFormErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validation = validateDispute({ reason, description });
    setErrors(validation);
    setServerError(null);
    if (Object.keys(validation).length > 0 || reason === '') return;

    setSubmitting(true);
    try {
      const created = await api.submitDispute(transactionId, {
        reason,
        description: description.trim(),
      });
      navigate(`/disputes/${created.id}`);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(err.fieldErrors as DisputeFormErrors);
        setServerError(err.message);
      } else {
        setServerError('We could not submit your dispute. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="card dispute-form" onSubmit={onSubmit} noValidate>
      <h2>Raise a dispute</h2>
      <p className="muted">
        Tell us what went wrong. We will confirm receipt and update you as the case progresses.
      </p>

      <label>
        Reason
        <select
          value={reason}
          onChange={(e) => setReason(e.target.value as DisputeReason | '')}
          aria-invalid={Boolean(errors.reason)}
        >
          <option value="">Select a reason</option>
          {DISPUTE_REASONS.map((r) => (
            <option key={r} value={r}>
              {humanise(r)}
            </option>
          ))}
        </select>
        {errors.reason && <span className="field-error">{errors.reason}</span>}
      </label>

      <label>
        Describe what happened
        <textarea
          rows={5}
          maxLength={DESCRIPTION_MAX + 200}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          aria-invalid={Boolean(errors.description)}
        />
        <span className="muted small">{description.trim().length} / {DESCRIPTION_MAX}</span>
        {errors.description && <span className="field-error">{errors.description}</span>}
      </label>

      {serverError && <p role="alert" className="error">{serverError}</p>}

      <button type="submit" disabled={submitting}>
        {submitting ? 'Submitting…' : 'Submit dispute'}
      </button>
    </form>
  );
}
