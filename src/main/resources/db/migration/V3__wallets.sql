CREATE TABLE IF NOT EXISTS wallets_0
(
    uid             UUID PRIMARY KEY,
    created_at      TIMESTAMP   NOT NULL,
    modified_at     TIMESTAMP,
    name            VARCHAR(32) NOT NULL,
    wallet_type_uid UUID        NOT NULL,
    user_uid        UUID        NOT NULL,
    status          VARCHAR(30) NOT NULL,
    balance         DECIMAL     NOT NULL DEFAULT 0,
    archived_at     TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wallets_1
(
    LIKE wallets_0 INCLUDING ALL
);
