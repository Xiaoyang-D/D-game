USE d_game;

-- 角色
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`) VALUES
(1, 'ADMIN', '管理员', '系统管理员'),
(2, 'USER', '普通用户', '普通社区用户');

-- 权限
INSERT INTO `sys_permission` (`id`, `perm_code`, `perm_name`, `description`) VALUES
(1, 'admin:user:manage', '用户管理', '管理用户'),
(2, 'admin:content:audit', '内容审核', '审核帖子与评论'),
(3, 'admin:role:manage', '角色管理', '管理角色权限'),
(4, 'game:manage', '游戏管理', '管理游戏库'),
(5, 'post:create', '发帖', '创建帖子'),
(6, 'comment:create', '评论', '发表评论');

-- 角色权限
INSERT INTO `role_permission_rel` (`id`, `role_id`, `permission_id`) VALUES
(1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5), (6, 1, 6),
(7, 2, 5), (8, 2, 6);

-- 管理员账号 admin / Admin@123456 (BCrypt)
INSERT INTO `user` (`id`, `username`, `password_hash`, `nickname`, `email`, `status`) VALUES
(1, 'admin', '$2a$10$/Yd92wXtQODhRkRloXLEGuMbKlX4XMTQXjrPyB9oOQ9OPmlG4/ZTK', '系统管理员', 'admin@dgame.com', 1);

INSERT INTO `user_role_rel` (`id`, `user_id`, `role_id`) VALUES
(1, 1, 1);

-- 游戏分类
INSERT INTO `game_category` (`id`, `name`, `sort_order`) VALUES
(1, '动作', 1),
(2, '角色扮演', 2),
(3, '策略', 3),
(4, '独立', 4);

-- 标签
INSERT INTO `tag` (`id`, `name`) VALUES
(1, '多人联机'),
(2, '开放世界'),
(3, '像素风'),
(4, '剧情向');

-- 版块
INSERT INTO `board` (`id`, `name`, `description`, `sort_order`) VALUES
(1, '综合讨论', '游戏综合讨论区', 1),
(2, '攻略分享', '游戏攻略与心得', 2),
(3, '组队交友', '寻找队友与交友', 3);

-- 示例游戏
INSERT INTO `game` (`id`, `name`, `category_id`, `description`, `developer`, `avg_rating`, `rating_count`) VALUES
(1, '星际探险', 1, '一款太空探索动作游戏', 'Star Studio', 8.50, 100),
(2, '王国传说', 2, '经典RPG冒险', 'Legend Games', 9.00, 200);

INSERT INTO `game_tag_rel` (`id`, `game_id`, `tag_id`) VALUES
(1, 1, 1), (2, 1, 2), (3, 2, 4);
