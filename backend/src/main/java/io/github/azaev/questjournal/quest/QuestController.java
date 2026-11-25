package io.github.azaev.questjournal.quest;

import io.github.azaev.questjournal.auth.CurrentUser;
import io.github.azaev.questjournal.quest.QuestDtos.AddLogEntryRequest;
import io.github.azaev.questjournal.quest.QuestDtos.CreateQuestRequest;
import io.github.azaev.questjournal.quest.QuestDtos.LogEntryDto;
import io.github.azaev.questjournal.quest.QuestDtos.QuestDto;
import io.github.azaev.questjournal.quest.QuestDtos.QuestWithLogDto;
import io.github.azaev.questjournal.quest.QuestDtos.SyncResponse;
import io.github.azaev.questjournal.quest.QuestDtos.UpdateQuestRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class QuestController {

    private final QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }

    /** Quest list, optionally filtered: {@code ?status=ACTIVE&status=IN_PROGRESS}. */
    @GetMapping("/quests")
    public List<QuestDto> list(@AuthenticationPrincipal Jwt jwt,
                               @RequestParam(name = "status", required = false) Set<QuestStatus> statuses) {
        return questService.list(CurrentUser.id(jwt), statuses);
    }

    @GetMapping("/quests/{id}")
    public QuestWithLogDto get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return questService.get(CurrentUser.id(jwt), id);
    }

    /** 201 for a new quest, 200 when the same client id was already created (safe retry). */
    @PostMapping("/quests")
    public ResponseEntity<QuestWithLogDto> create(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody CreateQuestRequest request) {
        QuestService.CreateResult result = questService.create(CurrentUser.id(jwt), request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.quest());
    }

    @PutMapping("/quests/{id}")
    public QuestWithLogDto update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                  @Valid @RequestBody UpdateQuestRequest request) {
        return questService.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/quests/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        questService.delete(CurrentUser.id(jwt), id);
    }

    /** Append an update to the quest journal. */
    @PostMapping("/quests/{id}/log")
    public ResponseEntity<LogEntryDto> addLogEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                   @Valid @RequestBody AddLogEntryRequest request) {
        QuestService.AppendResult result = questService.addLogEntry(CurrentUser.id(jwt), id, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.entry());
    }

    /** Incremental sync: pass the previous response's serverTime as {@code since}. */
    @GetMapping("/sync")
    public SyncResponse sync(@AuthenticationPrincipal Jwt jwt,
                             @RequestParam(name = "since", required = false) Instant since) {
        return questService.sync(CurrentUser.id(jwt), since);
    }
}
