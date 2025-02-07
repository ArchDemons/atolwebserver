CREATE TABLE IF NOT EXISTS "devices" (
    "id"    varchar(16),
    "name"  varchar(128) NOT NULL DEFAULT '',
    "is_active" bool NOT NULL DEFAULT false,
    "is_default"    bool NOT NULL DEFAULT false,
    "model" integer NOT NULL DEFAULT 500,
    "user_password" varchar(16) NOT NULL DEFAULT '',
    "access_password"   varchar(16) NOT NULL DEFAULT '',
    "port"  varchar(10) NOT NULL DEFAULT '',
    "com"   varchar(32) NOT NULL DEFAULT '',
    "usb"   varchar(32) NOT NULL DEFAULT '',
    "baud_rate" integer NOT NULL DEFAULT 115200,
    "ip_addr"   varchar(128) NOT NULL DEFAULT '',
    "ip_port"   integer NOT NULL DEFAULT 0,
    "mac"   varchar(17) NOT NULL DEFAULT '',
    "ofd_channel"   varchar(10) NOT NULL DEFAULT 'none',
    "use_global_sp" bool NOT NULL DEFAULT true,
    "scripts_path"  varchar(256) NOT NULL DEFAULT '',
    "use_global_icds"   bool NOT NULL DEFAULT true,
    "invert_cd_status"  bool NOT NULL DEFAULT false,
    "use_global_hl" bool NOT NULL DEFAULT true,
    "header_lines"  text NOT NULL DEFAULT '',
    "use_global_fl" bool NOT NULL DEFAULT true,
    "footer_lines"  text NOT NULL DEFAULT '',
    "block_id"  integer DEFAULT null,
    PRIMARY KEY("id")
);

CREATE TABLE IF NOT EXISTS "block_records" (
    "id"    integer,
    "uuid"  varchar(36) NOT NULL,
    "device_id" varchar(16) NOT NULL,
    "data"  text NOT NULL DEFAULT '',
    "is_paper_error"    bool DEFAULT false,
    "is_fn_error"   bool DEFAULT false,
    "is_connection_error"   bool DEFAULT false,
    "created_time"  datetime DEFAULT (datetime('now', 'localtime')),
    PRIMARY KEY("id" AUTOINCREMENT),
    CONSTRAINT "fk_req_id" FOREIGN KEY("device_id") REFERENCES "devices"("id") on delete cascade
);

CREATE TABLE IF NOT EXISTS "global_settings" (
    "id"    integer DEFAULT 0,
    "scripts_path"  varchar(256) NOT NULL DEFAULT '',
    "invert_cd_status"  bool NOT NULL DEFAULT false,
    "header_lines"  text NOT NULL DEFAULT '',
    "footer_lines"  text NOT NULL DEFAULT '',
    "block_on_print_errors" bool NOT NULL DEFAULT false,
    "delete_requests_after" int NOT NULL DEFAULT 43200,
    "validate_requests_on_add"  bool NOT NULL DEFAULT false,
    PRIMARY KEY("id")
);

CREATE TABLE IF NOT EXISTS "requests" (
    "id"    integer,
    "uuid"  varchar(36) UNIQUE,
    "device_id" varchar(16) NOT NULL,
    "created_time"  datetime NOT NULL DEFAULT (datetime('now', 'localtime')),
    "finished_time" datetime,
    "data"  text NOT NULL DEFAULT '',
    "is_ready"  bool NOT NULL DEFAULT false,
    "is_canceled"   bool NOT NULL DEFAULT false,
    "in_progress"   bool NOT NULL DEFAULT false,
    "callback_result_url"   text,
    "callback_result_complete"  boolean NOT NULL DEFAULT false,
    PRIMARY KEY("id" AUTOINCREMENT)
);

CREATE TABLE IF NOT EXISTS "results" (
    "id"    integer,
    "request_id"    integer NOT NULL,
    "number"    integer NOT NULL,
    "created_time"  datetime NOT NULL DEFAULT (datetime('now', 'localtime')),
    "updated_time"  datetime,
    "status"    integer NOT NULL DEFAULT 0,
    "error_code"    integer NOT NULL DEFAULT 0,
    "error_description" text NOT NULL DEFAULT '',
    "result_data"   text NOT NULL DEFAULT '',
    PRIMARY KEY("id" AUTOINCREMENT),
    CONSTRAINT "fk_req_id" FOREIGN KEY("request_id") REFERENCES "requests"("id") on delete cascade
);

CREATE TABLE IF NOT EXISTS "users" (
    "name"  varchar(128) NOT NULL,
    "pwd"   text NOT NULL,
    PRIMARY KEY("name")
);

CREATE UNIQUE INDEX IF NOT EXISTS "idx_results" ON "results" (
    "request_id",
    "number"
);

INSERT INTO requests (uuid, device_id, created_time, "data", is_ready, is_canceled, callback_result_url)
    SELECT uuid, '', "timestamp", "data", is_ready, is_canceled, '' FROM json_tasks;

INSERT INTO results (request_id, "number", created_time, updated_time, status, error_code, error_description, result_data)
    SELECT uuid, "number", "timestamp", "timestamp", status, error_code, error_description, result_data FROM json_results;

DROP TRIGGER IF EXISTS clear_results;

DROP TRIGGER IF EXISTS clear_tasks;

DROP TABLE IF EXISTS json_tasks;

DROP TABLE IF EXISTS json_results;

INSERT INTO global_settings (id) VALUES (0);

DROP TABLE IF EXISTS settings;

DROP TABLE IF EXISTS "state";

CREATE TRIGGER clear_requests before insert on requests begin
    delete from requests where
    ((julianday('now', 'localtime') - julianday(finished_time)) * 24 * 60 >=
      (select  delete_requests_after from global_settings)
    );
end;
