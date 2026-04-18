package com.netbridge.module.task.schedule;

import com.netbridge.module.task.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskLifecycleScheduler.class);

    private final TaskService taskService;

    @Value("${netbridge.task.lifecycle.timeout-cleanup-enabled:true}")
    private boolean timeoutCleanupEnabled;

    @Value("${netbridge.task.lifecycle.timeout-minutes:10}")
    private int timeoutMinutes;

    public TaskLifecycleScheduler(TaskService taskService) {
        this.taskService = taskService;
    }

    @Scheduled(fixedDelayString = "${netbridge.task.lifecycle.timeout-cleanup-fixed-delay-ms:60000}")
    public void cleanupTimedOutTasks() {
        if (!timeoutCleanupEnabled) {
            return;
        }
        long affected = taskService.cleanupTimedOutTasks(timeoutMinutes);
        if (affected > 0) {
            log.info("Timed out {} task(s) exceeding {} minute(s)", affected, timeoutMinutes);
        }
    }
}
