# MySociety API Gateway

Reactive Spring Boot 3 / Java 21 API gateway for MySociety services. It is infrastructure only: it authenticates
requests, applies rate limits and resilience policies, and proxies traffic without making business or membership
decisions.

## Run

1. Copy `.env.example` to `.env`, set a random `JWT_HMAC_SECRET` of at least 32 characters, and export it to the shell.
2. Start Redis: `docker compose up -d redis`.
3. Run locally: `gradlew.bat bootRun --args="--spring.profiles.active=local"` (Windows) or
   `./gradlew bootRun --args='--spring.profiles.active=local'`.

Build and test with `gradlew.bat clean build` (Windows) or `./gradlew clean build`. Docker is optional; test execution
never requires Docker.

## Routes

| Public path                                                                                | Upstream                                                 |
|--------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `/api/v1/auth/**`                                                                          | `IDENTITY_SERVICE_URL` (default `http://localhost:8081`) |
| `/api/v1/users/**`                                                                         | `IDENTITY_SERVICE_URL`                                   |
| `/api/v1/societies/**`, `/api/v1/buildings/**`, `/api/v1/units/**`, `/api/v1/residents/**` | `SOCIETY_SERVICE_URL` (default `http://localhost:8082`)  |

The gateway preserves the API path. `JWT_HMAC_SECRET`, `JWT_ISSUER` (default `mysociety-identity`), service URLs, Redis
location, CORS origins, and tracing sampling are environment configurable.

## Security and observability

Protected API routes require a valid HS256 JWT issued by `mysociety-identity` for audience `mysociety-api`; login and
refresh paths are public. The gateway never logs request bodies or security credentials, propagates an
`X-Correlation-Id`, uses Redis-backed per-principal/IP rate limits, and converts circuit-breaker failures to a generic
RFC 9457 response. It only propagates authentication context; downstream services must validate society membership.

Health endpoints are `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness`; Prometheus
metrics are at `/actuator/prometheus`; OpenAPI is at `/v3/api-docs` and `/swagger-ui.html`. Micrometer's OpenTelemetry
bridge is included; configure an OTLP exporter through standard OpenTelemetry environment variables when deploying.
