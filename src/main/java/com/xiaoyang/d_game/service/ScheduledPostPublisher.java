package com.xiaoyang.d_game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledPostPublisher {

    private final PostService postService;

    @Scheduled(fixedDelayString = "${app.post.schedule.poll-interval-ms:60000}")
    public void publishScheduledPosts() {
        try {
            postService.publishDuePosts();
        } catch (RuntimeException exception) {
            log.error("Failed to publish scheduled posts", exception);
        }
    }
}
