package com.netbridge.module.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.netbridge.module.server.entity.InstallTokenEntity;

public interface InstallTokenService extends IService<InstallTokenEntity> {

    long expireUnusedTokens();

    InstallTokenEntity revokeToken(Long userId, Long tokenId);
}
