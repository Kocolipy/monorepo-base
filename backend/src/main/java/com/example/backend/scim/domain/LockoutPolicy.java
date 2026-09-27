package com.example.backend.scim.domain;

/**
 * How many consecutive failed logins a User tolerates before it locks. Held as a
 * value rather than read from configuration where it is applied, so the lockout rule
 * can be exercised without a running application.
 *
 * <p>There is deliberately no second field. A lockout has no duration: it is lifted
 * by an administrator's Unlock and by nothing else, so a policy that carried a
 * window would be describing a lift no code performs. The one constraint on the
 * configured number is that at least one attempt has to be possible — nothing here
 * imposes a minimum threshold beyond that, because a deployment that wants to lock
 * on the first failure is making a policy choice rather than a mistake.
 *
 * <p>Lives in this slice rather than beside the login surface because the state it
 * governs does: a User's failure run is part of the authentication state the SCIM
 * User owns, and a rule whose subject is here while the rule itself was elsewhere is
 * a rule two slices have to agree about.
 */
public record LockoutPolicy(int maxAttempts) {

    public LockoutPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("A lockout policy needs at least one attempt");
        }
    }
}
