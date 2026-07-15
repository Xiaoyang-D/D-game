package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDraftSaveReq {

    private Long boardId;

    private Long gameId;

    @Size(max = 40, message = "标题长度不能超过40")
    private String title;

    @Size(max = 20000, message = "正文长度不能超过20000")
    private String content;

    @Size(max = 5, message = "话题最多选择5个")
    private List<String> topicNames;

    private Long collectionId;

    private Boolean isOriginal;

    private Boolean containsAiGenerated;

    private LocalDateTime scheduledPublishAt;
}
