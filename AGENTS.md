# AGENTS.md - MonSplit AI Assistant Guide

This document provides guidance for AI assistants working on the MonSplit project.

## Project Overview

**MonSplit** is a web-based expense splitting and sharing application that allows users to create groups, share expenses, and track who owes what.

### Technology Stack
- **Backend Framework:** Spring Boot 4.0.0
- **Architecture:** Spring Modulith 2.0.0 (Modular Monolith)
- **Frontend:** Vaadin Flow 25.0.0-beta9 (Java-based UI)
- **Database:** PostgreSQL 18
- **Migrations:** Flyway
- **Authentication:** Spring Security + OAuth2 Client
- **Build Tool:** Maven
- **Java Version:** Java 25
- **License:** GPL-3.0

## Build and Run Commands

### Development
```bash
# Build the project
./mvnw clean install

# Run in development mode (with hot reload)
./mvnw spring-boot:run

# Start Docker services (PostgreSQL + MailHog)
docker compose up -d

# Stop Docker services
docker compose down
```

### Code Quality
```bash
# Check code formatting
./mvnw spotless:check

# Apply code formatting
./mvnw spotless:apply

# Run tests
./mvnw test

# Run with production profile
./mvnw spring-boot:run -Pproduction
```

## Architecture: Spring Modulith + Hexagonal (Per-Module)

MonSplit follows a **modular monolith** architecture using Spring Modulith, where each module internally implements **Hexagonal Architecture** (Ports & Adapters pattern).

### Module Package Structure

Each module follows this explicit structure:

```
dev.iamkavindu.monsplit.{module}/          # MODULE ROOT = Public API
├── {Module}Service.java                   # Public facade (Application Service)
├── {Event}Event.java                      # Domain events (for inter-module communication)
│
├── domain/                                # INTERNAL - Core domain layer
│   ├── model/                             # Domain model
│   │   ├── {Aggregate}.java               # Aggregate root
│   │   ├── {Entity}.java                  # Entities
│   │   └── {ValueObject}.java             # Value objects
│   ├── {Module}Repository.java            # Port interface (outbound)
│   └── {Module}DomainService.java         # Domain services (business logic)
│
├── application/                           # INTERNAL - Application layer
│   ├── {UseCase}UseCase.java              # Use case implementations
│   └── port/                              # Inbound ports (optional)
│       └── {UseCase}Port.java             # Use case interfaces
│
└── adapter/                               # INTERNAL - Adapter layer
    ├── in/                                # Driving adapters (inbound)
    │   ├── web/                           # Web UI
    │   │   └── {Module}View.java          # Vaadin views
    │   └── rest/                          # REST API (if needed)
    │       └── {Module}Controller.java    # REST controllers
    └── out/                               # Driven adapters (outbound)
        ├── persistence/                   # Database
        │   ├── Jpa{Module}Repository.java # JPA repository implementation
        │   └── {Entity}Entity.java        # JPA entities (separate from domain)
        ├── notification/                  # Email/notifications
        │   └── Email{Module}Adapter.java
        └── external/                      # External services
            └── {External}Client.java
```

### Key Architectural Principles

1. **Public API at Module Root**
   - Only classes at the package root (`dev.iamkavindu.monsplit.{module}`) are accessible to other modules
   - These typically include: Application Services and Domain Events
   - All subpackages (`domain/`, `application/`, `adapter/`) are internal

2. **Hexagonal Architecture Layers**
   - **Domain Layer:** Pure business logic, no framework dependencies
   - **Application Layer:** Orchestrates use cases, coordinates domain objects
   - **Adapter Layer:** Implements technical details (DB, UI, external services)

3. **Dependency Rule**
   - Dependencies point inward: Adapters → Application → Domain
   - Domain has no dependencies on outer layers
   - Ports (interfaces) are defined in domain/application, implemented in adapters

4. **Inter-Module Communication**
   - Modules communicate **only** via domain events
   - Use `ApplicationEventPublisher` to publish events
   - Use `@ApplicationModuleListener` or `@EventListener` to consume events
   - Never import classes from other modules directly

5. **Ports & Adapters**
   - **Inbound Ports:** Use case interfaces (what the application offers)
   - **Outbound Ports:** Repository/service interfaces (what the application needs)
   - **Adapters:** Concrete implementations of ports

## Code Style

### Formatting
- **Formatter:** Palantir Java Format (enforced by Spotless Maven plugin)
- **Auto-format on compile:** `./mvnw spotless:apply`
- Code must pass `./mvnw spotless:check` before committing

### Lombok
- Use Lombok annotations to reduce boilerplate
- Common annotations: `@Data`, `@Builder`, `@Value`, `@RequiredArgsConstructor`
- Use `@Slf4j` for logging

### Naming Conventions
- **Packages:** `dev.iamkavindu.monsplit.{module}.{layer}`
- **Aggregates:** Noun, e.g., `Expense`, `Group`, `Payment`
- **Use Cases:** Verb + Noun, e.g., `CreateExpenseUseCase`, `SplitExpenseUseCase`
- **Events:** Past tense, e.g., `ExpenseCreatedEvent`, `PaymentReceivedEvent`
- **Repositories:** `{Aggregate}Repository` (interface)
- **Repository Impls:** `Jpa{Aggregate}Repository` (JPA implementation)

## Spring Modulith Guidelines

### Module Definition
A module is defined by a top-level package under `dev.iamkavindu.monsplit`:

```
dev.iamkavindu.monsplit.expense/   ← Module
dev.iamkavindu.monsplit.group/     ← Module
dev.iamkavindu.monsplit.payment/   ← Module
dev.iamkavindu.monsplit.user/      ← Module
```

### Event-Driven Communication

**Publishing Events:**
```java
@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ApplicationEventPublisher eventPublisher;
    
    public void createExpense(CreateExpenseCommand cmd) {
        // ... business logic
        eventPublisher.publishEvent(new ExpenseCreatedEvent(expenseId));
    }
}
```

**Consuming Events:**
```java
@ApplicationModuleListener
void onExpenseCreated(ExpenseCreatedEvent event) {
    // Handle event in another module
}
```

### Event Publication Table
Spring Modulith uses the `event_publication` table to ensure reliable event delivery:
- Events are persisted before publishing
- Failed event handlers can be retried
- Completed events are marked with `completion_date`

### Module Verification
Use `@ApplicationModuleTest` to verify module boundaries:

```java
@ApplicationModuleTest
class ModuleStructureTests {
    @Test
    void verifiesModuleStructure(ApplicationModules modules) {
        modules.verify();
    }
}
```

## Vaadin Flow Guidance

### Overview
MonSplit uses **Vaadin Flow** (Java-based UI), not React/Hilla. All frontend code is written in Java.

### View Structure
Views are located in `adapter/in/web/` package:

```java
@Route("expenses")
@PermitAll
public class ExpenseView extends VerticalLayout {
    private final ExpenseService expenseService;
    
    public ExpenseView(ExpenseService expenseService) {
        this.expenseService = expenseService;
        initializeView();
    }
    
    private void initializeView() {
        // Build UI using Vaadin components
        add(new H1("Expenses"));
        // ...
    }
}
```

### Key Patterns
- **Routing:** Use `@Route` annotation for navigation
- **Security:** Use `@PermitAll`, `@RolesAllowed`, or `@AnonymousAllowed`
- **Components:** Use Vaadin 25 components (`Button`, `Grid`, `TextField`, etc.)
- **Layouts:** Extend `VerticalLayout`, `HorizontalLayout`, or implement `AppLayout`
- **Dependency Injection:** Constructor injection of services

### Spring Security Integration
- Vaadin Spring Security integration is enabled via `vaadin-spring-security` dependency
- OAuth2 login is configured in Spring Security configuration
- Use `SecurityContextHolder` to access current user

### Resources
- Vaadin components live in `src/main/frontend/` (generated)
- Custom styles can be added to themes
- Vaadin handles all asset bundling

## Testing Strategy

### Unit Tests (Domain Layer)
Test domain logic in isolation, no Spring context needed:

```java
class ExpenseTest {
    @Test
    void shouldSplitExpenseEqually() {
        // Pure domain testing
    }
}
```

### Integration Tests (with Spring Context)
Use Testcontainers for database:

```java
@SpringBootTest
@Testcontainers
class ExpenseServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");
    
    @Test
    void shouldPersistExpense() {
        // Test with real DB
    }
}
```

### Module Tests (Verify Boundaries)
```java
@ApplicationModuleTest
class ExpenseModuleTests {
    @Test
    void verifiesModuleStructure(ApplicationModules modules) {
        modules.verify();
    }
}
```

### UI Tests (Vaadin Components)
Use Karibu Testing for component testing:

```java
@SpringBootTest
@ExtendWith(KaribuTestingExtension.class)
class ExpenseViewTest {
    @Test
    void shouldDisplayExpenses() {
        // Test Vaadin components
    }
}
```

### E2E Tests (Browser Automation)
Use Playwright for end-to-end testing:

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ExpenseE2ETest {
    @Test
    void userCanCreateExpense() {
        // Use Playwright API
    }
}
```

## Database

### Technology
- **Database:** PostgreSQL 18
- **Migration Tool:** Flyway
- **ORM:** JPA with Hibernate

### Flyway Migrations
- **Location:** `src/main/resources/db/migration`
- **Naming Convention:** `V{YYYY.MM.DD.HH.MM}__description.sql`
- **Example:** `V2025.12.16.10.30__create_expense_table.sql`

### Migration Example
```sql
-- V2025.12.16.10.30__create_expense_table.sql
CREATE TABLE expense (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NOT NULL
);

CREATE INDEX idx_expense_created_by ON expense(created_by);
```

### JPA Configuration
- **DDL Auto:** `validate` (Flyway manages schema)
- **Show SQL:** `false` in production, can enable for debugging
- **Naming Strategy:** Default Spring Boot naming strategy

### Best Practices
- Use UUID for primary keys
- Use `TIMESTAMP WITH TIME ZONE` for dates
- Create indexes for foreign keys
- Keep migrations immutable (never edit existing migrations)
- Test migrations with Testcontainers

## Example Module Structure

Here's a concrete example of how the **Expense** module would be structured:

```
dev.iamkavindu.monsplit.expense/
│
├── ExpenseService.java                    # Public API - Application Service
├── ExpenseCreatedEvent.java               # Public API - Domain Event
├── ExpenseSplitEvent.java                 # Public API - Domain Event
│
├── domain/
│   ├── model/
│   │   ├── Expense.java                   # Aggregate Root
│   │   ├── ExpenseShare.java              # Entity
│   │   ├── Money.java                     # Value Object
│   │   ├── ExpenseStatus.java             # Enum
│   │   └── ExpenseParticipant.java        # Value Object
│   ├── ExpenseRepository.java             # Port (interface)
│   └── ExpenseDomainService.java          # Domain Service
│
├── application/
│   ├── CreateExpenseUseCase.java          # Use Case
│   ├── SplitExpenseUseCase.java           # Use Case
│   ├── RecordPaymentUseCase.java          # Use Case
│   └── port/
│       └── CreateExpensePort.java         # Optional: Inbound port interface
│
└── adapter/
    ├── in/
    │   └── web/
    │       ├── ExpenseView.java           # Main expense view
    │       ├── CreateExpenseDialog.java   # Dialog component
    │       └── ExpenseGrid.java           # Grid component
    │
    └── out/
        ├── persistence/
        │   ├── JpaExpenseRepository.java  # JPA implementation of ExpenseRepository
        │   ├── ExpenseEntity.java         # JPA entity (maps to DB)
        │   └── ExpenseShareEntity.java    # JPA entity
        └── notification/
            └── EmailExpenseNotificationAdapter.java
```

### Code Example - Complete Flow

**1. Domain Model (Pure Business Logic)**
```java
// domain/model/Expense.java
@Getter
@AllArgsConstructor
public class Expense {
    private final ExpenseId id;
    private String name;
    private Money amount;
    private List<ExpenseShare> shares;
    private ExpenseStatus status;
    
    public void splitEqually(List<UserId> participants) {
        Money shareAmount = amount.divide(participants.size());
        this.shares = participants.stream()
            .map(userId -> new ExpenseShare(userId, shareAmount))
            .toList();
    }
    
    public void markAsPaid() {
        this.status = ExpenseStatus.PAID;
    }
}
```

**2. Repository Port (Interface in Domain)**
```java
// domain/ExpenseRepository.java
public interface ExpenseRepository {
    Expense save(Expense expense);
    Optional<Expense> findById(ExpenseId id);
    List<Expense> findByParticipant(UserId userId);
}
```

**3. Use Case (Application Layer)**
```java
// application/CreateExpenseUseCase.java
@Service
@RequiredArgsConstructor
@Transactional
public class CreateExpenseUseCase {
    private final ExpenseRepository expenseRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    public ExpenseId execute(CreateExpenseCommand command) {
        Expense expense = new Expense(
            ExpenseId.generate(),
            command.name(),
            Money.of(command.amount()),
            Collections.emptyList(),
            ExpenseStatus.PENDING
        );
        
        expense.splitEqually(command.participants());
        expenseRepository.save(expense);
        
        eventPublisher.publishEvent(
            new ExpenseCreatedEvent(expense.getId(), expense.getShares())
        );
        
        return expense.getId();
    }
}
```

**4. Public Service Facade**
```java
// ExpenseService.java (at module root)
@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final CreateExpenseUseCase createExpenseUseCase;
    private final SplitExpenseUseCase splitExpenseUseCase;
    
    public ExpenseId createExpense(CreateExpenseCommand command) {
        return createExpenseUseCase.execute(command);
    }
    
    public void splitExpense(ExpenseId id, List<UserId> participants) {
        splitExpenseUseCase.execute(id, participants);
    }
}
```

**5. Adapter - JPA Repository Implementation**
```java
// adapter/out/persistence/JpaExpenseRepository.java
@Repository
@RequiredArgsConstructor
class JpaExpenseRepository implements ExpenseRepository {
    private final SpringDataExpenseRepository springRepo;
    private final ExpenseMapper mapper;
    
    @Override
    public Expense save(Expense expense) {
        ExpenseEntity entity = mapper.toEntity(expense);
        ExpenseEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Expense> findById(ExpenseId id) {
        return springRepo.findById(id.value())
            .map(mapper::toDomain);
    }
}
```

**6. Adapter - Vaadin View**
```java
// adapter/in/web/ExpenseView.java
@Route("expenses")
@PermitAll
@RequiredArgsConstructor
public class ExpenseView extends VerticalLayout {
    private final ExpenseService expenseService;
    
    public ExpenseView() {
        add(new H1("Expenses"));
        
        Button createButton = new Button("Create Expense", 
            e -> openCreateDialog());
        add(createButton);
        
        Grid<ExpenseDTO> grid = createExpenseGrid();
        add(grid);
    }
    
    private void openCreateDialog() {
        CreateExpenseDialog dialog = new CreateExpenseDialog(expenseService);
        dialog.open();
    }
}
```

## Summary

When working on MonSplit:
1. ✅ Follow the per-module Hexagonal Architecture structure
2. ✅ Keep module root for public API only
3. ✅ Use domain events for inter-module communication
4. ✅ Apply Spotless formatting before committing
5. ✅ Write tests at appropriate layers
6. ✅ Use Flyway for all database schema changes
7. ✅ Build Vaadin views in the `adapter/in/web/` package
8. ✅ Inject dependencies via constructor
9. ✅ Verify module boundaries with `@ApplicationModuleTest`
10. ✅ Keep domain logic pure (no framework dependencies)

---

For questions or clarifications, refer to the README.md or Spring Modulith/Vaadin documentation.

