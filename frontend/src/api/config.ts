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

/**
 * Usernames pre-filled on the SSO login page for the two sign-in buttons. Empty by default, so production
 * builds do not show demo usernames. The SSO build (make up-sso) sets them to the local Keycloak demo users.
 */
export const ssoLoginHints = {
  customer: import.meta.env.VITE_SSO_CUSTOMER_HINT ?? '',
  reviewer: import.meta.env.VITE_SSO_REVIEWER_HINT ?? '',
};
