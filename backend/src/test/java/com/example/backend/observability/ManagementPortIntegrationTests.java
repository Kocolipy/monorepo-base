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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
 * <p>The Admin test runs first on purpose. {@code ManagementSessionConfiguration} runs
 * once, while this class's context boots, and PIT credits that boot to whichever test
 * method triggered it and re-runs only that method against each mutant. Running the one
 * test that depends on the session filter first is what lets mutation testing see it.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0")
@Import(ContainerTestConfiguration.class)
@AutoConfigureMetrics
class ManagementPortIntegrationTests {

    private static final String ADMIN = "test-admin";

    private static final String ADMIN_PASSWORD = "test-admin-password";

    @LocalServerPort
    private int applicationPort;

    @LocalManagementPort
    private int managementPort;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void the_scrape_moves_off_the_application_port() throws Exception {
        assertThat(managementPort).isNotEqualTo(applicationPort);
        assertThat(get(applicationPort, "/actuator/prometheus").statusCode())
                .as("the application port no longer serves actuator")
                .isIn(401, 404);
        assertThat(get(applicationPort, "/actuator/prometheus").body())
                .doesNotContain("http_server_requests");
    }

    @Test
    void the_management_port_still_requires_authentication_for_the_scrape() throws Exception {
        HttpResponse<String> scrape = get(managementPort, "/actuator/prometheus");

        assertThat(scrape.statusCode()).isEqualTo(401);
        assertThat(scrape.body()).doesNotContain("http_server_requests");
    }

    @Test
    @Order(1)
    void an_admin_session_reads_the_scrape_on_the_management_port() throws Exception {
        // Cookies are scoped to the host, not the port (RFC 6265 §8.5), so a session
        // opened on the application port is presented to the management port as well.
        CookieManager jar = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient admin = HttpClient.newBuilder().cookieHandler(jar).build();
        admin.send(request(applicationPort, "/api/auth/me").GET().build(),
                HttpResponse.BodyHandlers.ofString());
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

        HttpResponse<String> scrape = admin.send(
                request(managementPort, "/actuator/prometheus").GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(scrape.statusCode()).isEqualTo(200);
        assertThat(scrape.body()).contains("http_server_requests_seconds_count");
    }

    @Test
    void health_stays_public_on_the_management_port() throws Exception {
        assertThat(get(managementPort, "/actuator/health").statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> get(int port, String path) throws Exception {
        return client.send(request(port, path).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpRequest.Builder request(int port, String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }
}
