CREATE TABLE IF NOT EXISTS "events"
(
    "id"              bigserial    NOT NULL PRIMARY KEY,
    "account_guid"    uuid         NULL,
    "guid"            uuid         NOT NULL,
    "aggregator"      varchar(128) NOT NULL,
    "aggregator_guid" uuid         NOT NULL,
    "creator_guid"    uuid         NULL,
    "created_at"      timestamp    NOT NULL,
    "type"            varchar(36)  NOT NULL,
    "version"         smallint,
    "skip"            boolean      NOT NULL DEFAULT false,
    "body"            jsonb        NOT NULL,
    "comment"         varchar(200)
);

COMMENT ON COLUMN events."account_guid" IS 'Allows split of DB per account';
COMMENT ON COLUMN events.guid IS 'GUID of Event';
COMMENT ON COLUMN events.aggregator IS 'Like User';
COMMENT ON COLUMN events."aggregator_guid" IS 'Like GUID of User';
COMMENT ON COLUMN events."creator_guid" IS 'GUID of User that created event';
COMMENT ON COLUMN events.type IS 'Like UserCreated';
COMMENT ON COLUMN events.version IS 'Version for body content for particular type';
COMMENT ON COLUMN events.skip IS 'To temporary skip some events in case of error';
COMMENT ON COLUMN events.body IS 'Event-specific data';

CREATE INDEX "events_aggregator_index" ON "events" ("account_guid", "aggregator", "aggregator_guid");
CREATE INDEX "events_type" ON "events" ("type");
