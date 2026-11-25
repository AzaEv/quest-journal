package io.github.azaev.questjournal.quest;

import io.github.azaev.questjournal.common.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quests")
public class Quest extends AssignedIdEntity {

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuestStatus status;

    /** null means "no reminder". */
    @Column(name = "remind_at")
    private Instant remindAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Soft delete: other devices must learn about deletions through sync. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Quest() {
    }

    public Quest(UUID id, UUID ownerId, String title, String description, QuestType type,
                 Instant remindAt, Instant now) {
        super(id);
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.status = QuestStatus.ACTIVE;
        this.remindAt = remindAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String description, QuestType type, QuestStatus status,
                       Instant remindAt, Instant now) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.status = status;
        this.remindAt = remindAt;
        this.updatedAt = now;
    }

    public void markDeleted(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public QuestType getType() {
        return type;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public Instant getRemindAt() {
        return remindAt;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
