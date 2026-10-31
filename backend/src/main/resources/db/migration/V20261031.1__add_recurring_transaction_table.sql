CREATE TABLE
    recurring_transaction (
        id BIGINT NOT NULL AUTO_INCREMENT,
        name VARCHAR(255) NOT NULL,
        price_sum DECIMAL(10, 2) NOT NULL,
        start_date DATE NOT NULL,
        next_occurrence_date DATE NOT NULL,
        recurrence_interval Enum ('WEEKLY', 'MONTHLY') NOT NULL,
        transaction_type Enum ('INCOME', 'OUTCOME') NOT NULL,
        status Enum ('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
        wallet_id BIGINT NOT NULL,
        created_at DATETIME (6) DEFAULT NULL,
        updated_at DATETIME (6) DEFAULT NULL,
        PRIMARY KEY (id),
        FOREIGN KEY (wallet_id) REFERENCES wallet (id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;