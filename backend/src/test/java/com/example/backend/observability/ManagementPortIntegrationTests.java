package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.ContainerTestConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * management port too. A network boundary is added, never swapped for the access rule.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0")
@Import(ContainerTestConfiguration.class)
@AutoConfigureMetrics
class ManagementPortIntegrationTests {

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
    void health_stays_public_on_the_management_port() throws Exception {
        assertThat(get(managementPort, "/actuator/health").statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> get(int port, String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
