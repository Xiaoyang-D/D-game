package com.xiaoyang.d_game.service;
import com.xiaoyang.d_game.dto.MessageDtos;
import com.xiaoyang.d_game.entity.Post;
import java.util.List;
/** 提及校验、关系保存、首次通知以及评论跳转定位。 */
public interface MentionService {
    List<MessageDtos.UserBrief> candidates(String q);
    /** 按真实用户数据重建安全标签，不信任客户端昵称。 */
    String normalizePost(String html);
    /** 只有实际公开的帖子才产生首次提及通知。 */
    void syncPost(Post post);
    /** 校验用户与 UTF-16 范围，再保存关系及通知。 */
    void syncComment(Long commentId, Long authorId, String content, List<MessageDtos.Mention> mentions);
    List<MessageDtos.Mention> commentMentions(Long commentId);
    /** 按评论列表相同的排序和分页定位，不可见目标拒绝跳转。 */
    MessageDtos.Location location(Long commentId, String sort, int size);
}
