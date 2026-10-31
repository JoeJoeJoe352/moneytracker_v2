ALTER TABLE transactions
ADD COLUMN recurring_transaction_id BIGINT NULL;

ALTER TABLE transactions ADD FOREIGN KEY (recurring_transaction_id) REFERENCES recurring_transaction (id);