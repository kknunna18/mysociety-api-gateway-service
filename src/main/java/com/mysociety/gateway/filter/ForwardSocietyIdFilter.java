package com.mysociety.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;

@Component
public class ForwardSocietyIdFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(ForwardSocietyIdFilter.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String societyId = exchange.getRequest().getHeaders().getFirst("x-society-id");
        log.debug("ForwardSocietyIdFilter - incoming x-society-id: {}", societyId);
        if (societyId != null && !societyId.isBlank()) {
            ServerHttpRequest mutated = exchange.getRequest().mutate().header("x-society-id", societyId).build();
            log.debug("ForwardSocietyIdFilter - forwarding header to downstream: {}", societyId);
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        // Dev convenience: if header missing, try to parse society_id directly from Bearer token payload (base64url decode)
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length()).trim();
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String payload = parts[1];
                    byte[] decoded = Base64.getUrlDecoder().decode(payload);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> claims = mapper.readValue(decoded, Map.class);
                    Object sidObj = claims.get("society_id");
                    if (sidObj != null) {
                        String sid = sidObj.toString();
                        if (!sid.isBlank()) {
                            ServerHttpRequest mutated = exchange.getRequest().mutate().header("x-society-id", sid).build();
                            log.debug("ForwardSocietyIdFilter - extracted society_id from Bearer token payload and forwarding: {}", sid);
                            return chain.filter(exchange.mutate().request(mutated).build());
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("ForwardSocietyIdFilter - failed to extract society_id from token payload: {}", e.toString());
            }
        }

        // Dev env fallback: if an explicit dev default is set, forward it
                String devSid = System.getenv("GATEWAY_DEV_SOCIETY_ID");
                if (devSid != null && !devSid.isBlank()) {
                    ServerHttpRequest mutated = exchange.getRequest().mutate().header("x-society-id", devSid).build();
                    log.warn("ForwardSocietyIdFilter - no society_id found; forwarding GATEWAY_DEV_SOCIETY_ID={}", devSid);
                    return chain.filter(exchange.mutate().request(mutated).build());
                }

                // Fallback: try reactive SecurityContext (if already populated)
                return ReactiveSecurityContextHolder.getContext()
                        .flatMap(ctx -> {
                            if (ctx == null) return Mono.empty();
                            var auth = ctx.getAuthentication();
                            if (!(auth instanceof JwtAuthenticationToken) || !auth.isAuthenticated()) return Mono.empty();
                            var jwt = ((JwtAuthenticationToken) auth).getToken();
                            if (jwt == null) return Mono.empty();
                            String sid = jwt.getClaimAsString("society_id");
                            if (sid == null || sid.isBlank()) return Mono.empty();
                            return Mono.just(sid);
                        })
                        .flatMap(sid -> {
                            ServerHttpRequest mutated = exchange.getRequest().mutate().header("x-society-id", sid).build();
                            log.debug("ForwardSocietyIdFilter - extracted society_id from JWT and forwarding: {}", sid);
                            return chain.filter(exchange.mutate().request(mutated).build());
                        })
                        .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        // run early
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
