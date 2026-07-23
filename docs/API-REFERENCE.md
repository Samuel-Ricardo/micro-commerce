# API Reference

**Document Status:** Draft  
**Version:** 1.0  
**Last Updated:** 2026-07-23

---

## 📋 Table of Contents

1. [API Overview](#api-overview)
2. [Authentication](#authentication)
3. [Request/Response Format](#requestresponse-format)
4. [Error Codes](#error-codes)
5. [Rate Limiting](#rate-limiting)
6. [Product Endpoints](#product-endpoints)
7. [Order Endpoints](#order-endpoints)
8. [Inventory Endpoints](#inventory-endpoints)

---

## API Overview

### Base URLs

```
Development:  http://localhost:9000/api
Staging:      https://staging-api.example.com/api
Production:   https://api.example.com/api
```

### API Versioning

All endpoints use versioned URLs:

```
GET /api/v1/products      ✅ Current version
GET /api/products         ❌ Deprecated (avoid)
GET /api/v2/products      🚀 Future version
```

### Response Format

All responses follow JSON:API specification:

```json
{
  "data": {
    "id": "123",
    "type": "product",
    "attributes": {
      "name": "Product Name",
      "price": 99.99
    }
  }
}
```

---

## Authentication

### OAuth2 Authorization Code Flow

All API requests require authentication using bearer tokens:

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Get Access Token

```http
POST /api/v1/auth/login HTTP/1.1
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

HTTP/1.1 200 OK
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

#### Token Refresh

```http
POST /api/v1/auth/refresh HTTP/1.1
Authorization: Bearer {refresh_token}

HTTP/1.1 200 OK
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 3600
}
```

---

## Request/Response Format

### Request Headers

```http
Content-Type: application/json
Authorization: Bearer {token}
Accept: application/json
X-Request-ID: {uuid}  # Optional, for tracing
```

### Pagination

All list endpoints support pagination:

```http
GET /api/v1/products?page=0&size=20&sort=name,asc

Response:
{
  "data": [...],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

### Sorting

```http
# Single field
GET /api/v1/products?sort=name,asc

# Multiple fields
GET /api/v1/products?sort=category,asc&sort=price,desc
```

### Filtering

```http
# Common filters
GET /api/v1/products?category=electronics&minPrice=100&maxPrice=500

# Date filters
GET /api/v1/orders?createdAfter=2026-01-01&createdBefore=2026-12-31
```

---

## Error Codes

### HTTP Status Codes

| Code | Meaning | Use Case |
|------|---------|----------|
| **200** | OK | Successful request |
| **201** | Created | Resource created |
| **204** | No Content | Successful delete |
| **400** | Bad Request | Invalid input |
| **401** | Unauthorized | Missing/invalid token |
| **403** | Forbidden | Insufficient permissions |
| **404** | Not Found | Resource doesn't exist |
| **409** | Conflict | Duplicate resource |
| **429** | Too Many Requests | Rate limit exceeded |
| **500** | Internal Server Error | Server error |
| **503** | Service Unavailable | Service down |

### Error Response Format

```json
{
  "timestamp": "2026-07-23T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid product price",
  "details": {
    "field": "price",
    "rejectedValue": "-10.00",
    "message": "Price must be positive"
  },
  "path": "/api/v1/products"
}
```

---

## Rate Limiting

### Limits

- **Per Minute**: 100 requests per minute per IP
- **Per Hour**: 5,000 requests per hour per IP
- **Per Day**: 50,000 requests per day per IP

### Rate Limit Headers

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1690027800

# When rate limited
HTTP/1.1 429 Too Many Requests
X-RateLimit-Retry-After: 60
```

---

## Product Endpoints

### List Products

```http
GET /api/v1/products?page=0&size=20 HTTP/1.1
Authorization: Bearer {token}

HTTP/1.1 200 OK
Content-Type: application/json

{
  "data": [
    {
      "id": "1",
      "type": "product",
      "attributes": {
        "name": "Laptop",
        "description": "High-performance laptop",
        "price": 999.99,
        "sku": "SKU-001",
        "category": "Electronics",
        "inStock": true,
        "stockLevel": 50,
        "imageUrl": "https://cdn.example.com/laptop.jpg",
        "createdAt": "2026-01-15T10:00:00Z",
        "updatedAt": "2026-07-20T14:30:00Z"
      }
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

### Get Product by ID

```http
GET /api/v1/products/1 HTTP/1.1
Authorization: Bearer {token}

HTTP/1.1 200 OK

{
  "data": {
    "id": "1",
    "type": "product",
    "attributes": {
      "name": "Laptop",
      "description": "High-performance laptop",
      "price": 999.99,
      "sku": "SKU-001",
      "category": "Electronics",
      "inStock": true,
      "stockLevel": 50,
      "imageUrl": "https://cdn.example.com/laptop.jpg"
    }
  }
}
```

### Create Product

```http
POST /api/v1/products HTTP/1.1
Authorization: Bearer {admin-token}
Content-Type: application/json

{
  "name": "New Laptop",
  "description": "Latest model laptop",
  "price": 1299.99,
  "sku": "SKU-002",
  "category": "Electronics",
  "stockLevel": 100,
  "imageUrl": "https://cdn.example.com/new-laptop.jpg"
}

HTTP/1.1 201 Created
Location: /api/v1/products/2

{
  "data": {
    "id": "2",
    "type": "product",
    "attributes": { ... }
  }
}
```

### Update Product

```http
PUT /api/v1/products/1 HTTP/1.1
Authorization: Bearer {admin-token}
Content-Type: application/json

{
  "price": 1099.99,
  "stockLevel": 45
}

HTTP/1.1 200 OK

{
  "data": {
    "id": "1",
    "type": "product",
    "attributes": { ... }
  }
}
```

### Delete Product

```http
DELETE /api/v1/products/1 HTTP/1.1
Authorization: Bearer {admin-token}

HTTP/1.1 204 No Content
```

---

## Order Endpoints

### List Orders

```http
GET /api/v1/orders?page=0&size=10 HTTP/1.1
Authorization: Bearer {token}

HTTP/1.1 200 OK

{
  "data": [
    {
      "id": "ORD-001",
      "type": "order",
      "attributes": {
        "customerId": "123",
        "status": "CONFIRMED",
        "totalPrice": 2099.97,
        "itemCount": 2,
        "shippingAddress": "123 Main St, City, State 12345",
        "createdAt": "2026-07-20T10:00:00Z"
      }
    }
  ],
  "meta": { ... }
}
```

### Get Order Details

```http
GET /api/v1/orders/ORD-001 HTTP/1.1
Authorization: Bearer {token}

HTTP/1.1 200 OK

{
  "data": {
    "id": "ORD-001",
    "type": "order",
    "attributes": {
      "customerId": "123",
      "status": "CONFIRMED",
      "items": [
        {
          "productId": "1",
          "productName": "Laptop",
          "quantity": 1,
          "unitPrice": 999.99,
          "subtotal": 999.99
        }
      ],
      "subtotal": 2099.97,
      "tax": 209.99,
      "shipping": 10.00,
      "totalPrice": 2319.96,
      "shippingAddress": "123 Main St, City, State 12345",
      "trackingNumber": "TRACK-123456",
      "createdAt": "2026-07-20T10:00:00Z"
    }
  }
}
```

### Create Order

```http
POST /api/v1/orders HTTP/1.1
Authorization: Bearer {token}
Content-Type: application/json

{
  "items": [
    {
      "productId": "1",
      "quantity": 1
    },
    {
      "productId": "2",
      "quantity": 2
    }
  ],
  "shippingAddress": "123 Main St, City, State 12345",
  "shippingMethod": "STANDARD"
}

HTTP/1.1 201 Created
Location: /api/v1/orders/ORD-001

{
  "data": {
    "id": "ORD-001",
    "type": "order",
    "attributes": { ... }
  }
}
```

### Cancel Order

```http
PUT /api/v1/orders/ORD-001/cancel HTTP/1.1
Authorization: Bearer {token}
Content-Type: application/json

{
  "reason": "Changed my mind"
}

HTTP/1.1 200 OK

{
  "data": {
    "id": "ORD-001",
    "type": "order",
    "attributes": {
      "status": "CANCELLED",
      ...
    }
  }
}
```

---

## Inventory Endpoints

### Check Inventory

```http
GET /api/v1/inventory/products/1 HTTP/1.1
Authorization: Bearer {token}

HTTP/1.1 200 OK

{
  "data": {
    "productId": "1",
    "stockLevel": 50,
    "reservedQuantity": 5,
    "availableQuantity": 45,
    "reorderPoint": 10,
    "lastUpdated": "2026-07-23T10:30:00Z"
  }
}
```

### Reserve Inventory

```http
POST /api/v1/inventory/reserve HTTP/1.1
Authorization: Bearer {token}
Content-Type: application/json

{
  "productId": "1",
  "quantity": 5,
  "orderId": "ORD-001"
}

HTTP/1.1 201 Created

{
  "data": {
    "reservationId": "RES-001",
    "productId": "1",
    "quantity": 5,
    "orderId": "ORD-001",
    "status": "RESERVED",
    "expiresAt": "2026-07-24T10:30:00Z"
  }
}
```

### Release Inventory

```http
DELETE /api/v1/inventory/reserve/RES-001 HTTP/1.1
Authorization: Bearer {token}

HTTP/1.1 204 No Content
```

---

## OpenAPI/Swagger

### Swagger UI

Access interactive API documentation:

```
http://localhost:9000/api/swagger-ui.html
```

### OpenAPI Specification

```
GET /api/v3/api-docs
GET /api/v3/api-docs.yaml
```

---

## Related Documentation

- [Implementation-Guide.md](./IMPLEMENTATION-GUIDE.md) — Setup and configuration
- [Architecture.md](./ARCHITECTURE.md) — Service design

---

## Document Metadata

- **Created:** 2026-07-23
- **Status:** Draft
- **Owner:** API Team
- **Audience:** Developers and API consumers

