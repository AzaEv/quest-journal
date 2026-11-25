package io.github.azaev.questjournal.quest;

import io.github.azaev.questjournal.common.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/** One line of the quest journal. Append-only: never updated after insert. */
@Entity
@Immutable
@Table(name = "quest_log")
public class QuestLogEntry extends AssignedIdEntity {

    @Column(name = "quest_id", nullable = false)
    private UUID questId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LogEntryKind kind;

    @Column(columnDefinition = "text")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 16)
    private QuestStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 16)
    private QuestStatus newStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected QuestLogEntry() {
    }

    private QuestLogEntry(UUID id, UUID questId, LogEntryKind kind, String text,
                          QuestStatus oldStatus, QuestStatus newStatus, Instant createdAt) {
        super(id);
        this.questId = questId;
        this.kind = kind;
        this.text = text;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.createdAt = createdAt;
    }

    public static QuestLogEntry created(UUID questId, Instant now) {
        return new QuestLogEntry(UUID.randomUUID(), questId, LogEntryKind.CREATED, null,
                null, QuestStatus.ACTIVE, now);
    }

    public static QuestLogEntry note(UUID id, UUID questId, String text, Instant now) {
        return new QuestLogEntry(id, questId, LogEntryKind.NOTE, text, null, null, now);
    }

    public static QuestLogEntry statusChange(UUID questId, QuestStatus from, QuestStatus to, Instant now) {
        return new QuestLogEntry(UUID.randomUUID(), questId, LogEntryKind.STATUS_CHANGE, null, from, to, now);
    }

    public UUID getQuestId() {
        return questId;
    }

    public LogEntryKind getKind() {
        return kind;
    }

    public String getText() {
        return text;
    }

    public QuestStatus getOldStatus() {
        return oldStatus;
    }

    public QuestStatus getNewStatus() {
        return newStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
