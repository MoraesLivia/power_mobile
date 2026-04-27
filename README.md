# CRM & Contract Signing API

Spring Boot application implementing a CRM flow integrated with a sequential contract signing process.

---

## Objective

This API allows:

- create clients
- create proposals with items
- send proposals to generate contracts
- sign or reject contracts with strict sequential order
- track all actions through an audit trail

---

## Implemented Flow

1. `POST /client` → creates a client
2. `POST /proposal` → creates a proposal linked to a client
3. `POST /proposal/{id}/send` → sends the proposal and generates a contract
4. `POST /contracts/{id}/sign?email=...` → signs in the defined order
5. `POST /contracts/{id}/reject?email=...` → rejects and cancels the contract
6. `GET /contracts/{id}` → retrieves contract status (`PENDING`, `SIGNED`, `REJECTED`)

---

## Architecture

The application follows a layered architecture:

- **Controller** → handles HTTP requests
- **Service** → contains business rules
- **Domain** → entities and core logic
- **Event Layer** → decouples proposal and contract flows

An event-driven approach is used to trigger contract creation from proposal submission.

---

## Technical Decisions

- Persistence using MySQL + JPA/Hibernate (`ddl-auto=update`)
- One contract per proposal
- Sequential signing enforced by `signingOrder`
- Audit trail stored in `contract_event` table
- Event abstraction via `EventPublisher`

---

## About RabbitMQ

The project is prepared for message broker integration (via `RabbitEventPublisher`), but RabbitMQ is not used at runtime in this delivery.

**Reason:** local environment limitations (Docker/WSL2 instability) prevented reliable broker execution during development.

**Mitigation:** `SimpleEventPublisher` is used as the active implementation (`@Primary`), ensuring the event-driven flow works in-process.

**Impact:** business functionality is fully preserved. Only the event transport changes (synchronous instead of broker-based).

---

## Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA
- MySQL
- Maven Wrapper (`mvnw.cmd`)

---

## Environment Setup

Use a `.env` file in the following format (no spaces around `=` and no quotes):

```dotenv
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/powermobile_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=your_db_user
SPRING_DATASOURCE_PASSWORD=your_db_password
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest
MYSQL_DATABASE=powermobile_db
MYSQL_ROOT_PASSWORD=your_mysql_root_password
```

`.env` is ignored by Git. Only `.env.example` should be versioned.

---

## Run Locally (without Docker)

Prerequisite: local MySQL running with database `powermobile_db`.

```powershell
Set-Location "C:\Dev\challenge\challenge"
.\mvnw.cmd spring-boot:run
```

API available at:  
`http://localhost:8080`

---

## Run with Docker (optional)

```powershell
Set-Location "C:\Dev\challenge\challenge"
docker compose up --build -d
docker compose ps
```

Logs:

```powershell
docker compose logs -f app
docker compose logs -f mysql
```

---

## Swagger / OpenAPI

With the application running, API documentation is available at:

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`

If the page does not load, confirm the app started successfully and dependency `springdoc-openapi-starter-webmvc-ui` is present in `pom.xml`.

OpenAPI metadata (title, description, version, contact) is configured in `src/main/java/com/powermobile/challenge/config/OpenApiConfig.java`.

---

## Quick Test

```powershell
$base = "http://localhost:8080"

# 1) Create client
$client = Invoke-RestMethod -Method Post -Uri "$base/client" -ContentType "application/json" -Body '{"clientName":"Client Name","clientEmail":"client@example.com"}'
$clientId = $client.id

# 2) Create proposal
$proposalBody = @{
  clientId = $clientId
  items = @(@{ itemName = "Item Name"; itemQuantity = 1; itemPrice = 100.00 })
} | ConvertTo-Json -Depth 10

$proposal = Invoke-RestMethod -Method Post -Uri "$base/proposal" -ContentType "application/json" -Body $proposalBody
$proposalId = $proposal.id

# 3) Send proposal (creates contract)
$signers = @(
  @{ email = "signer1@example.com"; signingOrder = 1 },
  @{ email = "signer2@example.com"; signingOrder = 2 }
) | ConvertTo-Json -Depth 10

Invoke-RestMethod -Method Post -Uri "$base/proposal/$proposalId/send" -ContentType "application/json" -Body $signers | Out-Null

# 4) Get latest contract
$contract = (Invoke-RestMethod -Method Get -Uri "$base/contracts" | Sort-Object id -Descending | Select-Object -First 1)
$contractId = $contract.id

# 5) Sequential signing
Invoke-RestMethod -Method Post -Uri "$base/contracts/$contractId/sign?email=signer1@example.com" | Out-Null
Invoke-RestMethod -Method Post -Uri "$base/contracts/$contractId/sign?email=signer2@example.com" | Out-Null

# 6) Final state
Invoke-RestMethod -Method Get -Uri "$base/contracts/$contractId" | ConvertTo-Json -Depth 10
```

Expected result: contract with `status = "SIGNED"`.

---

## Main Endpoints

- `POST /client`
- `GET /client`
- `GET /client/{id}`
- `POST /proposal`
- `GET /proposal`
- `GET /proposal/{id}`
- `POST /proposal/{id}/items`
- `POST /proposal/{id}/send`
- `GET /contracts`
- `GET /contracts/{id}`
- `POST /contracts/{id}/sign?email=...`
- `POST /contracts/{id}/reject?email=...`

---

## Audit

Audit events are stored in `contract_event`:

```sql
SELECT id, contract_id, type, description, created_at
FROM contract_event
ORDER BY id DESC;
```

---

## Testing

Tests are implemented using JUnit and Mockito.

Run from project root `C:\Dev\challenge\challenge`.

Run all tests:

```powershell
.\mvnw.cmd test
```

Run a single test class:

```powershell
.\mvnw.cmd -Dtest=ContractServiceTest test
```

Run a single test method:

```powershell
.\mvnw.cmd -Dtest=ContractServiceTest#sign_shouldMarkContractAsSignedWhenLastSignerSigns test
```

Expected output on success:
- `Tests run: X, Failures: 0, Errors: 0`
- `BUILD SUCCESS`

If a test fails, Maven prints:
- `BUILD FAILURE`
- the failing class/method and stack trace

Test reports are generated at:
- `target/surefire-reports`

---

## Troubleshooting

- Docker issues → check Docker Desktop / WSL2
- Database errors → verify datasource credentials
- `.env` format → must be `KEY=value` (no quotes, no spaces)