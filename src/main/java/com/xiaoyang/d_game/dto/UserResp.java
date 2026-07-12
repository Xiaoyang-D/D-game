package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserResp {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String mobile;

    private String avatarUrl;

    private String bio;

    private Integer status;

    private List<String> roles;

    private LocalDateTime gmtCreate;
}
