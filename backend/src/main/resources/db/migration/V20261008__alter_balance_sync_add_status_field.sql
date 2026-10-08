ALTER TABLE balance_sync
ADD COLUMN status TINYINT NOT NULL DEFAULT 0 AFTER actual_balance;
