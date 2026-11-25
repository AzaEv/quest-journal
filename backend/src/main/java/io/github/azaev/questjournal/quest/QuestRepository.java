package io.github.azaev.questjournal.quest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestRepository extends JpaRepository<Quest, UUID> {

    Optional<Quest> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);

    List<Quest> findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID ownerId);

    List<Quest> findByOwnerIdAndDeletedAtIsNullAndStatusInOrderByUpdatedAtDesc(UUID ownerId,
                                                                             Collection<QuestStatus> statuses);

    /** Sync: everything changed since the given moment, deleted quests included (tombstones). */
    List<Quest> findByOwnerIdAndUpdatedAtGreaterThanEqualOrderByUpdatedAtAsc(UUID ownerId, Instant since);

    /** Sync without a cursor: the full state, deleted quests excluded. */
    List<Quest> findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtAsc(UUID ownerId);
}
