package com.example.backend.authorization.config;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The {@code app.authorization} block, as bound: Role definitions and the Group-to-Role mapping.
 *
 * <p>Lists rather than maps on purpose. Spring merges a map property across configuration sources
 * key by key, so a deployment replacing the shipped development mapping would silently keep every
 * development entry it did not mention. A list from a higher-precedence source REPLACES the lower
 * one whole, which is what "replace the mapping" has to mean.
 *
 * <p>Permission names stay strings here, so an unknown one reaches {@code RoleMapping}'s validation
 * and is reported by name rather than failing as an anonymous enum conversion.
 *
 * @param roles  each Role's name and Permission names
 * @param groups each mapped Group's stable id, the Role it confers, and the Superuser marker
 */
@ConfigurationProperties("app.authorization")
public record RoleMappingProperties(List<RoleProperties> roles, List<GroupProperties> groups) {

    /** One Role definition. */
    public record RoleProperties(String name, List<String> permissions) {
    }

    /** One mapping entry; {@code superuser} defaults to false. */
    public record GroupProperties(UUID id, String role, boolean superuser) {
    }
}
