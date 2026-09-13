# 🏦 CBII Bank Online NetBanking System (Full-Stack)

A complete, enterprise-ready **Full-Stack Online Banking System** built with **Spring Boot 3, Java 21, Spring Security, JWT, Spring Data JPA, Hibernate, MySQL, OpenAPI / Swagger**, and a responsive **HTML5/Bootstrap 5** frontend.

---

## 📑 Table of Contents

- [Overview &amp; Key Highlights](#-overview--key-highlights)
- [Tech Stack](#-tech-stack)
- [Architecture &amp; Design Patterns](#-architecture--design-patterns)
- [Database Schema](#-database-schema)
- [Security &amp; JWT Architecture](#-security--jwt-architecture)
- [Atomic Money Transfer &amp; Concurrency Safeguards](#-atomic-money-transfer--concurrency-safeguards)
- [API Documentation (REST Catalog)](#-api-documentation-rest-catalog)
- [Frontend User Interface](#-frontend-user-interface)
- [Demo Credentials](#-demo-credentials)
- [Setup &amp; Running Locally](#-setup--running-locally)
- [Docker &amp; Containerization](#-docker--containerization)
- [Running Automated Tests](#-running-automated-tests)
- [Technical Interview Talking Points](#-technical-interview-talking-points)

---

## 🌟 Overview &amp; Key Highlights

CBII Bank Online NetBanking System is designed with production-level banking requirements in mind:
- **Layered Architecture:** Clear boundary separation between Controller, Service, Repository, DTO, and Entity layers.
- **Stateless JWT Authentication &amp; RBAC:** Granular role-based authorization for `CUSTOMER`, `BANK_EMPLOYEE`, and `ADMIN`.
- **Atomic Fund Transfers:** Fully ACID-compliant transactions with **Pessimistic Database Locking (`PESSIMISTIC_WRITE`)** and deterministic resource lock ordering to prevent race conditions and deadlocks.
- **Retail Banking Workflows:** Multi-account management (Savings/Checking), payee/beneficiary directory with IFSC validation, loan application &amp; branch approval workflows.
- **Admin &amp; Operations Portal:** Full system oversight with metrics dashboard, account freeze/unfreeze controls, transaction audit trails, and loan decisions.
- **100% Automated Test Suite:** Unit and integration testing using JUnit 5, Mockito, and Spring Boot MockMvc.

---

## 🛠 Tech Stack

### Backend
- **Core:** Java 21 (LTS), Spring Boot 3.3.3
- **Security:** Spring Security 6, JWT (`jjwt` 0.12.6), BCrypt Password Hashing
- **Persistence &amp; ORM:** Spring Data JPA, Hibernate ORM 6.5
- **Database:** MySQL 8.0 (Production / Docker), H2 (In-memory testing)
- **Validation:** Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@DecimalMin`, etc.)
- **API Documentation:** SpringDoc OpenAPI 3 (Swagger UI)
- **Productivity:** Project Lombok
- **Testing:** JUnit 5, Mockito, Spring Boot Test, Spring Security Test

### Frontend
- **Structure &amp; Layout:** HTML5, CSS3, Bootstrap 5.3.3, Bootstrap Icons
- **Logic &amp; Communication:** Vanilla ES6+ JavaScript (Fetch API with centralized JWT interceptor)

### DevOps &amp; Containers
- **Containerization:** Multi-stage Dockerfile (Eclipse Temurin 21 JRE Alpine)
- **Orchestration:** Docker Compose (Spring Boot + MySQL 8.0)

---

## 🏗 Architecture &amp; Design Patterns

```
                                 [ Browser / Client ]
                                           |
                                  REST APIs / JSON
                                  (Bearer JWT Header)
                                           v
                             [ JwtAuthenticationFilter ]
                                           |
                       +-------------------+-------------------+
                       |                                       |
           [ Public Endpoints ]                     [ Protected Endpoints ]
     (/api/auth/**, /swagger-ui/**)              (SecurityFilterChain + RBAC)
                       |                                       |
                       +-------------------+-------------------+
                                           |
                                           v
                             [ Controller Layer (DTOs) ]
                                           |
                                           v
                        [ Service Layer (@Transactional) ]
                   - Pessimistic Locking on Transfer/Debit
                   - Business Validations & Rules
                                           |
                                           v
                          [ Repository Layer (JPA / SQL) ]
                                           |
                                           v
                              [ MySQL Database Engine ]
```

---

## 🗄 Database Schema

The database consists of 5 normalized tables with indexes, constraints, and foreign key relationships:

1. **`users`**:
   - `id` (PK, BIGINT AUTO_INCREMENT)
   - `name` (VARCHAR(100), NOT NULL)
   - `email` (VARCHAR(100), UNIQUE, NOT NULL, Indexed)
   - `password` (VARCHAR(255), NOT NULL, BCrypt hashed)
   - `phone` (VARCHAR(20))
   - `role` (VARCHAR(20), `CUSTOMER`, `BANK_EMPLOYEE`, `ADMIN`)
   - `created_at`, `updated_at` (TIMESTAMP)

2. **`accounts`**:
   - `id` (PK, BIGINT AUTO_INCREMENT)
   - `account_number` (VARCHAR(20), UNIQUE, NOT NULL, Indexed)
   - `account_type` (VARCHAR(20), `SAVINGS`, `CHECKING`)
   - `balance` (DECIMAL(15,2), NOT NULL)
   - `status` (VARCHAR(20), `ACTIVE`, `BLOCKED`, `CLOSED`)
   - `user_id` (FK -> `users.id`, NOT NULL)
   - `created_at` (TIMESTAMP)

3. **`transactions`**:
   - `id` (PK, BIGINT AUTO_INCREMENT)
   - `transaction_reference` (VARCHAR(64), UNIQUE, NOT NULL, Indexed)
   - `source_account_id` (FK -> `accounts.id`, NULL for cash deposit)
   - `target_account_id` (FK -> `accounts.id`, NULL for cash withdrawal)
   - `type` (VARCHAR(20), `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`)
   - `amount` (DECIMAL(15,2), NOT NULL)
   - `status` (VARCHAR(20), `SUCCESS`, `FAILED`, `PENDING`)
   - `description` (VARCHAR(255))
   - `timestamp` (TIMESTAMP, Indexed)

4. **`beneficiaries`**:
   - `id` (PK, BIGINT AUTO_INCREMENT)
   - `user_id` (FK -> `users.id`, NOT NULL)
   - `beneficiary_name` (VARCHAR(100), NOT NULL)
   - `account_number` (VARCHAR(20), NOT NULL)
   - `bank_name` (VARCHAR(100), NOT NULL)
   - `ifsc_code` (VARCHAR(20), NOT NULL)
   - `created_at` (TIMESTAMP)

5. **`loans`**:
   - `id` (PK, BIGINT AUTO_INCREMENT)
   - `user_id` (FK -> `users.id`, NOT NULL)
   - `amount` (DECIMAL(15,2), NOT NULL)
   - `interest_rate` (DECIMAL(5,2), NOT NULL)
   - `tenure_months` (INT, NOT NULL)
   - `status` (VARCHAR(20), `PENDING`, `APPROVED`, `REJECTED`)
   - `remarks` (VARCHAR(255))
   - `created_at`, `updated_at` (TIMESTAMP)

---

## 🔒 Security &amp; JWT Architecture

1. **Stateless Authentication:** Every protected endpoint requires `Authorization: Bearer <token>`.
2. **Password Security:** All passwords are encrypted with `BCryptPasswordEncoder` before database persistence.
3. **Role-Based Authorization:**
   - Public: `/api/auth/**`, Swagger UI
   - Customer &amp; Authenticated: `/api/customers/**`, `/api/accounts/**`, `/api/transactions/**`, `/api/beneficiaries/**`, `/api/loans/**`
   - Staff/Admin: `/api/admin/**`, `/api/loans/{id}/status`
4. **CORS:** Configured for cross-origin client consumption with proper allowed headers and methods.

---

## ⚡ Atomic Money Transfer &amp; Concurrency Safeguards

The fund transfer method (`TransactionService.transfer`) is engineered for banking reliability:
1. **Isolation Level:** `@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)` ensures complete rollback if any step fails.
2. **Pessimistic Write Locking (`LockModeType.PESSIMISTIC_WRITE`):** Employs `SELECT ... FOR UPDATE` on account rows so simultaneous transfers cannot create dirty reads or overdrafts.
3. **Deadlock Prevention:** Source and Target accounts are locked in a deterministic lexicographical order (sorting by account number) so concurrent mutual transfers between Account A and Account B never deadlock.
4. **Multi-point Validation:**
   - Verifies accounts exist and are distinct.
   - Verifies accounts are `ACTIVE` (not `BLOCKED` or `CLOSED`).
   - Verifies caller owns the source account.
   - Verifies source account has sufficient balance.

---

## 📡 API Documentation (REST Catalog)

Interactive documentation is available at **`http://localhost:8080/swagger-ui.html`**.

### 1. Authentication (`/api/auth`)
| Method | Endpoint | Description | Role |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register customer/staff &amp; get JWT | Public |
| `POST` | `/api/auth/login` | Authenticate &amp; return JWT token | Public |

### 2. Customer Management (`/api/customers`)
| Method | Endpoint | Description | Role |
|---|---|---|---|
| `GET` | `/api/customers/profile` | Get current user's profile | Authenticated |
| `PUT` | `/api/customers/profile` | Update user name &amp; phone | Authenticated |
| `PUT` | `/api/customers/change-password` | Change user password | Authenticated |
| `GET` | `/api/customers/accounts` | Get list of user's accounts | Authenticated |

### 3. Bank Accounts (`/api/accounts`)
| Method | Endpoint | Description | Role |
|---|---|---|---|
| `POST` | `/api/accounts` | Open a new Savings/Checking account | Authenticated |
| `GET` | `/api/accounts/{id}` | Get account details by ID | Owner / Admin |
| `GET` | `/api/accounts/{id}/balance` | Get account balance | Owner / Admin |
| `PUT` | `/api/accounts/{id}/close` | Close account (requires ₹0 balance) | Owner / Admin |

### 4. Transactions (`/api/transactions`)
| Method | Endpoint | Description | Role |
|---|---|---|---|
| `POST` | `/api/transactions/deposit` | Deposit funds into an account | Authenticated |
| `POST` | `/api/transactions/withdraw` | Withdraw funds from account | Owner / Admin |
| `POST` | `/api/transactions/transfer` | Atomic transfer between accounts | Owner / Admin |
| `GET` | `/api/transactions` | Paged transaction history | Authenticated |
| `GET` | `/api/transactions/{id}` | Get specific transaction details | Involved User / Admin |

### 5. Beneficiaries (`/api/beneficiaries`)
| Method | Endpoint | Description | Role |
|---|---|---|---|
| `POST` | `/api/beneficiaries` | Add a saved payee / beneficiary | Authenticated |
| `GET` | `/api/beneficiaries` | List all saved payees | Authenticated |
| `DELETE` | `/api/beneficiaries/{id}` | Remove a beneficiary | Owner |

### 6. Loans (`/api/loans`)
| Method | Endpoint | Description | Role |
|---|---|---|---|
| `POST` | `/api/loans/apply` | Apply for personal / retail loan | Authenticated |
| `GET` | `/api/loans/my-loans` | List user's loan applications | Authenticated |
| `GET` | `/api/loans/{id}` | Get loan application details | Owner / Admin |
| `PUT` | `/api/loans/{id}/status` | Approve or reject a loan | Admin / Employee |

### 7. Admin &amp; Operations (`/api/admin`)
| Method | Endpoint | Description | Role |
|---|---|---|---|
| `GET` | `/api/admin/stats` | Bank system summary metrics | Admin / Employee |
| `GET` | `/api/admin/customers` | Directory of all customers | Admin / Employee |
| `GET` | `/api/admin/accounts` | Directory of all bank accounts | Admin / Employee |
| `GET` | `/api/admin/loans` | Directory of all loan applications | Admin / Employee |
| `GET` | `/api/admin/transactions` | Global system transaction audit log | Admin / Employee |
| `PUT` | `/api/admin/accounts/{id}/block` | Freeze / Block bank account | Admin / Employee |
| `PUT` | `/api/admin/accounts/{id}/unblock` | Unfreeze / Activate bank account | Admin / Employee |
| `PUT` | `/api/admin/loans/{id}/approve` | Approve a pending loan | Admin / Employee |
| `PUT` | `/api/admin/loans/{id}/reject` | Reject a pending loan | Admin / Employee |

---

## 💻 Frontend User Interface

The frontend is located in `/frontend` and provides clean, responsive HTML5/Bootstrap 5 pages:
- **`index.html`**: Professional landing page with bank features and login links.
- **`login.html`**: Secure login with one-click demo credentials filling.
- **`register.html`**: Instant customer onboarding with automatic welcome account.
- **`dashboard.html`**: Financial overview, total net balance, quick deposit/withdraw widget, recent activity.
- **`accounts.html`**: Account cards, open new Savings/Checking account, close account.
- **`transfer.html`**: Transfer money with payee selector, manual account mode, real-time balance badge.
- **`transactions.html`**: Searchable and filterable transaction history with pagination and detail modal.
- **`beneficiaries.html`**: Payee directory with IFSC format validation.
- **`loans.html`**: Retail loan application with dynamic monthly EMI calculator.
- **`profile.html`**: Personal information and password change manager.
- **`admin.html`**: System KPIs, account freeze controls, global transaction audit, and loan approval decisions.

---

## 🔑 Demo Credentials

Pre-seeded in the database on first run:

| Role | Email | Password | Pre-configured Assets |
|---|---|---|---|
| **System Admin** | `devkmishra402@gmail.com` | `DevAnil@789974` | Full administrative privileges |
| **Bank Employee** | `employee@cbiibank.com` | `DevAnil@789974` | Account &amp; loan management privileges |
| **Customer 1 (Aarav Patel)** | `aarav.patel@gmail.com` | `Customer@123` | Savings: `100100200300` (₹1,50,000), Checking: `100100200301` (₹75,000) |
| **Customer 2 (Ananya Verma)** | `ananya.verma@gmail.com` | `Customer@123` | Savings: `200200300400` (₹85,000) |

---

## 🚀 Setup &amp; Running Locally

### Prerequisites
- **Java 21 LTS**
- **Maven 3.8+**
- **MySQL 8.0** (running locally on port 3306)

### 1. Database Setup
Create the MySQL database:
```sql
CREATE DATABASE IF NOT EXISTS bank_db;
```

### 2. Configure Credentials (Optional)
If your MySQL root password is not `root`, either edit `backend/src/main/resources/application.properties` or set environment variables:
```bash
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
```

### 3. Build &amp; Start the Backend
```bash
cd backend
mvn clean spring-boot:run
```
The server will start at `http://localhost:8080`.

### 4. Open the Frontend
Open `frontend/index.html` directly in your web browser, or serve it using any HTTP server:
```bash
# Using Python
python -m http.server 3000 --directory frontend

# Using Node / npx
npx serve frontend
```

---

## 🐳 Docker &amp; Containerization

Run the full system (Spring Boot + MySQL) with a single command:

```bash
docker-compose up --build
```

Docker Compose spins up:
1. `mysql-db`: MySQL 8.0 with persistent volume `mysql_bank_data` and healthcheck.
2. `banking-app`: Spring Boot container waiting for MySQL healthiness, running on port 8080.

---

## 🧪 Running Automated Tests

Run the complete test suite:

```bash
cd backend
mvn clean test
```

All 29+ unit and integration tests run in isolated in-memory H2 test scope.

---

## 🎯 Technical Interview Talking Points

1. **How do you prevent race conditions and dirty reads during simultaneous transfers?**
   - We use Spring's `@Transactional(isolation = Isolation.READ_COMMITTED)` paired with JPA Pessimistic Write locks (`@Lock(LockModeType.PESSIMISTIC_WRITE)`). This issues SQL `SELECT ... FOR UPDATE`, placing exclusive row-level locks on the sender and receiver accounts.

2. **How is deadlock prevented when two users transfer to each other simultaneously?**
   - We enforce deterministic lock acquisition ordering by sorting account numbers before acquiring locks (`sourceAcc.compareTo(targetAcc)`), guaranteeing both threads acquire locks in the exact same sequence.

3. **How does the JWT stateless architecture work?**
   - The client exchanges credentials at `/api/auth/login`. The server signs a cryptographically secure HS256 JWT containing `userId`, `roles`, and expiry. The `JwtAuthenticationFilter` intercepts requests, extracts the bearer token, validates the signature, and sets the Spring `SecurityContextHolder`.

4. **Why DTOs instead of exposing Entities?**
   - Prevents over-fetching and circular serialization issues.
   - Prevents Mass Assignment security vulnerabilities (attackers modifying roles or balances directly).
   - Allows strict Jakarta validation constraints on request models.
#   C B I I B a n k  
 