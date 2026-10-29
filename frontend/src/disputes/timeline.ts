import type { DisputeStatus } from '../api/types';
import { humanise } from '../format';

/**
 * Label for one step of a dispute timeline. The API omits null fields, so an opening event arrives with
 * no fromStatus at all. Treat a missing value the same as null.
 */
export function describeTransition(from: DisputeStatus | null | undefined, to: DisputeStatus): string {
  if (!from) return 'Dispute raised';
  return `${humanise(from)} → ${humanise(to)}`;
}
