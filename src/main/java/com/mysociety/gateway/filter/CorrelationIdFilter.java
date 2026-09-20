package com.mysociety.gateway.filter;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements WebFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String resolvedId = correlationId;
        exchange.getResponse().getHeaders().set(HEADER, resolvedId);
        ServerWebExchange updatedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> headers.set(HEADER, resolvedId)))
                .build();
        long started = System.nanoTime();
        return chain.filter(updatedExchange)
                .doFinally(signal -> log.info("gateway_request correlationId={} method={} path={} status={} durationMs={}",
                        resolvedId, updatedExchange.getRequest().getMethod(), updatedExchange.getRequest().getPath().value(),
                        responseStatus(updatedExchange), (System.nanoTime() - started) / 1_000_000));
    }

    private int responseStatus(ServerWebExchange exchange) {
        return exchange.getResponse().getStatusCode() == null ? 0 : exchange.getResponse().getStatusCode().value();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
