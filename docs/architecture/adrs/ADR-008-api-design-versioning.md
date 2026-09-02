# ADR-008: REST API Design & Versioning Strategy

**Status:** ✅ Implementado  
**Data:** 2026-07-23  
**Decisores:** Architecture Team  
**Standards:** OpenAPI 3.0, REST Level 3 (Richardson Maturity Model)

---

## 📋 Contexto

APIs evoluem. Versões antigas de clientes precisam funcionar enquanto novas features são adicionadas.

**Problema:**
```
v1: GET /products → {id, name, price}
v2: Novo cliente precisa de {id, name, price, stock, discount}

Opções:
❌ Quebrar v1 (downtime para clientes antigos)
❌ Manter ambas em memória (complexidade)
✅ API Versioning + backward compatibility
```

## 🎯 Decisão

**Implementar REST API com:**
- 📝 OpenAPI 3.0 documentation
- 🔗 HATEOAS (Hypermedia) Level 3
- 📦 Content negotiation versioning
- ✅ Backward compatibility

```
┌────────────────────────────────────────┐
│        API Gateway (Port 9000)         │
│                                        │
│  GET /api/v1/products                 │
│  GET /api/v2/products                 │
│  GET /api/v3/products                 │
│                                        │
│  Content negotiation:                 │
│  Accept: application/vnd.myapp.v2+json│
└────────────────────────────────────────┘
         │
         ├─→ Product Service (port 8080)
         │   ├─ /products (v1)
         │   ├─ /products/v2 (v2)
         │   └─ /products/v3 (v3)
```

## 🔧 Implementação Técnica

### 1️⃣ **API Versioning com URL Path**

```java
@RestController
@RequestMapping("/api")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    // ===== V1 API (Legacy) =====
    @GetMapping("/v1/products")
    public ResponseEntity<List<ProductV1DTO>> getProductsV1() {
        List<Product> products = productService.findAll();
        
        return ResponseEntity.ok(products.stream()
            .map(p -> ProductV1DTO.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .build())
            .toList());
    }
    
    @GetMapping("/v1/products/{id}")
    public ResponseEntity<ProductV1DTO> getProductByIdV1(
            @PathVariable String id) {
        
        Product product = productService.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        
        return ResponseEntity.ok(ProductV1DTO.builder()
            .id(product.getId())
            .name(product.getName())
            .price(product.getPrice())
            .build());
    }
    
    // ===== V2 API (Current) =====
    @GetMapping("/v2/products")
    public ResponseEntity<List<ProductV2DTO>> getProductsV2(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Product> products = productService.findAll(
            PageRequest.of(page, size));
        
        return ResponseEntity.ok(products.stream()
            .map(this::toProductV2DTO)
            .toList());
    }
    
    @GetMapping("/v2/products/{id}")
    public ResponseEntity<ProductV2DTO> getProductByIdV2(
            @PathVariable String id) {
        
        Product product = productService.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        
        return ResponseEntity.ok(toProductV2DTO(product));
    }
    
    @PostMapping("/v2/products")
    public ResponseEntity<ProductV2DTO> createProductV2(
            @Valid @RequestBody CreateProductV2Request request) {
        
        Product product = productService.create(request);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(product.getId())
            .toUri();
        
        return ResponseEntity.created(location)
            .body(toProductV2DTO(product));
    }
    
    // ===== V3 API (Latest) with HATEOAS =====
    @GetMapping("/v3/products")
    public ResponseEntity<CollectionModel<ProductV3DTO>> getProductsV3(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Product> products = productService.findAll(
            PageRequest.of(page, size));
        
        List<ProductV3DTO> dtos = products.stream()
            .map(this::toProductV3DTO)
            .toList();
        
        CollectionModel<ProductV3DTO> collectionModel = 
            CollectionModel.of(dtos,
                linkTo(methodOn(ProductController.class)
                    .getProductsV3(page, size))
                    .withSelfRel());
        
        if (products.hasNext()) {
            collectionModel.add(
                linkTo(methodOn(ProductController.class)
                    .getProductsV3(page + 1, size))
                    .withRel("next"));
        }
        
        return ResponseEntity.ok(collectionModel);
    }
    
    @GetMapping("/v3/products/{id}")
    public ResponseEntity<EntityModel<ProductV3DTO>> getProductByIdV3(
            @PathVariable String id) {
        
        Product product = productService.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        
        ProductV3DTO dto = toProductV3DTO(product);
        
        EntityModel<ProductV3DTO> model = EntityModel.of(dto,
            linkTo(methodOn(ProductController.class)
                .getProductByIdV3(id))
                .withSelfRel(),
            linkTo(methodOn(ProductController.class)
                .getProductsV3(0, 20))
                .withRel("all-products"),
            linkTo(methodOn(ProductController.class)
                .getProductByIdV3(id))
                .withRel("update"),
            linkTo(methodOn(ProductController.class)
                .getProductByIdV3(id))
                .withRel("delete")
        );
        
        return ResponseEntity.ok(model);
    }
    
    // DTOs por versão
    private ProductV2DTO toProductV2DTO(Product product) {
        return ProductV2DTO.builder()
            .id(product.getId())
            .name(product.getName())
            .price(product.getPrice())
            .stock(product.getStock())
            .category(product.getCategory())
            .createdAt(product.getCreatedAt())
            .build();
    }
    
    private ProductV3DTO toProductV3DTO(Product product) {
        return ProductV3DTO.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice())
            .discountedPrice(product.getDiscountedPrice())
            .stock(product.getStock())
            .category(product.getCategory())
            .supplier(product.getSupplier())
            .rating(product.getRating())
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
    }
}

// DTOs
@Data
@Builder
class ProductV1DTO {
    private String id;
    private String name;
    private BigDecimal price;
}

@Data
@Builder
class ProductV2DTO {
    private String id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String category;
    private LocalDateTime createdAt;
}

@Data
@Builder
class ProductV3DTO {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Integer stock;
    private String category;
    private SupplierInfo supplier;
    private Double rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2️⃣ **Content Negotiation Versioning**

```java
@Configuration
public class ContentNegotiationConfig implements WebMvcConfigurer {
    
    @Override
    public void configureContentNegotiation(
            ContentNegotiationConfigurer configurer) {
        
        configurer
            .ignoreAcceptHeader(false)  // Respect Accept header
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML)
            // Custom vendor types for versioning
            .mediaType("v1+json", 
                MediaType.valueOf("application/vnd.myapp.v1+json"))
            .mediaType("v2+json", 
                MediaType.valueOf("application/vnd.myapp.v2+json"))
            .mediaType("v3+json", 
                MediaType.valueOf("application/vnd.myapp.v3+json"));
    }
}

@RestController
@RequestMapping("/products")
public class ProductControllerContentNegotiation {
    
    @GetMapping(
        produces = "application/vnd.myapp.v1+json"
    )
    public List<ProductV1DTO> getProductsV1() {
        // V1 implementation
        return List.of();
    }
    
    @GetMapping(
        produces = "application/vnd.myapp.v2+json"
    )
    public List<ProductV2DTO> getProductsV2() {
        // V2 implementation
        return List.of();
    }
    
    @GetMapping(
        produces = "application/vnd.myapp.v3+json"
    )
    public List<ProductV3DTO> getProductsV3() {
        // V3 implementation
        return List.of();
    }
}

// Client usage:
// curl -H "Accept: application/vnd.myapp.v2+json" http://localhost:8080/products
// curl -H "Accept: application/vnd.myapp.v3+json" http://localhost:8080/products
```

### 3️⃣ **OpenAPI 3.0 Documentation**

```java
@Configuration
@EnableSwagger2
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Micro-Commerce Product API")
                .version("3.0.0")
                .description("Product management API with versioning support")
                .contact(new Contact()
                    .name("API Support")
                    .email("support@micro-commerce.com")))
            .servers(Arrays.asList(
                new Server()
                    .url("http://localhost:9000")
                    .description("Development Server"),
                new Server()
                    .url("https://api.micro-commerce.com")
                    .description("Production Server")))
            .externalDocs(new ExternalDocumentation()
                .description("Find more info here")
                .url("https://example.com/docs"));
    }
    
    @Bean
    public GroupedOpenApi productApiV1() {
        return GroupedOpenApi.builder()
            .group("products-v1")
            .pathsToMatch("/api/v1/products/**")
            .build();
    }
    
    @Bean
    public GroupedOpenApi productApiV2() {
        return GroupedOpenApi.builder()
            .group("products-v2")
            .pathsToMatch("/api/v2/products/**")
            .build();
    }
    
    @Bean
    public GroupedOpenApi productApiV3() {
        return GroupedOpenApi.builder()
            .group("products-v3")
            .pathsToMatch("/api/v3/products/**")
            .build();
    }
}

// Documentação automática no Swagger:
// http://localhost:8080/swagger-ui.html
```

**OpenAPI Spec YAML:**
```yaml
openapi: 3.0.0
info:
  title: Product API
  version: 3.0.0
  
paths:
  /api/v3/products:
    get:
      summary: List all products
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/ProductV3'
    post:
      summary: Create new product
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateProductRequest'
      responses:
        '201':
          description: Created
          
components:
  schemas:
    ProductV3:
      type: object
      properties:
        id:
          type: string
        name:
          type: string
        price:
          type: number
          format: decimal
        stock:
          type: integer
        rating:
          type: number
      required:
        - id
        - name
        - price
```

### 4️⃣ **API Deprecation & Migration Path**

```java
@Component
public class DeprecationHeaderFilter implements WebFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, 
                            WebFilterChain chain) {
        
        String path = exchange.getRequest().getPath().toString();
        
        if (path.contains("/api/v1/")) {
            // V1 deprecated
            exchange.getResponse().getHeaders().add(
                "Deprecated", "true");
            exchange.getResponse().getHeaders().add(
                "Sunset", "Wed, 31 Dec 2025 23:59:59 GMT");
            exchange.getResponse().getHeaders().add(
                "Link", "</api/v2/products>; rel=\"successor-version\"");
            
            log.warn("V1 API called: {} - Client should migrate to V2",
                path);
        }
        
        if (path.contains("/api/v2/")) {
            // V2 will be deprecated soon
            exchange.getResponse().getHeaders().add(
                "Warning", "199 - \"Deprecated API\"");
            exchange.getResponse().getHeaders().add(
                "Link", "</api/v3/products>; rel=\"successor-version\"");
        }
        
        return chain.filter(exchange);
    }
}

// Client receives:
// HTTP/1.1 200 OK
// Deprecated: true
// Sunset: Wed, 31 Dec 2025 23:59:59 GMT
// Link: </api/v2/products>; rel="successor-version"
// Warning: 199 - "Deprecated API"
```

### 5️⃣ **Error Response Consistency**

```java
@Data
@AllArgsConstructor
public class ApiErrorResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String traceId;  // For debugging
    private List<FieldError> fieldErrors;
    
    @Data
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String code;
        private String message;
    }
}

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound(
            ProductNotFoundException ex,
            HttpServletRequest request) {
        
        ApiErrorResponse error = new ApiErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.NOT_FOUND.value(),
            "NOT_FOUND",
            ex.getMessage(),
            request.getRequestURI(),
            MDC.get("traceId"),
            null
        );
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        List<ApiErrorResponse.FieldError> fieldErrors = 
            ex.getBindingResult().getFieldErrors().stream()
            .map(err -> new ApiErrorResponse.FieldError(
                err.getField(),
                err.getCode(),
                err.getDefaultMessage()))
            .toList();
        
        ApiErrorResponse error = new ApiErrorResponse(
            LocalDateTime.now().toString(),
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            "Invalid request parameters",
            request.getRequestURI(),
            MDC.get("traceId"),
            fieldErrors
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }
}
```

## 📊 Versioning Strategy Comparison

| Strategy | Pros | Cons | Usage |
|----------|------|------|-------|
| **URL Path** | Clear, visible | Multiple endpoints | ✅ Current (v1, v2, v3) |
| **Content Negotiation** | Elegant | Harder to discover | Query parameters |
| **Header** | Clean URLs | Hidden from logs | Sometimes |
| **Query Parameter** | Flexible | Ugly URLs | Rarely |

## 🎯 Migration Path

```
┌─────────────────────────────────────────┐
│ V1 API (Deprecated)                      │
│ Sunset: 31-12-2025                       │
│ Status: LEGACY                           │
│ Usage: 10% of traffic                    │
└─────────────────────────────────────────┘
         ↓ (Deprecation period)
┌─────────────────────────────────────────┐
│ V2 API (Current)                         │
│ Status: STABLE                           │
│ Usage: 50% of traffic                    │
│ Will Deprecate: 31-12-2026               │
└─────────────────────────────────────────┘
         ↓ (New features)
┌─────────────────────────────────────────┐
│ V3 API (Latest)                          │
│ Status: RECOMMENDED                      │
│ Usage: 40% of traffic                    │
│ Full HATEOAS support                     │
└─────────────────────────────────────────┘
```

---

**Relacionado:** ADR-001, ADR-004  
**Referências:**
- [REST API Best Practices - Zalando](https://opensource.zalando.com/restful-api-guidelines/)
- [OpenAPI Specification](https://spec.openapis.org/oas/v3.0.3)
- [API Versioning - Martin Fowler](https://martinfowler.com/articles/enterpriseREST.html)
- [HATEOAS Hypermedia - Spring Docs](https://spring.io/projects/spring-hateoas)
