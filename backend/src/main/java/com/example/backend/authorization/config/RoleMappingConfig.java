package com.example.backend.authorization.config;

import com.example.backend.authorization.domain.RoleMapping;
import com.example.backend.authorization.domain.RoleMapping.GroupAssignment;
import com.example.backend.authorization.domain.RoleMapping.RoleDefinition;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Where the role mapping enters the application: bound from {@code app.authorization}, validated,
 * and published as the one {@link RoleMapping} bean.
 *
 * <p>Validation is the bean's construction, so an invalid mapping fails startup before anything
 * that resolves Permissions exists. Whether each mapped Group exists is checked later in startup,
 * once the directory has been seeded; see {@code ScimSeedConfig}.
 */
@Configuration
@EnableConfigurationProperties(RoleMappingProperties.class)
public class RoleMappingConfig {

    @Bean
    public RoleMapping roleMapping(RoleMappingProperties properties) {
        List<RoleDefinition> roles = properties.roles() == null ? List.of()
                : properties.roles().stream()
                        .map(role -> new RoleDefinition(role.name(), role.permissions()))
                        .toList();
        List<GroupAssignment> groups = properties.groups() == null ? List.of()
                : properties.groups().stream()
                        .map(group -> new GroupAssignment(
                                group.id(), group.role(), group.superuser()))
                        .toList();
        return RoleMapping.of(roles, groups);
    }
}
