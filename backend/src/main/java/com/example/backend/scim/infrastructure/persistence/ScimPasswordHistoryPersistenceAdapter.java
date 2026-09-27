package com.example.backend.scim.infrastructure.persistence;

import com.example.backend.scim.domain.PasswordHistoryPolicy;
import com.example.backend.scim.domain.ScimPasswordHistoryRepository;
import com.example.backend.scim.infrastructure.persistence.entity.ScimPasswordHistoryEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Maps the password history port onto {@code scim_user_password_history}. */
@Repository
class ScimPasswordHistoryPersistenceAdapter implements ScimPasswordHistoryRepository {

    private final ScimPasswordHistoryJpaRepository history;

    ScimPasswordHistoryPersistenceAdapter(ScimPasswordHistoryJpaRepository history) {
        this.history = history;
    }

    @Override
    public List<String> findRecentHashes(UUID userId) {
        return history.findAllByUserIdOrderBySetAtDescIdDesc(userId).stream()
                .limit(PasswordHistoryPolicy.RETAINED)
                .map(ScimPasswordHistoryEntity::getPasswordHash)
                .toList();
    }

    /**
     * Appends, then deletes everything past the newest {@link PasswordHistoryPolicy#RETAINED}.
     *
     * <p>Flushed before the trim reads, so the entry just appended is one of the rows ordered —
     * otherwise the trim would count the history as it stood before this change and keep one too
     * many. The caller holds the User's resource lock, so no concurrent change can interleave
     * between the append and the trim.
     */
    @Override
    public void record(UUID userId, String passwordHash, Instant setAt) {
        history.saveAndFlush(
                new ScimPasswordHistoryEntity(UUID.randomUUID(), userId, passwordHash, setAt));
        List<ScimPasswordHistoryEntity> newestFirst =
                history.findAllByUserIdOrderBySetAtDescIdDesc(userId);
        if (newestFirst.size() > PasswordHistoryPolicy.RETAINED) {
            history.deleteAllInBatch(
                    newestFirst.subList(PasswordHistoryPolicy.RETAINED, newestFirst.size()));
        }
    }
}
