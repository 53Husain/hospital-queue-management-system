# Smart Hospital Queue Management System

A Java backend and Bootstrap frontend demonstrating patient registration, doctor queues, appointments, and admin dashboard statistics. Built with **Java 21, Spring Boot 3.5.4, Spring Data JPA, and MySQL**.

**Status:** a learning/demo application with BCrypt password hashing, server-side session authentication, CSRF protection, and role/record-ownership checks. These controls do not make this a production healthcare system. Use synthetic patient data; operational security, privacy requirements, and deployment hardening need further work.

## Implemented Capabilities

- Public patient registration; only authenticated administrators can create doctor/admin accounts.
- Login establishes an HTTP session with an HttpOnly cookie. Logout invalidates the session.
- Patients can list doctors and book/read appointments or update profiles belonging to themselves.
- Doctors can view and operate their own queue, and read/complete appointments assigned to them.
- Administrators can view aggregate counts and provision staff. They do not automatically receive access to patient profiles or doctor queue operations.
- Priority ordering: EMERGENCY before NORMAL, then ascending token number.
- Doctor dashboard refresh through HTTP polling every five seconds.
- Static HTML/CSS/JavaScript pages served by Spring Boot, using same-origin API URLs.

## Architecture and Source Guide

Requests pass through Spring Security filters, controller ownership checks, services, JPA repositories, and the database.

| Source | Responsibility |
| --- | --- |
| [HospitalQueueApplication.java](src/main/java/com/shqms/HospitalQueueApplication.java) | Spring Boot entry point |
| [SecurityConfig.java](src/main/java/com/shqms/config/SecurityConfig.java) | API authentication, admin restriction, session persistence, CSRF and logout |
| [RecordAccess.java](src/main/java/com/shqms/config/RecordAccess.java) | Database-backed patient/doctor ownership checks |
| [AdminBootstrap.java](src/main/java/com/shqms/config/AdminBootstrap.java) | Optional creation of the first administrator |
| [controller/](src/main/java/com/shqms/controller) | HTTP routes and method authorization |
| [service/impl/](src/main/java/com/shqms/service/impl) | Registration, appointments, queues, profiles, and dashboard logic |
| [repository/](src/main/java/com/shqms/repository) | Database access |
| [entity/](src/main/java/com/shqms/entity) | User, Patient, Doctor, Hospital, and Appointment entities |
| [QueueManager.java](src/main/java/com/shqms/dsa/queue/QueueManager.java) | PriorityQueue and appointment comparator |
| [api.js](src/main/resources/static/js/api.js) | Session-aware requests, CSRF headers, logout and HTML escaping |
| [application.properties](src/main/resources/application.properties) | Database and server settings |

## Local Setup

### 1. Prerequisites and clone

Install **JDK 21**, Maven, Git, and a running MySQL server. No Maven wrapper is included.

```sh
git clone https://github.com/53Husain/hospital-queue-management-system.git
cd hospital-queue-management-system
java -version
mvn -version
```

### 2. Configure MySQL

Create a local demo database with your database administration account:

```sql
CREATE DATABASE IF NOT EXISTS hospital_queue_db;
```

Set credentials in the terminal that launches the application. No database password is supplied by default.

PowerShell:

```powershell
$env:SPRING_DATASOURCE_USERNAME = 'YOUR_LOCAL_MYSQL_USER'
$env:SPRING_DATASOURCE_PASSWORD = 'YOUR_LOCAL_MYSQL_PASSWORD'
```

Bash:

```bash
export SPRING_DATASOURCE_USERNAME='YOUR_LOCAL_MYSQL_USER'
export SPRING_DATASOURCE_PASSWORD='YOUR_LOCAL_MYSQL_PASSWORD'
```

Replace those values locally. `DB_PASSWORD` is also supported as the password fallback. The account needs table-creation/update permissions because `spring.jpa.hibernate.ddl-auto=update` is enabled. The default URL is `jdbc:mysql://localhost:3306/hospital_queue_db`; override it with `SPRING_DATASOURCE_URL` if needed. Do not commit working credentials.

### 3. Provision the first administrator (optional)

Patient registration works without an administrator, but creating doctors requires one. On a database without an ADMIN user, explicitly enable bootstrap for one startup:

PowerShell:

```powershell
$env:APP_BOOTSTRAP_ADMIN_ENABLED = 'true'
$env:APP_BOOTSTRAP_ADMIN_EMAIL = 'admin@example.com'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = 'REPLACE_WITH_A_UNIQUE_PASSWORD'
mvn spring-boot:run
```

Bash:

```bash
export APP_BOOTSTRAP_ADMIN_ENABLED=true
export APP_BOOTSTRAP_ADMIN_EMAIL='admin@example.com'
read -rs -p 'New admin password: ' APP_BOOTSTRAP_ADMIN_PASSWORD; echo
export APP_BOOTSTRAP_ADMIN_PASSWORD
mvn spring-boot:run
```

Use your own email and a unique password of at least 12 characters and at most 72 UTF-8 bytes. Bootstrap hashes the password, does nothing when an ADMIN already exists, and refuses to promote an existing non-admin email. It never resets existing passwords. Run this initialization with one application instance. Stop the application after initialization and remove all three bootstrap variables before normal startup.

### 4. Run

```sh
mvn spring-boot:run
```

Or package and launch with the same database environment variables:

```sh
mvn clean package
java -jar target/hospital-queue-management-system-0.0.1-SNAPSHOT.jar
```

| Local URL | Purpose |
| --- | --- |
| `http://localhost:8080/` | Plain-text application-running response |
| `http://localhost:8080/index.html` | Landing page |
| `http://localhost:8080/pages/register.html` | Patient registration |
| `http://localhost:8080/pages/login.html` | Login for all roles |

The root controller handles `/`, so use the explicit page URLs for the frontend. Static pages are public; the APIs enforce access control. The browser's stored IDs and role only support display/navigation and cannot authorize an API request.

### 5. Register, log in, and exercise the demo

Register a patient in the browser, then log in. For API clients, preserve cookies between requests and:

1. Call `GET /api/auth/csrf`. Read `headerName` and `token` from its JSON response.
2. Send that header and session cookie on `POST /api/auth/register` or `/api/auth/login`.
3. After login, preserve the **new session cookie** and fetch `/api/auth/csrf` again. Login invalidates the old session and its CSRF token.
4. Include the current CSRF header on every POST, PUT, PATCH, and DELETE request, including logout. GET requests still need authentication except the public pages and CSRF endpoint.

Patient registration body (sample values, not an existing account):

```json
{
  "fullName": "Demo Patient",
  "email": "patient@example.com",
  "password": "demo-only-password",
  "phoneNumber": "0000000000",
  "role": "PATIENT"
}
```

Public registration rejects non-patient roles. The role may also be omitted for patient registration. Name/email/password are validated; passwords must contain at least 12 characters and fit within BCrypt's 72-byte limit.

Login body:

```json
{
  "email": "patient@example.com",
  "password": "demo-only-password"
}
```

Login returns `userId`, `patientId`, `doctorId`, `fullName`, `role`, and `message`, and establishes the session. It does not issue a JWT.

To create a doctor, log in as the bootstrapped administrator in an API client. POST the registration fields with a new email and `"role": "DOCTOR"` to `/api/admin/users`, including cookies and CSRF header. `"role": "ADMIN"` creates another administrator. This endpoint has no administration form yet. A new doctor has empty specialization and zero experience/rating.

Then use separate patient and doctor sessions (separate browser profiles or cookie jars):

1. As the patient, list doctors with `GET /api/doctors` and book from the patient dashboard or standalone `/pages/book-appointment.html` form.
2. An API booking uses `POST /api/appointments` with the logged-in patient's `patientId`, a real `doctorId`, and `priority` (`NORMAL` or `EMERGENCY`). Another patient's ID is rejected.
3. As the assigned doctor, read `/api/queue/{doctorId}`, POST `/api/queue/next/{doctorId}`, then PUT `/api/queue/complete/{appointmentId}`.
4. As the administrator, read `/api/admin/dashboard` for aggregate counts.
5. POST `/api/auth/logout` to end the session. Clearing browser storage alone does not log out.

## REST API and Access Rules

| Method | Endpoint | Access |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Public; obtains a session CSRF token |
| POST | `/api/auth/register` | Public, CSRF required; patient registration only |
| POST | `/api/auth/login` | Public, CSRF required; establishes session |
| POST | `/api/auth/logout` | CSRF required; invalidates session |
| POST | `/api/admin/users` | ADMIN; create DOCTOR or ADMIN |
| GET | `/api/admin/dashboard` | ADMIN; aggregate dashboard counts |
| GET | `/api/doctors` | Any authenticated user |
| POST | `/api/appointments` | PATIENT; own patient ID only |
| GET | `/api/appointments` | Any authenticated user; diagnostic string only |
| GET | `/api/appointments/{id}` | Owning patient or assigned doctor |
| GET | `/api/appointments/patient/{patientId}` | Owning patient |
| GET | `/api/queue/{doctorId}` | Owning doctor |
| POST | `/api/queue/next/{doctorId}` | Owning doctor |
| GET | `/api/queue/position/{appointmentId}` | Owning patient or assigned doctor |
| PUT | `/api/queue/complete/{appointmentId}` | Assigned doctor |
| GET / PUT | `/api/patient/profile/{patientId}` | Owning patient |

Unauthenticated protected requests return 401; missing/invalid CSRF or forbidden access returns 403. Authentication validation failures return 400, duplicate registration email returns 409, and invalid credentials return a generic 401 message. Booking requires positive patient/doctor IDs and a priority (400 for invalid input). Queue conflicts return 409: an active consultation, an empty queue, or completion of an appointment that is not IN_PROGRESS. Missing records in booking/transition services return 404; ownership checks can reject inaccessible records with 403 first. Other business-error responses are not yet standardized.

## Existing Accounts and Deployment Changes

- **Plaintext passwords no longer work.** There is deliberately no plaintext fallback or automatic migration. Existing accounts need administrator-controlled offline replacement with BCrypt hashes of newly chosen passwords, or fresh demo accounts with different emails. No reset/recovery UI is implemented. Bootstrap does not reset an existing administrator, so an existing plaintext ADMIN also requires offline recovery. Back up an existing database before any manual migration; do not delete patient records to resolve authentication.
- Session timeout is 30 minutes of inactivity. Sessions are server-side and are lost on restart; roles are captured at login. After manually changing roles or credentials, invalidate existing sessions (restart this single-instance demo) before relying on the new permissions.
- For HTTPS hosting, set `SESSION_COOKIE_SECURE=true`. The default is false for local HTTP development. SameSite=Lax and HttpOnly are configured. Cross-origin browser access is not enabled.
- The old committed database password has been removed from current configuration; history still contains the old value. Whether it was ever a live credential cannot be established from this repository.

## Queue Behavior and Remaining Limitations

The queue service rebuilds a Java `PriorityQueue` from database appointments for each operation. Calling next selects a BOOKED appointment and sets it to IN_PROGRESS after checking for an existing consultation. Queue lists include BOOKED and IN_PROGRESS; position considers BOOKED only and returns `-1` for appointments outside that set.

Booking, call-next, and completion run in transactions and acquire the same database row lock for their doctor. Locking reads see the latest committed queue/token state even under an older MySQL transaction snapshot. Calls for the same doctor wait until the preceding transaction commits or rolls back. Only BOOKED → IN_PROGRESS → COMPLETED is supported by these operations; a second active consultation and invalid/repeated completion return 409. Different doctors can advance independently.

Tokens use the doctor's largest existing token plus one, allocated while holding the lock. Estimated wait still uses the historical appointment count multiplied by 15 minutes: it is not a live queue estimate, and tokens do not reset daily. The locks protect these service paths, not direct SQL writers. Lock failures roll back; there is no automatic retry. No schema migration is required, but existing duplicate tokens or multiple active consultations are not automatically repaired.

The standalone booking form loads doctors, preserves a valid doctor selection, submits through the session/CSRF helper, displays the returned token, and reports errors inline. It prevents duplicate clicks while pending and after success on that page; the API does not provide idempotency keys for retries after an uncertain network outcome.

The doctor dashboard polls every five seconds, enables completion only for IN_PROGRESS rows, and reports transition conflicts. This is not WebSocket push. The separate queue-status script loads once. Broader request validation, rate limiting, password recovery, database migrations, accurate wait estimation, and production deployment hardening remain future work.

## Tests and Verification

JDK 21 and Maven are required. The full verification additionally requires Docker and access to pull the MySQL 8.4 Testcontainers image.

```sh
# H2 authentication/ownership and bootstrap tests; no Docker required
mvn test
# Includes MySQL concurrency integration tests and packaging
mvn clean verify
# Mocked DOM/HTTP tests for request helper, booking and doctor dashboard
node --test src/test/js/*.test.cjs
```

Authentication integration tests use the real Spring Security filters, controllers, services and JPA repositories with an isolated **H2 database in MySQL mode**. They cover password hashing, patient-only registration, privileged staff creation, validation, bad/legacy credentials, CSRF, session rotation/logout, patient ownership, and doctor assignment. Bootstrap tests cover first-admin behavior.

`QueueConcurrencyIT` uses **MySQL 8.4 in Docker**, separate transactions and coordinated concurrent workers. It covers simultaneous next/completion requests, completion-versus-next serialization, stale transaction snapshots, rollback, emergency/FIFO ordering, invalid transitions, concurrent token allocation, independent doctors, and HTTP booking/transition contracts.

[Java CI](.github/workflows/ci.yml) runs Maven verification/packaging and Node tests on pushes and pull requests, and uploads both unit and integration test reports. Node tests use mocked DOM/HTTP responses; they are not rendered-browser end-to-end tests. These tests do not establish production readiness, and a deployed browser-to-MySQL workflow has not been verified in this update.

The screenshots below are historical repository images, retained without regeneration. In particular, registration now permits only patients; the old role selector is no longer present.

---

# Database Schema

<img width="1280" height="845" alt="database-schema" src="https://github.com/user-attachments/assets/e6e734cd-113e-4c84-a2d0-ead893acd59f" />


---

# Application Screenshots

## Login

<img width="871" height="755" alt="login" src="https://github.com/user-attachments/assets/0179cc1c-0e30-4f60-ae78-c853c550e912" />


---

## Registration

<img width="1060" height="841" alt="Register" src="https://github.com/user-attachments/assets/d763a2d0-3a86-4d33-945f-1f170a2008d5" />


---

## Patient Dashboard

<img width="1901" height="915" alt="patient" src="https://github.com/user-attachments/assets/31043350-937d-4819-8ee7-bf912869448b" />


---
## Patient Waiting 

<img width="1906" height="902" alt="patient waiting " src="https://github.com/user-attachments/assets/1532905f-b6bf-4522-9fbf-a29349b39557" />


## My Appointments

<img width="1900" height="906" alt="my appointment" src="https://github.com/user-attachments/assets/4e8da302-2b0b-4f0a-84d1-afb30899711b" />


## Analytics Dashboard

<img width="1897" height="910" alt="admin" src="https://github.com/user-attachments/assets/6b621b1f-f50a-4477-ad0e-1cf56ed7c3c7" />


---


# Author

**Husain Lalji** · B.Tech Information Technology

Java · Spring Boot · MySQL · REST APIs · Data Structures

[GitHub](https://github.com/53Husain) · [LinkedIn](https://www.linkedin.com/in/husain-lalji)
