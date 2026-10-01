package com.example.backend.scim.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives the dispatcher's own refusals in the SCIM namespace the SCIM error document.
 *
 * <p>A path no endpoint serves ({@code 404}), a method an endpoint does not implement
 * ({@code 405}), an {@code Accept} it cannot satisfy ({@code 406}) and a body type it does not read
 * ({@code 415}) are all refused before a SCIM handler is selected, so the namespace's controller
 * advice never sees them. The dispatcher answers each with {@code sendError}, which on a real
 * container forwards to the application's error page — a document a SCIM client cannot parse, from
 * a different security chain. This wrapper turns that {@code sendError} into the error document
 * itself, so every refusal in the namespace has one shape whichever layer produced it.
 *
 * <p>Headers set before the refusal survive: a {@code 405} keeps the {@code Allow} header the
 * dispatcher wrote. The bearer challenges are unaffected, because they set a status rather than
 * sending an error, and carry no body by design.
 */
class ScimDispatcherErrorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(request, new ErrorDocumentResponse(response));
    }

    /** Renders {@code sendError} as a SCIM error document instead of an error dispatch. */
    private static final class ErrorDocumentResponse extends HttpServletResponseWrapper {

        ErrorDocumentResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void sendError(int status) throws IOException {
            render(status);
        }

        /** The message is discarded: the dispatcher's messages quote the request. */
        @Override
        public void sendError(int status, String message) throws IOException {
            render(status);
        }

        /**
         * Discards anything buffered and writes the document in its place. A response already
         * committed cannot take an error any more: {@code resetBuffer} refuses it with the
         * {@link IllegalStateException} the Servlet API specifies for {@code sendError}.
         */
        private void render(int status) throws IOException {
            resetBuffer();
            ScimErrorDocument.write((HttpServletResponse) getResponse(), status);
            flushBuffer();
        }
    }
}
