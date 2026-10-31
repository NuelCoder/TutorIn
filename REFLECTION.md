# Technical Reflection

**What changed over three weeks?**
The project began as a standard CRUD API but rapidly evolved in complexity. I transitioned from basic synchronous processing to integrating Redis for rate-limiting and Apache Kafka for asynchronous email delivery. Security requirements pushed me to implement a much stricter authentication flow, separating access tokens from refresh tokens entirely.

**What remains risky?**
Operating distributed infrastructure (Kafka and Zookeeper) adds significant operational overhead. In a production environment, if the Kafka broker goes down, the email delivery system fails silently unless robust dead-letter queues and monitoring alerts are properly configured. Additionally, managing timezone conversions between server time (UTC) and local client time remains a persistent edge case for calendar systems.

**What I would do next?**
If given another month, I would integrate Stripe's API to handle session payments and hold funds in escrow until the session is completed. I would also introduce WebSockets via Spring Integration to build a real-time messaging interface so students and tutors can communicate directly within the platform.