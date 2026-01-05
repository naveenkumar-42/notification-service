# Notification Service

A Spring Boot microservice that manages notifications (Email, SMS, Push) with RabbitMQ-based queuing, retry logic, scheduled notifications, and auditing.

This README documents the project's main features, configuration keys, available endpoints, important classes and functions, how to run the service, and how to test it.

---

## Table of Contents

- Project Overview
- Features
- Tech Stack
- Prerequisites
- Configuration
- Build & Run
- HTTP Endpoints
- Important Classes & Functions
- Database Schema (entities)
- RabbitMQ / Messaging
- Email / SMS (Twilio) Setup
- Testing
- Next Steps / Improvements

---

## Project Overview

This service accepts notification requests (EMAIL, SMS, PUSH), persists events, routes them to a RabbitMQ queue for processing, delivers notifications via channel-specific senders, supports retries and dead-lettering, and maintains audit logs.

It also supports scheduling notifications (send at a future time) and provides filtering endpoints for a frontend to query history and status.


## Features

- REST API to submit notifications and query history/status
- Priority queuing using RabbitMQ (priority 0-10)
- Scheduled notifications processor (runs every 60 seconds)
- Retry logic with a dead-letter queue (DLQ) after max retries
- Audit logging for lifecycle events
- Email (HTML + simple) and SMS (Twilio) senders (Email uses Spring Mail)


## Tech Stack

- Java 17+ (project uses modern Java syntax)
- Spring Boot (Web, Data JPA, AMQP, Scheduling)
- RabbitMQ
- MySQL / MariaDB (configured via application.properties)
- Maven


## Prerequisites

- Java 17 or later
- Maven 3.6+
- RabbitMQ running and accessible
- MySQL/MariaDB database
- Optional: Twilio account for SMS and SMTP credentials for email


## Configuration

Primary configuration is in `src/main/resources/application.properties`. For production, prefer environment variables or an externalized config store.

Important property keys (and explanation):

- Server
  - server.port - HTTP port (default 8080)

- Datasource
  - spring.datasource.url - JDBC URL for MySQL
  - spring.datasource.username
  - spring.datasource.password

- JPA
  - spring.jpa.hibernate.ddl-auto - e.g. update (used in development)

- RabbitMQ
  - spring.rabbitmq.host
  - spring.rabbitmq.port
  - spring.rabbitmq.username
  - spring.rabbitmq.password
  - spring.rabbitmq.virtual-host
  - spring.rabbitmq.requested-heartbeat
  - spring.rabbitmq.connection-timeout

- Email (Spring Mail)
  - spring.mail.host (smtp.gmail.com)
  - spring.mail.port
  - spring.mail.username
  - spring.mail.password
  - spring.mail.properties.mail.smtp.auth
  - spring.mail.properties.mail.smtp.starttls.enable

- Notification sender config (prefix: `notification.sender`)
  - notification.sender.email.username
  - notification.sender.email.password
  - notification.sender.email.from-address
  - notification.sender.email.reply-to

  - notification.sender.sms.account-sid   (Twilio)
  - notification.sender.sms.auth-token    (Twilio)
  - notification.sender.sms.from-number  (Twilio)

Security note: Do NOT commit secrets (SMTP password, Twilio tokens) into VCS; use environment variables or a secrets manager.


## Build & Run

1. Build:

   mvn clean package

2. Run (development):

   mvn spring-boot:run

3. Run the packaged JAR:

   java -jar target/notification-service-*.jar


## HTTP Endpoints

Base URL: http://localhost:8080

- POST /api/notifications/send
  - Description: Submit a notification request. Accepts JSON body `NotificationRequest`.
  - Body example:
    {
      "notificationType": "WELCOME_EMAIL",
      "recipient": "user@example.com",
      "subject": "Welcome!",
      "message": "Hello there",
      "scheduledTime": "2025-12-19T11:45"  // optional for scheduling
    }
  - Response: `NotificationResponse` with eventId and status (QUEUED or SCHEDULED)

- GET /api/notifications/status/{eventId}
  - Description: Get event details for a notification event
  - Response: `NotificationEvent` (entity JSON)

- GET /api/notifications/history
  - Description: Returns all notification events (history)

- GET /api/notifications/status-filter/{status}
  - Description: Get events filtered by status (case-insensitive)

- GET /api/notifications/priority-filter/{priority}
  - Description: Get events filtered by priority (CRITICAL, HIGH, MEDIUM, LOW)

- GET /api/notifications/channel-filter/{channel}
  - Description: Get events filtered by channel (EMAIL, SMS, PUSH)

- GET /api/notifications/filter?status=&priority=&channel=&dateRange=
  - Description: Combined filter endpoint; `dateRange` accepts: `24h`, `7d`, `30d`, or `all`

- GET /api/notifications/health
  - Description: Health check — returns a simple text message

Test endpoints (dev/test controller): Base path `/api/test`

- GET /api/test/email/simple - sends a simple text email to a configured address (for testing)
- GET /api/test/email/html - sends an HTML email (test)
- POST /api/test/email/custom - send a custom email; body uses `NotificationRequest` DTO
- GET /api/test/sms/config - returns whether Twilio credentials are configured
- GET /api/test/sms/instructions - textual instructions for Twilio setup
- GET /api/test/health - service health
- GET /api/test/endpoints - list of available test endpoints


## Important Classes & Functions

I'll list the major components and the key public methods to help you navigate the codebase.

- Controllers
  - `com.notification.controller.NotificationController`
    - sendNotification(NotificationRequest request) - POST /api/notifications/send
    - getStatus(Long eventId) - GET /api/notifications/status/{eventId}
    - getHistory() - GET /api/notifications/history
    - getByStatus(String status) - GET /api/notifications/status-filter/{status}
    - getByPriority(String priority) - GET /api/notifications/priority-filter/{priority}
    - getByChannel(String channel) - GET /api/notifications/channel-filter/{channel}
    - getFilteredEvents(...) - GET /api/notifications/filter
    - health() - GET /api/notifications/health

  - `com.notification.controller.NotificationTestController`
    - testSimpleEmail() - GET /api/test/email/simple
    - testHtmlEmail() - GET /api/test/email/html
    - testCustomEmail(NotificationRequest request) - POST /api/test/email/custom
    - testSmsConfig() - GET /api/test/sms/config
    - getSmsInstructions() - GET /api/test/sms/instructions
    - healthCheck() - GET /api/test/health
    - getTestEndpoints() - GET /api/test/endpoints

- Services
  - `com.notification.service.NotificationService`
    - sendNotification(NotificationRequest request) - validates rule, saves event, schedules or queues message
    - getEventsByStatus(String status)
    - getEventsByPriority(String priority)
    - getEventsByChannel(String channel)
    - getFilteredEvents(String status, String priority, String channel, String dateRange)
    - getEventStatus(Long eventId)
    - getAllEvents()

  - `com.notification.service.NotificationProducer`
    - sendNotification(NotificationEvent event) - publishes message to exchange/routing key
    - sendToRetryQueue(NotificationEvent event) - republish event for retry

  - `com.notification.service.NotificationConsumer`
    - consumeNotification(NotificationEvent event) - Rabbit listener processing main queue
    - handleFailure(NotificationEvent event, Exception e) - retry or DLQ logic

  - `com.notification.service.DLQConsumer`
    - processDLQ(NotificationEvent event) - handles messages from DLQ

  - `com.notification.service.NotificationDeliveryService`
    - deliver(NotificationEvent event) - routes to Email/SMS/Push
    - deliverEmail(NotificationEvent event)
    - deliverSms(NotificationEvent event)
    - deliverPush(NotificationEvent event)

  - `com.notification.service.ScheduledNotificationProcessor`
    - processScheduledNotifications() - Scheduled task that moves due scheduled events to queue

- Senders & Config
  - `com.notification.service.sender.SenderConfig` - configuration properties bound from `notification.sender.*`
  - Email sender (not present as a separate file in analyzed sources) - the project uses Spring's JavaMailSender via `NotificationTestController` for tests and may include a dedicated `EmailSender` class referenced by `NotificationDeliveryService` (check /service/sender package for additional files)

- Config
  - `com.notification.config.RabbitMQConfig` - exchange/queue/bindings and JSON message converter

- Repositories / Entities
  - Entities: `NotificationEvent`, `NotificationRule`, `AuditLog`
  - Repositories: `NotificationEventRepository`, `NotificationRuleRepository`, `AuditLogRepository`


## Database Schema (entities)

- notification_event table (entity: NotificationEvent)
  - id, recipient, message, channel, priority, notificationType, subject, status,
    retryCount, failureReason, createdAt, updatedAt, scheduledAt

- notification_rule table (entity: NotificationRule)
  - id, notificationType, channel, priority, retryLimit, isActive, createdAt, updatedAt

- audit_log table (entity: AuditLog)
  - id, eventId, action, details, timestamp


## RabbitMQ / Messaging

- Exchange: `notification.exchange`
- Main queue: `notification.queue` (x-dead-letter-exchange/ x-dead-letter-routing-key set to DLQ)
- DLQ: `notification.dlq`
- Routing keys: `notification.send` (main), `notification.dlq`

The project uses a JSON message converter and attaches priority headers to messages (0-10). The consumer listens on `notification.queue` and DLQ consumer listens on `notification.dlq`.


## Email / SMS (Twilio) Setup

- Email: Uses Spring Mail (`spring.mail.*` properties). For Gmail SMTP, ensure you create an app password or enable proper access; prefer environment variables.

- Twilio (SMS): configure `notification.sender.sms.account-sid`, `notification.sender.sms.auth-token`, and `notification.sender.sms.from-number`.

Security note: The repository currently contains example values in `application.properties` — remove them and use environment variables or CI/CD secrets.


## Testing

- Unit / Integration tests can be run via Maven:

  mvn test

- Manual tests (curl examples):

  - Send a notification (EMAIL):

    curl -X POST http://localhost:8080/api/notifications/send \
      -H "Content-Type: application/json" \
      -d '{"notificationType":"WELCOME_EMAIL","recipient":"user@example.com","subject":"Hi","message":"Hello"}'

  - Get history:

    curl http://localhost:8080/api/notifications/history

  - Use test endpoints for emails:

    curl http://localhost:8080/api/test/email/simple


## Next Steps / Improvements

- Add proper EmailSender implementation and unit tests for it.
- Externalize configuration (Spring Cloud Config / Vault) to avoid secrets in properties.
- Add metrics (Prometheus) and health checks endpoints (actuator).
- Add more robust validation and better error handling for scheduled times and recipient formats.
- Add DTOs and mappers to decouple entities from API responses.


---

If you'd like, I can commit this README to the repo (already created), redact the secrets from `application.properties`, or generate a simplified environment variable `.env` example file and a quick start script. Tell me which of these you'd like next.
