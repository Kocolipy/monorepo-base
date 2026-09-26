package com.example.backend.auth.domain;

/**
 * Which account is the deployment's local recovery identity — the one principal
 * exempt from lockout.
 *
 * <p>The exemption exists because a lockout no longer lifts on its own. A locked
 * recovery identity would therefore be a deployment nobody can get into, which is
 * a worse outcome than the cost this accepts: unbounded online password guessing
 * against this one account. What answers that guessing is the Argon2id
 * verification cost every attempt pays, the refusal being indistinguishable from
 * every other refusal, and each failure being counted and audited — not a lock.
 *
 * <p>Held as a value rather than read from configuration where it is applied, so
 * "is this the account that must never lock" can be exercised without a running
 * application. It is the recovery account's {@code username} because that is what
 * deployment configuration names it by and what startup seeding creates it under;
 * once a SCIM User carries a Bootstrap marker of its own, this is the one place
 * that has to change.
 */
public record BootstrapAdmin(String username) {

    public BootstrapAdmin {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("The bootstrap admin needs a username");
        }
    }

    /**
     * Whether this account is that recovery identity, and so must never be locked
     * however long its failure run grows.
     */
    public boolean identifies(Account account) {
        return account != null && username.equals(account.username());
    }
}
