package com.example.backend.scim;

import com.example.backend.scim.domain.DuplicateUserNameException;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimLoginState;
import com.example.backend.scim.domain.ScimPageRequest;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserProfile;
import com.example.backend.scim.domain.ScimUserRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * SCIM User store for tests, standing in for the JPA adapter. Shared so the lockout and the
 * authority derivation are asserted against one persistence behaviour rather than against a
 * slightly different fake per test class.
 *
 * <p>Keyed by the resource's stable id, exactly as the real table is: {@code userName} is a
 * mutable attribute looked up by scanning, not the row's identity.
 *
 * <p>Two behaviours here are load-bearing rather than conveniences, because a fake that got
 * them wrong would hide the defect the real design exists to prevent:
 *
 * <ul>
 *   <li>{@link #updateLoginState} writes the login columns ONLY and does not advance the
 *       version. A fake that replaced the whole row would hide the lost update the narrow
 *       port prevents, and one that bumped the version would hide that a failed login must
 *       not move a connector's ETag.
 *   <li>{@link #create} refuses a reservation and {@link #createReserved} applies one, so a
 *       test cannot accidentally mint a protected resource through the provisioning path —
 *       which is the guarantee the real adapter makes by passing a literal null.
 * </ul>
 */
public final class InMemoryScimUserRepository implements ScimUserRepository {

    private final Map<UUID, ScimUser> stored = new LinkedHashMap<>();

    private int writes;

    @Override
    public ScimUser create(ScimUser user) {
        return insert(user, null);
    }

    @Override
    public ScimUser createReserved(ScimUser user, ReservedResourceName reservedName) {
        if (findByReservedName(reservedName).isPresent()) {
            throw new DuplicateUserNameException(
                    new IllegalStateException("that reservation is already held"));
        }
        return insert(user, reservedName);
    }

    @Override
    public Optional<ScimUser> findById(UUID id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public Optional<ScimUser> findByNormalizedUserName(NormalizedUserName normalizedUserName) {
        return stored.values().stream()
                .filter(user -> user.profile().normalizedUserName().equals(normalizedUserName))
                .findFirst();
    }

    @Override
    public Optional<ScimUser> findByReservedName(ReservedResourceName reservedName) {
        return stored.values().stream()
                .filter(user -> reservedName == user.reservedName())
                .findFirst();
    }

    @Override
    public void updateLoginState(UUID id, ScimLoginState loginState) {
        ScimUser current = stored.get(id);
        if (current == null) {
            // The real adapter's UPDATE matches no row and reports nothing, because the User
            // was deleted between the read and the write. Silence is the behaviour.
            return;
        }
        stored.put(id, new ScimUser(
                current.id(),
                current.profile(),
                // The stored hash is kept, as the adapter keeps it: this write carries the failure
                // run only, so it cannot revert a password changed while a login attempt ran.
                new ScimLoginState(
                        current.login().passwordHash(),
                        loginState.failedLoginAttempts(),
                        loginState.lockedAt()),
                current.reservedName(),
                // Deliberately unchanged: a failure run is not a SCIM attribute.
                current.version(),
                current.createdAt(),
                current.lastModifiedAt()));
        writes++;
    }

    @Override
    public Optional<ScimUser> updateActive(UUID id, boolean active, Instant now) {
        ScimUser current = stored.get(id);
        if (current == null) {
            return Optional.empty();
        }
        ScimUser updated = new ScimUser(
                current.id(),
                withActive(current.profile(), active),
                current.login(),
                current.reservedName(),
                // `active` IS a SCIM attribute, so the version moves — unlike the login state.
                current.version() + 1,
                current.createdAt(),
                now);
        stored.put(id, updated);
        writes++;
        return Optional.of(updated);
    }

    /** No lock to take in memory; a single-threaded test has no second writer to exclude. */
    @Override
    public Optional<ScimUser> findByIdForUpdate(UUID id) {
        return findById(id);
    }

    /**
     * Replaces the profile and credential, keeping the STORED failure run — the port's promise
     * that a replacement never writes those two — and advancing the version once.
     */
    @Override
    public Optional<ScimUser> replace(ScimUser user, Instant now) {
        ScimUser current = stored.get(user.id());
        if (current == null) {
            return Optional.empty();
        }
        if (stored.values().stream().anyMatch(other -> !other.id().equals(user.id())
                && other.profile().normalizedUserName()
                        .equals(user.profile().normalizedUserName()))) {
            throw new com.example.backend.scim.domain.DuplicateUserNameException(null);
        }
        ScimUser updated = new ScimUser(
                current.id(),
                user.profile(),
                new ScimLoginState(
                        user.login().passwordHash(),
                        current.login().failedLoginAttempts(),
                        current.login().lockedAt()),
                current.reservedName(),
                current.version() + 1,
                current.createdAt(),
                now);
        stored.put(user.id(), updated);
        writes++;
        return Optional.of(updated);
    }

    /** Removes the User; the Groups it belonged to are the in-memory Group repository's concern. */
    @Override
    public void deleteById(UUID id, Instant now) {
        stored.remove(id);
        writes++;
    }

    @Override
    public List<ScimUser> findAllOrderedByNormalizedUserName() {
        return stored.values().stream()
                .sorted(Comparator.comparing(user -> user.profile().normalizedUserName().value()))
                .toList();
    }

    @Override
    public List<ScimUser> findAllById(List<UUID> ids) {
        return ids.stream().map(stored::get).filter(java.util.Objects::nonNull).toList();
    }

    /** How many Users are stored. */
    public long size() {
        return stored.size();
    }

    /** The stored User, failing the calling test when there is none. */
    public ScimUser require(String userName) {
        return findByNormalizedUserName(NormalizedUserName.of(userName)).orElseThrow(
                () -> new AssertionError("No SCIM User stored for " + userName));
    }

    /** Puts a User in the store directly, for a test arranging state rather than exercising a write. */
    public ScimUser given(ScimUser user) {
        stored.put(user.id(), user);
        return user;
    }

    /**
     * How many writes this store has taken. Lets a test assert that a login with nothing to
     * clear writes nothing, which is the only observable difference between skipping the write
     * and performing a redundant one.
     */
    public int writes() {
        return writes;
    }

    private ScimUser insert(ScimUser user, ReservedResourceName reservedName) {
        ScimUser nonNull = Objects.requireNonNull(user);
        if (findByNormalizedUserName(nonNull.profile().normalizedUserName()).isPresent()) {
            throw new DuplicateUserNameException(
                    new IllegalStateException("that normalized userName is already held"));
        }
        ScimUser toStore = new ScimUser(
                nonNull.id(),
                nonNull.profile(),
                nonNull.login(),
                reservedName,
                nonNull.version(),
                nonNull.createdAt(),
                nonNull.lastModifiedAt());
        stored.put(toStore.id(), toStore);
        writes++;
        return toStore;
    }

    /**
     * A profile with {@code active} replaced, which the domain has no transition for because
     * nothing in production needs one: the real adapter writes the column and re-reads the row,
     * so the flipped value never passes through a domain object.
     */
    private static ScimUserProfile withActive(ScimUserProfile current, boolean active) {
        return new ScimUserProfile(
                current.userName(),
                current.name(),
                current.displayName(),
                current.preferredLanguage(),
                current.locale(),
                current.timezone(),
                active,
                current.emails());
    }
}
