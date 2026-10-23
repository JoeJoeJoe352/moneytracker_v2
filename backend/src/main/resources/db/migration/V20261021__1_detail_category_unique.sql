ALTER TABLE transaction_detail_category MODIFY category_id BIGINT NOT NULL,
MODIFY transaction_detail_id BIGINT NOT NULL,
ADD CONSTRAINT UC_category_detail UNIQUE (category_id, transaction_detail_id);