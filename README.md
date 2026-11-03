# SecureBank — Banking Management System Backend

A robust, production-ready banking management backend built with **Java 17+**, **Spring Boot 3.3.4**, **Spring Data JPA**, **Spring Security (Stateless JWT)**, and **MySQL**.

This project implements core **Object-Oriented Programming (OOP)** principles (Abstraction, Inheritance, Polymorphism, and Encapsulation) alongside database concurrency safeguards (Pessimistic Locking & ACID transactions).

---

## 1. Architecture & Design Patterns

The project follows a standard multi-tiered enterprise architecture:

```
c:\Users\Vanitha\Desktop\Banking System\
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── securebank
    │   │           ├── SecureBankApplication.java
    │   │           ├── config/           # Spring Security, Password Encoders
    │   │           ├── controller/       # AuthController, AccountController, TransactionController
    │   │           ├── dto/              # Request / Response Data Transfer Objects
    │   │           ├── entity/           # User, Account (abstract), SavingsAccount, CurrentAccount, Transaction
    │   │           ├── exception/        # Custom Exceptions & GlobalExceptionHandler (@RestControllerAdvice)
    │   │           ├── repository/       # Spring Data JPA Repositories with Pessimistic Locking
    │   │           ├── security/         # JWT Provider, CustomUserDetailsService, JwtAuthenticationFilter
    │   │           └── service/          # AuthService, AccountService, TransactionService
    │   └── resources
    │       ├── application.properties    # Environment-driven database & JWT properties
    │       └── schema.sql                # Production MySQL DDL script
    └── test
        ├── java
        │   └── com
        │       └── securebank
        │           ├── domain/           # OOP Domain Unit Tests (AccountDomainTest)
        │           └── integration/      # End-to-end REST API Integration Tests (BankingSystemIntegrationTest)
        └── resources
            └── application-test.properties # In-memory H2 configuration for tests
```

---

## 2. Core OOP Concepts Demonstrated

### A. Abstraction
- [`Account`](file:///src/main/java/com/securebank/entity/Account.java) is an `abstract` class. It cannot be directly instantiated and defines the contract for bank accounts, including the abstract method `public abstract void withdraw(BigDecimal amount);` and `public abstract String displayDetails();`.

### B. Inheritance & Constructor Chaining (`super`)
- Both [`SavingsAccount`](file:///src/main/java/com/securebank/entity/SavingsAccount.java) and [`CurrentAccount`](file:///src/main/java/com/securebank/entity/CurrentAccount.java) extend [`Account`](file:///src/main/java/com/securebank/entity/Account.java).
- Subclass constructors invoke `super(accountNumber, user, accountHolderName, initialBalance, AccountType.SAVINGS)` to initialize common base fields.

### C. Polymorphism & Method Overriding
- **Polymorphic Execution:** When `account.withdraw(amount)` is called in [`TransactionService`](file:///src/main/java/com/securebank/service/TransactionService.java), the runtime dynamically dispatches to either `SavingsAccount.withdraw()` or `CurrentAccount.withdraw()` based on the underlying object type.
- **Overridden Methods:**
  - `SavingsAccount.withdraw(amount)` enforces that the resulting balance cannot fall below the minimum balance of **₹1,000**.
  - `CurrentAccount.withdraw(amount)` allows overdraft down to **-₹5,000**.
  - Both override `displayDetails()` to print subclass-specific terms.

### D. Encapsulation
- All state fields (`balance`, `accountNumber`, `passwordHash`, `pinHash`, etc.) are declared `private` or `protected`.
- Mutations to balances are strictly guarded via domain methods (`deposit()`, `withdraw()`, `applyYearlyInterest()`). There is no public `setBalance()`.

### E. Custom Domain Exceptions & Constants
- [`InsufficientFundsException`](file:///src/main/java/com/securebank/exception/InsufficientFundsException.java): Custom runtime exception providing the current balance, attempted amount, and exact shortfall.
- `public static final BigDecimal MINIMUM_BALANCE = new BigDecimal("1000.00");`
- `public static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("5000.00");`
- `public static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("0.04");`

---

## 3. Relational Database Design (JPA Joined Table Strategy)

The domain inheritance hierarchy maps cleanly to normalized relational tables via `@Inheritance(strategy = InheritanceType.JOINED)`:

1. **`users` table**: Primary user records, BCrypt hashed passwords and PINs, and failed login tracking.
2. **`accounts` table**: Holds common attributes (`id`, `account_number`, `user_id`, `balance`, `account_type`, `version`).
3. **`savings_accounts` table**: Foreign key referencing `accounts(id)`, holds `minimum_balance` and `interest_rate`.
4. **`current_accounts` table**: Foreign key referencing `accounts(id)`, holds `overdraft_limit`.
5. **`transactions` table**: Immutable financial audit log referencing `accounts(id)` with transaction type, balance after, counterparty account, and transfer reference.

---

## 4. REST API Documentation & Sample JSON

### Base URL: `http://localhost:8080`

### A. Authentication Endpoints

#### 1. Register User
- **Method / Endpoint**: `POST /api/auth/register`
- **Security**: Public
- **Request Body**:
```json
{
  "username": "alice_wonder",
  "email": "alice@example.com",
  "password": "Password@123",
  "pin": "1234",
  "fullName": "Alice Wonderland",
  "phoneNumber": "9876543210"
}
```
- **Response (201 Created)**:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "username": "alice_wonder",
  "role": "ROLE_CUSTOMER",
  "message": "User registered successfully"
}
```

#### 2. User Login
- **Method / Endpoint**: `POST /api/auth/login`
- **Security**: Public
- **Request Body**:
```json
{
  "username": "alice_wonder",
  "password": "Password@123"
}
```
- **Response (200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "username": "alice_wonder",
  "role": "ROLE_CUSTOMER",
  "message": "Login successful"
}
```

*Note on Security Policy: Failed login attempts increment on the server. On 3 consecutive wrong passwords, the account is locked (HTTP 423 Locked).*

---

### B. Account Endpoints (Requires `Authorization: Bearer <token>`)

#### 3. Open Account
- **Method / Endpoint**: `POST /api/accounts`
- **Request Body**:
```json
{
  "accountType": "SAVINGS",
  "initialDeposit": 2500.00,
  "pin": "1234"
}
```
- **Response (201 Created)**:
```json
{
  "id": 1,
  "accountNumber": "SB712391024",
  "accountHolderName": "Alice Wonderland",
  "accountType": "SAVINGS",
  "balance": 2500.00,
  "minimumBalance": 1000.00,
  "overdraftLimit": null,
  "interestRate": 0.0400,
  "details": "Account Type: SAVINGS | Account No: SB712391024 | Holder: Alice Wonderland | Balance: ₹2500.00 | Min Balance: ₹1000.00 | Interest Rate: 4%",
  "createdAt": "2026-10-18T13:40:00"
}
```

#### 4. List User Accounts
- **Method / Endpoint**: `GET /api/accounts`
- **Response (200 OK)**: List of `AccountResponse` objects owned by the authenticated caller.

#### 5. Get Account by ID
- **Method / Endpoint**: `GET /api/accounts/{id}`
- **Security**: Authenticated user must be the account owner (validated in service layer). Returns `403 Forbidden` if another user attempts access.

#### 6. Get Recent Transactions
- **Method / Endpoint**: `GET /api/accounts/{id}/transactions?limit=5`
- **Response (200 OK)**: Array of the latest 5 transactions.

#### 7. Get Account Statement
- **Method / Endpoint**: `GET /api/accounts/{id}/statement`

---

### C. Transaction Endpoints (Requires `Authorization: Bearer <token>`)

#### 8. Deposit Funds
- **Method / Endpoint**: `POST /api/accounts/{id}/deposits`
- **Request Body**:
```json
{
  "amount": 1500.00,
  "description": "Salary deposit"
}
```
- **Response (201 Created)**:
```json
{
  "id": 2,
  "accountId": 1,
  "accountNumber": "SB712391024",
  "transactionType": "DEPOSIT",
  "amount": 1500.00,
  "balanceAfter": 4000.00,
  "description": "Salary deposit",
  "transferReference": null,
  "counterpartyAccountNumber": null,
  "createdAt": "2026-10-18T13:42:00"
}
```

#### 9. Withdraw Funds
- **Method / Endpoint**: `POST /api/accounts/{id}/withdrawals`
- **Request Body**:
```json
{
  "amount": 500.00,
  "pin": "1234",
  "description": "ATM Withdrawal"
}
```
- **Response (201 Created)**: Returns `TransactionResponse` with updated `balanceAfter`.

#### 10. Inter-Account Fund Transfer
- **Method / Endpoint**: `POST /api/transfers`
- **Request Body**:
```json
{
  "sourceAccountNumber": "SB712391024",
  "destinationAccountNumber": "CA981273615",
  "amount": 1000.00,
  "pin": "1234",
  "description": "Tuition fee payment"
}
```
- **Response (201 Created)**:
```json
{
  "id": 4,
  "accountId": 1,
  "accountNumber": "SB712391024",
  "transactionType": "TRANSFER_DEBIT",
  "amount": 1000.00,
  "balanceAfter": 2500.00,
  "description": "Transfer to CA981273615 - Tuition fee payment",
  "transferReference": "TRF-8F21B30A",
  "counterpartyAccountNumber": "CA981273615",
  "createdAt": "2026-10-18T13:44:00"
}
```

---

## 5. Centralized Error Handling

Handled by [`GlobalExceptionHandler`](file:///src/main/java/com/securebank/exception/GlobalExceptionHandler.java):

```json
{
  "timestamp": "2026-10-18T13:45:00",
  "status": 400,
  "error": "Insufficient Funds",
  "message": "Savings account requires a minimum balance of ₹1000.00. Shortfall: ₹500.00",
  "path": "/api/accounts/1/withdrawals",
  "details": {
    "currentBalance": 3500.00,
    "attemptedAmount": 3000.00,
    "shortfall": 500.00
  }
}
```

---

## 6. How to Run and Test

### Prerequisites
- JDK 17+
- Maven 3.8+
- MySQL 8.0+ (for production/local run; tests run automatically on embedded H2)

### Run Tests
```powershell
mvn clean test
```
All 12 tests (8 domain OOP tests + 4 API/security integration tests) execute with zero failures.

### Configure MySQL & Run Application Locally
Create `securebank_db` using the supplied `src/main/resources/schema.sql` only for a new database. Do not run its table-creation statements against an existing database; Hibernate's local `update` setting preserves existing rows.

Set the connection details and a private JWT key in the shell that will run Maven:
```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="securebank_db"
$env:DB_USER="root"
$env:DB_PASSWORD="<your-local-mysql-password>"
$env:JWT_SECRET="<a-private-random-secret-at-least-32-bytes-long>"

mvn spring-boot:run
```

`DB_PASSWORD` and `JWT_SECRET` are required; do not commit their values. If the local MySQL account has no password, set `DB_PASSWORD` to an empty string. The backend binds to `PORT`, then `SERVER_PORT`, then `8080`. If port 8080 is already serving this application, reuse it rather than starting a duplicate; inspect any other port owner before stopping it.

### Production Deployment Configuration

The project has no platform-specific deployment files or selected hosting provider. Deploy the executable JAR to a Java 17-compatible service and provision a MySQL database first. For platforms exposing a `PORT` variable, the app binds to it automatically.

1. Create the production database and apply the existing schema through the database provider's safe migration/import process. Do not point production at local/test data.
2. Set `SPRING_PROFILES_ACTIVE=prod`.
3. Configure these environment variables in the hosting platform's secret/configuration manager:

   | Variable | Required value |
   |---|---|
   | `DB_HOST` | Database hostname |
   | `DB_PORT` | Database port (defaults to `3306`) |
   | `DB_NAME` | Existing database name |
   | `DB_USER` | Least-privilege database user |
   | `DB_PASSWORD` | Database password |
   | `DB_SSL_MODE` | MySQL Connector/J mode; production defaults to `VERIFY_IDENTITY` |
   | `JWT_SECRET` | Private random secret of at least 32 UTF-8 bytes |
   | `JWT_EXPIRATION_MS` | Optional token lifetime in milliseconds |
   | `CORS_ALLOWED_ORIGINS` | Comma-separated, exact HTTPS frontend origins, without paths |

   Example shapes only: `DB_HOST=<mysql-host>`, `JWT_SECRET=<private-random-32-byte-or-longer-value>`, `CORS_ALLOWED_ORIGINS=https://<your-frontend-host>`. Never use these placeholders literally. The database certificate and hostname must be valid for `VERIFY_IDENTITY`; configure the provider's recommended SSL mode if it uses a different trust setup.
4. Build and deploy with `mvn clean package`; run `java -jar target/banking-system-1.0.0-SNAPSHOT.jar`.
5. In `frontend/js/config.js`, set the API base to the deployed backend's HTTPS origin (or have the frontend host inject `window.SECUREBANK_API_BASE_URL` before the shared config is loaded). The default origins are local-development-only.

The `prod` profile requires DB credentials, a JWT secret, and CORS origins, validates rather than modifies the schema, disables SQL logging, and uses TLS identity verification by default. Local development keeps `ddl-auto=update`; production schema changes must be reviewed and applied separately. The backend does not serve the standalone frontend, so `/` is not a public login page: serve `frontend/` separately and open its `index.html`. Banking APIs remain authenticated.

### Deploy the Frontend to Vercel

The root `vercel.json` sets Vercel's output directory to the existing `frontend/` folder, so the deployed site root serves the existing login page and its CSS, JavaScript, and HTML pages. In Vercel, connect this repository with the repository root as the project root and redeploy; no Node.js build step is required.

This deploys only the static frontend. Vercel's Node.js runtime setting does not run the Spring Boot application or provide MySQL. The frontend's production API origin must be set to the HTTPS URL of a separately deployed Java backend in `frontend/js/config.js` (`window.SECUREBANK_API_BASE_URL` can override it when your hosting setup injects that value). Set the backend's `CORS_ALLOWED_ORIGINS` to the exact Vercel frontend origin. Until that backend URL is configured and the backend is deployed, the pages can load but login and banking API requests cannot succeed.
