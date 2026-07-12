package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class CheckInResultResp {

    private LocalDate checkInDate;

    private int streakDays;

    private int pointsEarned;

    private List<String> newBadges = new ArrayList<>();
}
