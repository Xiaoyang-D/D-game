package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.GameBoardSetting;
import org.apache.ibatis.annotations.Mapper;

/** 游戏分区设置持久化接口。 */
@Mapper
public interface GameBoardSettingMapper extends BaseMapper<GameBoardSetting> {
    /** 定时发布重新查询作者角色，不依赖请求身份。 */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM user_role_rel ur JOIN sys_role r ON r.id = ur.role_id "
            + "JOIN `user` u ON u.id = ur.user_id WHERE ur.user_id = #{userId} "
            + "AND ur.is_deleted = 0 AND r.is_deleted = 0 AND r.role_code = 'ADMIN' AND u.is_deleted = 0 AND u.status = 1")
    int countAdministrator(Long userId);
}
