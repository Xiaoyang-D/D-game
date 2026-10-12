package com.xiaoyang.d_game.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
public record NotificationResp(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        @JsonSerialize(using = ToStringSerializer.class) Long receiverId,
        @JsonSerialize(using = ToStringSerializer.class) Long senderId,
        Integer type, String title, String content, Integer targetType,
        @JsonSerialize(using = ToStringSerializer.class) Long targetId,
        Integer isRead, Integer isSilent, LocalDateTime gmtCreate, String senderNickname, String senderAvatarUrl,
        String targetUrl, boolean targetAvailable) { }
