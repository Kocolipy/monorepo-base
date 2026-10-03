package com.example.backend;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The one place a Postgres container is declared for tests.
 *
 * <p>{@code ddl-auto: validate} plus a Flyway migration written in PostgreSQL
 * syntax (PL/pgSQL triggers, partial and expression indexes) means the test
 * database has to genuinely be Postgres — H2 cannot run {@code db/migration} as shipped, and a
 * dialect emulation layer would verify a schema the deployed service does not
 * have. {@code @ServiceConnection} registers the container's JDBC URl,
 * username and password directly onto {@code spring.datasource.*}, so no test
 * {@code application.yaml} needs to know the container exists.
 *
 * <p>{@code @Bean} rather than a JUnit {@code @Container} field: the bean is
 * created once per test {@code ApplicationContext}, and Spring's context cache
 * reuses that context — container included — across every {@code @SpringBootTest}
 * class that imports this configuration with identical context configuration,
 * rather than paying a fresh container start per class.
 *
 * <p>It also imports {@link SeededBootstrapAdminTestConfiguration}, so every context that gets a
 * database starts with the seeded Bootstrap Admin's first-login change already completed.
 */
@Import(SeededBootstrapAdminTestConfiguration.class)
public class ContainerTestConfiguration {

    private static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse("postgres:18.6-alpine");

    private static final DockerImageName REDIS_IMAGE =
            DockerImageName.parse("redis:8.2-alpine");

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(POSTGRES_IMAGE);
    }

    /**
     * The one place a Redis container is declared for tests, for the same reason
     * Postgres is: a container declared as a {@code @Bean} here is created once
     * per test {@code ApplicationContext} and shared through Spring's context
     * cache across every {@code @SpringBootTest} that imports this configuration
     * with identical context configuration.
     *
     * <p>Previously each Redis-backed integration class started its own
     * {@code GenericContainer} in a {@code static} block and bound it with
     * {@code @DynamicPropertySource}. A per-class dynamic property is a distinct
     * context-cache key, so every such class booted its own context (and needed
     * {@code @DirtiesContext} to evict the container-bound context afterwards).
     * Moving the container here collapses them onto the shared context instead.
     *
     * <p>{@code @ServiceConnection("redis")} names the connection type explicitly
     * because a {@code GenericContainer} — unlike {@code PostgreSQLContainer} —
     * carries no built-in service hint, so Spring Boot cannot infer it from the
     * container type alone.
     */
    @Bean
    @ServiceConnection("redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);
    }
}
