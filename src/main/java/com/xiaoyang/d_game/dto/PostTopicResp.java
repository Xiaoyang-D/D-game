package com.xiaoyang.d_game.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class PostTopicResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;
}
