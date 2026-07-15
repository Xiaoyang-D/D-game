package com.xiaoyang.d_game.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 公开用户主页响应。
 *
 * <p>这个 DTO 只暴露普通访客可以安全看到的信息，不返回邮箱、手机号、密码哈希等敏感字段。
 * 前端可直接拿它渲染“公开主页”的头图、资料区和统计区。</p>
 */
public class UserProfileResp {

    /**
     * 用户 ID。
     *
     * <p>统一按字符串序列化，避免前端在 JavaScript 里因为雪花 ID 过大而丢精度。</p>
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 昵称。 */
    private String nickname;

    /** 头像地址。 */
    private String avatarUrl;

    /** 个人简介。 */
    private String bio;

    /** 注册时间。 */
    private LocalDateTime gmtCreate;

    /** 已发布的公开帖子数量。 */
    private Long postCount;

    /** 获得的点赞总数。 */
    private Long likeCount;

    /** 关注数。 */
    private Long followingCount;

    /** 粉丝数。 */
    private Long followerCount;

    /** 收藏数。 */
    private Long favoriteCount;

    /** 当前访问者是否就是这个用户自己。 */
    @JsonProperty("isSelf")
    private boolean self;

    /** 当前访问者是否已经关注了这个用户。 */
    @JsonProperty("isFollowing")
    private boolean following;
}
