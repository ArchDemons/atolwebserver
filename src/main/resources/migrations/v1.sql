CREATE TABLE IF NOT EXISTS json_tasks (
    uuid        VARCHAR (36) PRIMARY KEY NOT NULL,
    data        TEXT,
    is_ready    BOOLEAN DEFAULT 0,
    timestamp   DATETIME
);

CREATE TABLE IF NOT EXISTS json_results (
    uuid              VARCHAR (36) NOT NULL,
    number            INTEGER      NOT NULL,
    timestamp         DATETIME,
    status            INTEGER DEFAULT 0,
    error_code        INTEGER DEFAULT 0,
    error_description TEXT DEFAULT '',
    result_data       TEXT DEFAULT '',
    PRIMARY KEY (uuid, number)
);

CREATE TABLE settings (
    "setting"   VARCHAR (32) PRIMARY KEY NOT NULL,
    "value"     TEXT
);

INSERT INTO settings (setting, value) VALUES ('clear_interval', '720');

CREATE TABLE "state" (
    "state_id" VARCHAR (32) PRIMARY KEY NOT NULL,
    "value" TEXT
);

CREATE TRIGGER IF NOT EXISTS clear_tasks BEFORE INSERT ON json_tasks BEGIN
    DELETE FROM json_tasks WHERE
    ((JULIANDAY('now', 'localtime') - JULIANDAY(timestamp)) * 24 >= (
        SELECT CAST(value as INTEGER) FROM settings WHERE setting = 'clear_interval')
    );
END;

CREATE TRIGGER IF NOT EXISTS clear_results BEFORE INSERT ON json_results BEGIN
    DELETE FROM json_results WHERE
    ((JULIANDAY('now', 'localtime') - JULIANDAY(timestamp)) * 24 >= (
        SELECT CAST(value as INTEGER) FROM settings WHERE setting = 'clear_interval')
    );
END;

INSERT INTO meta (key, value) VALUES ('version', '1')
