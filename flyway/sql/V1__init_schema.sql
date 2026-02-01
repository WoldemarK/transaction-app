CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Создание типа для платежей
DO $$
    BEGIN
        CREATE TYPE payment_type AS ENUM ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER');
    EXCEPTION
        WHEN duplicate_object THEN NULL;
    END $$;

-- Broadcast-таблица (реплицируется на все шарды)
CREATE TABLE IF NOT EXISTS wallet_types (
                                            uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                            created_at TIMESTAMP NOT NULL DEFAULT now(),
                                            modified_at TIMESTAMP,
                                            name VARCHAR(32) NOT NULL,
                                            currency_code VARCHAR(3) NOT NULL,
                                            status VARCHAR(18) NOT NULL,
                                            archived_at TIMESTAMP,
                                            user_type VARCHAR(15),
                                            creator VARCHAR(255),
                                            modifier VARCHAR(255)
);

-- Шардируемые таблицы (одинаковая структура на всех шардах)
CREATE TABLE IF NOT EXISTS wallets (
                                       uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                       created_at TIMESTAMP NOT NULL DEFAULT now(),
                                       modified_at TIMESTAMP,
                                       name VARCHAR(32) NOT NULL,
                                       wallet_type_uid UUID NOT NULL REFERENCES wallet_types(uid) ON DELETE RESTRICT,
                                       user_uid UUID NOT NULL,
                                       status VARCHAR(30) NOT NULL,
                                       balance DECIMAL(19,4) NOT NULL DEFAULT 0.0,
                                       archived_at TIMESTAMP
);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_wallets_user_uid ON wallets(user_uid);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_wallets_status ON wallets(status);

CREATE TABLE IF NOT EXISTS transactions (
                                            uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                            created_at TIMESTAMP NOT NULL DEFAULT now(),
                                            modified_at TIMESTAMP,
                                            user_uid UUID NOT NULL,
                                            wallet_uid UUID NOT NULL REFERENCES wallets(uid) ON DELETE CASCADE,
                                            amount DECIMAL(19,4) NOT NULL DEFAULT 0.0,
                                            type payment_type NOT NULL,
                                            status VARCHAR(32) NOT NULL,
                                            comment VARCHAR(256),
                                            fee DECIMAL(19,4),
                                            target_wallet_uid UUID REFERENCES wallets(uid) ON DELETE SET NULL,
                                            payment_method_id BIGINT,
                                            failure_reason VARCHAR(256)
);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_user_uid ON transactions(user_uid);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_wallet_uid ON transactions(wallet_uid);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_created_at ON transactions(created_at DESC);