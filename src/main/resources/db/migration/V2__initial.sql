CREATE TABLE "users"
(
    "id"           bigserial    NOT NULL PRIMARY KEY,
    "guid"         uuid         NOT NULL,
    "account_id"   bigint       NOT NULL,
    "account_guid" uuid         NULL,
    "display_name" varchar(128)          DEFAULT NULL,
    "first_name"   varchar(128)          DEFAULT NULL,
    "second_name"  varchar(128)          DEFAULT NULL,
    "email"        varchar(128) NOT NULL DEFAULT '',
    "created_at"   timestamp    NOT NULL
);

CREATE UNIQUE INDEX "users_guid" ON "users" ("guid");


CREATE TABLE "accounts"
(
    "id"         bigserial    NOT NULL PRIMARY KEY,
    "guid"       uuid         NOT NULL,
    "name"       varchar(128) NOT NULL DEFAULT '',
    "created_at" timestamp    NOT NULL,
    "user_id"    bigint       NOT NULL
);

COMMENT ON COLUMN accounts."user_id" IS 'Owner';

CREATE UNIQUE INDEX "accounts_guid" ON "accounts" ("guid");
