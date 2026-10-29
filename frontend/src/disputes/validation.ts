import type { CreateDisputeRequest, DisputeReason } from '../api/types';

export const DESCRIPTION_MIN = 10;
export const DESCRIPTION_MAX = 1000;

export const DISPUTE_REASONS: DisputeReason[] = [
  'UNAUTHORISED_TRANSACTION',
  'DUPLICATE_CHARGE',
  'INCORRECT_AMOUNT',
  'NOT_AS_DESCRIBED',
  'SERVICE_CANCELLED',
  'OTHER',
];

export type DisputeFormErrors = Partial<Record<keyof CreateDisputeRequest, string>>;

/**
 * Mirrors the server-side constraints so the customer gets feedback before submitting.
 * The server remains the authority and its field errors are still shown.
 */
export function validateDispute(input: { reason: string; description: string }): DisputeFormErrors {
  const errors: DisputeFormErrors = {};
  if (!DISPUTE_REASONS.includes(input.reason as DisputeReason)) {
    errors.reason = 'Choose a reason for the dispute';
  }
  const length = input.description.trim().length;
  if (length < DESCRIPTION_MIN) {
    errors.description = `Describe the problem in at least ${DESCRIPTION_MIN} characters`;
  } else if (length > DESCRIPTION_MAX) {
    errors.description = `Keep the description under ${DESCRIPTION_MAX} characters`;
  }
  return errors;
}

export const REVIEW_NOTE_MIN = 10;
export const REVIEW_NOTE_MAX = 500;

/** Same limits as the API. Notes are shown to the customer, so they must be real sentences. */
export function validateReviewNote(note: string): string | null {
  const length = note.trim().length;
  if (length < REVIEW_NOTE_MIN) return `Write at least ${REVIEW_NOTE_MIN} characters for the customer`;
  if (length > REVIEW_NOTE_MAX) return `Keep the note under ${REVIEW_NOTE_MAX} characters`;
  return null;
}
