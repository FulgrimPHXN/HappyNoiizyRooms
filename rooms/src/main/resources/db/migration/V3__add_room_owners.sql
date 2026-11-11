INSERT INTO room_players (room_id, user_id, role)
SELECT id, owner_id, 'GM' FROM rooms
ON CONFLICT (room_id, user_id) DO UPDATE SET role = 'GM';
