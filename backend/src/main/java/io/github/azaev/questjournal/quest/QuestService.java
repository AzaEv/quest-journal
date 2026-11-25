package io.github.azaev.questjournal.quest;

import io.github.azaev.questjournal.common.ApiException;
import io.github.azaev.questjournal.config.AppProperties;
import io.github.azaev.questjournal.quest.QuestDtos.AddLogEntryRequest;
import io.github.azaev.questjournal.quest.QuestDtos.CreateQuestRequest;
import io.github.azaev.questjournal.quest.QuestDtos.LogEntryDto;
import io.github.azaev.questjournal.quest.QuestDtos.QuestDto;
import io.github.azaev.questjournal.quest.QuestDtos.QuestWithLogDto;
import io.github.azaev.questjournal.quest.QuestDtos.SyncResponse;
import io.github.azaev.questjournal.quest.QuestDtos.UpdateQuestRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class QuestService {

    /** Result of an idempotent create: {@code created == false} means the quest already existed. */
    public record CreateResult(QuestWithLogDto quest, boolean created) {
    }

    /** Result of an idempotent log append. */
    public record AppendResult(LogEntryDto entry, boolean created) {
    }

    private final QuestRepository quests;
    private final QuestLogRepository log;
    private final AppProperties props;
    private final Clock clock;

    public QuestService(QuestRepository quests, QuestLogRepository log, AppProperties props, Clock clock) {
        this.quests = quests;
        this.log = log;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<QuestDto> list(UUID ownerId, Collection<QuestStatus> statuses) {
        List<Quest> found = (statuses == null || statuses.isEmpty())
                ? quests.findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(ownerId)
                : quests.findByOwnerIdAndDeletedAtIsNullAndStatusInOrderByUpdatedAtDesc(ownerId, statuses);
        return found.stream().map(QuestDto::from).toList();
    }

    @Transactional(readOnly = true)
    public QuestWithLogDto get(UUID ownerId, UUID questId) {
        return withLog(findOwned(ownerId, questId));
    }

    @Transactional
    public CreateResult create(UUID ownerId, CreateQuestRequest req) {
        UUID id = req.id() != null ? req.id() : UUID.randomUUID();

        Optional<Quest> existing = quests.findById(id);
        if (existing.isPresent()) {
            Quest quest = existing.get();
            if (!quest.getOwnerId().equals(ownerId)) {
                throw ApiException.conflict("Quest id is already in use");
            }
            return new CreateResult(withLog(quest), false); // retry of an already applied request
        }

        Instant now = now();
        QuestType type = req.type() != null ? req.type() : QuestType.SIDE;
        Quest quest = quests.save(new Quest(id, ownerId, req.title().strip(), blankToNull(req.description()),
                type, req.remindAt(), now));
        log.save(QuestLogEntry.created(id, now));
        return new CreateResult(withLog(quest), true);
    }

    @Transactional
    public QuestWithLogDto update(UUID ownerId, UUID questId, UpdateQuestRequest req) {
        Quest quest = findOwned(ownerId, questId);
        if (quest.getVersion() != req.version()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Quest was changed on another device, reload it and try again",
                    Map.of("current", QuestDto.from(quest)));
        }

        Instant now = now();
        QuestStatus oldStatus = quest.getStatus();
        quest.update(req.title().strip(), blankToNull(req.description()), req.type(), req.status(),
                req.remindAt(), now);
        if (oldStatus != req.status()) {
            log.save(QuestLogEntry.statusChange(questId, oldStatus, req.status(), now));
        }
        quests.flush(); // bump the version now so the response carries the new one
        return withLog(quest);
    }

    @Transactional
    public void delete(UUID ownerId, UUID questId) {
        findOwned(ownerId, questId).markDeleted(now());
    }

    @Transactional
    public AppendResult addLogEntry(UUID ownerId, UUID questId, AddLogEntryRequest req) {
        findOwned(ownerId, questId);
        if (req.id() != null) {
            Optional<QuestLogEntry> existing = log.findById(req.id());
            if (existing.isPresent()) {
                if (!existing.get().getQuestId().equals(questId)) {
                    throw ApiException.conflict("Log entry id is already in use");
                }
                return new AppendResult(LogEntryDto.from(existing.get()), false);
            }
        }
        UUID id = req.id() != null ? req.id() : UUID.randomUUID();
        QuestLogEntry entry = log.save(QuestLogEntry.note(id, questId, req.text().strip(), now()));
        return new AppendResult(LogEntryDto.from(entry), true);
    }

    /**
     * Incremental sync for offline-first clients.
     * {@code since == null} returns the full current state; otherwise everything changed after
     * {@code since} minus a small overlap window, so a transaction that committed slightly late is
     * still picked up. Clients de-duplicate by id.
     */
    @Transactional(readOnly = true)
    public SyncResponse sync(UUID ownerId, Instant since) {
        Instant serverTime = now();
        if (since == null) {
            return new SyncResponse(serverTime,
                    quests.findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtAsc(ownerId).stream().map(QuestDto::from).toList(),
                    log.findAllOwned(ownerId).stream().map(LogEntryDto::from).toList());
        }
        Instant from = since.minus(props.sync().overlap());
        return new SyncResponse(serverTime,
                quests.findByOwnerIdAndUpdatedAtGreaterThanEqualOrderByUpdatedAtAsc(ownerId, from).stream()
                        .map(QuestDto::from).toList(),
                log.findOwnedSince(ownerId, from).stream().map(LogEntryDto::from).toList());
    }

    private Quest findOwned(UUID ownerId, UUID questId) {
        return quests.findByIdAndOwnerIdAndDeletedAtIsNull(questId, ownerId)
                .orElseThrow(() -> ApiException.notFound("Quest"));
    }

    private QuestWithLogDto withLog(Quest quest) {
        List<LogEntryDto> entries = log.findByQuestIdOrderByCreatedAtAsc(quest.getId()).stream()
                .map(LogEntryDto::from).toList();
        return new QuestWithLogDto(QuestDto.from(quest), entries);
    }

    private Instant now() {
        // PostgreSQL stores microseconds; truncating keeps Java and DB values identical
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }
}
