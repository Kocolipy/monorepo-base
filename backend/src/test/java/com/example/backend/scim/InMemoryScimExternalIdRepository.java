package com.example.backend.scim;

import com.example.backend.scim.domain.ScimExternalIdRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A {@link ScimExternalIdRepository} in memory, holding aliases as connector/resource pairs. */
public final class InMemoryScimExternalIdRepository implements ScimExternalIdRepository {

    /** One alias: whose namespace it is in, and which resource it names. */
    public record Alias(UUID connectorId, UUID resourceId, String externalId) {
    }

    private final List<Alias> aliases = new ArrayList<>();

    @Override
    public int deleteAllForConnector(UUID connectorId) {
        List<Alias> doomed = aliases.stream()
                .filter(alias -> alias.connectorId().equals(connectorId))
                .toList();
        aliases.removeAll(doomed);
        return doomed.size();
    }

    public void seed(UUID connectorId, String externalId) {
        aliases.add(new Alias(connectorId, UUID.randomUUID(), externalId));
    }

    public List<Alias> all() {
        return List.copyOf(aliases);
    }
}
