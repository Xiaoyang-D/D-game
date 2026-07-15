package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.UserLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * 用户点赞 Mapper。
 */
public interface UserLikeMapper extends BaseMapper<UserLike> {

    /**
     * 物理删除指定点赞关系。
     *
     * <p>点赞关系表存在 user_id + target_type + target_id 唯一键。如果使用逻辑删除，
     * 取消点赞后的旧记录仍会占用唯一键，导致用户再次点赞时报 DuplicateKeyException。
     * 因此该关系表取消点赞时直接物理删除。</p>
     */
    @Delete("DELETE FROM user_like WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    int deletePhysicallyByUserAndTarget(@Param("userId") Long userId,
                                        @Param("targetType") Integer targetType,
                                        @Param("targetId") Long targetId);
}
