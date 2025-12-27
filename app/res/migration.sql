ATTACH DATABASE 'data.db' AS old_db;

DELETE FROM food_info_drinks;
DELETE FROM users;
DELETE FROM food_infos;

DELETE FROM sqlite_sequence WHERE name IN ('users', 'food_infos');

INSERT INTO users (id, chat_id, username, name, phone, alias, sex, role)
SELECT
    u.id,
    u.chat_id,
    u.username,
    u.real_name, -- ~ 'name'
    u.phone,
    u.nik_name,  -- ~ 'alias'
    u.sex,
    CASE u.role
        WHEN 'DEFAULT' THEN 'GUEST'
        ELSE u.role
    END
FROM old_db.Users AS u;

DETACH DATABASE old_db;

SELECT count(*) FROM users;