CREATE TABLE
    transfer_transaction_sides (
        id BIGINT NOT NULL AUTO_INCREMENT,
        src_transaction_id BIGINT NOT NULL,
        dst_transaction_id BIGINT NOT NULL,
        PRIMARY KEY (id),
        UNIQUE KEY uk_transfer_transaction_sides (src_transaction_id, dst_transaction_id),
        FOREIGN KEY (src_transaction_id) REFERENCES transactions (id),
        FOREIGN KEY (dst_transaction_id) REFERENCES transactions (id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
