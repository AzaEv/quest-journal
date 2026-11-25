package io.github.azaev.questjournal;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestApiTest extends ApiTestSupport {

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerUser();
    }

    @Test
    void createQuestWithReminderWritesCreatedLogEntry() throws Exception {
        postAs(token, """
                {"title":"  Hack the mainframe  ","description":"before the deadline","type":"MAIN",
                 "remindAt":"2026-12-10T15:00:00Z"}
                """, "/api/quests")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quest.title").value("Hack the mainframe"))
                .andExpect(jsonPath("$.quest.type").value("MAIN"))
                .andExpect(jsonPath("$.quest.status").value("ACTIVE"))
                .andExpect(jsonPath("$.quest.remindAt").value("2026-12-10T15:00:00Z"))
                .andExpect(jsonPath("$.quest.version").value(0))
                .andExpect(jsonPath("$.log", hasSize(1)))
                .andExpect(jsonPath("$.log[0].kind").value("CREATED"));
    }

    @Test
    void createWithoutReminderDefaultsToSideQuest() throws Exception {
        postAs(token, """
                {"title":"Buy synth-noodles"}
                """, "/api/quests")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quest.type").value("SIDE"))
                .andExpect(jsonPath("$.quest.remindAt", nullValue()));
    }

    @Test
    void createWithSameClientIdIsIdempotent() throws Exception {
        UUID id = UUID.randomUUID();
        String body = """
                {"id":"%s","title":"Offline quest"}
                """.formatted(id);
        postAs(token, body, "/api/quests").andExpect(status().isCreated());
        postAs(token, body, "/api/quests")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quest.id").value(id.toString()))
                .andExpect(jsonPath("$.log", hasSize(1)));

        // another user cannot hijack the id
        postAs(registerUser(), body, "/api/quests").andExpect(status().isConflict());
    }

    @Test
    void updateStatusAppendsStatusChangeAndBumpsVersion() throws Exception {
        String id = createQuest("Upgrade cyberdeck");

        putAs(token, """
                {"version":0,"title":"Upgrade cyberdeck","type":"SIDE","status":"IN_PROGRESS",
                 "remindAt":"2026-12-01T09:00:00Z"}
                """, "/api/quests/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quest.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.quest.version").value(1))
                .andExpect(jsonPath("$.log", hasSize(2)))
                .andExpect(jsonPath("$.log[1].kind").value("STATUS_CHANGE"))
                .andExpect(jsonPath("$.log[1].oldStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.log[1].newStatus").value("IN_PROGRESS"));

        // removing the reminder = sending remindAt null; same status -> no new log entry
        putAs(token, """
                {"version":1,"title":"Upgrade cyberdeck","type":"SIDE","status":"IN_PROGRESS","remindAt":null}
                """, "/api/quests/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quest.remindAt", nullValue()))
                .andExpect(jsonPath("$.log", hasSize(2)));
    }

    @Test
    void staleVersionIsConflictWithCurrentState() throws Exception {
        String id = createQuest("Race condition");
        String update = """
                {"version":0,"title":"%s","type":"SIDE","status":"ACTIVE"}
                """;
        putAs(token, update.formatted("From PC"), "/api/quests/{id}", id).andExpect(status().isOk());

        putAs(token, update.formatted("From phone"), "/api/quests/{id}", id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.current.title").value("From PC"))
                .andExpect(jsonPath("$.current.version").value(1));
    }

    @Test
    void notesAreAppendedToTheJournalIdempotently() throws Exception {
        String id = createQuest("Find the fixer");
        UUID noteId = UUID.randomUUID();
        String note = """
                {"id":"%s","text":"Fixer hangs out at the night market"}
                """.formatted(noteId);

        postAs(token, note, "/api/quests/{id}/log", id)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("NOTE"))
                .andExpect(jsonPath("$.text").value("Fixer hangs out at the night market"));
        postAs(token, note, "/api/quests/{id}/log", id).andExpect(status().isOk());

        getAs(token, "/api/quests/{id}", id)
                .andExpect(jsonPath("$.log", hasSize(2)))
                .andExpect(jsonPath("$.log[1].id").value(noteId.toString()));

        postAs(token, """
                {"text":"   "}
                """, "/api/quests/{id}/log", id).andExpect(status().isBadRequest());
    }

    @Test
    void usersCannotSeeEachOthersQuests() throws Exception {
        String id = createQuest("Secret op");
        String stranger = registerUser();

        getAs(stranger, "/api/quests/{id}", id).andExpect(status().isNotFound());
        postAs(stranger, """
                {"text":"pwned"}
                """, "/api/quests/{id}/log", id).andExpect(status().isNotFound());
        deleteAs(stranger, "/api/quests/{id}", id).andExpect(status().isNotFound());
        getAs(stranger, "/api/quests").andExpect(jsonPath("$", hasSize(0)));
        getAs(stranger, "/api/sync").andExpect(jsonPath("$.quests", hasSize(0)));
    }

    @Test
    void listFiltersByStatus() throws Exception {
        createQuest("Active one");
        String done = createQuest("Done one");
        putAs(token, """
                {"version":0,"title":"Done one","type":"SIDE","status":"COMPLETED"}
                """, "/api/quests/{id}", done).andExpect(status().isOk());

        getAs(token, "/api/quests").andExpect(jsonPath("$", hasSize(2)));
        getAs(token, "/api/quests?status=COMPLETED")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Done one"));
        getAs(token, "/api/quests?status=ACTIVE&status=IN_PROGRESS")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Active one"));
    }

    @Test
    void deleteIsSoftAndPropagatesThroughSync() throws Exception {
        String id = createQuest("Doomed quest");
        String firstSync = json(getAs(token, "/api/sync").andExpect(status().isOk()));
        String since = JsonPath.read(firstSync, "$.serverTime");
        assertThat(JsonPath.<List<String>>read(firstSync, "$.quests[*].id")).containsExactly(id);
        assertThat(JsonPath.<List<String>>read(firstSync, "$.logEntries[*].kind")).containsExactly("CREATED");

        deleteAs(token, "/api/quests/{id}", id).andExpect(status().isNoContent());
        getAs(token, "/api/quests/{id}", id).andExpect(status().isNotFound());
        getAs(token, "/api/quests").andExpect(jsonPath("$", hasSize(0)));

        // incremental sync returns the tombstone so other devices can drop the quest
        getAs(token, "/api/sync?since={since}", since)
                .andExpect(jsonPath("$.quests[0].id").value(id))
                .andExpect(jsonPath("$.quests[0].deletedAt").isNotEmpty());

        // a full sync no longer contains it
        getAs(token, "/api/sync").andExpect(jsonPath("$.quests", hasSize(0)));
    }

    @Test
    void incrementalSyncReturnsNewNotes() throws Exception {
        String id = createQuest("Long quest");
        String since = JsonPath.read(json(getAs(token, "/api/sync")), "$.serverTime");

        postAs(token, """
                {"text":"Chapter two"}
                """, "/api/quests/{id}/log", id).andExpect(status().isCreated());

        String delta = json(getAs(token, "/api/sync?since={since}", since));
        assertThat(JsonPath.<List<String>>read(delta, "$.logEntries[*].text")).contains("Chapter two");
    }

    @Test
    void unknownQuestIsNotFound() throws Exception {
        getAs(token, "/api/quests/{id}", UUID.randomUUID()).andExpect(status().isNotFound());
    }

    private String createQuest(String title) throws Exception {
        String body = json(postAs(token, """
                {"title":"%s"}
                """.formatted(title), "/api/quests").andExpect(status().isCreated()));
        return JsonPath.read(body, "$.quest.id");
    }
}
