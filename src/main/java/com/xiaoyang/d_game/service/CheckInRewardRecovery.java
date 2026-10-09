package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xiaoyang.d_game.dto.CheckInRewardResp;
import com.xiaoyang.d_game.entity.CheckInRewardTask;
import com.xiaoyang.d_game.mapper.CheckInRewardTaskMapper;
import com.xiaoyang.d_game.repository.CheckInBitmapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 任务入库先于 Redis 写入；奖励事务提交后才删除任务。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInRewardRecovery {
    private static final int BATCH_SIZE = 50;
    private static final int RETRY_DELAY_SECONDS = 60;
    private final CheckInRewardTaskMapper taskMapper;
    private final CheckInBitmapRepository bitmapRepository;
    private final GrowthService growthService;

    public void enqueue(Long userId, LocalDate date) {
        if (isPending(userId, date)) {
            return;
        }
        CheckInRewardTask task = new CheckInRewardTask();
        task.setUserId(userId);
        task.setCheckInDate(date);
        task.setNextAttemptAt(LocalDateTime.now());
        try {
            taskMapper.insert(task);
        } catch (DuplicateKeyException exception) {
            // 并发请求已保存同一用户、同一天的任务。
            log.debug("签到补偿任务已存在, userId={}, date={}", userId, date);
        }
    }

    public boolean isPending(Long userId, LocalDate date) {
        return taskMapper.selectCount(taskQuery(userId, date)) > 0;
    }

    public CheckInRewardResp complete(Long userId, LocalDate date) {
        bitmapRepository.markCheckedIn(userId, date);
        int streak = bitmapRepository.calcStreak(userId, date);
        // 外部 GrowthService 代理返回时，积分和徽章事务已经提交。
        CheckInRewardResp reward = growthService.onCheckInSuccess(userId, date, streak);
        taskMapper.delete(taskQuery(userId, date));
        return reward;
    }

    @Scheduled(fixedDelayString = "${app.check-in.reward-retry-interval-ms:60000}")
    public void retryPendingRewards() {
        try {
            var tasks = taskMapper.selectList(new LambdaQueryWrapper<CheckInRewardTask>()
                    .select(CheckInRewardTask::getId, CheckInRewardTask::getUserId,
                            CheckInRewardTask::getCheckInDate, CheckInRewardTask::getNextAttemptAt)
                    .le(CheckInRewardTask::getNextAttemptAt, LocalDateTime.now())
                    .orderByAsc(CheckInRewardTask::getNextAttemptAt)
                    .orderByAsc(CheckInRewardTask::getId)
                    .last("LIMIT " + BATCH_SIZE));
            for (CheckInRewardTask task : tasks) {
                // 延后本任务，避免持续失败的任务占满下一批。
                taskMapper.update(null, new LambdaUpdateWrapper<CheckInRewardTask>()
                        .eq(CheckInRewardTask::getId, task.getId())
                        .set(CheckInRewardTask::getNextAttemptAt,
                                LocalDateTime.now().plusSeconds(RETRY_DELAY_SECONDS)));
                try {
                    complete(task.getUserId(), task.getCheckInDate());
                } catch (RuntimeException exception) {
                    log.warn("签到奖励补偿失败，将继续重试, userId={}, date={}",
                            task.getUserId(), task.getCheckInDate(), exception);
                }
            }
        } catch (RuntimeException exception) {
            log.error("读取或更新签到补偿任务失败", exception);
        }
    }

    private LambdaQueryWrapper<CheckInRewardTask> taskQuery(Long userId, LocalDate date) {
        return new LambdaQueryWrapper<CheckInRewardTask>()
                .eq(CheckInRewardTask::getUserId, userId)
                .eq(CheckInRewardTask::getCheckInDate, date);
    }
}
