package com.example.backend.scim.application;

import com.example.backend.audit.domain.AuditFilterShape;
import com.example.backend.scim.domain.ScimFilter;
import com.example.backend.scim.domain.ScimFilterPath;

/**
 * A parsed filter as the audit trail records it: its structure, with every value dropped.
 *
 * <p>The translation is total and value-free by construction. It reads only the paths,
 * operators and logical nodes of a {@link ScimFilter}; the literal each comparison carries is
 * never read, so there is no code path by which one could reach the trail. What arrives on the
 * other side is a tree of the audit slice's own enums.
 */
final class ScimAuditFilterShapes {

    private ScimAuditFilterShapes() {
    }

    /** The shape of the filter, or {@code null} when there was none. */
    static AuditFilterShape of(ScimFilter filter) {
        return filter == null ? null : shape(filter, false);
    }

    private static AuditFilterShape shape(ScimFilter filter, boolean inValuePath) {
        return switch (filter) {
            case ScimFilter.And and -> new AuditFilterShape.And(
                    shape(and.left(), inValuePath), shape(and.right(), inValuePath));
            case ScimFilter.Or or -> new AuditFilterShape.Or(
                    shape(or.left(), inValuePath), shape(or.right(), inValuePath));
            case ScimFilter.Not not -> new AuditFilterShape.Not(shape(not.inner(), inValuePath));
            case ScimFilter.Presence presence -> new AuditFilterShape.Presence(
                    attribute(presence.attribute().path()), inValuePath);
            case ScimFilter.Comparison comparison -> new AuditFilterShape.Comparison(
                    attribute(comparison.attribute().path()),
                    AuditFilterShape.Operator.valueOf(comparison.operator().name()),
                    inValuePath);
            case ScimFilter.ValuePath valuePath -> new AuditFilterShape.ValuePath(
                    attribute(valuePath.attribute().path()), shape(valuePath.inner(), true));
        };
    }

    /**
     * The audit vocabulary's name for a path. The two enums share constant names, and
     * {@code ScimAuditFilterShapesTests} maps every filterable path so a constant added to one
     * and not the other fails the build rather than a query.
     */
    static AuditFilterShape.Attribute attribute(ScimFilterPath path) {
        return AuditFilterShape.Attribute.valueOf(path.name());
    }
}
