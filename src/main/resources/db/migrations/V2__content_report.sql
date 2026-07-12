CREATE TABLE IF NOT EXISTS `content_report` (
    `id` BIGINT UNSIGNED NOT NULL,
    `reporter_id` BIGINT UNSIGNED NOT NULL,
    `target_type` TINYINT UNSIGNED NOT NULL,
    `target_id` BIGINT UNSIGNED NOT NULL,
    `reason` VARCHAR(500) NOT NULL,
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    `handle_note` VARCHAR(500) NOT NULL DEFAULT '',
    `handler_id` BIGINT UNSIGNED DEFAULT NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reporter_target` (`reporter_id`, `target_type`, `target_id`),
    KEY `idx_status_create` (`status`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='content report';
