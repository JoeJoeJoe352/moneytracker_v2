-- tinyint -> varchar ('0'/'1'), átírás 'ACTIVE'/'DISABLED'-re, végül enum
-- (közvetlen tinyint -> enum konverzió a számot enum indexként értelmezné)

ALTER TABLE wallet DROP CHECK wallet_chk_1;
ALTER TABLE wallet MODIFY status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
UPDATE wallet SET status = CASE status WHEN '1' THEN 'DISABLED' ELSE 'ACTIVE' END;
ALTER TABLE wallet MODIFY status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE category DROP CHECK category_chk_1;
ALTER TABLE category MODIFY status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
UPDATE category SET status = CASE status WHEN '1' THEN 'DISABLED' ELSE 'ACTIVE' END;
ALTER TABLE category MODIFY status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE balance_sync MODIFY status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
UPDATE balance_sync SET status = CASE status WHEN '1' THEN 'DISABLED' ELSE 'ACTIVE' END;
ALTER TABLE balance_sync MODIFY status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE';
