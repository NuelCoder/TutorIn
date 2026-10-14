# TutorIn API

**Track:** Backend Development  
**Stack:** Java 17, Spring Boot, Spring Data JPA, PostgreSQL

## 1. Idea One-Pager

**Target User:** Students looking for a tutor, and tutors offering sessions.  
**Problem:** Booking a tutor usually happens over WhatsApp back-and-forth, resulting in slots getting double-booked, forgotten, or confused.  
**Solution:** TutorIn is a backend REST API that gives tutors one centralized place to publish their availability and students one place to browse and book open slots safely, with strict database constraints to prevent double-booking.

**Core Flow:**
1. A tutor creates an availability slot.
2. A student lists open slots.
3. The student books a slot (creates a Session).

**In Scope:**
* Tutor creates and manages availability slots.
* Student browses and books open slots.
* No double-booking or overlapping slots.
* Session status tracking (BOOKED, COMPLETED, CANCELLED).
* Admin moderation (subjects, reports, suspensions).

**Out of Scope:**
* Payments.
* Real-time chat.
* Video calls.

**Top 3 Risks:**
1. Booking and overlap logic is trickier than plain CRUD.
2. The API is large
3. Time zone handling for slot times.

---

## 2. Data Model

The system consists of the following core entities:
* **User:** Base authentication entity for anyone logging in (Roles: STUDENT, TUTOR, ADMIN).
* **Tutor:** Profile data for users with the TUTOR role (includes bio).
* **Student:** Profile data for users learning on the platform.
* **Subject:** Academic subjects that can be taught (e.g., Calculus, Physics).
* **TutorSubject:** Join table linking tutors to subjects, including their specific hourly rate.
* **AvailabilitySlot:** A time window a tutor offers (managed with pessimistic locking).
* **Session:** A confirmed booking of a slot by a student.
* **Report:** A moderation flag created by users for Admin review.

---

## 3. API Specification (Availability Slots)

All times must be submitted in ISO-8601 UTC format (e.g., `2026-12-01T14:00:00Z`).

| Method | Endpoint | Purpose | Status Codes |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/slots` | Create a new availability slot | 201 Created, 400 Bad Request, 404 Not Found |
| **GET** | `/api/v1/slots/tutor/{tutorId}` | List all slots for a specific tutor | 200 OK, 404 Not Found |
| **GET** | `/api/v1/slots/{id}` | Get details of a specific slot | 200 OK, 404 Not Found |
| **PUT** | `/api/v1/slots/{id}` | Update an unbooked slot's time range | 200 OK, 400 Bad Request, 404 Not Found |
| **DELETE** | `/api/v1/slots/{id}` | Delete an unbooked slot | 204 No Content, 400 Bad Request, 404 Not Found |

### Sample Payload: Create Slot (`POST /api/v1/slots`)

```json
{
  "tutorId": "fc86d921-d93a-49b5-b21e-96016eb154a3",
  "startTime": "2026-12-01T14:00:00Z",
  "endTime": "2026-12-01T16:00:00Z"
}
```

---

## 4. How to Run Locally

### Prerequisites

* Java 17 or newer installed
* Maven (or use the included `./mvnw` wrapper)
* PostgreSQL running locally on port 5432

### Setup Steps

1. **Clone the repository:**

   ```bash
   git clone https://github.com/NuelCoder/TutorIn.git
   cd TutorIn
   ```

2. **Create the database:**
   Open your PostgreSQL terminal or pgAdmin and run:

   ```sql
   CREATE DATABASE tutor_in;
   ```

3. **Configure environment variables:**
   Copy the `.env.example` file to create your local `.env` file. *(Never commit your actual `.env` file).*

   ```bash
   cp .env.example .env
   ```

   Add your database credentials to the `.env` file:

   ```properties
   DB_URL=jdbc:postgresql://localhost:5432/tutor_in
   DB_USER=your_postgres_username
   DB_PASSWORD=your_postgres_password
   ```

4. **Run the application:**
   The application will automatically generate the database schema and seed dummy data on startup.

   ```bash
   ./mvnw spring-boot:run
   ```

5. **Test the API:**
   The server runs at `http://localhost:8080`. Check the console logs on startup for the generated dummy `tutorId` to use in your API tests.