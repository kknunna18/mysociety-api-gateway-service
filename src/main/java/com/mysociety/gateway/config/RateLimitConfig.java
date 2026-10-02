package com.mysociety.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimitConfig {

    @Bean
    KeyResolver principalOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> "principal:" + principal.getName())
                .switchIfEmpty(Mono.fromSupplier(() -> "ip:" + clientIp(exchange)));
    }

    private String clientIp(ServerWebExchange exchange) {
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        return address == null || address.getAddress() == null ? "unknown" : address.getAddress().getHostAddress();
    }
}
