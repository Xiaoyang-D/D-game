package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.GameRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
/**
 * 游戏评分 Mapper。
 */
public interface GameRatingMapper extends BaseMapper<GameRating> {

    /**
     * 汇总某个游戏的平均分和评分人数。
     *
     * <p>服务层在用户新增或更新评分后调用该方法，并把结果回写到 game 表的冗余汇总字段。</p>
     */
    @Select("SELECT COALESCE(AVG(score), 0) AS avgRating, COUNT(*) AS ratingCount "
            + "FROM game_rating WHERE game_id = #{gameId} AND is_deleted = 0")
    Map<String, Object> selectRatingSummary(Long gameId);
}
