# TutorIn API

A backend REST API where tutors publish availability and students book open slots, with no double-booking.

**Track:** Backend Development
**Stack:** Java 17, Spring Boot, Spring Data JPA, PostgreSQL

## The problem

Booking a tutor usually happens over WhatsApp back-and-forth, so slots get double-booked, forgotten or confused. TutorIn gives tutors one place to publish their availability and students one place to book it.

## Core flow

1. A tutor creates an availability slot.
2. A student lists open slots (filter by tutor or availability).
3. The student books a slot, which creates a session.

## Entities

- **Tutor:** a user that offers services
- **Student:** a user that asks for the services
- **Subject:** a subject that can be taught
- **TutorSubject:** links tutors to the subjects they teach
- **AvailabilitySlot:** a time window a tutor offers
- **Session:** a booking of a slot by a student (BOOKED, COMPLETED, CANCELLED)

## Planned endpoints (Week 1: AvailabilitySlot CRUD)

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/slots` | Create a slot |
| GET | `/api/slots` | List slots (`?tutorId=`, `?available=true`) |
| GET | `/api/slots/{id}` | Get one slot |
| PUT | `/api/slots/{id}` | Update a slot |
| DELETE | `/api/slots/{id}` | Delete a slot |

The full API spec and ERD are in the [`docs`](docs) folder.

## How to run locally

### Prerequisites

- Java 17 or newer
- Maven (or the included `mvnw` wrapper)
- PostgreSQL running locally

### Steps

1. Clone the repo:
   ```
   git clone <your-repo-url>
   cd <repo-folder>
   ```
2. Create a database, for example `tutorin`.
3. Copy `.env.example` to `.env` and fill in your own values. Never commit `.env`.
4. Export the variables (or set them in your IDE run configuration):
   ```
   export DB_URL=jdbc:postgresql://localhost:5432/tutor_in
   export DB_USER=your_db_user
   export DB_PASSWORD=your_db_password
   export JWT_SECRET=your_long_random_secret
   ```
5. Start the app:
   ```
   ./mvnw spring-boot:run
   ```
6. The API runs at `http://localhost:8080`.

## Sample requests

Sample requests will be added in `docs/requests.http` as endpoints are built.

## Project structure

```
src/main/java/.../controller   REST controllers
src/main/java/.../service      Business logic
src/main/java/.../repository   Spring Data repositories
src/main/java/.../entity       JPA entities
src/main/java/.../dto          Request and response objects
src/main/java/.../config       System configuration(AppConfig, SecurityConfig)
docs/                          One-pager, work plan, ERD, API spec
```

## Roadmap

- **Week 1:** design the data model and API, build slot CRUD, seed data.
- **Week 2:** authentication and roles, relationships, validation, error handling.
- **Week 3:** tests, documentation, security notes, deployment.

## Live API

Not deployed yet. A link will be added here once it is live.
