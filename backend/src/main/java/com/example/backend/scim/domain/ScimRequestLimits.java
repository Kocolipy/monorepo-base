package com.example.backend.scim.domain;

/**
 * The per-request safety bounds the SCIM namespace enforces that are about the request as a whole
 * rather than about one parser.
 *
 * <p>These are not rate limits — throttling is the deployment edge's responsibility — but bounds on
 * how much work one request may ask for. The filter parser keeps its own bounds beside the grammar
 * they limit ({@link ScimFilterParser#MAX_NODES} and the rest); these two have no single parser to
 * live in, because the body bound is enforced before any parser runs and the PATCH bound applies to
 * the User and the Group reader alike.
 */
public final class ScimRequestLimits {

    /** The largest request body the namespace reads: 1 MiB. Larger is {@code 413}. */
    public static final long MAX_BODY_BYTES = 1024L * 1024L;

    /** The most operations one PatchOp may carry. More is {@code 400 invalidValue}. */
    public static final int MAX_PATCH_OPERATIONS = 100;

    private ScimRequestLimits() {
    }
}
