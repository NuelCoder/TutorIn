# TutorIn API

**Track:** Backend Development
**Stack:** Java 17, Spring Boot, Spring Data JPA, PostgreSQL

---

## 1. Idea One-Pager

**Target user:** Students looking for a tutor, and tutors offering sessions.
**Problem:** Booking a tutor usually happens over WhatsApp back-and-forth, so slots get double-booked, forgotten or confused.
**Solution:** TutorIn is a backend REST API that gives tutors one place to publish their availability and students one place to browse and book open slots safely, with strict checks to prevent double-booking.

**Core flow**

1. A tutor creates an availability slot.
2. A student lists open slots.
3. The student books a slot (creates a session).

**In scope**

- Tutor creates and manages availability slots.
- Student browses and books open slots.
- No double-booking or overlapping slots.
- Session status tracking (BOOKED, COMPLETED, CANCELLED).
- Admin moderation (subjects, reports, suspensions).

**Out of scope**

- Payments
- Real-time chat
- Video calls

**Top 3 risks**

1. Booking and overlap logic is trickier than plain CRUD.
2. The API is large.
3. Time zone handling for slot times.

---

## 2. Data Model

- **User:** a student account (role USER).
- **Tutor:** a tutor account, with a bio.
- **Admin:** an admin account that moderates the platform.
- **Subject:** an academic subject that can be taught (for example Calculus, Physics).
- **TutorSubject:** links a tutor to a subject they teach, with an hourly rate.
- **AvailabilitySlot:** a time window a tutor offers (managed with pessimistic locking).
- **Session:** a confirmed booking of a slot by a student.
- **Report:** a complaint filed by a student or tutor, reviewed by an admin.
- **SystemAuditLogs:** a record of admin actions.

---

## 3. API Specification

Key endpoints across the system. The full specification is in [`docs/api-spec`](docs/), and a ready-to-run request for every endpoint is in [`requests.http`](requests.http).

All times use ISO 8601 in UTC (for example `2026-12-01T14:00:00Z`). Except for the public endpoints, send the access token as `Authorization: Bearer <token>`.

| Area | Method | Endpoint | Who | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| Auth | POST | `/api/v1/auth/user-login` | Public | Log in as a student (`tutor-login` and `admin-login` for the other roles) |
| Auth | POST | `/api/v1/auth/logout` | Logged in | Revoke tokens and clear refresh cookies |
| Registration | POST | `/api/v1/users/register-user` | Public | Register a student and send an OTP (tutors use `/tutors/register-tutor`) |
| Registration | POST | `/api/v1/users/verify-user` | Public | Verify the OTP, create the account and log in |
| Subjects | POST | `/api/v1/subjects` | Admin | Add a subject |
| Subjects | GET | `/api/v1/subjects` | Logged in | List subjects |
| Tutor subjects | POST | `/api/v1/tutor-subjects` | Tutor | Register to teach a subject |
| Tutor subjects | GET | `/api/v1/tutor-subjects/search?title=` | Logged in | Find tutors by subject |
| Slots | POST | `/api/v1/slots` | Tutor | Create an availability slot (409 on overlap) |
| Slots | GET | `/api/v1/slots/tutor/{tutorId}` | Logged in | List a tutor's slots |
| Sessions | POST | `/api/v1/sessions` | Student | Book a slot (409 if already booked) |
| Sessions | GET | `/api/v1/sessions/student` | Student | List my upcoming sessions (`/sessions/tutor` for tutors) |
| Sessions | PUT | `/api/v1/sessions/{sessionId}/cancel` | Student or tutor | Cancel a session and free the slot |
| Reports | POST | `/api/v1/reports` | Student or tutor | Report a student or tutor |
| Reports | PUT | `/api/v1/reports/{reportId}/resolve` | Admin | Resolve or dismiss a report |
| Admin | PUT | `/api/v1/admins/users/suspend?userEmail=` | Admin | Suspend a student (`/tutors/suspend?tutorEmail=` for tutors) |
| Admin | POST | `/api/v1/admins/users/failover/registration-otp?userEmail=` | Admin | Generate an OTP by hand when email fails |
| Admin | GET | `/api/v1/admins/audit-logs` | Admin | List recent admin actions |

**Sample payload: book a session (`POST /api/v1/sessions`)**

```json
{
   "slotId": "b7a4f2d1-6e93-4c58-a1b0-3f9d8e5c2a74",
   "subjectId": "5e8a2b94-1c7d-4a36-b0f5-9d3c6e2a8b17"
}
```

---

## 4. How to Run Locally

### Prerequisites

- Java 17 or newer
- Maven (or the included `./mvnw` wrapper)
- PostgreSQL running locally on port 5432

### Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/NuelCoder/TutorIn.git
   cd TutorIn
   ```

2. **Create the database**

   ```sql
   CREATE DATABASE tutor_in;
   ```

3. **Configure environment variables.** Copy `.env.example` to `.env` (never commit your real `.env`) and fill in your values:

   ```bash
   cp .env.example .env
   ```

   ```properties
   DB_URL=jdbc:postgresql://localhost:5432/tutor_in
   DB_USER=your_postgres_username
   DB_PASSWORD=your_postgres_password
   ```

4. **Run the application.** It creates the database schema and seeds dummy data on startup.

   ```bash
   ./mvnw spring-boot:run
   ```

5. **Test the API.** The server runs at `http://localhost:8080`. Log in with `POST /api/v1/auth/tutor-login`, then call the endpoints with the returned token. Check the console on startup for any seeded IDs.

---

## 5. Testing

**Command:** `mvn clean test`

**What is tested**

- **Controllers (integration):** REST endpoints are tested with `MockMvc` and an H2 in-memory database. Tests check successful responses (200/201) and failures, including validation errors (400) and security rejections (401/403).
- **Services (unit):** Core business logic is tested in isolation with Mockito: slot overlap validation, OTP generation and verification rules, and role-based access checks.

**What is not tested**

- **Repositories:** Standard Spring Data JPA interfaces are not unit-tested. Custom queries are covered indirectly by the integration tests.
- **External services (Kafka, SMTP):** Message brokers and mail servers are mocked.

**Why:** This keeps the core business rules fast to test while still checking the HTTP and security layers, without depending on external networks or slow database I/O.

---

## 6. Security & Performance Notes

### 🔒 Security Implementation

- **Secrets Management:** All sensitive configuration data, database credentials, and cryptographic keys are strictly isolated from the source code and injected via environment variables.
- **Authentication & Session Security:** The API utilizes a stateless JWT architecture for authorization. To mitigate common web vulnerabilities (like XSS), long-lived refresh tokens are handled exclusively via securely configured, HttpOnly cookies.
- **Abuse Prevention:** Critical routes, particularly authentication and OTP generation endpoints, are protected by rate-limiting mechanisms backed by Redis to prevent brute-force attacks and email spam.
- **Data Integrity:** Strict input validation and sanitization are enforced at the controller layer to ensure all incoming payloads conform to expected constraints before reaching the core business logic.
- **CORS Policy:** Cross-Origin Resource Sharing (CORS) is explicitly configured to only accept requests from trusted frontend client domains.

### ⚡ Performance Optimization

- **Database Indexing:** Strategic database indexes have been applied to frequently queried fields, foreign keys, and filtering criteria. This eliminates slow full-table scans and ensures rapid data retrieval as the user base scales.
- **Query Optimization:** The data access layer uses optimized ORM fetching strategies (lazy loading) and connection pooling to minimize database load and memory consumption.
- **Scalable Data Delivery:** List endpoints are structured to support pagination, ensuring that large datasets (like system reports or available tutors) are delivered efficiently without overwhelming network bandwidth or client memory.

---

## 7. Local Infrastructure Setup

The project includes a complete containerized infrastructure stack using Docker Compose to ensure a seamless local development experience without installing complex dependencies directly on your host machine.

**Included services**

- **PostgreSQL:** Primary relational database.
- **Redis:** In-memory data store for caching and rate tracking.
- **MailHog:** Local SMTP testing server (captures all outbound emails safely).
- **Apache Kafka & Zookeeper:** Event broker for asynchronous messaging and workflows.

**Quick start**

1. Ensure Docker Desktop (or the Docker daemon) is running on your machine.
2. From the root of the project, start the background services in detached mode:

   ```bash
   docker-compose up -d
   ```

3. Run the Spring Boot application. It will automatically connect to these local containers.
4. **Tip:** You can view all generated OTPs and system emails in the MailHog web interface at `http://localhost:8025`.

## 8. Architecture, Key Decisions & Limitations

**System Architecture**
TutorIn is built as a modular monolithic Spring Boot application. It utilizes a layered architecture (Controller, Service, Repository) communicating with a PostgreSQL database. Redis is integrated for fast-access rate limiting (login attempts) and token management, while Apache Kafka handles asynchronous event-driven tasks like sending OTP emails to prevent blocking the main HTTP request threads.

**Key Technical Decisions**
1. **Hybrid JWT & HttpOnly Cookies:** To balance stateless API benefits with security, short-lived JWTs are returned in the response body for API access, while long-lived refresh tokens are stored securely in HttpOnly cookies to mitigate XSS attacks.
2. **Pessimistic Locking on Availability:** To guarantee that two students cannot double-book a tutor at the exact same millisecond, pessimistic locking is applied at the database level during the booking transaction.
3. **Failover Admin Controls:** Given the unreliability of email networks, manual OTP generation routes were built into the Admin panel to manually unblock users during registration or password resets.

**Current Limitations**
- **No Native Payments:** The system handles booking logistics but does not process financial transactions.
- **No Real-Time Chat:** Communication must happen off-platform; there are no WebSocket implementations for real-time messaging between students and tutors.

---
## Declaration

I declare that this project is entirely my own work.
- **Libraries used:** Spring Boot (Web, Data JPA, Security, Validation), PostgreSQL Driver, SpringDoc OpenAPI (Swagger), JUnit 5, Mockito, Bitnami Kafka, and Redis.
- **Boilerplates:** The project was initialized using Spring Initializr.
- **AI Use:** AI tools Gemini and Claude were used for planning workflow and documentation purposes