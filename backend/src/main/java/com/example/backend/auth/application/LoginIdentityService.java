package com.example.backend.auth.application;

import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ReservedResourceName;
import com.example.backend.scim.domain.ScimGroupRepository;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserRepository;
import java.util.UUID;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Reports a SCIM User to Spring Security, with the authority its Group membership confers.
 *
 * <p>This is the login path's whole view of the directory. It replaces the account aggregate's
 * {@code AccountService}, which read a table of its own; there is no second identity to read any
 * more, because a SCIM User carries the profile, the credential and the authentication state
 * together.
 *
 * <p>It lives in this slice rather than in {@code scim} because it serves the password-login
 * surface, not the SCIM protocol: it reaches the directory through the SCIM ports and adds
 * Spring Security's vocabulary on top. The dependency runs one way only — {@code auth} knows
 * about {@code scim}, and nothing in {@code scim} knows this class exists, which is what keeps
 * the two slices free of a cycle.
 *
 * <p>It reads and never writes. Administrative changes are {@link IdentityAdministrationService}'s,
 * so nothing the authentication path depends on is also able to mutate an identity.
 *
 * <h2>Authority is derived, not stored</h2>
 *
 * <p>There is no role column. Every active User gets {@code ROLE_USER} — baseline access is what
 * being an active identity means, and a redundant "Users" Group would be a second place for the
 * same fact — and a direct member of the Admin group additionally gets {@code ROLE_ADMIN}.
 *
 * <p>Derived HERE, which is once per login, and that is the specified behaviour rather than a
 * limitation: a session carries the authorities it was issued with, so adding a User to the
 * Admin group grants administrative access at its next login and never mid-session. Recomputing
 * per request would make an authority change take effect at an unpredictable moment and would
 * put a database read on every authenticated request.
 *
 * <p>Membership is asked as a one-row question rather than by loading the Admin group and
 * scanning it: the latter reads every administrator's membership to answer something about one
 * User.
 */
@Service
public class LoginIdentityService implements UserDetailsService {

    /** Baseline access, which every active User has by being one. */
    private static final String USER_ROLE = "USER";

    /** The authority the Admin group confers, formerly the {@code ADMIN} role column. */
    private static final String ADMIN_ROLE = "ADMIN";

    /**
     * The only authority a User with a pending required password change receives: it may read its
     * own requirement, submit the change and log out, and nothing else. Deliberately not a role and
     * not combined with {@code ROLE_USER} or {@code ROLE_ADMIN} — a flagged Admin holds no
     * administrative authority until the credential is replaced, and the filter chain, which grants
     * every other application endpoint to {@code ROLE_USER} only, refuses it everywhere else.
     *
     * <p>Decided here, at authentication, rather than per request: the flag is not enumerable before
     * login (the User authenticates normally), and a session carries the authority it was issued
     * with, so clearing the flag takes effect at the next login — which the change forces, by
     * revoking every session.
     */
    public static final String PASSWORD_CHANGE_REQUIRED_AUTHORITY = "PASSWORD_CHANGE_REQUIRED";

    /**
     * A fixed passphrase encoded with the same {@link PasswordEncoder} this service is
     * configured with, standing in for a credentialless User's absent hash. Computed once, on
     * first use, from whatever encoder is injected — mirroring how
     * {@code DaoAuthenticationProvider} builds its own dummy hash for an unknown username —
     * rather than a literal encoded string fixed at compile time, which would silently stop
     * matching the encoder's parameters the moment they changed.
     *
     * <p>No password verifies against it — the encoded value matches nothing a caller can submit
     * — so {@code DaoAuthenticationProvider} still runs one real Argon2id comparison, at the
     * same cost as a genuine hash, before refusing. That uniformity, not the string's content,
     * is why one is needed at all: passing {@code null} through to
     * {@code User.withUsername(...).password(...)} would throw before any comparison happened,
     * which is a different and distinguishable failure mode from a wrong password.
     */
    private volatile String noPasswordSetMarker;

    private final ScimUserRepository users;
    private final ScimGroupRepository groups;
    private final PasswordEncoder passwordEncoder;

    public LoginIdentityService(
            ScimUserRepository users,
            ScimGroupRepository groups,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.groups = groups;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Reports the User to Spring Security, including whether it is currently locked and whether
     * an administrator has deactivated it. Carrying both here is what rejects such a User with
     * its correct password: {@code DaoAuthenticationProvider} checks account status before it
     * checks the password, so the credentials are never even compared.
     *
     * <p>A credentialless User — one with no password hash set — reports
     * {@link #noPasswordSetMarker()} rather than {@code null}: the User exists and may be active
     * and unlocked, but nothing submitted can match a hash nobody wrote, so it is refused on the
     * password check like any other wrong password, in the same amount of work.
     *
     * <p>The lookup is by the NORMALIZED userName, which is what uniqueness is decided on, so a
     * correct password is not refused because of how the name was typed. The returned
     * {@code UserDetails} still names the User by its stored {@code userName} — that stays Spring
     * Security's own vocabulary, and the login and {@code /me} responses continue to report it.
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        ScimUser user = users.findByNormalizedUserName(normalized(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        User.UserBuilder builder = User.withUsername(user.profile().userName())
                .password(user.login().hasPassword()
                        ? user.login().passwordHash()
                        : noPasswordSetMarker());
        if (user.login().isPasswordChangeRequired()) {
            builder.authorities(PASSWORD_CHANGE_REQUIRED_AUTHORITY);
        } else {
            builder.roles(rolesOf(user));
        }
        return builder
                .accountLocked(user.login().isLocked())
                // `active` is what the administrative listing reports, so authentication has to
                // honour it: an inactive User that could still log in would make the listing a
                // lie.
                .disabled(!user.profile().active())
                .build();
    }

    /**
     * The stable id behind a userName, for a caller that has just authenticated it and needs to
     * key application-owned state — the session index, the counter feature — by that id rather
     * than by the userName Spring Security itself keeps using.
     */
    public UUID resolveUserId(String username) {
        return users.findByNormalizedUserName(normalized(username))
                .map(ScimUser::id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Baseline access, plus administrative authority when the User is a direct member of the Admin
     * group.
     *
     * <p>{@code ADMIN} is placed FIRST, because a caller reading a single role off the
     * authorities — which the login response does — must see the higher one. That ordering is the
     * kind of coupling worth stating rather than discovering: the alternative is a response that
     * reports an administrator as an ordinary user.
     */
    private String[] rolesOf(ScimUser user) {
        return groups.isMemberOfReservedGroup(user.id(), ReservedResourceName.ADMIN_GROUP)
                ? new String[] {ADMIN_ROLE, USER_ROLE}
                : new String[] {USER_ROLE};
    }

    /**
     * The submitted name in the form uniqueness is decided on, or a refusal.
     *
     * <p>A blank submission cannot normalize, and normalization throws on one. It is turned into
     * the same {@code UsernameNotFoundException} an unknown name produces, because that is what it
     * is: no User holds a blank userName, and a distinguishable failure here would tell a caller
     * that its input was rejected for a different reason than being wrong.
     */
    private static NormalizedUserName normalized(String username) {
        try {
            return NormalizedUserName.of(username);
        } catch (IllegalArgumentException blank) {
            throw new UsernameNotFoundException("User not found", blank);
        }
    }

    /**
     * Lazily computed and cached: encoding is the expensive Argon2id step this marker exists to
     * force on the refusal path, so it must happen once per process, not on every credentialless
     * login attempt.
     */
    private String noPasswordSetMarker() {
        String cached = noPasswordSetMarker;
        if (cached == null) {
            synchronized (this) {
                cached = noPasswordSetMarker;
                if (cached == null) {
                    cached = passwordEncoder.encode("no-password-set");
                    noPasswordSetMarker = cached;
                }
            }
        }
        return cached;
    }
}
