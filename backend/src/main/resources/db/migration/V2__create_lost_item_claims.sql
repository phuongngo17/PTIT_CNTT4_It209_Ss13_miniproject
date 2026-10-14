CREATE TABLE lost_item_claims (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    claimant_id BIGINT NOT NULL,
    proof VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at DATETIME(6) NOT NULL,
    reviewed_at DATETIME(6),
    CONSTRAINT uk_claim_post_user UNIQUE (post_id, claimant_id),
    CONSTRAINT fk_claim_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_claim_user FOREIGN KEY (claimant_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_claim_post_status ON lost_item_claims(post_id, status);
