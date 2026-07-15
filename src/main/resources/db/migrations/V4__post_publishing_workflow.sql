ALTER TABLE `post`
    MODIFY COLUMN `board_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '版块ID',
    ADD COLUMN `collection_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '个人合集ID' AFTER `game_id`,
    ADD COLUMN `is_original` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否原创' AFTER `content`,
    ADD COLUMN `contains_ai_generated` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否包含AI生成内容' AFTER `is_original`,
    ADD COLUMN `scheduled_publish_at` DATETIME DEFAULT NULL COMMENT '计划进入审核队列的时间' AFTER `status`,
    ADD KEY `idx_status_schedule` (`status`, `scheduled_publish_at`);

CREATE TABLE IF NOT EXISTS `post_topic` (
    `id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(64) NOT NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_topic_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区话题';

CREATE TABLE IF NOT EXISTS `post_topic_rel` (
    `id` BIGINT UNSIGNED NOT NULL,
    `post_id` BIGINT UNSIGNED NOT NULL,
    `topic_id` BIGINT UNSIGNED NOT NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_topic` (`post_id`, `topic_id`),
    KEY `idx_topic_post` (`topic_id`, `post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子话题关联';

CREATE TABLE IF NOT EXISTS `post_collection` (
    `id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(64) NOT NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_collection_name` (`user_id`, `name`),
    KEY `idx_collection_user` (`user_id`, `gmt_modified`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人合集';
