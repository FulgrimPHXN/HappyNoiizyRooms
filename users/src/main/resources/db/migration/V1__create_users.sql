CREATE TABLE users (
    id            UUID PRIMARY KEY,
    version       BIGINT NOT NULL,
    user_name     VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL
);
