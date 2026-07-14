package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * 站内通知 Mapper。
 */
public interface NotificationMapper extends BaseMapper<Notification> {
}
