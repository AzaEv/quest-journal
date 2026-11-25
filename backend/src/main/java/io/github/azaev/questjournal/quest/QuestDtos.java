package io.github.azaev.questjournal.quest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class QuestDtos {

    private QuestDtos() {
    }

    /**
     * @param id       optional; a client-generated UUID makes the request idempotent (safe to retry after going offline)
     * @param type     defaults to SIDE
     * @param remindAt null = no reminder
     */
    public record CreateQuestRequest(
            UUID id,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 10_000) String description,
            QuestType type,
            Instant remindAt) {
    }

    /**
     * Full replacement of the editable fields. The client sends the {@code version} it last saw;
     * if someone changed the quest in between, the server answers 409 with the current state.
     */
    public record UpdateQuestRequest(
            @NotNull Long version,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 10_000) String description,
            @NotNull QuestType type,
            @NotNull QuestStatus status,
            Instant remindAt) {
    }

    /** @param id optional client-generated UUID for idempotency */
    public record AddLogEntryRequest(UUID id, @NotBlank @Size(max = 10_000) String text) {
    }

    public record QuestDto(UUID id, String title, String description, QuestType type, QuestStatus status,
                           Instant remindAt, long version, Instant createdAt, Instant updatedAt,
                           Instant deletedAt) {
        static QuestDto from(Quest q) {
            return new QuestDto(q.getId(), q.getTitle(), q.getDescription(), q.getType(), q.getStatus(),
                    q.getRemindAt(), q.getVersion(), q.getCreatedAt(), q.getUpdatedAt(), q.getDeletedAt());
        }
    }

    public record LogEntryDto(UUID id, UUID questId, LogEntryKind kind, String text,
                              QuestStatus oldStatus, QuestStatus newStatus, Instant createdAt) {
        static LogEntryDto from(QuestLogEntry e) {
            return new LogEntryDto(e.getId(), e.getQuestId(), e.getKind(), e.getText(),
                    e.getOldStatus(), e.getNewStatus(), e.getCreatedAt());
        }
    }

    public record QuestWithLogDto(QuestDto quest, List<LogEntryDto> log) {
    }

    /**
     * @param serverTime pass it back as {@code since} on the next sync
     * @param quests     changed quests; ones with {@code deletedAt != null} must be removed locally
     * @param logEntries new journal entries; de-duplicate by id
     */
    public record SyncResponse(Instant serverTime, List<QuestDto> quests, List<LogEntryDto> logEntries) {
    }
}
