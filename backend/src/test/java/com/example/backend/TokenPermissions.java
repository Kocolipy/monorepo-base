package com.example.backend;

import com.example.backend.authorization.domain.Permission;
import com.example.backend.scim.domain.ConnectorTokenPermissions;
import java.util.Set;

/**
 * Connector token Permission sets the tests mint with.
 *
 * <p>{@link #ALL} is every Permission a token can carry, and is also what the tests' issuing
 * administrator is taken to hold, so a test that is not about the no-escalation rule is never
 * refused by it. {@link #READ} is the old "read only" token: both read Permissions.
 */
public final class TokenPermissions {

    /** Every directory Permission: what the old {@code READ_WRITE} scope allowed. */
    public static final Set<Permission> ALL = ConnectorTokenPermissions.DIRECTORY;

    /** Both read Permissions: what the old {@code READ_ONLY} scope allowed. */
    public static final Set<Permission> READ = Set.of(Permission.USER_READ, Permission.GROUP_READ);

    /** {@link #ALL} as a JSON array body fragment. */
    public static final String ALL_JSON =
            "[\"group:read\",\"group:write\",\"user:read\",\"user:write\"]";

    /** {@link #READ} as a JSON array body fragment. */
    public static final String READ_JSON = "[\"group:read\",\"user:read\"]";

    private TokenPermissions() {
    }

    /** {@code permissions} as a token's own value type. */
    public static ConnectorTokenPermissions of(Set<Permission> permissions) {
        return new ConnectorTokenPermissions(permissions);
    }
}
