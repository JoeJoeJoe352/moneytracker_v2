-- A szinkronizálás időpontja (UTC) a user időzónája szerinti nap helyett, így időzónától független
-- statisztikák készíthetők. A meglévő sorok a napjuk éjfélét kapják (DATE -> DATETIME konverzió)
ALTER TABLE balance_sync
CHANGE COLUMN sync_date synced_at DATETIME(6) NOT NULL;
