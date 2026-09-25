package com.example.backend;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The one place a Postgres container is declared for tests.
 *
 * <p>{@code ddl-auto: validate} plus Flyway migrations written in PostgreSQL
 * syntax (gen_random_uuid(), ON DELETE CASCADE) mean the test database has to
 * genuinely be Postgres — H2 cannot run {@code db/migration} as shipped, and a
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
 */
public class ContainerTestConfiguration {

    private static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse("postgres:18.6-alpine");

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(POSTGRES_IMAGE);
    }
}
