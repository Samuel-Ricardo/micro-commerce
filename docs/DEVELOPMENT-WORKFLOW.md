# Development Workflow

**Document Status:** Draft  
**Version:** 1.0  
**Last Updated:** 2026-07-23

---

## 📋 Table of Contents

1. [Git Workflow](#git-workflow)
2. [Coding Standards](#coding-standards)
3. [Pull Request Process](#pull-request-process)
4. [Code Review](#code-review)
5. [Local Development Setup](#local-development-setup)
6. [Testing Before Commit](#testing-before-commit)
7. [Troubleshooting Guide](#troubleshooting-guide)

---

## Git Workflow

### Branch Naming Convention

```
feature/{STORY-ID}-{description}     # New features
bugfix/{ISSUE-ID}-{description}      # Bug fixes
docs/{description}                    # Documentation
refactor/{description}                # Code refactoring
chore/{description}                   # Build, dependencies, etc.

Examples:
- feature/SHOP-123-add-product-search
- bugfix/SHOP-456-fix-order-validation
- docs/update-api-reference
- refactor/simplify-payment-logic
- chore/upgrade-spring-boot
```

### Commit Message Convention

```
[TYPE] {STORY-ID}: {Description}

TYPE: feat, fix, docs, style, refactor, test, chore
STORY-ID: SHOP-123 (from project management system)
Description: Clear, concise description (imperative mood)

Examples:
[feat] SHOP-123: Add product search endpoint
[fix] SHOP-456: Fix order validation for negative quantities
[docs] Update API reference with new endpoints
[refactor] Simplify payment processing logic
[test] Add integration tests for order service
[chore] Update Spring Boot to 3.4.1
```

### Git Workflow Steps

```bash
# 1. Create feature branch from main
git checkout main
git pull origin main
git checkout -b feature/SHOP-123-add-search

# 2. Make changes and commit regularly
git add src/main/java/com/js/ProductService.java
git commit -m "[feat] SHOP-123: Implement product search"

# 3. Keep branch updated with main
git fetch origin
git rebase origin/main

# 4. Push to remote
git push origin feature/SHOP-123-add-search

# 5. Create Pull Request on GitHub
# (see Pull Request Process)

# 6. After PR approval, merge to main
git checkout main
git pull origin main
git merge --no-ff feature/SHOP-123-add-search
git push origin main

# 7. Delete local and remote branch
git branch -d feature/SHOP-123-add-search
git push origin --delete feature/SHOP-123-add-search
```

---

## Coding Standards

### Java Code Style

#### Class Structure

```java
public class OrderService {
    
    // Static variables
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    // Instance variables (private)
    private OrderRepository orderRepository;
    private InventoryService inventoryService;
    
    // Constructor
    @Autowired
    public OrderService(OrderRepository orderRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }
    
    // Public methods
    public OrderDTO createOrder(OrderRequest request) {
        // Implementation
    }
    
    // Private helper methods
    private void validateOrder(OrderRequest request) {
        // Implementation
    }
}
```

#### Naming Conventions

```java
// Classes and Interfaces
public class ProductService { }        // PascalCase
public interface ProductRepository { } // PascalCase

// Methods and Variables
public void createProduct() { }        // camelCase
private String productName;            // camelCase

// Constants
private static final int MAX_RETRIES = 3;  // UPPER_SNAKE_CASE

// Boolean methods
public boolean isActive() { }
public boolean hasPermission() { }
public boolean canDelete() { }
```

#### Error Handling

```java
// GOOD: Specific exception handling
try {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
} catch (ProductNotFoundException e) {
    logger.warn("Product {} not found", id);
    throw e;
} catch (Exception e) {
    logger.error("Unexpected error retrieving product", e);
    throw new RuntimeException("Failed to retrieve product", e);
}

// BAD: Generic exception handling
try {
    Product product = productRepository.findById(id).get();
} catch (Exception e) {
    e.printStackTrace();  // ❌ Don't use printStackTrace
}
```

#### Logging

```java
// Use structured logging
logger.info("Creating order for customer: {}", customerId);
logger.debug("Order details: {}", orderDetails);
logger.warn("Low inventory for product: {}", productId);
logger.error("Failed to process payment", exception);

// DON'T use concatenation
logger.info("Creating order for customer: " + customerId);  // ❌ Inefficient

// DON'T use System.out
System.out.println("Processing order");  // ❌ Not captured in logs
```

### Code Formatting

- **Indentation**: 4 spaces
- **Line Length**: Max 120 characters
- **Imports**: Sorted, one per line
- **Blank Lines**: Between methods and logical sections

```bash
# Format with Maven
mvn spotless:apply

# Check formatting without changes
mvn spotless:check
```

---

## Pull Request Process

### Create Pull Request

1. **Push branch to remote**
   ```bash
   git push origin feature/SHOP-123-add-search
   ```

2. **Create PR on GitHub**
   - Title: `[feat] SHOP-123: Add product search endpoint`
   - Description: Use PR template

3. **PR Template** (auto-populated)
   ```markdown
   ## Description
   Brief description of changes
   
   ## Related Issue
   Closes #123
   
   ## Changes Made
   - Change 1
   - Change 2
   
   ## Testing
   How to test these changes
   
   ## Checklist
   - [ ] Code follows style guidelines
   - [ ] Tests added/updated
   - [ ] Documentation updated
   - [ ] No breaking changes
   ```

### Code Review Requirements

```yaml
Required Reviews: 1
Dismissal Stale Reviews: Enabled
Require Status Checks: true
  - Build must pass
  - Tests must pass (>80% coverage)
  - Code quality must be A (SonarQube)
  - No high-severity issues
```

### Review Checklist for Reviewers

- [ ] Code follows naming conventions
- [ ] Error handling is appropriate
- [ ] No SQL injection vulnerabilities
- [ ] Tests are comprehensive
- [ ] Documentation is updated
- [ ] No hardcoded secrets
- [ ] Performance implications considered
- [ ] Backwards compatible

---

## Code Review

### Review Comments Best Practices

```javascript
// ❌ DON'T: Nitpick style issues
"Why is there an extra space here?"

// ✅ DO: Explain why it matters
"Variable names should follow camelCase convention for consistency"

// ❌ DON'T: Be dismissive
"This is wrong"

// ✅ DO: Explain and suggest
"Consider using eager loading instead of lazy loading to avoid N+1 queries.
See: [Architecture.md](../ARCHITECTURE.md#n1-query-problem)"

// ✅ DO: Approve with suggestions
"Looks good! Consider also adding index on customer_id for performance."
```

### Conflict Resolution

If conflicts arise during review:

1. **Sync with main branch**
   ```bash
   git fetch origin
   git rebase origin/main
   ```

2. **Resolve conflicts locally**
   ```bash
   # Edit conflicting files
   # Mark as resolved
   git add .
   git rebase --continue
   ```

3. **Push resolved conflicts**
   ```bash
   git push origin feature/SHOP-123-add-search --force-with-lease
   ```

---

## Local Development Setup

### Initial Setup

```bash
# Clone repository
git clone https://github.com/samuel-ricardo/micro-commerce.git
cd micro-commerce

# Install pre-commit hooks
cp .githooks/* .git/hooks/
chmod +x .git/hooks/*

# Set up IDE
# Open in IntelliJ IDEA or VS Code

# Start local environment
docker-compose up -d
```

### IDE Configuration (IntelliJ IDEA)

```
1. Open Project Structure (Ctrl+Alt+Shift+S)
2. Set JDK: OpenJDK 21
3. Configure Maven: Maven 3.8+
4. Enable code inspections:
   - Settings → Editor → Inspections → Java
   - Enable: Data flow warnings, potential bugs
5. Set up code formatting:
   - Settings → Editor → Code Style → Java
   - Import scheme: .idea/codeStyleConfig.xml
```

---

## Testing Before Commit

### Run Tests Locally

```bash
# Run all tests
mvn clean test

# Run specific test class
mvn test -Dtest=ProductServiceTest

# Run with coverage
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Pre-Commit Checks

```bash
#!/bin/bash
# .git/hooks/pre-commit

echo "Running pre-commit checks..."

# 1. Build project
mvn clean compile || exit 1

# 2. Run tests
mvn test || exit 1

# 3. Check code style
mvn spotless:check || exit 1

# 4. Check coverage
mvn jacoco:check || exit 1

echo "✅ All checks passed!"
```

---

## Troubleshooting Guide

### Common Issues

#### Maven Dependency Issues

```bash
# Problem: "Cannot find artifact"
# Solution: Clear cache and re-download
mvn clean
rm -rf ~/.m2/repository/
mvn dependency:resolve

# Problem: Version conflicts
# Solution: Check dependency tree
mvn dependency:tree
```

#### Spring Boot Application Won't Start

```bash
# Problem: Port already in use
# Solution: Kill process on port
lsof -ti:8080 | xargs kill -9

# Problem: Database connection failed
# Solution: Check database is running
docker-compose ps mysql

# Problem: Bean creation failed
# Solution: Check logs for stack trace
mvn spring-boot:run 2>&1 | grep -A 20 "Error"
```

#### Git Conflicts

```bash
# Problem: Cannot merge branch
# Solution: Rebase and resolve conflicts
git fetch origin
git rebase origin/main
# Edit conflicting files
git add .
git rebase --continue

# Problem: Accidentally committed to main
# Solution: Create feature branch and reset main
git branch feature/restore-commit
git reset --hard HEAD~1
```

#### Docker Issues

```bash
# Problem: Container won't start
# Solution: Check logs
docker-compose logs {service-name}

# Problem: Database schema not created
# Solution: Restart with clean volume
docker-compose down -v
docker-compose up -d

# Problem: Port conflicts
# Solution: Update docker-compose.yaml with different ports
```

---

## Development Tips

### Debugging in IDE

```java
// Add breakpoints and run in Debug mode
public void createOrder(OrderRequest request) {
    // Breakpoint here (press F9 to resume)
    Order order = Order.builder()
        .customerId(request.getCustomerId())
        .build();
    
    // Step over (F10) or step into (F11)
    validate(order);
    
    orderRepository.save(order);
}
```

### Using Postman for API Testing

```
1. Create collection: "Micro-Commerce API"
2. Create requests for each endpoint
3. Set base URL: {{baseUrl}}/api/v1
4. Store token in environment variable
5. Use pre-request scripts for setup
6. Generate API documentation from collection
```

---

## Related Documentation

- [Coding-Standards.md](../standards/CODING-STANDARDS.md)
- [Implementation-Guide.md](./IMPLEMENTATION-GUIDE.md)
- [Testing-Strategy.md](./TESTING-STRATEGY.md)

---

## Document Metadata

- **Created:** 2026-07-23
- **Status:** Draft
- **Owner:** Development Team
- **Audience:** All developers

