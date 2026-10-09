package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

@Mapper
/**
 * 用户关注 Mapper。
 */
public interface UserFollowMapper extends BaseMapper<UserFollow> {
    /** 恢复取消过的关注，保留关系主键；只由一个并发请求完成状态切换。 */
    @Update("UPDATE user_follow SET is_deleted = 0, gmt_modified = CURRENT_TIMESTAMP "
            + "WHERE follower_id = #{followerId} AND followee_id = #{followeeId} AND is_deleted = 1")
    int restoreFollow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    /** 当前读用于确认并发插入后的有效关注，避免事务快照遗漏已提交关系。 */
    @Select("SELECT id FROM user_follow WHERE follower_id = #{followerId} "
            + "AND followee_id = #{followeeId} AND is_deleted = 0 FOR UPDATE")
    Long findActiveFollowForUpdate(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);
}
