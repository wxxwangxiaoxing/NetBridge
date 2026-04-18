package com.netbridge.framework.web.socket;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@AutoConfiguration
@EnableWebSocketMessageBroker
public class WebSocketAutoConfiguration {

    @Bean
    public WebSocketMessageBrokerConfigurer webSocketMessageBrokerConfigurer(
            ObjectProvider<List<ChannelInterceptor>> channelInterceptorsProvider
    ) {
        List<ChannelInterceptor> channelInterceptors = channelInterceptorsProvider.getIfAvailable(List::of);
        return new WebSocketMessageBrokerConfigurer() {
            @Override
            public void registerStompEndpoints(StompEndpointRegistry registry) {
                registry.addEndpoint("/ws")
                        .setAllowedOriginPatterns("*");
            }

            @Override
            public void configureMessageBroker(MessageBrokerRegistry registry) {
                registry.enableSimpleBroker("/topic", "/queue");
                registry.setApplicationDestinationPrefixes("/app");
                registry.setUserDestinationPrefix("/user");
            }

            @Override
            public void configureClientInboundChannel(ChannelRegistration registration) {
                channelInterceptors.forEach(registration::interceptors);
            }
        };
    }
}
