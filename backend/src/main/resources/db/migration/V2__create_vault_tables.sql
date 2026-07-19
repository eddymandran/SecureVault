CREATE TABLE vaults (
                        id           UUID PRIMARY KEY,
                        owner_id     UUID NOT NULL,
                        name         VARCHAR(255) NOT NULL,
                        description  TEXT,
                        created_at   TIMESTAMPTZ NOT NULL,
                        updated_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_vaults_owner_id ON vaults(owner_id);