# Banking REST API

A RESTful banking API built with Java 17 and Spring Boot 3, implementing user management, bank accounts, and financial transactions with JWT authentication.

---

## Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
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

- Java 17 JDK ([Eclipse Temurin](https://adoptium.net/) recommended)
- Maven 3.x (or use the included `mvnw` wrapper)

### Running the Application

```bash
# Clone the repository
git clone <repository-url>
cd banking-api

# Run with Maven wrapper (recommended)
./mvnw spring-boot:run        # macOS/Linux
.\mvnw spring-boot:run        # Windows PowerShell

# Or build and run the JAR
./mvnw clean package
java -jar target/banking-api-0.0.1-SNAPSHOT.jar
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

All endpoints are prefixed with `/api/v1`.

### Authentication (Public)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/auth/register` | Register a new user |
| `POST` | `/auth/login` | Login and receive JWT |

### Users (Authenticated)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/users/me` | Get current user profile |

### Accounts (Authenticated)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/accounts` | Create a new account |
| `GET` | `/accounts` | List all accounts |
| `GET` | `/accounts/{id}` | Get account by ID |

### Transactions (Authenticated)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/accounts/{id}/transactions/deposit` | Deposit money |
| `POST` | `/accounts/{id}/transactions/withdraw` | Withdraw money |
| `GET` | `/accounts/{id}/transactions` | List transactions |
| `GET` | `/accounts/{id}/transactions/{txId}` | Get transaction by ID |

---

## Authentication

All endpoints except `/auth/register` and `/auth/login` require a JWT Bearer token.

Include the token in every request header: