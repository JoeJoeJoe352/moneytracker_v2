-- ugyanaz, mint a V20261021.4-ben: int -> varchar -> enum
-- (a transactions.status-on nincs CHECK constraint)

ALTER TABLE transactions MODIFY status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
UPDATE transactions SET status = CASE status WHEN '1' THEN 'DISABLED' ELSE 'ACTIVE' END;
ALTER TABLE transactions MODIFY status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE';
