package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.PostTopicRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostTopicRelMapper extends BaseMapper<PostTopicRel> {

    @Delete("DELETE FROM post_topic_rel WHERE post_id = #{postId}")
    int deletePhysicallyByPostId(@Param("postId") Long postId);
}
