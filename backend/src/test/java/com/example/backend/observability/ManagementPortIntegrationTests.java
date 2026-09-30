package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.ContainerTestConfiguration;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * The internal-interface deployment: actuator moved to its own port with
 * {@code MANAGEMENT_SERVER_PORT}, as infra/README.md's "Operational telemetry" section
 * describes.
 *
 * <p>What the documentation promises and this pins: moving the scrape off the public port
 * takes it off the public port — the application port no longer serves it — and does NOT
 * take it out from behind the Admin gate, because the application chain guards the
 * management port too. A network boundary is added, never swapped for the access rule —
 * and the gate still admits an Admin session there, so Prometheus has a working path.
 *
 * <p>Every expectation sits in ONE test method, with soft assertions so each still reports
 * on its own. {@code ManagementSessionConfiguration} runs once, while this class's context
 * boots. PIT credits that boot to whichever test method triggered it and re-runs only that
 * method against each mutant, so an expectation in any other method is invisible to
 * mutation testing. Splitting this class up again would let those mutants survive.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0")
@Import(ContainerTestConfiguration.class)
@AutoConfigureMetrics
class ManagementPortIntegrationTests {

    private static final String ADMIN = "test-admin";

    private static final String ADMIN_PASSWORD = "test-admin-password";

    private static final String REQUESTS = "http_server_requests_seconds_count";

    @LocalServerPort
    private int applicationPort;

    @LocalManagementPort
    private int managementPort;

    private final HttpClient anonymous = HttpClient.newHttpClient();

    @Test
    void the_scrape_moves_to_the_management_port_and_stays_behind_the_admin_gate()
            throws Exception {
        SoftAssertions softly = new SoftAssertions();

        // Off the application port.
        softly.assertThat(managementPort).isNotEqualTo(applicationPort);
        HttpResponse<String> onApplicationPort =
                get(anonymous, applicationPort, "/actuator/prometheus");
        softly.assertThat(onApplicationPort.statusCode())
                .as("the application port no longer serves actuator")
                .isIn(401, 404);
        softly.assertThat(onApplicationPort.body()).doesNotContain(REQUESTS);

        // Still behind the gate on the management port.
        HttpResponse<String> anonymousScrape =
                get(anonymous, managementPort, "/actuator/prometheus");
        softly.assertThat(anonymousScrape.statusCode())
                .as("anonymous scrape on the management port")
                .isEqualTo(401);
        softly.assertThat(anonymousScrape.body()).doesNotContain(REQUESTS);

        // Health stays public, so the ALB health check keeps working on the new port.
        softly.assertThat(get(anonymous, managementPort, "/actuator/health").statusCode())
                .as("health on the management port")
                .isEqualTo(200);

        // An Admin session is admitted. Cookies are scoped to the host, not the port
        // (RFC 6265 §8.5), so a session opened on the application port is presented to
        // the management port as well.
        HttpClient admin = adminSession();
        HttpResponse<String> adminScrape = get(admin, managementPort, "/actuator/prometheus");
        softly.assertThat(adminScrape.statusCode())
                .as("Admin scrape on the management port")
                .isEqualTo(200);
        softly.assertThat(adminScrape.body()).contains(REQUESTS);

        softly.assertAll();
    }

    /** A password login through the real CSRF double-submit, as the SPA does it. */
    private HttpClient adminSession() throws Exception {
        CookieManager jar = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient admin = HttpClient.newBuilder().cookieHandler(jar).build();
        get(admin, applicationPort, "/api/auth/me");
        String xsrf = jar.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no XSRF-TOKEN cookie was issued"));
        HttpResponse<String> login = admin.send(request(applicationPort, "/api/auth/login")
                .header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", xsrf)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"" + ADMIN + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).as("the Admin login itself").isEqualTo(200);
        return admin;
    }

    private static HttpResponse<String> get(HttpClient client, int port, String path)
            throws Exception {
        return client.send(request(port, path).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpRequest.Builder request(int port, String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }
}
