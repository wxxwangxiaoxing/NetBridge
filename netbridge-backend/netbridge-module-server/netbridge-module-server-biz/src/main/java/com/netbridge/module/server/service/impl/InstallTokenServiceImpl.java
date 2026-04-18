package com.netbridge.module.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.netbridge.framework.web.exception.BusinessException;
import com.netbridge.module.server.entity.InstallTokenEntity;
import com.netbridge.module.server.mapper.InstallTokenMapper;
import com.netbridge.module.server.service.InstallTokenService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InstallTokenServiceImpl extends ServiceImpl<InstallTokenMapper, InstallTokenEntity> implements InstallTokenService {

    @Override
    public long expireUnusedTokens() {
        List<InstallTokenEntity> expiredTokens = lambdaQuery()
                .eq(InstallTokenEntity::getStatus, "unused")
                .lt(InstallTokenEntity::getExpiresAt, LocalDateTime.now())
                .list();
        if (expiredTokens.isEmpty()) {
            return 0L;
        }
        expiredTokens.forEach(token -> token.setStatus("expired"));
        updateBatchById(expiredTokens);
        return expiredTokens.size();
    }

    @Override
    public InstallTokenEntity revokeToken(Long userId, Long tokenId) {
        InstallTokenEntity entity = lambdaQuery()
                .eq(InstallTokenEntity::getId, tokenId)
                .eq(InstallTokenEntity::getUserId, userId)
                .one();
        if (entity == null) {
            throw BusinessException.notFound("install token not found");
        }
        if ("used".equalsIgnoreCase(entity.getStatus())) {
            throw BusinessException.conflict("used install token cannot be revoked");
        }
        if ("expired".equalsIgnoreCase(entity.getStatus())) {
            throw BusinessException.conflict("install token already expired");
        }
        if ("revoked".equalsIgnoreCase(entity.getStatus())) {
            return entity;
        }
        entity.setStatus("revoked");
        updateById(entity);
        return entity;
    }
}
