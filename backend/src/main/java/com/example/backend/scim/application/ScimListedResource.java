package com.example.backend.scim.application;

import java.util.UUID;

/**
 * A resource a base search can return: a User or a Group, as its connector sees it.
 *
 * <p>Sealed so a renderer handling a mixed page switches over exactly the two, and a third
 * resource type added later fails to compile there rather than rendering as nothing.
 */
public sealed interface ScimListedResource permits ScimUserResource, ScimGroupResource {

    /** The resource's stable id. */
    UUID id();
}
