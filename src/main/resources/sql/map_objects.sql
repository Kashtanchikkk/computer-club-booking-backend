CREATE TABLE IF NOT EXISTS map_objects (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(100),
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    seat_id BIGINT NULL
);

CREATE INDEX IF NOT EXISTS idx_map_objects_club_id ON map_objects (club_id);
CREATE INDEX IF NOT EXISTS idx_map_objects_type ON map_objects (type);
CREATE INDEX IF NOT EXISTS idx_map_objects_seat_id ON map_objects (seat_id);
