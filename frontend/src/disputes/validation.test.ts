import { validateDispute, validateReviewNote } from './validation';

describe('validateDispute', () => {
  it('accepts a known reason and a meaningful description', () => {
    expect(validateDispute({ reason: 'DUPLICATE_CHARGE', description: 'Charged twice for one ride' })).toEqual({});
  });

  it('requires a reason', () => {
    expect(validateDispute({ reason: '', description: 'Charged twice for one ride' })).toHaveProperty('reason');
  });

  it('rejects descriptions that are too short after trimming', () => {
    expect(validateDispute({ reason: 'OTHER', description: '   short   ' })).toHaveProperty('description');
  });

  it('rejects descriptions over the maximum length', () => {
    const description = 'x'.repeat(1001);
    expect(validateDispute({ reason: 'OTHER', description })).toHaveProperty('description');
  });
});

describe('validateReviewNote', () => {
  it('accepts a customer-facing note within the limits', () => {
    expect(validateReviewNote('Merchant confirmed the duplicate charge.')).toBeNull();
  });

  it('rejects notes that are too short or too long', () => {
    expect(validateReviewNote('ok')).not.toBeNull();
    expect(validateReviewNote('x'.repeat(501))).not.toBeNull();
  });
});
