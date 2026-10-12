package com.xiaoyang.d_game.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoyang.d_game.entity.ChatConversation;
import com.xiaoyang.d_game.dto.MessageDtos.UserBrief;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {
    IPage<UserBrief> friends(Page<UserBrief> page, @Param("userId") Long userId, @Param("q") String q);
    long unread(@Param("userId") Long userId);
}
