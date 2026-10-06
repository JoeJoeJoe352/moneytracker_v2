ALTER TABLE transactions
ADD COLUMN special_type Enum ('SYNC', 'RECURRING') DEFAULT NULL AFTER transaction_type;