package io.github.azaev.questjournal.quest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface QuestLogRepository extends JpaRepository<QuestLogEntry, UUID> {

    List<QuestLogEntry> findByQuestIdOrderByCreatedAtAsc(UUID questId);

    @Query("""
            select e from QuestLogEntry e
            where e.createdAt >= :since
              and e.questId in (select q.id from Quest q where q.ownerId = :ownerId and q.deletedAt is null)
            order by e.createdAt asc
            """)
    List<QuestLogEntry> findOwnedSince(@Param("ownerId") UUID ownerId, @Param("since") Instant since);

    @Query("""
            select e from QuestLogEntry e
            where e.questId in (select q.id from Quest q where q.ownerId = :ownerId and q.deletedAt is null)
            order by e.createdAt asc
            """)
    List<QuestLogEntry> findAllOwned(@Param("ownerId") UUID ownerId);
}
