package io.github.azaev.questjournal.quest;

public enum LogEntryKind {
    /** Written automatically when a quest is created. */
    CREATED,
    /** A free-form update the user appended to the quest. */
    NOTE,
    /** Written automatically when the quest status changes. */
    STATUS_CHANGE
}
