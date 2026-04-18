package com.netbridge.framework.security.socket;

import com.netbridge.framework.security.model.LoginUser;
import com.netbridge.framework.security.service.JwtTokenService;
import com.netbridge.framework.web.exception.BusinessException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;
import java.util.Set;

@Component
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    private static final Set<String> ALLOWED_SUBSCRIPTIONS = Set.of(
            "/user/queue/server/status",
            "/user/queue/task/status",
            "/user/queue/service/status"
    );

    private final JwtTokenService jwtTokenService;

    public WebSocketJwtChannelInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);
            if (token == null || token.isBlank()) {
                throw new MessagingException("missing websocket token");
            }
            try {
                LoginUser loginUser = jwtTokenService.parseToken(token);
                accessor.setUser(new UsernamePasswordAuthenticationToken(loginUser, null, AuthorityUtils.NO_AUTHORITIES));
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                if (sessionAttributes != null) {
                    sessionAttributes.put("userId", loginUser.getUserId());
                    sessionAttributes.put("username", loginUser.getUsername());
                }
            } catch (BusinessException ex) {
                throw new MessagingException(ex.getMessage(), ex);
            }
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getUser() == null) {
                throw new MessagingException("unauthorized websocket subscription");
            }
            String destination = accessor.getDestination();
            if (destination == null || !ALLOWED_SUBSCRIPTIONS.contains(destination)) {
                throw new MessagingException("websocket subscription destination is not allowed");
            }
        }
        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String authorization = firstHeader(accessor, "Authorization");
        if (authorization == null) {
            authorization = firstHeader(accessor, "authorization");
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        String token = firstHeader(accessor, "token");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    private String firstHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }
}
