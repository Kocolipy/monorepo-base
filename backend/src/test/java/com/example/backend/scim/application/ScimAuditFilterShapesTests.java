package com.example.backend.scim.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.audit.domain.AuditFilterShape;
import com.example.backend.scim.domain.ScimFilterParser;
import com.example.backend.scim.domain.ScimFilterPath;
import com.example.backend.scim.domain.ScimResourceType;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** A filter's shape, as the audit trail records it: structure kept, every value dropped. */
class ScimAuditFilterShapesTests {

    private static final Set<ScimResourceType> BOTH =
            Set.of(ScimResourceType.USER, ScimResourceType.GROUP);

    private static String shape(String filter) {
        return ScimAuditFilterShapes.of(ScimFilterParser.parse(filter, BOTH)).render();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "userName eq \"alice@example.com\"                     | userName eq ?",
            "userName NE \"a\"                                     | userName ne ?",
            "name.familyName co \"x\" or locale sw \"en\"          | (name.familyName co ? or locale sw ?)",
            "active eq true and displayName pr                    | (active eq ? and displayName pr)",
            "not (meta.created gt \"2020-01-01T00:00:00Z\")       | not (meta.created gt ?)",
            "emails[type eq \"work\" and value ew \"@x.example\"] | emails[(type eq ? and value ew ?)]",
            "members[value pr] or groups.$ref le \"z\"            | (members[value pr] or groups.$ref le ?)",
            "urn:ietf:params:scim:schemas:core:2.0:User:userName ge \"a\" | userName ge ?",
            "emails lt \"a\"                                      | emails.value lt ?",
            "displayName eq null                                   | displayName eq ?"})
    void the_shape_is_canonical_paths_and_operators_with_every_value_a_placeholder(
            String filter, String expected) {
        assertThat(shape(filter)).isEqualTo(expected);
    }

    @Test
    void no_filter_has_no_shape() {
        assertThat(ScimAuditFilterShapes.of(null)).isNull();
    }

    /** Every filterable path has an audit name: a path added to one vocabulary alone fails here. */
    @ParameterizedTest
    @EnumSource(value = ScimFilterPath.class, names = "PASSWORD", mode = EnumSource.Mode.EXCLUDE)
    void every_filterable_path_maps_to_the_audit_attribute_of_the_same_path(ScimFilterPath path) {
        assertThat(ScimAuditFilterShapes.attribute(path).path()).isEqualTo(path.canonical());
    }

    /** ...and the audit vocabulary has nothing the filter vocabulary does not, password least of all. */
    @Test
    void the_audit_vocabulary_is_exactly_the_filterable_paths() {
        assertThat(AuditFilterShape.Attribute.values()).hasSize(ScimFilterPath.values().length - 1);
        assertThat(AuditFilterShape.Attribute.values())
                .extracting(AuditFilterShape.Attribute::path)
                .doesNotContain("password");
    }
}
