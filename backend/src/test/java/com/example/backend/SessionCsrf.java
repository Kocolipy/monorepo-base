package com.example.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The SPA's CSRF handshake, for a MockMvc test: ask {@code GET /api/auth/csrf} for the token
 * bound to the request's own session, and send it back in the header that endpoint names.
 *
 * <p>Deliberately the real round trip rather than a token minted from the repository bean or
 * Spring Security's {@code csrf()} post-processor. The token is bound to a session now, so a
 * test that minted its own would have to reach into that session to plant it — and would then
 * pass whatever the endpoint, the session binding or the masking did. Fetching it the way the
 * SPA does keeps every unsafe request in the suite a witness to that contract.
 *
 * <p>Applied when the request is performed, so it sees the session the test attached to the
 * builder whether that was before or after this call, and works for both ways a suite carries
 * one: a Spring Session cookie, or a {@link MockHttpSession} when the context runs no session
 * repository filter. A request carrying neither gets the session the fetch created — which is
 * what a guest's login needs — attached to it, exactly as a browser would carry it.
 */
public final class SessionCsrf {

    /** The endpoint the SPA fetches its token from. */
    public static final String PATH = "/api/auth/csrf";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private SessionCsrf() {
    }

    /** {@code request} with the token of its own session, fetched through {@code mvc}. */
    public static MockHttpServletRequestBuilder withCsrf(
            MockMvc mvc, MockHttpServletRequestBuilder request) {
        return request.with(from(mvc));
    }

    /** The handshake as a post-processor, for a builder chain that already uses {@code with}. */
    public static RequestPostProcessor from(MockMvc mvc) {
        return request -> {
            MvcResult fetched = fetch(mvc, request);
            carryTheSessionForward(fetched, request);
            JsonNode body = read(fetched);
            request.addHeader(body.get("headerName").asText(), body.get("token").asText());
            return request;
        };
    }

    private static MvcResult fetch(MockMvc mvc, MockHttpServletRequest request) {
        MockHttpServletRequestBuilder fetch = get(PATH);
        if (request.getCookies() != null) {
            fetch.cookie(request.getCookies());
        }
        if (request.getSession(false) instanceof MockHttpSession session) {
            fetch.session(session);
        }
        try {
            MvcResult result = mvc.perform(fetch).andReturn();
            if (result.getResponse().getStatus() != 200) {
                throw new AssertionError("GET " + PATH + " answered "
                        + result.getResponse().getStatus());
            }
            return result;
        } catch (Exception failed) {
            throw new IllegalStateException("GET " + PATH + " failed", failed);
        }
    }

    /**
     * Attaches the session the token belongs to. A cookie the fetch set replaces one of the same
     * name — a dead session id the test carried in is superseded by the session minted for it —
     * and a container session the fetch created becomes the request's own.
     */
    private static void carryTheSessionForward(MvcResult fetched, MockHttpServletRequest request) {
        Map<String, Cookie> cookies = new LinkedHashMap<>();
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies()).forEach(c -> cookies.put(c.getName(), c));
        }
        for (Cookie issued : fetched.getResponse().getCookies()) {
            if (issued.getMaxAge() != 0) {
                cookies.put(issued.getName(), new Cookie(issued.getName(), issued.getValue()));
            }
        }
        if (!cookies.isEmpty()) {
            request.setCookies(cookies.values().toArray(Cookie[]::new));
        }
        HttpSession created = fetched.getRequest().getSession(false);
        if (request.getSession(false) == null && created instanceof MockHttpSession session) {
            request.setSession(session);
        }
    }

    private static JsonNode read(MvcResult fetched) {
        try {
            return JSON.readTree(fetched.getResponse().getContentAsString());
        } catch (Exception unreadable) {
            throw new IllegalStateException("GET " + PATH + " returned no token body", unreadable);
        }
    }
}
