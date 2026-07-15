CREATE DATABASE IF NOT EXISTS d_game DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE d_game;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `username`        VARCHAR(64)     NOT NULL COMMENT '用户名',
    `password_hash`   VARCHAR(128)    NOT NULL COMMENT '密码哈希',
    `nickname`        VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '昵称',
    `email`           VARCHAR(128)    DEFAULT NULL COMMENT '邮箱',
    `mobile`          VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    `avatar_url`      VARCHAR(512)    DEFAULT NULL COMMENT '头像URL',
    `bio`             VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '个人简介',
    `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态:1正常 0封禁',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_mobile` (`mobile`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `role_code`       VARCHAR(64)     NOT NULL COMMENT '角色编码',
    `role_name`       VARCHAR(64)     NOT NULL COMMENT '角色名称',
    `description`     VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '描述',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `perm_code`       VARCHAR(128)    NOT NULL COMMENT '权限编码',
    `perm_name`       VARCHAR(128)    NOT NULL COMMENT '权限名称',
    `description`     VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '描述',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_perm_code` (`perm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 用户角色关联
CREATE TABLE IF NOT EXISTS `user_role_rel` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `role_id`         BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色权限关联
CREATE TABLE IF NOT EXISTS `role_permission_rel` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `role_id`         BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `permission_id`   BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 游戏分类
CREATE TABLE IF NOT EXISTS `game_category` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `name`            VARCHAR(64)     NOT NULL COMMENT '分类名称',
    `sort_order`      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏分类表';

-- 游戏表
CREATE TABLE IF NOT EXISTS `game` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `name`            VARCHAR(128)    NOT NULL COMMENT '游戏名称',
    `category_id`     BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
    `cover_url`       VARCHAR(512)    DEFAULT NULL COMMENT '封面URL',
    `description`     VARCHAR(1000)   NOT NULL DEFAULT '' COMMENT '简介',
    `developer`       VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '开发商',
    `release_date`    DATE            DEFAULT NULL COMMENT '发行日期',
    `avg_rating`      DECIMAL(3,2)    NOT NULL DEFAULT 0.00 COMMENT '平均评分',
    `rating_count`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '评分人数',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_name` (`name`(20))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏表';

-- 标签表
CREATE TABLE IF NOT EXISTS `tag` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `name`            VARCHAR(64)     NOT NULL COMMENT '标签名称',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- 游戏标签关联
CREATE TABLE IF NOT EXISTS `game_tag_rel` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `game_id`         BIGINT UNSIGNED NOT NULL COMMENT '游戏ID',
    `tag_id`          BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_tag` (`game_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏标签关联表';

-- 游戏评分
CREATE TABLE IF NOT EXISTS `game_rating` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `game_id`         BIGINT UNSIGNED NOT NULL COMMENT '游戏ID',
    `score`           TINYINT UNSIGNED NOT NULL COMMENT '评分1-10',
    `summary`         VARCHAR(300)    NOT NULL DEFAULT '' COMMENT '短评',
    `pros`            VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '优点',
    `cons`            VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '缺点',
    `playtime_hours`  INT UNSIGNED    DEFAULT NULL COMMENT '游玩时长(小时)',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_game` (`user_id`, `game_id`),
    KEY `idx_game_id` (`game_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏评分表';

-- 版块表
CREATE TABLE IF NOT EXISTS `board` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `name`            VARCHAR(64)     NOT NULL COMMENT '版块名称',
    `description`     VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '描述',
    `sort_order`      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版块表';

-- 帖子表
CREATE TABLE IF NOT EXISTS `post` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `board_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '版块ID',
    `game_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '关联游戏ID',
    `collection_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '个人合集ID',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '作者ID',
    `title`           VARCHAR(200)    NOT NULL COMMENT '标题',
    `content`         LONGTEXT        NOT NULL COMMENT '正文',
    `is_original`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否原创',
    `contains_ai_generated` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否包含AI生成内容',
    `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态:0草稿 1待审核 2已通过 3已拒绝',
    `scheduled_publish_at` DATETIME DEFAULT NULL COMMENT '计划进入审核队列的时间',
    `view_count`      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '浏览数',
    `like_count`      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count`   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '评论数',
    `favorite_count`  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '收藏数',
    `version`         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_board_status` (`board_id`, `status`, `gmt_create`),
    KEY `idx_game_status` (`game_id`, `status`, `gmt_create`),
    KEY `idx_status_schedule` (`status`, `scheduled_publish_at`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子表';

-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `post_id`         BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `parent_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父评论ID,0为顶级',
    `content`         VARCHAR(2000)   NOT NULL COMMENT '评论内容',
    `like_count`      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '点赞数',
    `status`          TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '状态:0草稿 1待审核 2已通过 3已拒绝',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_post_parent` (`post_id`, `parent_id`, `gmt_create`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 点赞表
CREATE TABLE IF NOT EXISTS `user_like` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `target_type`     TINYINT UNSIGNED NOT NULL COMMENT '目标类型:1帖子 2评论 3游戏',
    `target_id`       BIGINT UNSIGNED NOT NULL COMMENT '目标ID',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- 收藏表
CREATE TABLE IF NOT EXISTS `user_favorite` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `target_type`     TINYINT UNSIGNED NOT NULL COMMENT '目标类型:1帖子 3游戏',
    `target_id`       BIGINT UNSIGNED NOT NULL COMMENT '目标ID',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- 关注表
CREATE TABLE IF NOT EXISTS `user_follow` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `follower_id`     BIGINT UNSIGNED NOT NULL COMMENT '关注者ID',
    `followee_id`     BIGINT UNSIGNED NOT NULL COMMENT '被关注者ID',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follower_followee` (`follower_id`, `followee_id`),
    KEY `idx_followee_id` (`followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注表';

-- 通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `receiver_id`     BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
    `sender_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '发送者ID',
    `type`            TINYINT UNSIGNED NOT NULL COMMENT '类型:1点赞 2评论 3关注 4系统 5审核',
    `title`           VARCHAR(128)    NOT NULL COMMENT '标题',
    `content`         VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '内容',
    `target_type`     TINYINT UNSIGNED DEFAULT NULL COMMENT '关联目标类型',
    `target_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '关联目标ID',
    `is_read`         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否已读:0否 1是',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_receiver_read` (`receiver_id`, `is_read`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 文件记录表
CREATE TABLE IF NOT EXISTS `file_record` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '上传用户ID',
    `file_key`        VARCHAR(256)    NOT NULL COMMENT '文件Key',
    `file_url`        VARCHAR(512)    NOT NULL COMMENT '文件URL',
    `file_name`       VARCHAR(256)    NOT NULL COMMENT '原始文件名',
    `file_size`       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    `content_type`    VARCHAR(128)    NOT NULL DEFAULT '' COMMENT 'MIME类型',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_key` (`file_key`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `operator_id`     BIGINT UNSIGNED NOT NULL COMMENT '操作人ID',
    `action`          VARCHAR(64)     NOT NULL COMMENT '操作动作',
    `target_type`     VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '目标类型',
    `target_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '目标ID',
    `detail`          VARCHAR(1000)   NOT NULL DEFAULT '' COMMENT '详情',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_operator_time` (`operator_id`, `gmt_create`),
    KEY `idx_action_time` (`action`, `gmt_create`),
    KEY `idx_target_time` (`target_type`, `target_id`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 用户积分流水表
CREATE TABLE IF NOT EXISTS `user_point_log` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `source_type`     VARCHAR(64)     NOT NULL COMMENT '积分来源类型',
    `source_key`      VARCHAR(128)    NOT NULL COMMENT '幂等业务键',
    `points_change`   INT             NOT NULL COMMENT '积分变化',
    `target_type`     VARCHAR(64)     DEFAULT NULL COMMENT '关联目标类型',
    `target_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '关联目标ID',
    `remark`          VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '备注',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_source_key` (`user_id`, `source_key`),
    KEY `idx_user_time` (`user_id`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分流水表';

-- 用户徽章表
CREATE TABLE IF NOT EXISTS `user_badge` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `badge_code`      VARCHAR(64)     NOT NULL COMMENT '徽章编码',
    `badge_name`      VARCHAR(64)     NOT NULL COMMENT '徽章名称',
    `description`     VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '徽章描述',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_badge` (`user_id`, `badge_code`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户徽章表';

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

-- 社区话题
CREATE TABLE IF NOT EXISTS `post_topic` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `name`            VARCHAR(64)     NOT NULL COMMENT '话题名称',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_topic_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区话题';

-- 帖子话题关联
CREATE TABLE IF NOT EXISTS `post_topic_rel` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `post_id`         BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
    `topic_id`        BIGINT UNSIGNED NOT NULL COMMENT '话题ID',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_topic` (`post_id`, `topic_id`),
    KEY `idx_topic_post` (`topic_id`, `post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子话题关联';

-- 个人合集
CREATE TABLE IF NOT EXISTS `post_collection` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `name`            VARCHAR(64)     NOT NULL COMMENT '合集名称',
    `gmt_create`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除:0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_collection_name` (`user_id`, `name`),
    KEY `idx_collection_user` (`user_id`, `gmt_modified`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人合集';
