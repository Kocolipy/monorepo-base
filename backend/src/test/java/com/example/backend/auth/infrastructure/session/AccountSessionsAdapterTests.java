package com.example.backend.auth.infrastructure.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;

/**
 * The adapter over a session repository that indexes by principal, which is the
 * only contract it depends on — the real one is Redis-backed, and none of what
 * this class does is Redis-specific.
 *
 * <p>What is worth pinning here is narrow and easy to get wrong: that it deletes
 * every session it found rather than the first, that it deletes nobody else's,
 * and that an account signed in nowhere is a no-op rather than an error.
 */
class AccountSessionsAdapterTests {

    private final IndexedSessions sessions = new IndexedSessions();

    private final AccountSessionsAdapter adapter = new AccountSessionsAdapter(sessions);

    @Test
    void endsEverySessionTheAccountHolds() {
        sessions.open("bob");
        sessions.open("bob");

        assertThat(adapter.revokeAll("bob")).isEqualTo(2);
        assertThat(sessions.principals()).isEmpty();
    }

    @Test
    void endsNoSessionBelongingToAnotherAccount() {
        String survivor = sessions.open("zoe");
        sessions.open("bob");

        adapter.revokeAll("bob");

        assertThat(sessions.findById(survivor)).isNotNull();
        assertThat(sessions.principals()).containsExactly("zoe");
    }

    @Test
    void reportsNoSessionsForAnAccountSignedInNowhere() {
        sessions.open("zoe");

        assertThat(adapter.revokeAll("bob")).isZero();
        assertThat(sessions.principals()).containsExactly("zoe");
    }

    /** A session store that can be searched by principal, and nothing more. */
    private static final class IndexedSessions
            implements FindByIndexNameSessionRepository<MapSession> {

        private final Map<String, MapSession> stored = new LinkedHashMap<>();

        /** Register a session for this account, as an accepted login would. */
        String open(String username) {
            MapSession session = createSession();
            session.setAttribute(PRINCIPAL_NAME_INDEX_NAME, username);
            save(session);
            return session.getId();
        }

        /** Who is still signed in, so a test can assert on the blast radius. */
        java.util.List<String> principals() {
            return stored.values().stream()
                    .map(session -> (String) session.getAttribute(PRINCIPAL_NAME_INDEX_NAME))
                    .distinct()
                    .toList();
        }

        @Override
        public Map<String, MapSession> findByIndexNameAndIndexValue(
                String indexName, String indexValue) {
            if (!PRINCIPAL_NAME_INDEX_NAME.equals(indexName)) {
                return Map.of();
            }
            return stored.values().stream()
                    .filter(session -> indexValue.equals(
                            session.getAttribute(PRINCIPAL_NAME_INDEX_NAME)))
                    .collect(Collectors.toMap(MapSession::getId, session -> session));
        }

        @Override
        public MapSession createSession() {
            return new MapSession();
        }

        @Override
        public void save(MapSession session) {
            stored.put(session.getId(), session);
        }

        @Override
        public MapSession findById(String id) {
            return stored.get(id);
        }

        @Override
        public void deleteById(String id) {
            stored.remove(id);
        }
    }
}
