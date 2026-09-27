package com.example.backend.scim.application;

import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.scim.domain.DuplicateUserNameException;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroup;
import com.example.backend.scim.domain.ScimGroupMember;
import com.example.backend.scim.domain.ScimGroupRepository;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserProfile;
import com.example.backend.scim.domain.ScimUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Establishes the deployment's recovery path on a database that does not have one: the Bootstrap
 * Admin, the Admin group, and the Bootstrap Admin's membership of it.
 *
 * <p>This is the only writer of a reservation marker. A reserved resource cannot be created by any
 * provisioning path — the ports expose {@code createReserved} operations that only this class
 * calls, and the marker column is not updatable — so the recovery resources exist because this ran
 * and for no other reason.
 *
 * <h2>Idempotent against the database, not against a read</h2>
 *
 * <p>Each seed looks for its resource by reservation and creates it when absent, and the created
 * resource's reservation is UNIQUE in the schema. So two instances starting at the same moment do
 * not produce two Admin groups: one INSERT succeeds and the other violates the constraint, which
 * is caught and read as "it is already there". A read-then-write check alone would let both pass.
 *
 * <h2>What it does not do</h2>
 *
 * <p>It never overwrites. A Bootstrap Admin that exists keeps its password, its failure run and
 * its lock — whatever the operator and the login path made them — because a restart that silently
 * reset the recovery credential to the configured one would undo a deliberate rotation.
 *
 * <p>It does not reconcile. If the reserved User exists but has been removed from the Admin group,
 * the membership is restored, because that membership is the deployment's authority and the SCIM
 * surface refuses to remove it — so its absence means something outside SCIM removed it, and
 * leaving it absent would leave a recovery identity with no authority to recover with.
 */
@Service
public class ScimSeedService {

    private final ScimUserRepository users;
    private final ScimGroupRepository groups;
    private final AuditTrail audit;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public ScimSeedService(
            ScimUserRepository users,
            ScimGroupRepository groups,
            AuditTrail audit,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.users = users;
        this.groups = groups;
        this.audit = audit;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Creates whatever of the recovery path is missing, and the ordinary configured identity with
     * it.
     *
     * <p>One transaction, so a deployment never comes up with a Bootstrap Admin that is not in the
     * Admin group: the two halves of the recovery path are useless apart, and a partial seed would
     * be a directory with an administrator that cannot administer.
     *
     * @param ordinary  the non-administrative identity the deployment is configured with
     * @param recovery  the Bootstrap Admin's configured credentials
     */
    @Transactional
    public void seed(SeededIdentity ordinary, SeededIdentity recovery) {
        Instant now = clock.instant();
        seedOrdinary(ordinary, now);
        ScimUser bootstrapAdmin = seedBootstrapAdmin(recovery, now);
        seedAdminGroup(bootstrapAdmin, now);
    }

    /**
     * The ordinary configured identity, unreserved and in no Group — so it has baseline access and
     * nothing else, which is what makes it useful for exercising the non-administrative paths.
     *
     * <p>A duplicate userName means it is already there. Nothing is written and nothing is audited:
     * the seed event records a resource coming into existence, and on this path none did.
     */
    private void seedOrdinary(SeededIdentity ordinary, Instant now) {
        try {
            users.create(newUser(ordinary, now));
        } catch (DuplicateUserNameException alreadySeeded) {
            // Already there. Nothing is written and nothing is audited: the seed event records a
            // resource coming into existence, and on this path none did.
        }
    }

    /**
     * The Bootstrap Admin, reserved as it is created.
     *
     * <p>Looked up by reservation first, which is the question that matters: a User holding the
     * configured userName but carrying no reservation is NOT the Bootstrap Admin, and treating it
     * as one is the silent failure this whole arrangement exists to avoid. So when the reservation
     * is absent the create is attempted, and a userName collision surfaces as
     * {@link DuplicateUserNameException} — a startup failure, loudly, rather than a deployment that
     * boots with an unprotected recovery identity.
     */
    private ScimUser seedBootstrapAdmin(SeededIdentity recovery, Instant now) {
        Optional<ScimUser> existing =
                users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN);
        if (existing.isPresent()) {
            return existing.get();
        }
        ScimUser created = users.createReserved(
                newUser(recovery, now), ReservedResourceName.BOOTSTRAP_ADMIN);
        audit.recordReservedResourceSeeded(created.id(), false);
        return created;
    }

    /**
     * The Admin group, reserved as it is created, with the Bootstrap Admin in it.
     *
     * <p>When the Group already exists its ordinary membership is left exactly as provisioning left
     * it — that membership is what external provisioning is for — and only the Bootstrap Admin's
     * own place in it is restored if something outside SCIM removed it.
     */
    private void seedAdminGroup(ScimUser bootstrapAdmin, Instant now) {
        Optional<ScimGroup> existing =
                groups.findByReservedName(ReservedResourceName.ADMIN_GROUP);
        if (existing.isEmpty()) {
            ScimGroup created = groups.createReserved(
                    ScimGroup.created(
                            UUID.randomUUID(),
                            ADMIN_GROUP_DISPLAY_NAME,
                            List.of(ScimGroupMember.reference(bootstrapAdmin.id())),
                            now),
                    ReservedResourceName.ADMIN_GROUP);
            audit.recordReservedResourceSeeded(created.id(), true);
            return;
        }
        ScimGroup adminGroup = existing.get();
        if (adminGroup.hasMember(bootstrapAdmin.id())) {
            return;
        }
        List<ScimGroupMember> restored = new ArrayList<>(adminGroup.members());
        restored.add(ScimGroupMember.reference(bootstrapAdmin.id()));
        groups.replace(adminGroup.replacedWith(adminGroup.displayName(), restored, now));
        // Audited because this is a WRITE that changes who holds Admin authority. It had been
        // silent, which meant a deployment whose administrative membership had been removed
        // outside SCIM restored it at startup leaving nothing in the trail to say either the
        // removal or the restore had happened.
        audit.recordReservedMembershipRestored(adminGroup.id(), bootstrapAdmin.id());
    }

    private ScimUser newUser(SeededIdentity identity, Instant now) {
        return ScimUser.created(
                UUID.randomUUID(),
                new ScimUserProfile(
                        identity.userName(),
                        null,
                        identity.userName(),
                        null,
                        null,
                        null,
                        true,
                        List.of()),
                passwordEncoder.encode(identity.password()),
                now);
    }

    /**
     * The Admin group's label.
     *
     * <p>Cosmetic, and deliberately so: the glossary is explicit that the Admin group's STABLE
     * RESOURCE ID carries the authorization meaning, which is why authority derivation resolves the
     * Group by its reservation and never by this string. It is also why the Group cannot be
     * renamed — not because the name matters, but because a renameable recovery resource is one a
     * connector can make unrecognisable to an administrator reading the directory.
     */
    static final String ADMIN_GROUP_DISPLAY_NAME = "Admins";

    /**
     * One configured startup identity. Whether it is the recovery one is decided by which argument
     * of {@link #seed} it is passed as, not by anything on this record: a seed that carried its own
     * privilege would be a value a future caller could construct with the wrong one.
     */
    public record SeededIdentity(String userName, String password) {
    }
}
