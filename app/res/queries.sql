SELECT
    u.name,
    u.username,
    fi.menu,
    GROUP_CONCAT(fid.drink, ', ') AS drinks,
    fi.additional
FROM users u
LEFT JOIN food_infos fi ON u.food_info_id = fi.id
LEFT JOIN food_info_drinks fid ON fi.id = fid.food_info_id
WHERE u.event_status = 'APPROVED' AND u.food_info_id IS NOT NULL
GROUP BY u.id;

SELECT drink, COUNT(*) as count
FROM food_info_drinks
GROUP BY drink
ORDER BY count DESC;
