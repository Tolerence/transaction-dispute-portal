export type TransactionStatus = 'PENDING' | 'POSTED' | 'REVERSED';

export type DisputeStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';

export type DisputeReason =
  | 'UNAUTHORISED_TRANSACTION'
  | 'DUPLICATE_CHARGE'
  | 'INCORRECT_AMOUNT'
  | 'NOT_AS_DESCRIBED'
  | 'SERVICE_CANCELLED'
  | 'OTHER';

/** Spring Data PagedModel as serialised by the API. */
export interface Page<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface Transaction {
  id: number;
  reference: string;
  merchantName: string;
  amount: number;
  currency: string;
  status: TransactionStatus;
  bookedAt: string;
  disputable: boolean;
  ineligibilityReason: string | null;
}

export interface DisputeEvent {
  /** Omitted by the API for the opening event. */
  fromStatus?: DisputeStatus | null;
  toStatus: DisputeStatus;
  actor: string;
  note: string | null;
  createdAt: string;
}

export interface Dispute {
  id: number;
  /** Only present on the reviewer view. */
  customerId?: string;
  transaction: {
    id: number;
    reference: string;
    merchantName: string;
    amount: number;
    currency: string;
    bookedAt: string;
  };
  reason: DisputeReason;
  description: string;
  status: DisputeStatus;
  createdAt: string;
  updatedAt: string;
  closedAt: string | null;
  events?: DisputeEvent[];
}

export interface CreateDisputeRequest {
  reason: DisputeReason;
  description: string;
}

export interface TransactionQuery {
  page?: number;
  size?: number;
  status?: TransactionStatus | '';
  from?: string;
  to?: string;
}

export interface DisputeQuery {
  page?: number;
  size?: number;
  status?: DisputeStatus | '';
}

export type ReviewDecision = 'ACCEPT' | 'REJECT';

export interface ReviewDecisionRequest {
  decision: ReviewDecision;
  note: string;
}
