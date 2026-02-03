CREATE TYPE payment_type AS ENUM ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER');

CREATE TABLE IF NOT EXISTS transactions_0
(
    uid               UUID PRIMARY KEY,
    created_at        TIMESTAMP    NOT NULL,
    modified_at       TIMESTAMP,
    user_uid          UUID         NOT NULL,
    wallet_uid        UUID         NOT NULL,
    amount            DECIMAL      NOT NULL,
    type              payment_type NOT NULL,
    status            VARCHAR(32)  NOT NULL,
    comment           VARCHAR(256),
    fee               DECIMAL,
    target_wallet_uid UUID,
    payment_method_id BIGINT,
    failure_reason    VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS transactions_1
(
    LIKE transactions_0 INCLUDING ALL
);
