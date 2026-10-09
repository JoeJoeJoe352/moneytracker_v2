CREATE TABLE
    balance_sync (
        id BIGINT NOT NULL AUTO_INCREMENT,
        wallet_id BIGINT NOT NULL,
        sync_transaction_id BIGINT NULL,
        sync_date DATE NOT NULL,
        actual_balance DECIMAL(10, 2) NOT NULL,
        PRIMARY KEY (id),
        UNIQUE KEY uk_balance_sync_transaction (sync_transaction_id),
        FOREIGN KEY (wallet_id) REFERENCES wallet (id),
        FOREIGN KEY (sync_transaction_id) REFERENCES transactions (id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;