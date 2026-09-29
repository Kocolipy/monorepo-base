package com.example.backend.scim;

import com.example.backend.scim.domain.ScimResourceType;
import com.example.backend.scim.domain.ScimTombstoneRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A {@link ScimTombstoneRepository} that keeps what it was told, for use-case unit tests. */
public final class InMemoryScimTombstoneRepository implements ScimTombstoneRepository {

    /** One recorded tombstone. */
    public record Tombstone(ScimResourceType type, UUID id, Instant deletedAt) {
    }

    private final List<Tombstone> recorded = new ArrayList<>();

    @Override
    public void record(ScimResourceType type, UUID id, Instant deletedAt) {
        recorded.add(new Tombstone(type, id, deletedAt));
    }

    public List<Tombstone> recorded() {
        return List.copyOf(recorded);
    }
}
