package com.mysociety.gateway;

import com.mysociety.gateway.filter.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"gateway.security.jwt-hmac-secret=01234567890123456789012345678901",
                "spring.data.redis.host=localhost", "spring.data.redis.port=1"})
@ActiveProfiles("test")
class GatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void configuresAllGatewayRoutes() {
        assertThat(routeLocator.getRoutes().collectList().block())
                .extracting(route -> route.getId())
                .contains("identity-auth", "identity-users", "society-domain");
    }

    @Test
    void deniesUnauthenticatedProtectedRoutesAndCreatesCorrelationId() {
        webTestClient.get().uri("/api/v1/users/1")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().exists(CorrelationIdFilter.HEADER)
                .expectHeader().contentType("application/problem+json")
                .expectBody()
                .jsonPath("$.title").isEqualTo("Unauthorized");
    }

    @Test
    void propagatesCallerCorrelationId() {
        webTestClient.get().uri("/api/v1/users/1")
                .header(CorrelationIdFilter.HEADER, "test-correlation-id")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(CorrelationIdFilter.HEADER, "test-correlation-id");
    }

    @Test
    void permitsAuthRoutesAndAppliesCors() {
        webTestClient.options().uri("/api/v1/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000");
    }

    @Test
    void permitsAuthRoutesWithoutAuthentication() {
        webTestClient.get().uri("/api/v1/auth/refresh")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Upstream service unavailable");
    }

    @Test
    void returnsStandardProblemDetailForFallback() {
        webTestClient.get().uri("/__gateway/fallback")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType("application/problem+json")
                .expectBody()
                .jsonPath("$.title").isEqualTo("Upstream service unavailable");
    }
}
