CREATE TABLE users (
    id            UUID PRIMARY KEY,
    username      VARCHAR(32)  NOT NULL,
    display_name  VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX ux_users_username ON users (lower(username));

CREATE TABLE quests (
    id          UUID PRIMARY KEY,
    owner_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    type        VARCHAR(16)  NOT NULL CHECK (type IN ('MAIN', 'SIDE')),
    status      VARCHAR(16)  NOT NULL CHECK (status IN ('ACTIVE', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    remind_at   TIMESTAMPTZ,
    version     BIGINT       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX ix_quests_owner_updated ON quests (owner_id, updated_at);

-- Append-only quest journal: entries are never edited, so devices never conflict on them.
CREATE TABLE quest_log (
    id         UUID PRIMARY KEY,
    quest_id   UUID        NOT NULL REFERENCES quests (id) ON DELETE CASCADE,
    kind       VARCHAR(16) NOT NULL CHECK (kind IN ('CREATED', 'NOTE', 'STATUS_CHANGE')),
    text       TEXT,
    old_status VARCHAR(16),
    new_status VARCHAR(16),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_quest_log_quest_created ON quest_log (quest_id, created_at);
CREATE INDEX ix_quest_log_created ON quest_log (created_at);
