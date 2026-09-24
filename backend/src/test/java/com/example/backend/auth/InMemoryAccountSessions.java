package com.example.backend.auth;

import com.example.backend.auth.domain.AccountSessions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Session registry for tests, standing in for the Spring Session adapter.
 *
 * <p>Records what was opened and what was revoked so a test can assert that the
 * right account's sessions ended and that nobody else's did — the whole point of
 * the port, and invisible in a mock that only counts calls.
 */
public final class InMemoryAccountSessions implements AccountSessions {

    private final Map<String, List<String>> live = new LinkedHashMap<>();

    private final List<String> revocations = new ArrayList<>();

    /** Record a session this account holds, as a successful login would. */
    public void open(String username, String sessionId) {
        live.computeIfAbsent(username, key -> new ArrayList<>()).add(sessionId);
    }

    /** The sessions this account still holds. */
    public List<String> sessionsOf(String username) {
        return List.copyOf(live.getOrDefault(username, List.of()));
    }

    /** Every username revoked, in order, including ones holding no session. */
    public List<String> revocations() {
        return List.copyOf(revocations);
    }

    @Override
    public int revokeAll(String username) {
        revocations.add(username);
        List<String> ended = live.remove(username);
        return ended == null ? 0 : ended.size();
    }
}
