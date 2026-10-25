# Smart Hospital Queue Management System

A Java backend and Bootstrap frontend for demonstrating patient registration, doctor queues, appointments, and admin dashboard statistics. Built with **Java 21, Spring Boot 3.5.4, Spring Data JPA, and MySQL**.

**Status:** a local learning/demo application. Login provides role information and dashboard routing, but the current backend permits all requests and stores passwords as plain text. Use synthetic data and demo-only passwords; this implementation is not ready for real patient information or public deployment.

## Features Present in the Source

- Registration and login with PATIENT, DOCTOR, and ADMIN role values.
- Patient workflows: list doctors, book appointments, view appointment history/status, and update a profile.
- Doctor workflows: view the queue, call the next patient, and complete appointments.
- Priority ordering: EMERGENCY before NORMAL, then ascending token number.
- Doctor dashboard refresh through HTTP polling every five seconds.
- Admin dashboard counts for patients, doctors, all appointments, completed/booked appointments, and emergency appointments.
- Static HTML/CSS/JavaScript pages served by Spring Boot.

These are application workflows, not a claim of enforced role-based API authorization.

## Architecture and Source Guide

Requests pass from REST controllers through service implementations to Spring Data JPA repositories and MySQL.

| Source | Responsibility |
| --- | --- |
| [HospitalQueueApplication.java](src/main/java/com/shqms/HospitalQueueApplication.java) | Spring Boot entry point |
| [controller/](src/main/java/com/shqms/controller) | HTTP routes |
| [service/impl/](src/main/java/com/shqms/service/impl) | Registration, appointments, queues, profiles, and dashboard logic |
| [repository/](src/main/java/com/shqms/repository) | Database access |
| [entity/](src/main/java/com/shqms/entity) | User, Patient, Doctor, Hospital, and Appointment entities |
| [dto/](src/main/java/com/shqms/dto) | Request and response objects |
| [QueueManager.java](src/main/java/com/shqms/dsa/queue/QueueManager.java) | PriorityQueue and appointment comparator |
| [SecurityConfig.java](src/main/java/com/shqms/config/SecurityConfig.java) | Current permissive security configuration |
| [static/](src/main/resources/static) | Browser pages and assets |
| [application.properties](src/main/resources/application.properties) | Database, Hibernate, and server settings |

The Maven project includes Spring Web, JPA, Security, Validation, MySQL Connector/J, Lombok, DevTools, and test dependencies. Declaring test dependencies does not mean tests have been implemented.

## Queue Behavior

The queue service rebuilds a Java `PriorityQueue` from database appointments for each queue operation. Emergency appointments precede normal appointments; token numbers break ties within the same priority.

Calling the next patient selects a BOOKED appointment and changes it to IN_PROGRESS. The service first checks whether that doctor already has an IN_PROGRESS appointment. The queue-list endpoint includes BOOKED and IN_PROGRESS appointments; the position endpoint considers only BOOKED appointments and returns `-1` when the requested appointment is not in that waiting set.

Completion sets the selected appointment to COMPLETED without validating that it was previously IN_PROGRESS.

Booking assigns a token using the doctor's total appointment count plus one. The estimated wait is that count multiplied by 15 minutes, including historical appointments. It is an initial estimate, not a continuously recalculated waiting time. Concurrent booking/call-next requests are not protected by explicit locking in these service methods.

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

Set your own credentials in the terminal that will launch the application. These environment variables override the committed properties.

**PowerShell:**

```powershell
$env:SPRING_DATASOURCE_USERNAME = 'YOUR_LOCAL_MYSQL_USER'
$env:SPRING_DATASOURCE_PASSWORD = 'YOUR_LOCAL_MYSQL_PASSWORD'
```

**Bash:**

```bash
export SPRING_DATASOURCE_USERNAME='YOUR_LOCAL_MYSQL_USER'
export SPRING_DATASOURCE_PASSWORD='YOUR_LOCAL_MYSQL_PASSWORD'
```

Replace the example values locally. The account needs permission to create/update the demo tables because `spring.jpa.hibernate.ddl-auto=update` is enabled.

The committed URL is `jdbc:mysql://localhost:3306/hospital_queue_db`; override it with `SPRING_DATASOURCE_URL` if necessary. The configuration contains a literal password: override it locally and do not commit working credentials. Whether that value was ever an active credential is not established by the repository.

### 3. Run

```sh
mvn spring-boot:run
```

Alternatively, package and launch with the same environment variables:

```sh
mvn clean package
java -jar target/hospital-queue-management-system-0.0.1-SNAPSHOT.jar
```

| Local URL | Purpose |
| --- | --- |
| `http://localhost:8080/` | Plain-text “Hospital Queue Management System is running!” response |
| `http://localhost:8080/index.html` | Static landing page |
| `http://localhost:8080/pages/register.html` | Registration page |
| `http://localhost:8080/pages/login.html` | Login page |

The root controller handles `/`, so use the explicit page URLs to open the frontend. Several scripts hard-code `http://localhost:8080`; changing the port or hosting remotely requires frontend changes too.

### 4. Create demo accounts

No seed data or preconfigured application credentials are present in the inspected tree. Register a demo patient and doctor, then sign in using your supplied credentials.

Registration accepts `fullName`, `email`, `password`, `phoneNumber`, and `role`. PATIENT registration creates a Patient record. DOCTOR registration creates a Doctor record with empty specialization and zero experience/rating.

Example JSON for `POST /api/auth/register`—sample values, not an existing account:

```json
{
  "fullName": "Demo Patient",
  "email": "patient@example.com",
  "password": "demo-only-password",
  "phoneNumber": "0000000000",
  "role": "PATIENT"
}
```

Use a different email and `"role": "DOCTOR"` for the doctor. The service also accepts ADMIN directly; there is currently no protected administrator-provisioning flow.

Example body for `POST /api/auth/login`:

```json
{
  "email": "patient@example.com",
  "password": "demo-only-password"
}
```

Login returns `userId`, `patientId`, `doctorId`, `fullName`, `role`, and `message`. It does not issue a JWT or establish an application-authenticated session. Use IDs returned by your own database rather than assuming they are 1.

### 5. Exercise the demo

1. Register patient and doctor accounts, then log in.
2. List doctors through `GET /api/doctors`.
3. Submit `POST /api/appointments` with real patient/doctor IDs:

```json
{
  "patientId": 1,
  "doctorId": 1,
  "priority": "NORMAL"
}
```

The IDs are illustrative. Valid priority values are NORMAL and EMERGENCY.

4. View `GET /api/queue/{doctorId}`.
5. Call `POST /api/queue/next/{doctorId}`.
6. Complete the selected appointment using `PUT /api/queue/complete/{appointmentId}`.
7. Read `GET /api/admin/dashboard` for counts.

This is a source-derived manual checklist, not a recorded end-to-end test.

## REST API Reference

Base URL: `http://localhost:8080`. Methods and paths match the controller annotations.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/auth/register` | Register; returns a plain-text message |
| POST | `/api/auth/login` | Check credentials and return identity/role information |
| GET | `/api/doctors` | List doctors |
| POST | `/api/appointments` | Book an appointment |
| GET | `/api/appointments` | Diagnostic string, not an appointment list |
| GET | `/api/appointments/{id}` | Read an appointment |
| GET | `/api/appointments/patient/{patientId}` | List a patient's appointments |
| GET | `/api/queue/{doctorId}` | Read the doctor's queue |
| POST | `/api/queue/next/{doctorId}` | Call the next patient |
| GET | `/api/queue/position/{appointmentId}` | Read waiting-queue position |
| PUT | `/api/queue/complete/{appointmentId}` | Mark an appointment completed |
| GET / PUT | `/api/patient/profile/{patientId}` | Read or update a patient profile |
| GET | `/api/admin/dashboard` | Dashboard counts |

The previous README's `PUT /api/queue/call-next/{doctorId}` and `GET /api/admin/analytics` do not match the current controllers; use the routes above.

## Current Limitations and Verification

- **Authorization:** `SecurityConfig` uses `anyRequest().permitAll()` and disables CSRF. Spring Security already exists as a dependency/configuration, but role-based API restrictions are not enforced.
- **Passwords:** the registration/login service stores and compares plain-text passwords.
- **Client state:** login saves IDs and roles in `localStorage` for dashboard routing. Those values do not secure endpoints or establish record ownership.
- **Validation:** the auth/appointment controllers do not use `@Valid`, and their inspected request DTOs lack validation constraints. Generic runtime exceptions are used instead of a consistent application error contract.
- **Refresh:** the doctor dashboard polls every five seconds; it is not WebSocket push. The queue-status script loads once when invoked.
- **Scheduling:** token allocation, waiting estimates, and appointment transitions have the limitations described in Queue Behavior.
- **Verification:** no automated tests, CI workflow, seed scripts, or migration scripts are present in the inspected tree. This README was checked against source/configuration; a Java 21/MySQL build and live end-to-end run were not performed for this documentation update.
- **Screenshots:** the existing images below are retained from the repository; they were not regenerated or independently verified in this update.

## Future Improvements

These are proposed work, not completed features:

- Password hashing and authenticated sessions or tokens.
- Server-side role/ownership checks and controlled doctor/admin provisioning.
- Request validation and consistent HTTP errors.
- Concurrency-safe token allocation and appointment transitions.
- Better waiting estimates and doctor scheduling.
- Automated unit/integration tests and CI.
- Environment-based configuration and removal of hard-coded frontend API origins.
- Doctor filtering and optional notifications.

A basic admin dashboard already exists; expanded administration would be additional work.

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
