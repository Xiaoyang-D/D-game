package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 持久化签到意图，奖励完成之前不删除，支持跨天及重启补偿。 */
@Data
@TableName("check_in_reward_task")
public class CheckInRewardTask {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private LocalDate checkInDate;
    private LocalDateTime nextAttemptAt;
}
