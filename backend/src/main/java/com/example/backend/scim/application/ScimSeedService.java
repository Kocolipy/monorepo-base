package com.example.backend.scim.application;

import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.scim.domain.DuplicateUserNameException;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroup;
import com.example.backend.scim.domain.ScimGroupMember;
import com.example.backend.scim.domain.ScimGroupRepository;
import com.example.backend.scim.domain.ScimSeedLock;
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
 * <h2>Idempotent by looking first, under a lock</h2>
 *
 * <p>Every write is preceded by a read that decides whether it is needed, and no write is ever
 * expected to fail. That is deliberate: against Postgres a unique-key violation aborts the whole
 * transaction and marks it rollback-only, so a seed that INSERTed and caught the violation as
 * "it is already there" failed every restart against a seeded database. Catching does not undo
 * the abort.
 *
 * <p>Read-then-write alone would let two instances starting together both read "absent", so the
 * transaction first takes {@link ScimSeedLock}. The second instance waits, then reads what the
 * first committed and writes nothing. The reservations stay UNIQUE in the schema as the backstop:
 * if the lock were ever bypassed, the outcome is a failed startup, never two Admin groups.
 *
 * <h2>The ordinary identity is a first-run convenience</h2>
 *
 * <p>The ordinary configured identity is created only in the run that creates the Bootstrap Admin
 * — that is, on a database seeding has never completed on. Afterwards it is an ordinary,
 * provisionable User: a connector or administrator that deletes or renames it has made a
 * deliberate change, and a restart that brought it back would undo that change. The recovery
 * path is different, which is why it alone is re-checked on every start.
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
    private final ScimSeedLock seedLock;
    private final AuditTrail audit;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public ScimSeedService(
            ScimUserRepository users,
            ScimGroupRepository groups,
            ScimSeedLock seedLock,
            AuditTrail audit,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.users = users;
        this.groups = groups;
        this.seedLock = seedLock;
        this.audit = audit;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Creates whatever of the recovery path is missing, and — on the run that creates the Bootstrap
     * Admin — the ordinary configured identity with it.
     *
     * <p>One transaction, so a deployment never comes up with a Bootstrap Admin that is not in the
     * Admin group: the two halves of the recovery path are useless apart, and a partial seed would
     * be a directory with an administrator that cannot administer.
     *
     * <p>The lock is the transaction's first statement, so every read after it sees what any
     * concurrent seed committed.
     *
     * @param ordinary  the non-administrative identity the deployment is configured with
     * @param recovery  the Bootstrap Admin's configured credentials
     */
    @Transactional
    public void seed(SeededIdentity ordinary, SeededIdentity recovery) {
        seedLock.acquire();
        Instant now = clock.instant();
        Optional<ScimUser> existingAdmin =
                users.findByReservedName(ReservedResourceName.BOOTSTRAP_ADMIN);
        ScimUser bootstrapAdmin;
        if (existingAdmin.isPresent()) {
            bootstrapAdmin = existingAdmin.get();
        } else {
            seedOrdinary(ordinary, now);
            bootstrapAdmin = seedBootstrapAdmin(recovery, now);
        }
        seedAdminGroup(bootstrapAdmin, now);
    }

    /**
     * The ordinary configured identity, unreserved and in no Group — so it has baseline access and
     * nothing else, which is what makes it useful for exercising the non-administrative paths.
     *
     * <p>A live User already holding the userName is left alone, and nothing is audited: the seed
     * event records a resource coming into existence, and on this path none did. Looked up rather
     * than INSERTed and caught; see the class comment for why a caught violation is fatal here.
     */
    private void seedOrdinary(SeededIdentity ordinary, Instant now) {
        if (users.findByNormalizedUserName(NormalizedUserName.of(ordinary.userName())).isPresent()) {
            return;
        }
        users.create(newUser(ordinary, now));
    }

    /**
     * The Bootstrap Admin, reserved as it is created. Called only once {@link #seed} has found no
     * User holding the reservation.
     *
     * <p>The reservation is the question that matters: a User holding the configured userName but
     * carrying no reservation is NOT the Bootstrap Admin, and treating it as one is the silent
     * failure this whole arrangement exists to avoid. So the create is attempted regardless, and a
     * userName collision surfaces as {@link DuplicateUserNameException} — a startup failure,
     * loudly, rather than a deployment that boots with an unprotected recovery identity. This is
     * the one write that is allowed to fail, because failing is its purpose.
     *
     * <p>Seeded with a password change required. Its password comes from deployment configuration
     * — and {@code application.yaml} carries working fallbacks on a public remote — so it is a
     * default credential known outside the User, which must be replaced before it is used for
     * anything else. Login confines the session until it is. The grace-period job exempts the
     * reserved User, so an unchanged recovery credential never deactivates the recovery path.
     */
    private ScimUser seedBootstrapAdmin(SeededIdentity recovery, Instant now) {
        ScimUser seeded = newUser(recovery, now);
        ScimUser flagged = new ScimUser(
                seeded.id(),
                seeded.profile(),
                seeded.login().withPasswordChangeRequired(now),
                seeded.reservedName(),
                seeded.version(),
                seeded.createdAt(),
                seeded.lastModifiedAt());
        ScimUser created = users.createReserved(flagged, ReservedResourceName.BOOTSTRAP_ADMIN);
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
