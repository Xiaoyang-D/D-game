package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.GameRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface GameRatingMapper extends BaseMapper<GameRating> {

    @Select("SELECT COALESCE(AVG(score), 0) AS avgRating, COUNT(*) AS ratingCount "
            + "FROM game_rating WHERE game_id = #{gameId} AND is_deleted = 0")
    Map<String, Object> selectRatingSummary(Long gameId);
}
