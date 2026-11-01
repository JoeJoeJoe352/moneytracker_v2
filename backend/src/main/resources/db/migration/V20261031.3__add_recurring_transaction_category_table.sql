CREATE TABLE
    recurring_transaction_category (
        id BIGINT NOT NULL AUTO_INCREMENT,
        recurring_transaction_id BIGINT NOT NULL,
        category_id BIGINT NOT NULL,
        PRIMARY KEY (id),
        UNIQUE KEY uk_recurring_transaction_category (recurring_transaction_id, category_id),
        FOREIGN KEY (recurring_transaction_id) REFERENCES recurring_transaction (id),
        FOREIGN KEY (category_id) REFERENCES category (id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
