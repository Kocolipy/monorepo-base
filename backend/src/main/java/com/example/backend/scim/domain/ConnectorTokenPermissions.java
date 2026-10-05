package com.example.backend.scim.domain;

import com.example.backend.authorization.domain.Permission;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * What a connector token may do: a set of Permissions from the shared vocabulary (ADR 0010).
 *
 * <p>The same names a User's Roles grant, so "this connector manages Groups but not Users" is
 * {@code group:read} and {@code group:write} and nothing else, and the no-escalation rule — a
 * token can carry only Permissions its creator holds — compares like with like. A token can carry
 * only the four {@link #DIRECTORY} Permissions: everything else in the vocabulary guards the
 * application chain, which no bearer token reaches, so granting it would mean nothing today and a
 * back door the day a route started honouring it.
 *
 * <p>A token being issued goes through {@link #requested}, which refuses the empty set. The
 * constructor still admits one, so that loading a stored token with no Permissions never fails:
 * such a token authenticates and may do nothing but read discovery.
 *
 * @param values the Permissions, every one of them a {@link #DIRECTORY} Permission
 */
public record ConnectorTokenPermissions(Set<Permission> values) {

    /** The only Permissions a token can carry: the SCIM chain's. */
    public static final Set<Permission> DIRECTORY = Set.copyOf(EnumSet.of(
            Permission.USER_READ, Permission.USER_WRITE,
            Permission.GROUP_READ, Permission.GROUP_WRITE));

    public ConnectorTokenPermissions {
        values = values == null ? Set.of() : Set.copyOf(values);
        if (!DIRECTORY.containsAll(values)) {
            throw new InvalidConnectorTokenPermissionsException(
                    "A token can carry only user:read, user:write, group:read and group:write");
        }
    }

    /**
     * The Permissions a token is being issued with, refused when there are none or when any of
     * them is not a {@link #DIRECTORY} Permission.
     */
    public static ConnectorTokenPermissions requested(Collection<Permission> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new InvalidConnectorTokenPermissionsException(
                    "A token must carry at least one Permission");
        }
        return new ConnectorTokenPermissions(Set.copyOf(requested));
    }

    /** Whether this token holds {@code permission}. */
    public boolean permits(Permission permission) {
        return values.contains(permission);
    }

    /** Whether this token may read at least one resource type. */
    public boolean permitsAnyRead() {
        return values.contains(Permission.USER_READ) || values.contains(Permission.GROUP_READ);
    }

    /** The resource types this token may read, which is what a base {@code /.search} spans. */
    public Set<ScimResourceType> readableTypes() {
        Set<ScimResourceType> readable = EnumSet.noneOf(ScimResourceType.class);
        for (ScimResourceType type : ScimResourceType.values()) {
            if (permits(type.readPermission())) {
                readable.add(type);
            }
        }
        return Set.copyOf(readable);
    }

    /** Whether every Permission here is one {@code held} also contains. */
    public boolean heldWithin(Set<Permission> held) {
        return held.containsAll(values);
    }

    /** The Permissions spelled as the wire spells them, sorted by that spelling. */
    public List<String> sortedValues() {
        return values.stream().sorted(Permission.BY_VALUE).map(Permission::value).toList();
    }
}
