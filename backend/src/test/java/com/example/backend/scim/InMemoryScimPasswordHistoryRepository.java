package com.example.backend.scim;

import com.example.backend.scim.domain.PasswordHistoryPolicy;
import com.example.backend.scim.domain.ScimPasswordHistoryRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A {@link ScimPasswordHistoryRepository} in memory, trimming as the adapter does. */
public final class InMemoryScimPasswordHistoryRepository implements ScimPasswordHistoryRepository {

    private final Map<UUID, List<String>> newestFirst = new LinkedHashMap<>();

    @Override
    public List<String> findRecentHashes(UUID userId) {
        return List.copyOf(newestFirst.getOrDefault(userId, List.of()));
    }

    @Override
    public void record(UUID userId, String passwordHash, Instant setAt) {
        List<String> history = newestFirst.computeIfAbsent(userId, key -> new ArrayList<>());
        history.add(0, passwordHash);
        while (history.size() > PasswordHistoryPolicy.RETAINED) {
            history.remove(history.size() - 1);
        }
    }
}
