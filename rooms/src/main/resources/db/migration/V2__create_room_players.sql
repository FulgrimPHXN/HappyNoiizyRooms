CREATE TABLE room_players (
    room_id UUID NOT NULL REFERENCES rooms(id),
    user_id UUID NOT NULL,
    role    VARCHAR(20) NOT NULL,
    PRIMARY KEY (room_id, user_id)
)
