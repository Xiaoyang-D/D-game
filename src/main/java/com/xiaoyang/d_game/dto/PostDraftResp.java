package com.xiaoyang.d_game.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDraftResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long boardId;

    private String boardName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long gameId;

    private String gameName;

    private String title;

    private String content;

    private Integer status;

    @JsonProperty("isOriginal")
    private Boolean isOriginal;

    private Boolean containsAiGenerated;

    private LocalDateTime scheduledPublishAt;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long collectionId;

    private String collectionName;

    private List<PostTopicResp> topics;

    private LocalDateTime gmtModified;
}
