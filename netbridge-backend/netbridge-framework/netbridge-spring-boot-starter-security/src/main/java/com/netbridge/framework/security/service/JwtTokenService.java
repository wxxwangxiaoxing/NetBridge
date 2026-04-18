package com.netbridge.framework.security.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.netbridge.framework.security.model.LoginUser;
import com.netbridge.framework.web.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtTokenService {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expireHours;

    public JwtTokenService(
            @Value("${netbridge.security.jwt-secret:netbridge-dev-secret}") String secret,
            @Value("${netbridge.security.jwt-expire-hours:24}") long expireHours
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
        this.expireHours = expireHours;
    }

    public String generateToken(Long userId, String username) {
        Instant now = Instant.now();
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(expireHours, ChronoUnit.HOURS)))
                .sign(algorithm);
    }

    public LoginUser parseToken(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            Long userId = jwt.getClaim("userId").asLong();
            String username = jwt.getClaim("username").asString();
            return new LoginUser(userId, username);
        } catch (Exception ex) {
            throw BusinessException.unauthorized("invalid or expired token");
        }
    }
}
