package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.domain.DuplicateUserNameException;
import com.example.backend.scim.domain.ScimEmail;
import com.example.backend.scim.domain.ScimName;
import com.example.backend.scim.domain.ScimPageRequest;
import com.example.backend.scim.domain.ScimResourceType;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserProfile;
import com.example.backend.scim.domain.ScimUserRepository;
import com.example.backend.scim.infrastructure.persistence.entity.ScimResourceEntity;
import com.example.backend.scim.infrastructure.persistence.entity.ScimUserEmailValue;
import com.example.backend.scim.infrastructure.persistence.entity.ScimUserEntity;
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

    ScimUserPersistenceAdapter(ScimUserJpaRepository users) {
        this.users = users;
    }

    /**
     * Writes the resource row and the User row, and reports the constraint violation as
     * a duplicate {@code userName}.
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
        try {
            return toDomain(users.saveAndFlush(toEntity(user)));
        } catch (DataIntegrityViolationException violation) {
            throw new DuplicateUserNameException(violation);
        }
    }

    @Override
    public Optional<ScimUser> findById(UUID id) {
        return users.findById(id).map(ScimUserPersistenceAdapter::toDomain);
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

    private static ScimUserEntity toEntity(ScimUser user) {
        ScimResourceEntity resource = new ScimResourceEntity(
                user.id(),
                ScimResourceType.USER.resourceTypeName(),
                user.version(),
                user.createdAt(),
                user.lastModifiedAt());
        ScimUserProfile profile = user.profile();
        ScimName name = profile.name();
        return new ScimUserEntity(
                resource,
                profile.userName(),
                profile.normalizedUserName().value(),
                user.passwordHash(),
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
                profile.emails().stream()
                        .map(email -> new ScimUserEmailValue(
                                email.value(), email.type(), email.primary()))
                        .toList());
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
        return new ScimUser(
                resource.getId(),
                profile,
                entity.getPasswordHash(),
                resource.getVersion(),
                resource.getCreatedAt(),
                resource.getLastModifiedAt());
    }
}
