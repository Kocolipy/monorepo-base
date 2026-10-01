/**
 * The length bounds of a new password, mirrored for the change-password form.
 *
 * The authority is the backend's
 * `backend/src/main/java/com/example/backend/scim/domain/PasswordPolicy.java`
 * (`MIN_LENGTH`, `MAX_LENGTH`); the SPA cannot import it, so a change there
 * must be repeated here. These values only let the form state the rule and
 * stop an obvious miss before submission — the backend still decides, and its
 * refusal message is what the User sees when it disagrees.
 *
 * The backend counts code points of the normalized password, while the HTML
 * `minLength` / `maxLength` attributes count UTF-16 code units, so the two can
 * differ for characters outside the Basic Multilingual Plane. That is why the
 * client-side check is a convenience and never the rule.
 */
export const PASSWORD_LENGTH = { min: 12, max: 256 } as const;
