CREATE TABLE rooms (
    id  UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    owner_id UUID NOT NULL,
    name    VARCHAR(60) NOT NULL,
    invite_code VARCHAR(255) UNIQUE
)