/**
 * Public, build-time settings. Nothing here is a secret: every value ends up in the browser bundle.
 */
export const oidcConfig = {
  issuer: import.meta.env.VITE_OIDC_ISSUER ?? '',
  clientId: import.meta.env.VITE_OIDC_CLIENT_ID ?? 'dispute-portal-web',
};

export const devLoginEnabled = import.meta.env.VITE_DEV_LOGIN === 'true';

/** Demo customer seeded by the dev profile and present in the local Keycloak realm. */
export const DEMO_CUSTOMER_ID = '6f1c2b7e-4d1a-4c8e-9a2b-000000000001';

/** Demo reviewer, present in the local Keycloak realm and the dev token endpoint. */
export const DEMO_REVIEWER_ID = '6f1c2b7e-4d1a-4c8e-9a2b-000000000003';
