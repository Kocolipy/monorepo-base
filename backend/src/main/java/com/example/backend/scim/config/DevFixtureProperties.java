package com.example.backend.scim.config;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The {@code app.dev-fixtures} block: the Groups and Users seeded so the development role mapping
 * has a User per Role. Off unless {@code enabled}; see {@code authorization.yaml}.
 *
 * @param enabled  whether to seed the fixtures at all
 * @param password every fixture User's password; required when enabled, with no fallback
 * @param groups   each fixture Group's stable id, label and single member's userName
 */
@ConfigurationProperties("app.dev-fixtures")
public record DevFixtureProperties(boolean enabled, String password, List<GroupFixture> groups) {

    /** One fixture Group. */
    public record GroupFixture(UUID id, String displayName, String member) {
    }
}
