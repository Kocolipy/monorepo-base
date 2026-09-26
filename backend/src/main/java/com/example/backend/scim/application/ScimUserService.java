package com.example.backend.scim.application;

import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.scim.domain.AuthenticatedConnector;
import com.example.backend.scim.domain.DuplicateUserNameException;
import com.example.backend.scim.domain.ScimExternalIdRepository;
import com.example.backend.scim.domain.ScimPageRequest;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creating and reading SCIM Users, as a connector asks for it.
 *
 * <p>Every method takes the {@link AuthenticatedConnector} rather than reading it from
 * a security context, for two reasons that both matter. An {@code externalId} is
 * connector-scoped, so a read that did not know who was asking could not resolve the
 * alias — and the failure mode of getting it wrong is disclosing one connector's
 * namespace to another. And an audit event names the acting connector, so making the
 * actor a parameter means no code path exists that records a SCIM operation with no
 * actor.
 *
 * <p>The returned projections carry no credential: {@link ScimUserResource} has no
 * field a hash could occupy, so the plaintext this service hashes has nowhere to
 * reappear.
 */
@Service
public class ScimUserService {

    private final ScimUserRepository users;
    private final ScimExternalIdRepository aliases;
    private final AuditTrail audit;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public ScimUserService(
            ScimUserRepository users,
            ScimExternalIdRepository aliases,
            AuditTrail audit,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.users = users;
        this.aliases = aliases;
        this.audit = audit;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Creates a User, its connector alias and its audit event in one transaction.
     *
     * <p>The password is hashed before {@link ScimUser} is constructed, so the
     * plaintext exists only as a local in this method and in the command that was
     * handed to it. Nothing that outlives the call holds it — not the domain object,
     * not the row, not the projection returned.
     *
     * <p>A {@code userName} already taken arrives as
     * {@link DuplicateUserNameException} from the failed INSERT rather than from a
     * prior read. The refusal is audited in a transaction of its own — this one is
     * already doomed — and then re-thrown for the adapter to render as a SCIM
     * {@code uniqueness} error.
     */
    @Transactional
    public ScimUserResource create(AuthenticatedConnector connector, NewScimUser command) {
        ScimUser user = ScimUser.created(
                UUID.randomUUID(),
                command.profile(),
                hashed(command.password()),
                clock.instant());
        ScimUser created;
        try {
            created = users.create(user);
        } catch (DuplicateUserNameException duplicate) {
            audit.recordScimUserCreateRejectedAsDuplicate(connector.connectorId());
            throw duplicate;
        }
        if (command.externalId() != null) {
            aliases.put(connector.connectorId(), created.id(), command.externalId());
        }
        audit.recordScimUserCreated(connector.connectorId(), created.id());
        return ScimUserResource.of(created, command.externalId());
    }

    /**
     * One User by its stable id, as this connector sees it.
     *
     * <p>Deliberately not audited. A single-resource retrieval is the ordinary unit of
     * provisioning traffic; recording it would bury the collection reads that indicate
     * an enumeration under millions of reads that indicate nothing. The distinction is
     * stated in the specification plan's audit contract, and the absence of a call here
     * is where it is enforced.
     */
    @Transactional(readOnly = true)
    public Optional<ScimUserResource> findById(AuthenticatedConnector connector, UUID id) {
        return users.findById(id).map(user -> projection(connector, user));
    }

    /**
     * A page of Users, and the total the page came from.
     *
     * <p>Audited as a bulk read before the page is returned, whatever the page size and
     * whatever comes back. The audit call is inside the transaction that reads, so a
     * trail that cannot record the read is a read that does not complete.
     */
    @Transactional
    public ScimUserListing list(AuthenticatedConnector connector, ScimPageRequest page) {
        long total = users.countAll();
        List<ScimUserResource> resources = page.count() == 0
                ? List.of()
                : users.findPage(page).stream()
                        .map(user -> projection(connector, user))
                        .toList();
        audit.recordScimUsersListed(connector.connectorId());
        return new ScimUserListing(resources, total, page);
    }

    /** The stored User as this connector sees it, alias included. */
    private ScimUserResource projection(AuthenticatedConnector connector, ScimUser user) {
        return ScimUserResource.of(
                user, aliases.find(connector.connectorId(), user.id()).orElse(null));
    }

    /**
     * The stored form of a submitted password, or null when none was submitted.
     *
     * <p>A missing password is a supported state rather than an error, so this returns
     * null instead of refusing: the User exists, cannot authenticate, and can be given
     * a credential later.
     */
    private String hashed(String password) {
        return password == null ? null : passwordEncoder.encode(password);
    }
}
