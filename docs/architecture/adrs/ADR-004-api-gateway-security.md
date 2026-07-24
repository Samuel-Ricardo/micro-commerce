# ADR-004: API Gateway com OAuth2 & Spring Security

**Status:** ✅ Implementado  
**Data:** 2026-07-23  
**Decisores:** Security & Architecture Team  
**Tech Stack:** Spring Cloud Gateway MVC, Keycloak, OAuth2, JWT

---

## 📋 Contexto

Sistema com múltiplos serviços necessita:

- 🔐 **Autenticação centralizada** - SSO
- 🛡️ **Autorização por roles** - RBAC
- 🚪 **Entrypoint único** - API Gateway
- 🔑 **Tokens seguros** - JWT com refresh tokens
- 📱 **Suporte multi-cliente** - Web, Mobile, External APIs

## 🎯 Decisão

**Implementar OAuth2 + OpenID Connect com Keycloak, mediado por API Gateway:**

```
┌─────────────────────────────────────────────────┐
│                   Clients                        │
│     (Web, Mobile, Desktop, External APIs)       │
└────────────┬────────────────────────────────────┘
             │
             ▼
    ┌────────────────────┐
    │   API Gateway      │
    │ (Port 9000:8080)   │
    │                    │
    │ ┌────────────────┐ │
    │ │ Security Filter│ │
    │ │ Route Handlers │ │
    │ │ Rate Limiting  │ │
    │ └────────────────┘ │
    └────────────────────┘
             │
             ├─→ Auth redirect to Keycloak
             │
             └─→ Routed to Services
                 (validated, enriched)

    ┌─────────────────┐
    │ Keycloak        │
    │ OAuth2/OIDC     │
    │ (Port 8181:8080)│
    └─────────────────┘
```

## 🔧 Implementação Técnica

### 1️⃣ **API Gateway Configuration**

**Dependências:**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-mvc</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

**Configuração:**

```yaml
# application.yml
server:
  port: 9000

spring:
  application:
    name: api-gateway

  cloud:
    gateway:
      routes:
        - id: product-service
          uri: http://product-service:8080
          predicates:
            - Path=/api/products/**
          filters:
            - StripPrefix=2
            - AuthorizeRequest
            - RateLimiter=productRateLimit

        - id: order-service
          uri: http://order-service:8081
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=2
            - AuthorizeRequest
            - JwtValidation

        - id: inventory-service
          uri: http://inventory-service:8082
          predicates:
            - Path=/api/inventory/**
          filters:
            - StripPrefix=2
            - AuthorizeRequest

      default-filters:
        - AuthenticationFilter

  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: api-gateway
            client-secret: ${KEYCLOAK_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/keycloak"
            scope: openid,profile,email,roles

        provider:
          keycloak:
            issuer-uri: http://keycloak:8080/realms/master
            authorization-uri: http://keycloak:8080/realms/master/protocol/openid-connect/auth
            token-uri: http://keycloak:8080/realms/master/protocol/openid-connect/token
            user-info-uri: http://keycloak:8080/realms/master/protocol/openid-connect/userinfo
            jwk-set-uri: http://keycloak:8080/realms/master/protocol/openid-connect/certs

      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8080/realms/master
          jws-algorithms: RS256
```

### 2️⃣ **Custom Security Filters**

**Authentication Filter:**

```java
@Component
public class AuthenticationGatewayFilter implements GlobalFilter {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private JwtValidator jwtValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                            GatewayFilterChain chain) {

        return Mono.defer(() -> {
            // Extrai JWT do header
            String token = extractToken(exchange.getRequest());

            if (token == null) {
                return unauthorized(exchange, "Missing authentication token");
            }

            // Valida JWT
            try {
                JwtPayload payload = jwtValidator.validate(token);

                // Enriquece request com claims
                ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-ID", payload.getSub())
                    .header("X-User-Roles", String.join(",", payload.getRoles()))
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .build();

                return chain.filter(exchange.mutate()
                    .request(mutatedRequest)
                    .build());

            } catch (ExpiredJwtException e) {
                return unauthorized(exchange, "Token expired");
            } catch (JwtException e) {
                return unauthorized(exchange, "Invalid token");
            }
        });
    }

    private String extractToken(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange,
                                    String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse()
            .writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(("{\"error\": \"" + message + "\"}").getBytes())));
    }
}
```

**JWT Validation Service:**

```java
@Component
public class JwtValidator {

    @Autowired
    private JwtDecoder jwtDecoder;

    public JwtPayload validate(String token) throws JwtException {
        Jwt jwt = jwtDecoder.decode(token);

        Map<String, Object> claims = jwt.getClaims();
        List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());

        return JwtPayload.builder()
            .sub((String) claims.get("sub"))
            .email((String) claims.get("email"))
            .roles(roles)
            .issuedAt(jwt.getIssuedAt())
            .expiresAt(jwt.getExpiresAt())
            .build();
    }
}

@Data
@Builder
class JwtPayload {
    private String sub;           // User ID
    private String email;
    private List<String> roles;   // ["admin", "user", "premium"]
    private Instant issuedAt;
    private Instant expiresAt;
}
```

**Authorization Filter (Role-Based):**

```java
@Component
public class AuthorizeRequestGatewayFilter
    implements GatewayFilter, Ordered {

    private final Map<String, List<String>> pathRoles = Map.ofEntries(
        Map.entry("/api/products/**", List.of("USER", "ADMIN")),
        Map.entry("/api/orders/**", List.of("USER", "ADMIN")),
        Map.entry("/api/admin/**", List.of("ADMIN")),
        Map.entry("/api/inventory/**", List.of("ADMIN", "INVENTORY_MANAGER"))
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                            GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        String rolesHeader = exchange.getRequest()
            .getHeaders().getFirst("X-User-Roles");

        // Verifica se path requer autorização
        List<String> requiredRoles = pathRoles.entrySet().stream()
            .filter(e -> path.matches(e.getKey().replace("**", ".*")))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);

        if (requiredRoles == null) {
            return chain.filter(exchange);
        }

        // Verifica se user tem um dos required roles
        if (rolesHeader == null ||
            Arrays.stream(rolesHeader.split(","))
                  .noneMatch(requiredRoles::contains)) {

            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;  // Executar antes de outros filters
    }
}
```

### 3️⃣ **Rate Limiting**

```java
@Component
public class RateLimitGatewayFilter implements GatewayFilter, Ordered {

    private final RateLimiter rateLimiter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                            GatewayFilterChain chain) {

        String userId = exchange.getRequest()
            .getHeaders().getFirst("X-User-ID");

        if (rateLimiter.allowRequest(userId)) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders()
            .add("X-RateLimit-Retry-After", "60");

        return exchange.getResponse().setComplete();
    }
}

@Component
public class RateLimiter {
    private final Map<String, RateLimitWindow> windows = new ConcurrentHashMap<>();
    private final long windowSizeMillis = 60 * 1000; // 1 min
    private final long maxRequests = 1000;           // 1000 req/min

    public boolean allowRequest(String userId) {
        RateLimitWindow window = windows.compute(userId,
            (k, v) -> v == null ? new RateLimitWindow() : v);

        return window.recordRequest(windowSizeMillis, maxRequests);
    }
}
```

### 4️⃣ **Keycloak Configuration**

**Realm Setup:**

```yaml
# main-realm-export.json
{
  "realm": "master",
  "enabled": true,
  "clients":
    [
      {
        "clientId": "api-gateway",
        "secret": "your-client-secret",
        "redirectUris": ["http://localhost:9000/*"],
        "webOrigins": ["*"],
        "publicClient": false,
        "standardFlowEnabled": true,
        "implicitFlowEnabled": false,
        "directAccessGrantsEnabled": true,
      },
      {
        "clientId": "product-service",
        "secret": "product-secret",
        "serviceAccountsEnabled": true,
      },
    ],
  "roles":
    {
      "realm":
        [
          { "name": "USER" },
          { "name": "ADMIN" },
          { "name": "INVENTORY_MANAGER" },
        ],
    },
  "users":
    [
      {
        "username": "user@example.com",
        "email": "user@example.com",
        "realmRoles": ["USER"],
        "credentials": [{ "type": "password", "value": "password123" }],
      },
      {
        "username": "admin@example.com",
        "email": "admin@example.com",
        "realmRoles": ["ADMIN"],
        "credentials": [{ "type": "password", "value": "admin123" }],
      },
    ],
}
```

**Docker Compose:**

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:24.0.1
  command: ["start-dev", "--import-realm"]
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin123
    DB_VENDOR: mysql
    DB_ADDR: keycloak-mysql
    DB_PORT: 3306
    DB_USER: keycloak
    DB_PASSWORD: keycloak123
  ports:
    - "8181:8080"
  volumes:
    - ./docker/keycloak/realms:/opt/keycloak/data/import/
  depends_on:
    - keycloak-mysql
```

## 🔐 OAuth2 Authorization Code Flow

```
1. User acessa http://localhost:9000
   │
   └─→ API Gateway detecta falta de token
       Redireciona para:
       http://keycloak:8080/realms/master/protocol/openid-connect/auth
       ?client_id=api-gateway
       &response_type=code
       &scope=openid profile email
       &redirect_uri=http://localhost:9000/login/oauth2/code/keycloak

2. User autentica em Keycloak
   │
   └─→ Keycloak retorna authorization code
       Redireciona para:
       http://localhost:9000/login/oauth2/code/keycloak?code=ABC123

3. API Gateway troca code por tokens
   │
   └─→ POST http://keycloak:8080/realms/master/protocol/openid-connect/token
       {
         "client_id": "api-gateway",
         "client_secret": "***",
         "code": "ABC123",
         "grant_type": "authorization_code"
       }

       Resposta:
       {
         "access_token": "eyJhbGc...",
         "token_type": "Bearer",
         "expires_in": 3600,
         "refresh_token": "eyJhbGc..."
       }

4. User recebe token (no header ou cookie)
   │
   └─→ Requisições subsequentes:
       GET /api/products
       Authorization: Bearer eyJhbGc...

       Gateway valida JWT e roteia para serviço
```

## 📊 Security Posture

| Aspecto             | Implementação          | Score      |
| ------------------- | ---------------------- | ---------- |
| **Autenticação**    | OAuth2 + JWT           | ⭐⭐⭐⭐⭐ |
| **Autorização**     | RBAC via roles         | ⭐⭐⭐⭐   |
| **Encryption**      | TLS + RS256 signing    | ⭐⭐⭐⭐⭐ |
| **Token Refresh**   | Refresh token rotation | ⭐⭐⭐⭐   |
| **Rate Limiting**   | Por user               | ⭐⭐⭐⭐   |
| **API Key Support** | ❌ Não implementado    | ⭐⭐       |
| **Audit Logging**   | ⚠️ Básico              | ⭐⭐⭐     |

## ⚠️ Melhorias Recomendadas

1. **Implementar CORS mais restritivo:**

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:4200",  // Angular frontend
            "https://app.example.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

2. **Implementar Token Introspection:**

```java
@Component
public class TokenIntrospectionFilter {

    @Autowired
    private RestTemplate restTemplate;

    public boolean isTokenValid(String token) {
        HttpEntity<String> request = new HttpEntity<>(
            "token=" + token + "&client_id=api-gateway&client_secret=***",
            createHeaders()
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "http://keycloak:8080/realms/master/protocol/openid-connect/token/introspect",
            request,
            Map.class
        );

        return (Boolean) response.getBody().getOrDefault("active", false);
    }
}
```

3. **Audit Logging:**

```java
@Aspect
@Component
public class SecurityAuditAspect {

    @Around("@annotation(SecurityAudit)")
    public Object auditSecurityEvent(ProceedingJoinPoint pjp) throws Throwable {
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        log.info("AUDIT - User: {} - Action: {} - Time: {}",
            userId,
            pjp.getSignature().getName(),
            LocalDateTime.now()
        );

        return pjp.proceed();
    }
}
```

---

**Relacionado:** ADR-001, ADR-009  
**Referências:**

- [OAuth2 in Spring Cloud Gateway](https://spring.io/guides/tutorials/spring-security-and-oauth2/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
