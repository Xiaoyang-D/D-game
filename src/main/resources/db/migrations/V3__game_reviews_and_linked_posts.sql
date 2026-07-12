ALTER TABLE `game_rating`
    ADD COLUMN `summary` VARCHAR(300) NOT NULL DEFAULT '' COMMENT '短评' AFTER `score`,
    ADD COLUMN `pros` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '优点' AFTER `summary`,
    ADD COLUMN `cons` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '缺点' AFTER `pros`,
    ADD COLUMN `playtime_hours` INT UNSIGNED DEFAULT NULL COMMENT '游玩时长(小时)' AFTER `cons`;

ALTER TABLE `post`
    ADD COLUMN `game_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联游戏ID' AFTER `board_id`,
    ADD KEY `idx_game_status` (`game_id`, `status`, `gmt_create`);
