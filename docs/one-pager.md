# TutorIn
Track: Backend Development
Stack: Java, Spring Boot, PostgreSQL
Target user: Students looking for a tutor, and tutors offering sessions
Problem: Booking a tutor happens over WhatsApp back-and-forth, so slots get double-booked, forgotten or confused.
Entities: Tutor, Student, Subject, TutorSubject, AvailabilitySlot, Session
Core flow: Tutor creates availability slot -> Student lists open slots -> Student books a slot (creates a Session)

In scope:
- Tutor creates and manages availability slots
- Student browses and books open slots
- No double-booking or overlapping slots
- Session status (BOOKED, COMPLETED, CANCELLED)

Out of scope:
- Payments
- Real-time chat
- Video calls

Top 3 risks:
1. Booking and overlap logic is trickier than plain CRUD
2. Deploying with a database by Week 3
3. Time zone handling for slot times
