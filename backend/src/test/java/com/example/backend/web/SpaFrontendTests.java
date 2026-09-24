package com.example.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import jakarta.servlet.RequestDispatcher;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ModelAndView;

@SpringBootTest
class SpaFrontendTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private SpaErrorViewResolver errorViewResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void servesFrontendEntryPointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    /**
     * "/api" itself is as much of an API path as anything beneath it, so the
     * public-GET allowance must not swallow it.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/api", "/api/count", "/api/session"})
    void keepsApiRoutesProtected(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Unmatched routes belong to the client-side router. A single-segment route
     * has nothing before the separator, and a dot in an earlier segment does not
     * make the route a file request.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/dashboard", "/account/settings", "/v1.0/settings"})
    void forwardsMissingClientSideRoutesToFrontendEntryPoint(String path) {
        ModelAndView resolved = resolve(path, HttpStatus.NOT_FOUND);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getViewName()).isEqualTo("forward:/index.html");
    }

    /** A missing file stays a 404 rather than returning the HTML shell. */
    @ParameterizedTest
    @ValueSource(strings = {"/assets/missing.js", "/favicon.ico"})
    void doesNotForwardPathsThatLookLikeFiles(String path) {
        assertThat(resolve(path, HttpStatus.NOT_FOUND)).isNull();
    }

    /** Reserved prefixes answer for themselves, exact match included. */
    @ParameterizedTest
    @ValueSource(strings = {"/api", "/api/missing", "/actuator", "/actuator/health"})
    void doesNotForwardReservedServerPaths(String path) {
        assertThat(resolve(path, HttpStatus.NOT_FOUND)).isNull();
    }

    /** The shell stands in for a missing route only, not for a failed one. */
    @Test
    void doesNotForwardErrorsOtherThanNotFound() {
        assertThat(resolve("/dashboard", HttpStatus.INTERNAL_SERVER_ERROR)).isNull();
    }

    @Test
    void doesNotForwardWhenTheErrorCarriesNoPath() {
        assertThat(errorViewResolver.resolveErrorView(
                new MockHttpServletRequest(), HttpStatus.NOT_FOUND, Map.of()))
                .isNull();
    }

    private ModelAndView resolve(String path, HttpStatus status) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, path);
        return errorViewResolver.resolveErrorView(request, status, Map.of());
    }
}
