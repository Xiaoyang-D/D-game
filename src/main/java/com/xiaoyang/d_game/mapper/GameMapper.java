package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.Game;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
/**
 * 游戏 Mapper。
 */
public interface GameMapper extends BaseMapper<Game> {

    /**
     * 按 ID 查询游戏并加行锁。
     *
     * <p>用于需要在事务内读取并更新同一游戏记录的场景，避免并发更新评分汇总时出现覆盖。</p>
     */
    @Select("SELECT * FROM game WHERE id = #{gameId} AND is_deleted = 0 FOR UPDATE")
    Game selectByIdForUpdate(Long gameId);
}
