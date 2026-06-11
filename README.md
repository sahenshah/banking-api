# Banking REST API — Eagle Bank

A RESTful banking API built with Java 21 and Spring Boot 3, implementing user management, bank accounts, and financial transactions with JWT authentication.

---

## Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.5.x | Application framework |
| Spring Security | 6.x | Authentication & authorisation |
| Spring Data JPA | 3.x | Data access layer |
| H2 Database | In-memory | Embedded database |
| JJWT | 0.12.3 | JWT generation & validation |
| JUnit 5 + Mockito | Latest | Testing |
| Maven | 3.x | Build tool |

---

## Getting Started

### Prerequisites

- Java 21 JDK or higher
- Maven 3.x (or use the included mvnw wrapper)

### Running the Application

```bash
# Clone the repository
git clone https://github.com/sahenshah/banking-api.git
cd banking-api

# Run with Maven wrapper
./mvnw spring-boot:run        # macOS/Linux
.\mvnw spring-boot:run        # Windows PowerShell
```

The application starts on **http://localhost:8080**

### Running the Tests

```bash
./mvnw test        # macOS/Linux
.\mvnw test        # Windows PowerShell
```

**32 tests** across unit and integration test suites.

---

## API Reference

All endpoints are prefixed with `/v1`.

### Authentication (Public)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/v1/users` | Register a new user |
| `POST` | `/v1/auth/login` | Login and receive JWT |

### Users (Authenticated)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/v1/users/{userId}` | Get user by ID |
| `PATCH` | `/v1/users/{userId}` | Update user details |
| `DELETE` | `/v1/users/{userId}` | Delete user |

### Accounts (Authenticated)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/v1/accounts` | Create a new account |
| `GET` | `/v1/accounts` | List all accounts |
| `GET` | `/v1/accounts/{accountId}` | Get account by ID |
| `PATCH` | `/v1/accounts/{accountId}` | Update account |
| `DELETE` | `/v1/accounts/{accountId}` | Delete account |

### Transactions (Authenticated)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/v1/accounts/{accountId}/transactions` | Create deposit or withdrawal |
| `GET` | `/v1/accounts/{accountId}/transactions` | List transactions |
| `GET` | `/v1/accounts/{accountId}/transactions/{transactionId}` | Get transaction by ID |

---

## Authentication

All endpoints except `POST /v1/users` and `POST /v1/auth/login` require a JWT Bearer token.

Include the token in every request header:

```
Authorization: Bearer <your-token>
```

Tokens expire after **1 hour**.

---

## Request & Response Examples

### Register

```
POST /v1/users
Content-Type: application/json

{
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@example.com",
    "password": "password123"
}
```

Response 201 Created:
```json
{
    "token": "eyJhbGci...",
    "token_type": "Bearer",
    "expires_in": 3600
}
```

### Login

```
POST /v1/auth/login
Content-Type: application/json

{
    "email": "john.doe@example.com",
    "password": "password123"
}
```

Response 200 OK:
```json
{
    "token": "eyJhbGci...",
    "token_type": "Bearer",
    "expires_in": 3600
}
```

### Get User

```
GET /v1/users/{userId}
Authorization: Bearer <token>
```

Response 200 OK:
```json
{
    "id": "2bc9c0db-e8eb-4045-82e3-25b0277e307f",
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@example.com",
    "created_at": "2026-01-01T10:00:00"
}
```

### Create Account

```
POST /v1/accounts
Authorization: Bearer <token>
Content-Type: application/json

{
    "account_type": "CURRENT"
}
```

Response 201 Created:
```json
{
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "account_number": "GB123456789012345678",
    "account_type": "CURRENT",
    "balance": 0.0000,
    "currency": "GBP",
    "owner_id": "2bc9c0db-e8eb-4045-82e3-25b0277e307f",
    "created_at": "2026-01-01T10:00:00",
    "updated_at": "2026-01-01T10:00:00"
}
```

### Create Transaction - Deposit

```
POST /v1/accounts/{accountId}/transactions
Authorization: Bearer <token>
Content-Type: application/json

{
    "type": "DEPOSIT",
    "amount": 500.00,
    "description": "Initial deposit"
}
```

Response 201 Created:
```json
{
    "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "transaction_type": "DEPOSIT",
    "amount": 500.00,
    "balance_after": 500.0000,
    "description": "Initial deposit",
    "account_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "created_at": "2026-01-01T10:05:00"
}
```

### Create Transaction - Withdrawal

```
POST /v1/accounts/{accountId}/transactions
Authorization: Bearer <token>
Content-Type: application/json

{
    "type": "WITHDRAWAL",
    "amount": 100.00,
    "description": "ATM withdrawal"
}
```

Response 201 Created - or 422 Unprocessable Entity if insufficient funds.

---

## Error Responses

All errors return a consistent structure:

```json
{
    "status": 404,
    "error": "Not Found",
    "message": "Account not found",
    "path": "/v1/accounts/invalid-id",
    "timestamp": "2026-01-01T10:00:00Z"
}
```

Validation errors include field-level detail:

```json
{
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "fieldErrors": {
        "email": "Email must be a valid email address",
        "password": "Password must be between 8 and 100 characters"
    },
    "path": "/v1/users",
    "timestamp": "2026-01-01T10:00:00Z"
}
```

### HTTP Status Codes

| Code | Meaning |
|---|---|
| 200 | Success |
| 201 | Resource created |
| 204 | Success - no content (DELETE) |
| 400 | Validation error |
| 401 | Missing or invalid token |
| 403 | Forbidden - accessing another user's resource |
| 404 | Resource not found |
| 409 | Conflict - duplicate email or deleting user with accounts |
| 422 | Business rule violation - insufficient funds |

---

## Architecture

```
com.bank.api
├── config/          <- Spring Security configuration
├── controller/      <- REST controllers (thin layer, no business logic)
├── domain/          <- JPA entities with business methods
├── dto/
│   ├── request/     <- Incoming request objects (validated)
│   └── response/    <- Outgoing response objects (safe subset of entity)
├── exception/       <- Custom exceptions + global error handler
├── repository/      <- Spring Data JPA interfaces
├── security/        <- JWT filter, UserDetailsService
└── service/         <- Business logic layer
```

### Key Design Decisions

**UUID Primary Keys**
All entities use UUID primary keys rather than sequential integers. This prevents enumeration attacks - an attacker cannot guess resource IDs by incrementing a counter.

**BigDecimal for Money**
All monetary values use BigDecimal with precision=19, scale=4. Using float or double is unsuitable for financial systems due to IEEE 754 binary floating point precision errors - 0.1 + 0.2 does not equal 0.3 in floating point arithmetic.

**Optimistic Locking on Account**
The Account entity uses @Version for optimistic locking. This prevents the lost-update concurrency bug where two simultaneous withdrawals both see sufficient funds and both succeed, resulting in a negative balance. The second commit detects the version mismatch and rolls back.

**Immutable Transactions**
Transaction entities have no setters and no updatedAt field. The transaction log is an append-only audit ledger. Each transaction records balanceAfter as a snapshot, enabling point-in-time balance reconstruction without replaying the full history.

**Rich Domain Model**
Business logic (deposit(), withdraw()) lives on the Account entity rather than in services. This ensures invariants cannot be bypassed by calling a different service method - you physically cannot modify the balance without going through the validation logic.

**JWT Authentication**
Stateless JWT Bearer token authentication. No server-side sessions. The token subject is the user UUID rather than email - email can change, UUID cannot.

**Defence in Depth on Authorisation**
Account access checks existence first (404 if not found) then ownership (403 if forbidden). This correctly distinguishes between a resource that does not exist and one that exists but the user cannot access.

**DTOs Separate from Entities**
Request and response DTOs are separate from JPA entities. This prevents accidental data leakage (passwords never appear in responses), protects against mass assignment attacks, and decouples the API contract from the database schema.

---

## Security Considerations

- Passwords hashed with BCrypt cost factor 12
- JWT signed with HMAC-SHA512
- Generic error messages on authentication failure - never reveals whether email exists or password was wrong
- Ownership verified at repository and service level on all account and transaction access
- Authorization failures return 403 Forbidden for existing resources and 404 Not Found for non-existent ones
- CSRF disabled - correct for stateless JWT APIs where Bearer tokens are never sent automatically by the browser

---

## Authorization Rules

| Scenario | Response |
|---|---|
| No token | 401 Unauthorized |
| Invalid or expired token | 401 Unauthorized |
| Accessing own resource | 200 OK |
| Accessing another user's resource | 403 Forbidden |
| Accessing non-existent resource | 404 Not Found |

---

## Testing

```
src/test/java/com/bank/api/
├── domain/
│   └── AccountTest.java                    <- 12 unit tests - domain logic
├── service/
│   ├── AuthServiceTest.java                <- 4 unit tests - registration rules
│   └── TransactionServiceTest.java         <- 7 unit tests - transaction rules
└── controller/
    └── AuthControllerIntegrationTest.java  <- 9 integration tests - full HTTP stack
```

Unit tests verify domain and service logic in isolation with no Spring context.
Integration tests load the full application context and verify the complete HTTP stack including security, validation, and database.

---

## Known Limitations and Production Differences

| Area | This Implementation | Production Difference |
|---|---|---|
| Database | H2 in-memory | PostgreSQL with HikariCP connection pooling |
| Schema management | JPA auto DDL | Flyway versioned migrations |
| JWT secret | application.yml | HashiCorp Vault / AWS Secrets Manager |
| Token revocation | Not implemented | Redis blacklist with TTL on logout |
| Concurrent withdrawal rejection | Returns 500 | Catch OptimisticLockException - return 409 Conflict |
| Pagination | Not implemented | All list endpoints paginated |
| Logging | Console | Structured JSON to Splunk/Datadog |
| Authentication | Local JWT | OAuth2 / enterprise SSO |
| Rate limiting | Not implemented | API Gateway (Kong / AWS API Gateway) |
| User deletion | Hard delete | Soft delete for GDPR and audit requirements |
| Role-based access control | Single ROLE_USER for all users | Multiple roles (CUSTOMER, TELLER, BRANCH_MANAGER, ADMIN) enforced via @PreAuthorize |
---

## H2 Console (Development Only)

An in-browser database console is available at:

```
http://localhost:8080/h2-console
```

| Field | Value |
|---|---|
| JDBC URL | jdbc:h2:mem:bankingdb |
| Username | sa |
| Password | leave blank |

This would be disabled in any production configuration.
