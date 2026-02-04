CREATE TABLE IF NOT EXISTS wallet_types
(
    uid           UUID PRIMARY KEY ,
    created_at    TIMESTAMP   NOT NULL DEFAULT now(),
    modified_at   TIMESTAMP,
    name          VARCHAR(32) NOT NULL,
    currency_code VARCHAR(3)  NOT NULL,
    status        VARCHAR(18) NOT NULL,
    archived_at   TIMESTAMP,
    user_type     VARCHAR(15),
    creator       VARCHAR(255),
    modifier      VARCHAR(255)
);
