# TutorIn Work Plan

Track: Backend Development
Stack: Java, Spring Boot, PostgreSQL

## Week 1: Design and build

- Idea one-pager, work plan, public repo with README and `.env.example`
- Task 1.1: data model (ERD) and REST API spec in `/docs`
- Task 1.2: project setup, database migrations, AvailabilitySlot CRUD (create, list, get one, update, delete) with validation and correct status codes (201, 200, 400, 404)
- Seed data and sample requests
- 5-minute presentation to mentors, then note the feedback
- Tag `week-1`

## Week 2: Auth and robustness

- Task 2.1: registration and login with hashed passwords and JWT; roles (TUTOR, STUDENT) and ownership rules (tutors manage only their own slots, students cancel only their own sessions); 401 and 403 responses
- Task 2.2: remaining entities and relationships (Subject, TutorSubject, Session), booking flow, overlap and double-booking prevention, validation, consistent error format
- Respond to Week 1 mentor feedback
- Keep an issue log
- Tag `week-2`

## Week 3: Quality and delivery

- Task 3.1: unit and integration tests for the booking rules and the endpoints; tests run with one command
- Task 3.2: API documentation (Swagger or OpenAPI), setup guide, security and performance notes, deployment with a live API
- Final package: walkthrough video, reflection, declaration
- Final checklist: no secrets in the repo or history
- Tag `week-3`

## Top 3 risks

| Risk | Why it matters | Mitigation |
| --- | --- | --- |
| Booking and overlap logic is harder than plain CRUD | Double-booking is the core promise of the product | Design the rules in Week 1, implement them in Week 2, and write tests for them in Week 3 |
| Deploying with a database by Week 3 | Free hosts can be slow or restrictive and deployment problems eat time | Pick the host and database early in Week 2, and deploy a basic version before Week 3 starts |
| Time zone handling for slot times | Wrong time zones lead to wrong bookings | Store all times in UTC, return ISO 8601 timestamps, and document the convention |

## Scope control

If time runs short, cut Subject filtering and the session COMPLETED status first. Slots, booking, auth and tests stay.
