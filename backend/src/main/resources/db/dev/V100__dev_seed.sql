-- Demo data for the local "dev" profile only. Never applied in production.
-- Demo customer 6f1c2b7e-4d1a-4c8e-9a2b-000000000001 matches the demo user in infra/keycloak (dev token endpoint and SSO).
-- Customer 6f1c2b7e-4d1a-4c8e-9a2b-000000000002 owns TXN-9001 (id 9), used to test cross-customer access.

INSERT INTO transactions (customer_id, reference, merchant_name, amount, currency, status, booked_at) VALUES
('6f1c2b7e-4d1a-4c8e-9a2b-000000000001', 'TXN-0001', 'Woolworths Sandton',      842.50, 'ZAR', 'POSTED',   TIMESTAMP '2026-10-20 09:14:00+02'),
('6f1c2b7e-4d1a-4c8e-9a2b-000000000001', 'TXN-0002', 'Uber Trip',                 64.00, 'ZAR', 'POSTED',   TIMESTAMP '2026-10-18 21:02:00+02'),
('6f1c2b7e-4d1a-4c8e-9a2b-000000000001', 'TXN-0003', 'Netflix',                  199.00, 'ZAR', 'POSTED',   TIMESTAMP '2026-10-15 00:05:00+02'),
('6f1c2b7e-4d1a-4c8e-9a2b-000000000001', 'TXN-0004', 'Takealot Online',         1299.99, 'ZAR', 'POSTED',   TIMESTAMP '2026-10-10 13:40:00+02'),
('6f1c2b7e-4d1a-4c8e-9a2b-000000000001', 'TXN-0005', 'Dis-Chem Pharmacy',        356.75, 'ZAR', 'PENDING',  TIMESTAMP '2026-10-28 11:30:00+02'),
('6f1c2b7e-4d1a-4c8e-9a2b-000000000001', 'TXN-0006', 'Airbnb Cape Town',       4800.00, 'ZAR', 'POSTED',   TIMESTAMP '2026-08-02 16:22:00+02'),
('6f1c2b7e-4d1a-4c8e-9a2b-000000000001', 'TXN-0007', 'Fitness Club Monthly',    450.00, 'ZAR', 'REVERSED', TIMESTAMP '2026-07-01 06:00:00+02'),
('6f1c2b7e-4d1a-4c8e-9a2b-000000000001', 'TXN-0008', 'Bolt Ride',                 97.40, 'ZAR', 'POSTED',   TIMESTAMP '2026-06-12 18:45:00+02'),
('6f1c2b7e-4d1a-4c8e-9a2b-000000000002', 'TXN-9001', 'Clicks Pharmacy',         215.00, 'ZAR', 'POSTED',   TIMESTAMP '2026-10-21 10:00:00+02');

INSERT INTO disputes (transaction_id, customer_id, reason, description, status, created_at, updated_at, closed_at, version) VALUES
((SELECT id FROM transactions WHERE reference = 'TXN-0006'), '6f1c2b7e-4d1a-4c8e-9a2b-000000000001', 'NOT_AS_DESCRIBED',
 'Property was smaller than the listing photos showed.', 'UNDER_REVIEW',
 TIMESTAMP '2026-08-10 10:00:00+02', TIMESTAMP '2026-08-12 09:00:00+02', NULL, 1),
((SELECT id FROM transactions WHERE reference = 'TXN-0007'), '6f1c2b7e-4d1a-4c8e-9a2b-000000000001', 'SERVICE_CANCELLED',
 'Cancelled my membership in June and was still charged.', 'ACCEPTED',
 TIMESTAMP '2026-07-05 08:30:00+02', TIMESTAMP '2026-07-20 15:10:00+02', TIMESTAMP '2026-07-20 15:10:00+02', 2),
((SELECT id FROM transactions WHERE reference = 'TXN-0008'), '6f1c2b7e-4d1a-4c8e-9a2b-000000000001', 'DUPLICATE_CHARGE',
 'Charged twice for the same ride.', 'REJECTED',
 TIMESTAMP '2026-06-14 07:00:00+02', TIMESTAMP '2026-06-19 12:00:00+02', TIMESTAMP '2026-06-19 12:00:00+02', 2);

INSERT INTO dispute_events (dispute_id, from_status, to_status, actor, note, created_at) VALUES
((SELECT id FROM disputes WHERE description = 'Property was smaller than the listing photos showed.'), NULL, 'SUBMITTED', '6f1c2b7e-4d1a-4c8e-9a2b-000000000001', NULL, TIMESTAMP '2026-08-10 10:00:00+02'),
((SELECT id FROM disputes WHERE description = 'Property was smaller than the listing photos showed.'), 'SUBMITTED', 'UNDER_REVIEW', 'ops-reviewer', 'Requested host statement', TIMESTAMP '2026-08-12 09:00:00+02'),
((SELECT id FROM disputes WHERE description = 'Cancelled my membership in June and was still charged.'), NULL, 'SUBMITTED', '6f1c2b7e-4d1a-4c8e-9a2b-000000000001', NULL, TIMESTAMP '2026-07-05 08:30:00+02'),
((SELECT id FROM disputes WHERE description = 'Cancelled my membership in June and was still charged.'), 'SUBMITTED', 'UNDER_REVIEW', 'ops-reviewer', NULL, TIMESTAMP '2026-07-08 09:00:00+02'),
((SELECT id FROM disputes WHERE description = 'Cancelled my membership in June and was still charged.'), 'UNDER_REVIEW', 'ACCEPTED', 'ops-reviewer', 'Cancellation confirmed in merchant logs', TIMESTAMP '2026-07-20 15:10:00+02'),
((SELECT id FROM disputes WHERE description = 'Charged twice for the same ride.'), NULL, 'SUBMITTED', '6f1c2b7e-4d1a-4c8e-9a2b-000000000001', NULL, TIMESTAMP '2026-06-14 07:00:00+02'),
((SELECT id FROM disputes WHERE description = 'Charged twice for the same ride.'), 'SUBMITTED', 'REJECTED', 'ops-reviewer', 'Second charge is a valid tip', TIMESTAMP '2026-06-19 12:00:00+02');
