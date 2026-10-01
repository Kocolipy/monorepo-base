package com.example.backend.scim.config;

import com.example.backend.scim.domain.ScimRequestBodyTooLargeException;
import com.example.backend.scim.domain.ScimRequestLimits;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bounds every SCIM request body at {@link ScimRequestLimits#MAX_BODY_BYTES}.
 *
 * <p>Two cases, because a body can arrive two ways. One that DECLARES a larger length is refused
 * with {@code 413} here, before a byte of it is read and before authentication — the declared length
 * is all the decision needs, and reading an oversized body only to refuse it is the cost the bound
 * exists to avoid. One that declares nothing (chunked) can only be measured as it is read, so its
 * stream is wrapped to fail with {@link ScimRequestBodyTooLargeException} the moment it crosses the
 * bound; the SCIM error advice renders that as the same {@code 413}.
 *
 * <p>Either way nothing is written: the refusal happens before the handler has a complete body to
 * act on.
 */
class ScimRequestBodyLimitFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long declared = request.getContentLengthLong();
        if (declared > ScimRequestLimits.MAX_BODY_BYTES) {
            ScimErrorDocument.write(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }
        chain.doFilter(declared < 0 ? new BoundedRequest(request) : request, response);
    }

    /** A request whose body stream refuses to yield more than the bound. */
    private static final class BoundedRequest extends HttpServletRequestWrapper {

        private BoundedInputStream stream;

        BoundedRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (stream == null) {
                stream = new BoundedInputStream(super.getInputStream());
            }
            return stream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    /** Counts what has been read and fails once the count passes the bound. */
    private static final class BoundedInputStream extends ServletInputStream {

        private final ServletInputStream delegate;

        private long read;

        BoundedInputStream(ServletInputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int next = delegate.read();
            if (next >= 0) {
                count(1);
            }
            return next;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = delegate.read(buffer, offset, length);
            if (n > 0) {
                count(n);
            }
            return n;
        }

        private void count(int n) throws ScimRequestBodyTooLargeException {
            read += n;
            if (read > ScimRequestLimits.MAX_BODY_BYTES) {
                throw new ScimRequestBodyTooLargeException();
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }
    }
}
