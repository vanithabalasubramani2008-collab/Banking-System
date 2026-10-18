# SecureBank Frontend Web Application

A clean, responsive, framework-free banking frontend built using **HTML5**, **CSS3**, and **Vanilla JavaScript (Fetch API)** designed to seamlessly integrate with the **SecureBank Spring Boot** backend and **MySQL** database.

---

## 1. Verified Backend API Integration Matrix

Every single frontend request and response strictly maps to the inspected Spring Boot backend:

| Frontend Feature | Actual Backend Endpoint | HTTP Method | Authentication Required | Request Body DTO | Response Body DTO |
|---|---|---|---|---|---|
| **User Registration** | `/api/auth/register` | `POST` | No (Public) | `RegisterRequest` (`username`, `email`, `password`, `pin`, `fullName`, `phoneNumber`) | `AuthResponse` (`token`, `tokenType`, `username`, `role`, `message`) |
| **User Login** | `/api/auth/login` | `POST` | No (Public) | `LoginRequest` (`username`, `password`) | `AuthResponse` (`token`, `tokenType`, `username`, `role`, `message`) |
| **Open Account** | `/api/accounts` | `POST` | Yes (`Bearer <token>`) | `CreateAccountRequest` (`accountType`, `initialDeposit`, `pin`) | `AccountResponse` (`id`, `accountNumber`, `accountHolderName`, `balance`, `minimumBalance`, `overdraftLimit`, `interestRate`, `details`, `createdAt`) |
| **List User Accounts** | `/api/accounts` | `GET` | Yes (`Bearer <token>`) | None | `List<AccountResponse>` |
| **Account Details** | `/api/accounts/{id}` | `GET` | Yes (`Bearer <token>`) | None | `AccountResponse` |
| **Recent Transactions** | `/api/accounts/{id}/transactions?limit=5` | `GET` | Yes (`Bearer <token>`) | None | `List<TransactionResponse>` |
| **Account Statement** | `/api/accounts/{id}/statement` | `GET` | Yes (`Bearer <token>`) | None | `AccountStatementResponse` (`account`, `recentTransactions`, `totalTransactionsCount`) |
| **Apply 4% Interest** | `/api/accounts/{id}/apply-interest` | `POST` | Yes (`Bearer <token>`) | None | `TransactionResponse` |
| **Deposit Funds** | `/api/accounts/{id}/deposits` | `POST` | Yes (`Bearer <token>`) | `DepositRequest` (`amount`, `description`) | `TransactionResponse` |
| **Withdraw Funds** | `/api/accounts/{id}/withdrawals` | `POST` | Yes (`Bearer <token>`) | `WithdrawalRequest` (`amount`, `pin`, `description`) | `TransactionResponse` |
| **Transfer Funds** | `/api/transfers` | `POST` | Yes (`Bearer <token>`) | `TransferRequest` (`sourceAccountNumber`, `destinationAccountNumber`, `amount`, `pin`, `description`) | `TransactionResponse` |

---

## 2. Directory Structure

```
frontend/
├── index.html              # Secure portal login page with client-side checks & show/hide pwd
├── dashboard.html          # Overview dashboard, liquid balance, account cards & recent txs
├── accounts.html           # Open new Savings or Current account with PIN authorization
├── deposit.html            # Deposit money with live account picker and positive check
├── withdraw.html           # Withdraw money with PIN authorization and rule validation
├── transfer.html           # Inter-account transfer form with two-step review modal
├── transactions.html       # Statement viewer with filter by Deposit/Withdrawal/Transfer
├── account-details.html    # Detailed account view, OOP rule summaries & apply-interest trigger
├── css/
│   └── style.css           # Responsive navy/slate design system, cards, tables, modal & alerts
├── js/
│   ├── config.js           # API_BASE_URL (http://localhost:8080) & DEMO_MODE toggle
│   ├── api.js              # Reusable Fetch API client, Bearer headers & error parser
│   ├── auth.js             # Login submission and 3-attempt lock handling
│   ├── navigation.js       # Session authentication guard and sidebar controller
│   ├── dashboard.js        # Dashboard metric aggregation and activity loading
│   ├── accounts.js         # Account opening form validation and submission
│   ├── operations.js       # Atomic deposits, withdrawals, and transfer confirmation modal
│   ├── transactions.js     # Transaction ledger and dynamic type filtering
│   └── account-details.js  # Account metadata and 4% interest credit trigger
└── README.md
```

---

## 3. How to Run Frontend & Backend

### Step 1: Start the Spring Boot Backend
From the root directory:
```powershell
mvn spring-boot:run
```
The backend starts on `http://localhost:8080`.

### Step 2: Serve the Frontend
You can serve the `frontend/` directory using any local HTTP static server or simply open `index.html` directly in your browser.

Using Python (built into Windows/macOS/Linux):
```powershell
cd frontend
python -m http.server 3000
```
Open your browser at:
```
http://localhost:3000
```

Alternatively, open `file:///C:/Users/Vanitha/Desktop/Banking%20System/frontend/index.html` in Chrome or Edge.

---

## 4. Normal Mode vs. Explicit Demo Mode

- **Normal Mode (Default)**: `CONFIG.DEMO_MODE = false` in `js/config.js`. The application communicates directly with the live Spring Boot REST APIs and displays genuine database responses from MySQL.
- **Explicit Demo Mode**: Set `CONFIG.DEMO_MODE = true` in `js/config.js` to visibly flag sample simulation mode. A banner appears at the top: `DEMO MODE — Sample Data Active`.

---

## 5. Security & Authentication Model

- **Stateless JWT Tokens**: Saved in browser `sessionStorage` under `securebank_token` upon successful login.
- **Never Persisted Secrets**: Raw passwords and transaction PINs are **never stored** in `localStorage`, `sessionStorage`, or cookies. They are transmitted solely inside the payload of authorized HTTPS/HTTP POST requests.
- **Failed Login Lockout**: The backend locks the user account after 3 consecutive wrong passwords (`HTTP 423 Locked`). The frontend detects this and advises contacting bank administration.
- **Session Expiration Guard**: If an API returns `HTTP 401 Unauthorized`, `api.js` automatically purges the token and redirects to `index.html?error=session_expired`.
