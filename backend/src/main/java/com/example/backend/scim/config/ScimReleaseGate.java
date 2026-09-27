package com.example.backend.scim.config;

/**
 * Whether this deployment serves the SCIM interface at all.
 *
 * <p>Users and Groups are one release capability: a directory that can create Users but has
 * no Groups cannot express authority, so a connector provisioning against it would build a
 * directory that means something different from the one it will provision against later. The
 * gate exists so the two halves can be built and merged in order without the half-built
 * surface ever being reachable in a real environment.
 *
 * <p>Closed by default while the two halves were being built, so neither could be reached in a real
 * environment half-finished. Open by default from the ticket that completed Groups — which is why
 * the default lives in {@code ScimSecurityConfig}'s {@code @Value} expression rather than here: the
 * release decision belongs to the code, not to a configuration file a deployment may replace.
 *
 * <p>The gate outlives its original reason on purpose. A deployment that authenticates by password
 * only and provisions nothing can still turn the namespace off, and while it is off the namespace
 * answers {@code 404} to every request whatever credential it carries — so the surface cannot even
 * be probed for existence.
 *
 * @param open whether the namespace answers
 */
public record ScimReleaseGate(boolean open) {
}
