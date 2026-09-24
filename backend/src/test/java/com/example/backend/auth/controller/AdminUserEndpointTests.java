package com.example.backend.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The endpoint as a caller meets it: the real filter chain, the real controller,
 * the application's own JSON converters, over the accounts startup seeding
 * created.
 *
 * <p>{@code AdminUserControllerTests} covers what the controller returns and
 * {@code SecurityConfigTests} covers which paths the chain guards. What neither
 * can see is the two composed — a response that is correct but reachable by the
 * wrong caller, or authorized but carrying the wrong JSON.
 *
 * <p>The whole web application context is wired in rather than a standalone
 * controller precisely so the wire format is the deployed one: a hand-built
 * MockMvc would render {@code createdAt} as a number, and these assertions would
 * then describe a format the running service does not produce.
 */
@SpringBootTest
class AdminUserEndpointTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void anAdministratorSeesEveryAccountWithItsRoleStatusAndCreationDate() throws Exception {
        mvc.perform(get("/api/admin/users").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username")
                        .value(Matchers.hasItems("test-admin", "test-user")))
                .andExpect(jsonPath("$[?(@.username == 'test-user')].email")
                        .value(Matchers.contains("test-user@example.com")))
                .andExpect(jsonPath("$[?(@.username == 'test-user')].role")
                        .value(Matchers.contains("USER")))
                .andExpect(jsonPath("$[?(@.username == 'test-admin')].role")
                        .value(Matchers.contains("ADMIN")))
                .andExpect(jsonPath("$[?(@.username == 'test-user')].enabled")
                        .value(Matchers.contains(true)))
                // ISO-8601 rather than an epoch number, which is what the SPA and
                // the OpenAPI document both describe.
                .andExpect(jsonPath("$[?(@.username == 'test-user')].createdAt")
                        .value(Matchers.contains(Matchers.matchesPattern(
                                "\\d{4}-\\d{2}-\\d{2}T.*Z"))));
    }

    /** The acceptance criterion that matters most: no hash on the wire, ever. */
    @Test
    void theListingNeverCarriesAPasswordHash() throws Exception {
        mvc.perform(get("/api/admin/users").session(authenticatedSession("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("password"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("$2a$"))))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void anAuthenticatedNonAdministratorIsForbidden() throws Exception {
        mvc.perform(get("/api/admin/users").session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnauthenticatedCallerIsUnauthorized() throws Exception {
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession authenticatedSession(String authority) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken("account", null, authority));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context);
        return session;
    }
}
