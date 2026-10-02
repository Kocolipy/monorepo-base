package com.example.backend.auth.infrastructure.session;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.example.backend.audit.CapturedLog;
import com.example.backend.observability.LogEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 *
 * <p>The fake indexes sessions by the same value {@link AccountSessionsAdapter}
 * writes and searches: the account's stable id, stringified. A session
 * belonging to one account is opened under that account's id rather than under
 * a username, since the adapter never sees or compares usernames.
 */
class AccountSessionsAdapterTests {

    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b0");
    private static final UUID ZOE = UUID.fromString("00000000-0000-0000-0000-0000000000e0");

    private final IndexedSessions sessions = new IndexedSessions();

    private final AccountSessionsAdapter adapter = new AccountSessionsAdapter(sessions);

    @Test
    void endsEverySessionTheAccountHolds() {
        sessions.open(BOB);
        sessions.open(BOB);

        assertThat(adapter.revokeAll(BOB)).isEqualTo(2);
        assertThat(sessions.principals()).isEmpty();
    }

    @Test
    void endsNoSessionBelongingToAnotherAccount() {
        String survivor = sessions.open(ZOE);
        sessions.open(BOB);

        adapter.revokeAll(BOB);

        assertThat(sessions.findById(survivor)).isNotNull();
        assertThat(sessions.principals()).containsExactly(ZOE.toString());
    }

    @Test
    void reportsNoSessionsForAnAccountSignedInNowhere() {
        sessions.open(ZOE);

        assertThat(adapter.revokeAll(BOB)).isZero();
        assertThat(sessions.principals()).containsExactly(ZOE.toString());
    }

    /** A login keeps the session it is completed in and ends every other one of the account's. */
    @Test
    void endsEveryOtherSessionTheAccountHoldsButKeepsTheRetainedOne() {
        String earlier = sessions.open(BOB);
        String another = sessions.open(BOB);
        String retained = sessions.open(BOB);
        String zoes = sessions.open(ZOE);

        assertThat(adapter.revokeAllExcept(BOB, retained)).isEqualTo(2);

        assertThat(sessions.findById(retained)).isNotNull();
        assertThat(sessions.findById(earlier)).isNull();
        assertThat(sessions.findById(another)).isNull();
        assertThat(sessions.findById(zoes)).as("nobody else's session ends").isNotNull();
    }

    /** A caller that held no session yet retains nothing, so every session of the account ends. */
    @Test
    void endsEverySessionWhenNoneIsRetained() {
        sessions.open(BOB);
        sessions.open(BOB);
        String zoes = sessions.open(ZOE);

        assertThat(adapter.revokeAllExcept(BOB, null)).isEqualTo(2);

        assertThat(sessions.principals()).containsExactly(ZOE.toString());
        assertThat(sessions.findById(zoes)).isNotNull();
    }

    /** Retaining a session the account does not hold neither fails nor spares one it does. */
    @Test
    void retainingASessionOfAnotherAccountSparesNoneOfThisOnes() {
        sessions.open(BOB);
        String zoes = sessions.open(ZOE);

        assertThat(adapter.revokeAllExcept(BOB, zoes)).isEqualTo(1);

        assertThat(sessions.principals()).containsExactly(ZOE.toString());
    }

    // ---- session-end records (#69) -----------------------------------------------------------

    /**
     * A revocation that ended sessions is one INFO {@code session-end} record: the cause, the
     * account by stable id as the target, and how many ended — never a session id, which is
     * the session's bearer credential.
     */
    @Test
    void aRevocationIsOneSessionEndRecordNamingTheCauseAndTheAccount() {
        String first = sessions.open(BOB);
        String second = sessions.open(BOB);

        ILoggingEvent record = onlyRecord(() -> adapter.revokeAll(BOB));

        assertThat(record.getLevel()).isEqualTo(Level.INFO);
        assertThat(record.getFormattedMessage()).isEqualTo("Sessions ended");
        assertThat(CapturedLog.fields(record))
                .containsEntry(LogEvent.ACTION, "session-end")
                .containsEntry(LogEvent.TYPE, List.of("end"))
                .containsEntry(LogEvent.OUTCOME, "success")
                .containsEntry(LogEvent.REASON, "revoked")
                .containsEntry(LogEvent.USER_TARGET_ID, BOB.toString())
                .containsEntry(LogEvent.SESSIONS_ENDED, 2);
        assertThat(CapturedLog.fields(record).toString() + record.getMDCPropertyMap())
                .doesNotContain(first).doesNotContain(second);
    }

    @Test
    void aLoginEndingTheOtherSessionsSaysSo() {
        sessions.open(BOB);
        String retained = sessions.open(BOB);

        ILoggingEvent record = onlyRecord(() -> adapter.revokeAllExcept(BOB, retained));

        assertThat(CapturedLog.fields(record))
                .containsEntry(LogEvent.REASON, "replaced-by-login")
                .containsEntry(LogEvent.SESSIONS_ENDED, 1);
    }

    /** Nothing ended, nothing to record — for either operation. */
    @Test
    void aRevocationThatEndedNothingWritesNoRecord() {
        String retained = sessions.open(BOB);

        try (CapturedLog captured = CapturedLog.attach()) {
            adapter.revokeAll(ZOE);
            adapter.revokeAllExcept(BOB, retained);

            assertThat(captured.withAction(Level.TRACE, LogEvent.KIND, "event")).isEmpty();
        }
    }

    private static ILoggingEvent onlyRecord(Runnable action) {
        try (CapturedLog captured = CapturedLog.attach()) {
            action.run();
            List<ILoggingEvent> records = captured.withAction(Level.TRACE, LogEvent.KIND, "event");
            assertThat(records).hasSize(1);
            return records.getFirst();
        }
    }

    /** A session store that can be searched by principal, and nothing more. */
    private static final class IndexedSessions
            implements FindByIndexNameSessionRepository<MapSession> {

        private final Map<String, MapSession> stored = new LinkedHashMap<>();

        /** Register a session for this account, as an accepted login would. */
        String open(UUID accountId) {
            MapSession session = createSession();
            session.setAttribute(PRINCIPAL_NAME_INDEX_NAME, accountId.toString());
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
