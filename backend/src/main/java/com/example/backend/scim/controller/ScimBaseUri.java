package com.example.backend.scim.controller;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * This service's SCIM base URI, absolute, taken from the current request.
 *
 * <p>From the request rather than from configuration so a deployment behind a host-rewriting
 * proxy renders the URI its clients actually use, and so there is no second place the base path
 * is written. One method for every renderer — Users, Groups, search and discovery — so every
 * {@code meta.location} and {@code $ref} the service emits is built the same way and honours the
 * same forwarded-header handling.
 *
 * <p>Depends on the request alone, never on a connector principal, so the public discovery
 * endpoints can call it before any credential is presented.
 */
final class ScimBaseUri {

    private ScimBaseUri() {
    }

    /** The absolute SCIM base URI, with no trailing slash. */
    static String current() {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(ScimSchemas.BASE_PATH)
                .build()
                .toUriString();
    }
}
