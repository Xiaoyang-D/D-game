package com.xiaoyang.d_game.service;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.dto.MessageDtos;
/** 本人好友与私信服务，所有会话入口均校验参与者身份。 */
public interface ChatService {
    /** 从两条有效关注记录分页查询好友，支持昵称筛选。 */
    PageResult<MessageDtos.UserBrief> friends(long page, long size, String q);
    /** 打开双方唯一会话，不恢复本人已清除的历史。 */
    MessageDtos.Conversation open(Long friendId);
    MessageDtos.Conversation detail(Long id);
    PageResult<MessageDtos.Conversation> conversations(long page, long size);
    /** before 查询历史，after 补取新消息，二者互斥，只返回清除位置之后的数据。 */
    MessageDtos.History history(Long id, Long before, Long after, int size);
    /** 同一客户端 UUID 的失败重试只保存一次消息。 */
    MessageDtos.Message send(Long id, MessageDtos.Send request);
    /** 两分钟内撤回本人消息，后续接口不再返回原文。 */
    void recall(Long id);
    /** 仅推进本人清除边界，不删除对方的共享消息。 */
    void clear(Long id);
    /** 推进本人实际展示位置，较旧位置不回退阅读进度。 */
    void read(Long id, Long seq);
    long unread();
}
