package com.example.backend.scim.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.scim.domain.ScimRequestBodyTooLargeException;
import com.example.backend.scim.domain.ScimRequestLimits;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Every SCIM body is bounded at 1 MiB: refused on a declared length before it is read, and failed
 * while streaming when it declared none.
 */
class ScimRequestBodyLimitFilterTests {

    private static final int LIMIT = (int) ScimRequestLimits.MAX_BODY_BYTES;

    private final ScimRequestBodyLimitFilter filter = new ScimRequestBodyLimitFilter();

    @Test
    void a_declared_length_over_the_bound_is_refused_before_the_chain_runs() throws Exception {
        MockHttpServletRequest request = request(new byte[LIMIT + 1]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ServletRequest> reached = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> reached.set(req));

        assertThat(reached.get()).as("the chain is not run").isNull();
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentType()).startsWith("application/scim+json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        JsonNode error = JsonMapper.builder().build().readTree(response.getContentAsString());
        assertThat(error.get("schemas").get(0).asText())
                .isEqualTo("urn:ietf:params:scim:api:messages:2.0:Error");
        assertThat(error.get("status").asText()).isEqualTo("413");
        assertThat(error.has("scimType")).isFalse();
        assertThat(error.get("detail").asText()).contains("1 MiB");
    }

    @Test
    void a_declared_length_at_the_bound_passes_the_request_through_untouched() throws Exception {
        MockHttpServletRequest request = request(new byte[LIMIT]);
        AtomicReference<ServletRequest> reached = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> reached.set(req));

        assertThat(reached.get()).isSameAs(request);
    }

    @Test
    void an_undeclared_length_is_read_up_to_the_bound_and_fails_past_it() throws Exception {
        AtomicReference<ServletRequest> reached = new AtomicReference<>();
        filter.doFilter(undeclared(new byte[LIMIT + 1]), new MockHttpServletResponse(),
                (req, res) -> reached.set(req));

        InputStream stream = ((HttpServletRequest) reached.get()).getInputStream();
        assertThat(stream.readNBytes(LIMIT)).hasSize(LIMIT);
        assertThatThrownBy(stream::read).isInstanceOf(ScimRequestBodyTooLargeException.class);
    }

    @Test
    void an_undeclared_length_fails_in_a_bulk_read_that_crosses_the_bound() throws Exception {
        AtomicReference<ServletRequest> reached = new AtomicReference<>();
        filter.doFilter(undeclared(new byte[LIMIT + 10]), new MockHttpServletResponse(),
                (req, res) -> reached.set(req));

        InputStream stream = ((HttpServletRequest) reached.get()).getInputStream();
        assertThat(stream.read(new byte[LIMIT - 1])).isEqualTo(LIMIT - 1);
        assertThatThrownBy(() -> stream.read(new byte[16], 0, 16))
                .isInstanceOf(ScimRequestBodyTooLargeException.class);
    }

    @Test
    void an_undeclared_body_within_the_bound_reads_to_its_end() throws Exception {
        AtomicReference<ServletRequest> reached = new AtomicReference<>();
        filter.doFilter(undeclared(new byte[LIMIT]), new MockHttpServletResponse(),
                (req, res) -> reached.set(req));

        HttpServletRequest bounded = (HttpServletRequest) reached.get();
        InputStream stream = bounded.getInputStream();
        assertThat(stream.readAllBytes()).hasSize(LIMIT);
        assertThat(stream.read()).isEqualTo(-1);
        assertThat(bounded.getInputStream()).as("one stream per request").isSameAs(stream);
    }

    @Test
    void the_reader_of_an_undeclared_body_is_bounded_and_decodes_its_charset() throws Exception {
        MockHttpServletRequest small = undeclared("é{}".getBytes(StandardCharsets.UTF_8));
        small.setCharacterEncoding((String) null);
        AtomicReference<ServletRequest> reached = new AtomicReference<>();
        filter.doFilter(small, new MockHttpServletResponse(), (req, res) -> reached.set(req));
        try (BufferedReader reader = ((HttpServletRequest) reached.get()).getReader()) {
            assertThat(reader.readLine()).isEqualTo("é{}");
        }

        MockHttpServletRequest latin = undeclared("é".getBytes(StandardCharsets.ISO_8859_1));
        latin.setCharacterEncoding("ISO-8859-1");
        filter.doFilter(latin, new MockHttpServletResponse(), (req, res) -> reached.set(req));
        try (BufferedReader reader = ((HttpServletRequest) reached.get()).getReader()) {
            assertThat(reader.readLine()).isEqualTo("é");
        }

        filter.doFilter(undeclared(new byte[LIMIT + 1]), new MockHttpServletResponse(),
                (req, res) -> reached.set(req));
        BufferedReader oversized = ((HttpServletRequest) reached.get()).getReader();
        char[] buffer = new char[8192];
        assertThatThrownBy(() -> {
            while (oversized.read(buffer) >= 0) {
                // drain until the bound is crossed
            }
        }).isInstanceOf(ScimRequestBodyTooLargeException.class);
    }

    @Test
    void the_bounded_stream_delegates_the_asynchronous_read_api() throws Exception {
        ServletInputStream delegate = mock(ServletInputStream.class);
        when(delegate.isFinished()).thenReturn(true, false);
        when(delegate.isReady()).thenReturn(false, true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/scim/v2/Users") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }

            @Override
            public ServletInputStream getInputStream() {
                return delegate;
            }
        };
        AtomicReference<ServletRequest> reached = new AtomicReference<>();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> reached.set(req));

        ServletInputStream bounded = ((HttpServletRequest) reached.get()).getInputStream();
        assertThat(bounded).isNotSameAs(delegate);
        assertThat(bounded.isFinished()).isTrue();
        assertThat(bounded.isFinished()).isFalse();
        assertThat(bounded.isReady()).isFalse();
        assertThat(bounded.isReady()).isTrue();
        ReadListener listener = mock(ReadListener.class);
        bounded.setReadListener(listener);
        verify(delegate).setReadListener(listener);
    }

    private static MockHttpServletRequest request(byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/scim/v2/Users");
        request.setContent(body);
        return request;
    }

    /** A request whose body arrives with no declared length, as a chunked one does. */
    private static MockHttpServletRequest undeclared(byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/scim/v2/Users") {
            @Override
            public int getContentLength() {
                return -1;
            }

            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContent(body);
        return request;
    }
}
