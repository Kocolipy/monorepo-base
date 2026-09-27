package com.example.backend.counter.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.scim.ScimIdentities;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Two of the seeded identities ({@code test-user}, {@code test-admin}) back the
 * single-user assertions below; a counter no longer exists independent of an
 * identity, since it is keyed by the SCIM User's stable resource id, so a test
 * needing a second independent user creates one through the real SCIM User
 * repository rather than inventing an arbitrary userName with nothing behind it.
 */
@SpringBootTest
@Import(com.example.backend.ContainerTestConfiguration.class)
@Transactional
class UserCounterServiceTests {

    @Autowired
    private UserCounterService service;

    @Autowired
    private ScimUserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Test
    void getsExistingCountWithoutChangingIt() {
        service.increment("test-user");
        service.increment("test-user");

        assertThat(service.getCount("test-user")).isEqualTo(2);
        assertThat(service.getCount("test-user")).isEqualTo(2);
    }

    @Test
    void getsZeroWhenUserHasNoCounterYet() {
        assertThat(service.getCount("test-user")).isZero();
    }

    @Test
    void maintainsIndependentCountsForEachUser() {
        assertThat(service.increment("test-user")).isEqualTo(1);
        assertThat(service.increment("test-user")).isEqualTo(2);
        assertThat(service.increment("test-admin")).isEqualTo(1);
    }

    @Test
    void resetSetsExistingCountToZero() {
        service.increment("test-user");
        service.increment("test-user");

        assertThat(service.reset("test-user")).isZero();
        assertThat(service.increment("test-user")).isEqualTo(1);
    }

    @Test
    void resetCreatesAZeroCountWhenUserHasNoCounterYet() {
        assertThat(service.reset("test-user")).isZero();
        assertThat(service.increment("test-user")).isEqualTo(1);
    }

    /**
     * The counter is keyed by the SCIM User's stable resource id. Renaming the
     * identity must not disconnect the tally from it.
     *
     * <p>The rename is written straight against the {@code scim_users} row, because
     * no production path changes a {@code userName}: the User port exposes only the
     * login-state and {@code active} writes, and the SCIM surface implements create,
     * read and list. That is the point of the assertion rather than a shortcut
     * around one — the tally has to survive the column changing under it, however
     * the change arrives.
     */
    @Test
    void survivesAUsernameChangeMadeDirectlyAgainstTheStore() {
        ScimUser created = users.create(ScimUser.created(
                UUID.randomUUID(),
                ScimIdentities.profile("original-name", true),
                "hash",
                ScimIdentities.NOW));
        service.increment("original-name");
        service.increment("original-name");

        rename(created.id(), "renamed");

        assertThat(service.getCount("renamed")).isEqualTo(2);
        assertThat(service.increment("renamed")).isEqualTo(3);
    }

    private void rename(UUID userId, String newUserName) {
        // The insert above is still in the persistence context; the UPDATE below
        // goes straight to the database on the same connection, so it has to see
        // the row.
        entityManager.flush();
        jdbc.update(
                "update scim_users set user_name = ?, normalized_user_name = ?"
                        + " where resource_id = ?",
                newUserName,
                NormalizedUserName.of(newUserName).value(),
                userId);
        // And the renamed row must be re-read rather than answered from the copy
        // Hibernate loaded before the rename.
        entityManager.clear();
    }
}
