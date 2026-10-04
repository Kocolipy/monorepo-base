package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.authorization.domain.Permission;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** One collection query: what it holds, what it refuses, and how it is narrowed. */
class ScimQueryTests {

    private static final Set<ScimResourceType> USERS = Set.of(ScimResourceType.USER);

    private static final Set<ScimResourceType> BOTH =
            Set.of(ScimResourceType.USER, ScimResourceType.GROUP);

    @Test
    void a_query_names_at_least_one_resource_type() {
        assertThatThrownBy(() -> new ScimQuery(null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScimQuery(Set.of(), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void a_query_keeps_what_it_was_given_and_defaults_only_the_page() {
        ScimSort sort = ScimSort.of("userName", "descending", USERS);
        ScimFilter filter = ScimFilterParser.parse("userName eq \"ada\"", USERS);
        ScimPageRequest page = new ScimPageRequest(3, 7);

        ScimQuery given = new ScimQuery(USERS, filter, sort, page);
        assertThat(given.types()).containsExactly(ScimResourceType.USER);
        assertThat(given.filter()).isEqualTo(filter);
        assertThat(given.sort()).isEqualTo(sort);
        assertThat(given.page()).isEqualTo(page);

        assertThat(new ScimQuery(USERS, null, null, null).page())
                .isEqualTo(ScimPageRequest.FIRST_PAGE);
    }

    /** The types are copied: a caller's set changing afterwards changes nothing. */
    @Test
    void a_query_copies_its_types() {
        Set<ScimResourceType> mutable = new HashSet<>(USERS);
        ScimQuery query = new ScimQuery(mutable, null, null, null);
        mutable.add(ScimResourceType.GROUP);

        assertThat(query.types()).containsExactly(ScimResourceType.USER);
    }

    @Test
    void the_textual_query_parses_its_filter_and_sort_against_its_types() {
        ScimPageRequest page = new ScimPageRequest(2, 5);

        ScimQuery query = ScimQuery.of(USERS, "userName eq \"ada\"", "userName", "descending", page);

        assertThat(query.types()).containsExactly(ScimResourceType.USER);
        assertThat(query.filter()).isEqualTo(ScimFilterParser.parse("userName eq \"ada\"", USERS));
        assertThat(query.sort()).isEqualTo(ScimSort.of("userName", "descending", USERS));
        assertThat(query.page()).isEqualTo(page);
    }

    @Test
    void the_textual_query_without_a_filter_has_none() {
        ScimQuery query = ScimQuery.of(USERS, null, null, null, null);

        assertThat(query.filter()).isNull();
        assertThat(query.sort()).isEqualTo(ScimSort.of(null, null, USERS));
        assertThat(query.page()).isEqualTo(ScimPageRequest.FIRST_PAGE);
    }

    /** Narrowing keeps the filter, sort and page as parsed and drops only the types. */
    @Test
    void a_query_narrowed_to_readable_types_keeps_everything_else() {
        ScimFilter filter = ScimFilterParser.parse("displayName pr", BOTH);
        ScimSort sort = ScimSort.of("displayName", null, BOTH);
        ScimPageRequest page = new ScimPageRequest(1, 9);
        ScimQuery both = new ScimQuery(BOTH, filter, sort, page);

        ScimQuery narrowed = both.restrictedTo(
                new ConnectorTokenPermissions(Set.of(Permission.GROUP_READ)).readableTypes());

        assertThat(narrowed.types()).containsExactly(ScimResourceType.GROUP);
        assertThat(narrowed.filter()).isEqualTo(filter);
        assertThat(narrowed.sort()).isEqualTo(sort);
        assertThat(narrowed.page()).isEqualTo(page);
        assertThat(both.types()).as("the original is untouched").isEqualTo(BOTH);
        assertThatThrownBy(() -> both.restrictedTo(Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Each resource type's wire name, endpoint, schema and Permissions. */
    @Test
    void each_resource_type_names_its_wire_spelling_and_permissions() {
        assertThat(ScimResourceType.USER.resourceTypeName()).isEqualTo("User");
        assertThat(ScimResourceType.USER.endpointName()).isEqualTo("Users");
        assertThat(ScimResourceType.USER.schemaUri())
                .isEqualTo("urn:ietf:params:scim:schemas:core:2.0:User");
        assertThat(ScimResourceType.USER.readPermission()).isEqualTo(Permission.USER_READ);
        assertThat(ScimResourceType.USER.writePermission()).isEqualTo(Permission.USER_WRITE);
        assertThat(ScimResourceType.GROUP.resourceTypeName()).isEqualTo("Group");
        assertThat(ScimResourceType.GROUP.endpointName()).isEqualTo("Groups");
        assertThat(ScimResourceType.GROUP.schemaUri())
                .isEqualTo("urn:ietf:params:scim:schemas:core:2.0:Group");
        assertThat(ScimResourceType.GROUP.readPermission()).isEqualTo(Permission.GROUP_READ);
        assertThat(ScimResourceType.GROUP.writePermission()).isEqualTo(Permission.GROUP_WRITE);
    }
}
