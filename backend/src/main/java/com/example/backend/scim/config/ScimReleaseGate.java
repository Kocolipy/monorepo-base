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
 * <p>Closed by default, and that default is the point: a deployment gets the SCIM interface
 * because someone turned it on, never because they did not know it was there. It is opened
 * by default from the ticket that completes Groups.
 *
 * @param open whether the namespace answers
 */
public record ScimReleaseGate(boolean open) {
}
