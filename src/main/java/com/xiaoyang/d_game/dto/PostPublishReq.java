package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostPublishReq {

    @NotNull(message = "版块不能为空")
    private Long boardId;

    private Long gameId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 40, message = "标题长度不能超过40")
    private String title;

    @NotBlank(message = "正文不能为空")
    @Size(max = 20000, message = "正文长度不能超过20000")
    private String content;

    @Size(max = 5, message = "话题最多选择5个")
    private List<String> topicNames;

    private Long collectionId;

    private Boolean isOriginal;

    private Boolean containsAiGenerated;

    private LocalDateTime scheduledPublishAt;
}
