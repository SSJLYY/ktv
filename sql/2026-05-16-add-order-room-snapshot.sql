ALTER TABLE `t_order`
    ADD COLUMN `room_name_snapshot` VARCHAR(50) DEFAULT NULL COMMENT 'Room name snapshot at order open time' AFTER `room_id`,
    ADD COLUMN `room_type_snapshot` VARCHAR(20) DEFAULT NULL COMMENT 'Room type snapshot at order open time' AFTER `room_name_snapshot`,
    ADD COLUMN `room_price_per_hour_snapshot` DECIMAL(10,2) DEFAULT NULL COMMENT 'Room hourly price snapshot at order open time' AFTER `room_type_snapshot`,
    ADD COLUMN `room_min_consumption_snapshot` DECIMAL(10,2) DEFAULT NULL COMMENT 'Room minimum consumption snapshot at order open time' AFTER `room_price_per_hour_snapshot`;

UPDATE `t_order` o
INNER JOIN `t_room` r ON r.id = o.room_id AND r.deleted = 0
SET o.room_name_snapshot = COALESCE(o.room_name_snapshot, r.name),
    o.room_type_snapshot = COALESCE(o.room_type_snapshot, r.type),
    o.room_price_per_hour_snapshot = COALESCE(o.room_price_per_hour_snapshot, r.price_per_hour),
    o.room_min_consumption_snapshot = COALESCE(o.room_min_consumption_snapshot, r.min_consumption)
WHERE o.deleted = 0
  AND o.status = 1;
