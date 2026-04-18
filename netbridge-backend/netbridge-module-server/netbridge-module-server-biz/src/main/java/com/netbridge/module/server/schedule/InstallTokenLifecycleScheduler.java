package com.netbridge.module.server.schedule;

import com.netbridge.module.server.service.InstallTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InstallTokenLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(InstallTokenLifecycleScheduler.class);

    private final InstallTokenService installTokenService;

    @Value("${netbridge.install-token.cleanup-enabled:true}")
    private boolean cleanupEnabled;

    public InstallTokenLifecycleScheduler(InstallTokenService installTokenService) {
        this.installTokenService = installTokenService;
    }

    @Scheduled(fixedDelayString = "${netbridge.install-token.cleanup-fixed-delay-ms:60000}")
    public void cleanupExpiredTokens() {
        if (!cleanupEnabled) {
            return;
        }
        long affected = installTokenService.expireUnusedTokens();
        if (affected > 0) {
            log.info("Expired {} install token(s)", affected);
        }
    }
}
