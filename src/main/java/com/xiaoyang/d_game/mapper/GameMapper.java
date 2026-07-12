package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.Game;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GameMapper extends BaseMapper<Game> {

    @Select("SELECT * FROM game WHERE id = #{gameId} AND is_deleted = 0 FOR UPDATE")
    Game selectByIdForUpdate(Long gameId);
}
