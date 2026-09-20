package com.mysociety.gateway.web;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class GatewayFallbackController {

    @RequestMapping(path = "/__gateway/fallback", produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    Mono<ProblemDetail> upstreamUnavailable() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "The requested service is temporarily unavailable.");
        problem.setTitle("Upstream service unavailable");
        problem.setType(URI.create("https://mysociety.example/problems/upstream-unavailable"));
        return Mono.just(problem);
    }
}
