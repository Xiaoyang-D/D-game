package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CheckInStatusResp {

    private boolean checkedInToday;

    private int streakDays;

    private LocalDate lastCheckInDate;
}
