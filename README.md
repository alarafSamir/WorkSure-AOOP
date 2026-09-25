## Current Project Update

This repository is submitted for the current Advanced OOP project milestone.

### Required milestone completed
- All controllers reviewed
- JSON request bodies refactored to typed request objects
- Request DTO classes added
- No controller uses Map as a request body
- Backend clean test passes

The UI included in this repository represents ongoing additional project work
and is not the focus of this milestone submission.

---

# WorkSure — AOOP service marketplace

WorkSure connects customers with service workers. This university project uses **plain HTML, CSS and JavaScript**, **Java**, **Spring Boot**, **MariaDB**, **JDBC** and **Gradle**. Spring Boot serves both the website and REST API at **http://localhost:5000**. There is no frontend package manager or separate frontend server.

## Implemented workflows

- Customers register/login, browse/filter services, view workers, book services, manage their profile, see booking history, and review completed bookings.
- Workers manage their profile/services, accept or reject assigned jobs, start and complete jobs, submit verification documents, and read received reviews.
- Admins see totals, users/workers, bookings and reviews; ban/unban non-admin accounts; and approve/reject worker documents.
- The plain frontend intentionally excludes payments, cart, wishlist, chat, notifications and other deferred features. Older backend modules for some of these remain preserved, but are not part of the final UI.

## Quick Start

Requirements: **Java 17+** (a full JDK including `javac`), **MariaDB**, **Git**, and a **browser**. The **Gradle wrapper is included**; no separate Gradle installation is needed. Its first run needs internet access to download Gradle and dependencies.

### 1. Clone the repository

```bash
git clone https://github.com/alarafSamir/WorkSure-AOOP.git
cd WorkSure-AOOP
```

### 2. Start MariaDB and create the database

On Fedora/Linux with systemd:

```bash
sudo systemctl start mariadb
mariadb -u root -p
```

At the MariaDB prompt:

```sql
CREATE DATABASE IF NOT EXISTS `worksure-aoop`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

Optional: `sudo systemctl enable mariadb` starts MariaDB automatically after reboot. On an empty database, the application initializes its schema and seed data automatically. Do not import a fresh schema over existing project records.

### 3. Configure credentials

The database is **`worksure-aoop`**. In `backend/src/main/resources/application.properties`, the defaults are:

```properties
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD:}
app.jwt-secret=${JWT_SECRET:change-me}
```

In the terminal where you will run the application, set your own values:

```bash
export DB_PASSWORD=your_mariadb_password
export JWT_SECRET=your-long-secret-key
```

Replace the placeholders with your MariaDB password and a long, private JWT signing secret. Quote values containing shell special characters. Without these variables, the password is empty and the JWT secret falls back to `change-me`; set them explicitly.

The default connection uses MariaDB's `root` account over TCP. If your installation uses socket-only root authentication, use a database account with privileges on `worksure-aoop` and set `SPRING_DATASOURCE_USERNAME` to that account's name. Set `DB_PASSWORD` to its password.

### 4. Run Spring Boot

From the repository root, in the same terminal:

```bash
cd backend
./gradlew bootRun
```

Keep the terminal open; stop the server with Ctrl+C.

### 5. Open the application

- Application: [http://localhost:5000](http://localhost:5000)
- Health check: [http://localhost:5000/health](http://localhost:5000/health)

Do not open HTML files directly from disk: their `/api` requests need the Spring Boot server.

### Build and run a JAR

From the repository root, with the environment variables above set:

```bash
cd backend
./gradlew clean build
java -jar build/libs/worksure-backend-1.0.0.jar
```

If already in `backend/`, skip `cd backend`. Stop `bootRun` before starting the JAR. Run the JAR from `backend/` so relative upload paths use the same files. The preserved real-time backend also uses port 9092; the plain UI does not use it. Nothing needs port 5173.

## Run Tests

From the repository root (skip `cd backend` if already there):

```bash
cd backend
./gradlew clean test
```

Controller request-object and request compatibility tests exist under `backend/src/test/java/`. They cover request binding, validation, partial updates, and selected controller rules using mocked dependencies. They do not replace live MariaDB/API regression checks. The HTML test report is generated at `backend/build/reports/tests/test/index.html`.

## Demo accounts

- Admin: `admin@gmail.com` / `12345`
- Worker: `rahim@gmail.com` / `12345` (also `karim@gmail.com`)
- Customer: register through the website.

These are local classroom demo accounts. New self-registered passwords require at least eight characters. Admin self-registration is not allowed.

## How the parts communicate

1. Spring Boot serves HTML, CSS and JavaScript from `backend/src/main/resources/static/`.
2. JavaScript calls same-origin `/api/...` endpoints using browser `fetch()`.
3. Protected requests attach the JWT as `Authorization: Bearer <token>`. The token is stored in the current tab's session storage.
4. Spring Security checks authentication and roles. Controllers enforce ownership/business rules.
5. The `Db` helper uses Spring JDBC and SQL to read/write MariaDB. The browser never connects directly to the database.

Client-side role redirects make navigation easier; backend authorization remains authoritative. Downloads use authenticated fetch requests, not tokens embedded in links.

## Business rules to explain in your viva

- Only customers create bookings.
- Only the assigned worker performs worker actions: `pending → accepted/rejected`, `accepted → in_progress`, `in_progress → completed`. Existing cancellation permissions remain unchanged.
- A customer may review only their own completed booking. Ratings are integers 1–5; only one review per booking is allowed. Comments are optional.
- A worker is verified if at least one document is approved. Admin decisions update the document, aggregate worker verification flag and audit log in one transaction. A worker-row lock serializes simultaneous decisions.
- New verification documents live in `backend/private-documents/`. Legacy verification files are moved out of public uploads at startup; existing verification flags are reconciled with approved documents.
- Only the owner worker or an admin can download a verification document through `GET /api/verification-documents/{id}`. Ordinary service images in `backend/uploads/` remain public.

## Project structure

```text
WorkSure-AOOP/
├── backend/
│   ├── build.gradle, settings.gradle, gradlew, gradlew.bat, gradle/
│   ├── src/main/java/com/worksure/
│   │   ├── web/          # REST controllers, request/ DTOs, and API errors
│   │   ├── security/     # JWT, authenticated user, role checks
│   │   ├── db/           # JDBC helper
│   │   ├── storage/      # Public uploads and private verification files
│   │   ├── config/, seed/, util/, socket/
│   │   └── WorkSureApplication.java
│   ├── src/main/resources/
│   │   ├── application.properties, schema.sql, service-catalog.json
│   │   └── static/
│   │       ├── index.html, login.html, register.html
│   │       ├── services.html, service.html, worker.html
│   │       ├── customer/ # Dashboard, bookings with reviews, profile
│   │       ├── worker/   # Dashboard, jobs, services, profile, verification, reviews
│   │       ├── admin/    # Dashboard, users, bookings, verification, reviews
│   │       ├── css/, js/, images/, favicon.svg, icons.svg
│   ├── src/test/java/   # Controller request and compatibility tests
│   ├── uploads/         # Runtime public images
│   └── private-documents/ # Runtime private documents; back up with DB
├── database/            # Preserved SQL setup files and service catalog
├── scripts/             # Optional documentation generators; not app dependencies
├── docs/                # Final migration report and removal manifest
├── README.md, README_JAVA.md, ProjectREADME.txt
├── WorkSure_AOOP_Project_Proposal.pdf
└── WorkSure_Easy_Explanation.docx
```

## OOP/AOOP concepts actually present

- **Encapsulation:** `JwtService`, `Db` and upload classes hide reusable implementation details behind methods.
- **Composition/dependency injection:** controllers receive helpers through constructors instead of constructing everything themselves.
- **Abstraction/interfaces:** `PasswordEncoder`, `Resource`, `WebMvcConfigurer` and `CommandLineRunner` provide common contracts.
- **Inheritance/polymorphism:** the JWT filter extends `OncePerRequestFilter` and overrides request handling. Spring calls interface implementations through their contracts.
- **Exceptions:** `ApiException` and the global handler translate failures into HTTP responses.
- **Transactions:** verification review changes succeed or roll back together.

Be precise: this project uses JDBC and Map-based query results, not JPA entities or a complete domain Service/Repository architecture. Controllers still contain substantial business logic.

## Existing limitations

- Admin booking/review APIs return the latest 200 records; the UI states this limit.
- Missing static files currently return HTTP 500 rather than 404 through the existing global error handler. Private documents are still protected.
- A service with bookings may fail deletion because of database constraints; the UI shows the backend error.
- Review creation and rating aggregation follow the existing backend implementation and are not one transaction; the duplicate pre-check can race under concurrent submissions. No backend review redesign was made.
- Old backend integrations and their configuration remain preserved. They are not frontend requirements.

See [the final migration report](docs/final-migration-report.md) for changes, tests, API coverage and test records. The original project and pre-cleanup snapshot are backed up outside the repository on the original development machine; those backups are not included in a clone.
