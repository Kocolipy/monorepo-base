package com.example.backend.scim.domain;

import java.io.IOException;

/**
 * A request body that ran past {@link ScimRequestLimits#MAX_BODY_BYTES} while it was being read.
 *
 * <p>An {@link IOException}, because it is raised from inside {@code InputStream.read} on a body
 * that declared no length: the bound can only be noticed by the reader that crosses it. Whatever
 * parser was reading wraps it, so the boundary that renders it looks for it in the cause chain
 * rather than expecting it on top.
 */
public class ScimRequestBodyTooLargeException extends IOException {

    public ScimRequestBodyTooLargeException() {
        super("The request body exceeds " + ScimRequestLimits.MAX_BODY_BYTES + " bytes.");
    }
}
