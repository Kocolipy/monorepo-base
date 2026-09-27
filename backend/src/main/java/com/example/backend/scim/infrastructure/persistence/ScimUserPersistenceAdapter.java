package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.domain.DuplicateUserNameException;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimEmail;
import com.example.backend.scim.domain.ScimLoginState;
import com.example.backend.scim.domain.ScimName;
import com.example.backend.scim.domain.ScimPageRequest;
import com.example.backend.scim.domain.ScimResourceType;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserProfile;
import com.example.backend.scim.domain.ScimUserRepository;
import com.example.backend.scim.infrastructure.persistence.entity.ScimLoginStateValue;
import com.example.backend.scim.infrastructure.persistence.entity.ScimResourceEntity;
import com.example.backend.scim.infrastructure.persistence.entity.ScimUserEmailValue;
import com.example.backend.scim.infrastructure.persistence.entity.ScimUserEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/** Maps the SCIM User port onto the normalized JPA model. */
@Repository
class ScimUserPersistenceAdapter implements ScimUserRepository {

    /**
     * The order the port promises. Named here because the derived query method name
     * already states it; this is what the {@link ScimOffsetPage} carries so the two
     * cannot disagree.
     */
    private static final Sort BY_NORMALIZED_USER_NAME = Sort.by("normalizedUserName");

    private final ScimUserJpaRepository users;
    private final ScimResourceJpaRepository resources;

    ScimUserPersistenceAdapter(
            ScimUserJpaRepository users, ScimResourceJpaRepository resources) {
        this.users = users;
        this.resources = resources;
    }

    /**
     * Writes the resource row and the User row, and reports the constraint violation as
     * a duplicate {@code userName}.
     *
     * <p>Passes a literal {@code null} reservation rather than reading one off the
     * argument. That is what makes "a provisioned resource cannot protect itself from
     * being changed" a property of this adapter instead of a property of whichever use
     * case happens to call it: {@link #createReserved} is the only path to a marker, and
     * it is reachable only from seeding.
     *
     * <p>{@code saveAndFlush} rather than {@code save}: the INSERT has to reach the
     * database inside this call for the violation to be attributable to it. A deferred
     * flush would surface the same violation at commit, by which time the create use
     * case has returned and the refusal could no longer be rendered as a
     * {@code 409 uniqueness} — it would be a 500 from a transaction that failed after
     * the response was decided.
     *
     * <p>The only unique constraint this INSERT can violate is the normalized
     * {@code userName}: the resource id and the User's primary key are freshly
     * generated UUIDs, and the emails' uniqueness was resolved by
     * {@link ScimEmail#canonical(List)} before the profile existed. So a violation here
     * has exactly one cause, and translating it to one exception is not a guess.
     */
    @Override
    public ScimUser create(ScimUser user) {
        return insert(user, null);
    }

    /**
     * Writes a reserved User — the Bootstrap Admin.
     *
     * <p>A violation here has one more possible cause than in {@link #create}: the
     * reservation is unique too, so a second attempt to seed the same recovery identity
     * violates it. Both causes mean the same thing to seeding, which is idempotent and
     * treats either as "it is already there", so both arrive as the same exception rather
     * than being distinguished by inspecting the constraint name — a string the database
     * owns and could change.
     */
    @Override
    public ScimUser createReserved(ScimUser user, ReservedResourceName reservedName) {
        return insert(user, reservedName);
    }

    @Override
    public Optional<ScimUser> findById(UUID id) {
        return users.findById(id).map(ScimUserPersistenceAdapter::toDomain);
    }

    /**
     * Locks the resource row, then reads the User. The lock query is the transaction's first read
     * of the resource, so the version it returns is the one the previous lock holder committed.
     */
    @Override
    public Optional<ScimUser> findByIdForUpdate(UUID id) {
        if (resources.lockById(id).isEmpty()) {
            return Optional.empty();
        }
        return findById(id);
    }

    /**
     * Writes the changed profile columns and credential, then advances the version once.
     *
     * <p>The entity is {@code @DynamicUpdate}, so the UPDATE names only the columns that differ
     * from what was loaded. That is what keeps this write from touching the failure-run columns
     * the login path writes without the resource lock: they are not assigned here, so they are
     * not dirty, so they are not written.
     *
     * <p>{@code saveAndFlush} so a taken {@code userName} surfaces here, attributable, rather than
     * at commit after the response is decided.
     */
    @Override
    public Optional<ScimUser> replace(ScimUser user, Instant now) {
        Optional<ScimUserEntity> stored = users.findById(user.id());
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        ScimUserEntity entity = stored.get();
        ScimUserProfile profile = user.profile();
        ScimName name = profile.name();
        entity.replaceProfile(
                profile.userName(),
                profile.normalizedUserName().value(),
                profile.active(),
                profile.displayName(),
                name.formatted(),
                name.familyName(),
                name.givenName(),
                name.middleName(),
                name.honorificPrefix(),
                name.honorificSuffix(),
                profile.preferredLanguage(),
                profile.locale(),
                profile.timezone(),
                emailValues(profile.emails()));
        entity.getLogin().replacePasswordHash(user.login().passwordHash());
        try {
            users.saveAndFlush(entity);
        } catch (DataIntegrityViolationException violation) {
            throw new DuplicateUserNameException(violation);
        }
        resources.advanceVersions(List.of(user.id()), now);
        return findById(user.id());
    }

    @Override
    public Optional<ScimUser> findByNormalizedUserName(NormalizedUserName normalizedUserName) {
        return users.findByNormalizedUserName(normalizedUserName.value())
                .map(ScimUserPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<ScimUser> findByReservedName(ReservedResourceName reservedName) {
        return users.findByResource_ReservedName(reservedName.storedValue())
                .map(ScimUserPersistenceAdapter::toDomain);
    }

    /**
     * Writes the authentication-state columns alone, leaving the resource row untouched.
     *
     * <p>The absence of a version bump here is the behaviour, not an omission: a failure
     * run and a lock instant are invisible to a SCIM client, so advancing the ETag would
     * make every rejected login look like a representation change.
     *
     * <p>A write that matches no row is silently nothing. The login path reads the User
     * and then writes it, so a zero here means the User was deleted in between — in which
     * case there is no failure run left to record and nothing for this to report.
     */
    @Override
    public void updateLoginState(UUID id, ScimLoginState loginState) {
        // The hash is deliberately not passed: see the port. Writing the hash read at the
        // start of a login attempt would revert a password changed while the attempt ran.
        users.updateLoginState(
                id,
                loginState.failedLoginAttempts(),
                loginState.lockedAt());
    }

    /**
     * Writes {@code active} and advances the resource's version, because {@code active}
     * is a SCIM attribute and deactivating a User changes what a connector reads.
     *
     * <p>Re-reads rather than returning a locally-assembled value: the version the caller
     * needs is the one the database just computed, and the increment happens there so a
     * concurrent bump cannot be lost.
     *
     * <p>Empty when no row matched, which the use case renders as a {@code 404}. Checking
     * the update's own row count rather than reading first is what keeps the answer from
     * being stale between the check and the write.
     */
    @Override
    public Optional<ScimUser> updateActive(UUID id, boolean active, Instant now) {
        if (users.updateActive(id, active) == 0) {
            return Optional.empty();
        }
        resources.advanceVersions(List.of(id), now);
        return findById(id);
    }

    @Override
    public List<ScimUser> findAllOrderedByNormalizedUserName() {
        return users.findAll(BY_NORMALIZED_USER_NAME).stream()
                .map(ScimUserPersistenceAdapter::toDomain)
                .toList();
    }

    /**
     * One page of Users, in the port's stable order.
     *
     * <p>No {@code count == 0} guard here. {@link
     * com.example.backend.scim.application.ScimUserService#list} already answers that
     * case without calling the port at all — a zero-size page is a request for
     * {@code totalResults} alone — so a second guard in the adapter is unreachable,
     * and an unreachable guard is worse than none: it reads as the rule's home while
     * the rule actually lives in the use case.
     */
    @Override
    public List<ScimUser> findPage(ScimPageRequest page) {
        return users.findAllByOrderByNormalizedUserNameAsc(
                        ScimOffsetPage.of(page.offset(), page.count(), BY_NORMALIZED_USER_NAME))
                .stream()
                .map(ScimUserPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return users.count();
    }

    private ScimUser insert(ScimUser user, ReservedResourceName reservedName) {
        try {
            return toDomain(users.saveAndFlush(toEntity(user, reservedName)));
        } catch (DataIntegrityViolationException violation) {
            throw new DuplicateUserNameException(violation);
        }
    }

    private static ScimUserEntity toEntity(ScimUser user, ReservedResourceName reservedName) {
        ScimResourceEntity resource = new ScimResourceEntity(
                user.id(),
                ScimResourceType.USER.resourceTypeName(),
                user.version(),
                user.createdAt(),
                user.lastModifiedAt(),
                reservedName == null ? null : reservedName.storedValue());
        ScimUserProfile profile = user.profile();
        ScimName name = profile.name();
        ScimLoginState login = user.login();
        return new ScimUserEntity(
                resource,
                profile.userName(),
                profile.normalizedUserName().value(),
                new ScimLoginStateValue(
                        login.passwordHash(), login.failedLoginAttempts(), login.lockedAt()),
                profile.active(),
                profile.displayName(),
                name.formatted(),
                name.familyName(),
                name.givenName(),
                name.middleName(),
                name.honorificPrefix(),
                name.honorificSuffix(),
                profile.preferredLanguage(),
                profile.locale(),
                profile.timezone(),
                emailValues(profile.emails()));
    }

    private static List<ScimUserEmailValue> emailValues(List<ScimEmail> emails) {
        return emails.stream()
                .map(email -> new ScimUserEmailValue(email.value(), email.type(), email.primary()))
                .toList();
    }

    private static ScimUser toDomain(ScimUserEntity entity) {
        ScimResourceEntity resource = entity.getResource();
        ScimUserProfile profile = new ScimUserProfile(
                entity.getUserName(),
                new ScimName(
                        entity.getFormattedName(),
                        entity.getFamilyName(),
                        entity.getGivenName(),
                        entity.getMiddleName(),
                        entity.getHonorificPrefix(),
                        entity.getHonorificSuffix()),
                entity.getDisplayName(),
                entity.getPreferredLanguage(),
                entity.getLocale(),
                entity.getTimezone(),
                entity.isActive(),
                entity.getEmails().stream()
                        .map(email -> new ScimEmail(
                                email.getValue(), email.getType(), email.isPrimary()))
                        .toList());
        ScimLoginStateValue login = entity.getLogin();
        return new ScimUser(
                resource.getId(),
                profile,
                new ScimLoginState(
                        login.getPasswordHash(),
                        login.getFailedLoginAttempts(),
                        login.getLockedAt()),
                ReservedResourceName.ofStoredValue(resource.getReservedName()).orElse(null),
                resource.getVersion(),
                resource.getCreatedAt(),
                resource.getLastModifiedAt());
    }
}
