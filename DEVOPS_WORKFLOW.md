# MediFlow Hospital Management System — DevOps & Architecture Guide

**Project Date:** September 2026  
**Version:** 1.0  
**Tech Stack:** Java Servlet/JSP, MySQL, Maven, Docker, Jenkins, Apache Tomcat, GitHub

---

## Table of Contents

1. [Overview](#overview)
2. [Frontend Architecture](#frontend-architecture)
3. [Backend Architecture](#backend-architecture)
4. [Source Code Management (Git & GitHub)](#source-code-management)
5. [Build System (Maven)](#build-system)
6. [CI/CD Pipeline (Jenkins)](#cicd-pipeline)
7. [Containerization (Docker)](#containerization)
8. [Deployment](#deployment)
9. [How to Run](#how-to-run)

---

## Overview

MediFlow is a **Hospital Management System** that handles:
- Patient records and medical history
- Doctor roster with availability scheduling
- Appointment booking (prevents double-booking)
- Prescription management
- Invoice & billing
- Role-based access control (Admin, Doctor, Receptionist)
- Analytics and reporting

**Two deployment targets:**
- **Docker** (local development): http://localhost:8090/hms/
- **Apache Tomcat** (production/demo): http://localhost:9090/hms/

Both pull code from the same GitHub repository and run identical WAR files.

---

## Frontend Architecture

### Design System: Claymorphism

The UI uses a **claymorphism** design aesthetic with:
- Soft inflated surfaces using three-shadow technique (outer drop, inset highlight, inset shade)
- Brand colors: Navy ink, teal, brand blue
- Responsive layout that works from 760px to 1900px+ viewports

### Key Frontend Components

#### Navigation Bar (`header.jspf`)
- **Sticky navigation pill** at the top with MediFlow branding
- **Dropdown menus** for Patients, Doctors, Appointments, Prescriptions, Billing
- **Account menu** (avatar button) with user info and Sign out
- Works with/without JavaScript (progressive enhancement)
- Keyboard accessible (hover + focus-within + click)

#### Account Menu (Recently Fixed)
Previously, the Sign out button sat **outside the navbar** on wide screens (bug in the navbar layout). Fixed by:
- Moving name, role, and Sign out into a dropdown panel under the avatar
- Tightening nav spacing from base rules (was only applied at breakpoints)
- Keeping the panel floating on narrow screens where nav menus go inline
- Result: Account button now fits perfectly inside the pill at every viewport width

#### CSS Architecture (`style.css`)
- **Design tokens** (colors, shadows, typography) defined in `:root`
- **CSS variables** for clay shadows: `--clay`, `--clay-sm`, `--clay-press`, `--clay-teal`
- **Breakpoint ladder**: 1460px → 1240px → 1060px → 880px (menu items tighten progressively)
- **CSS-only charts** for analytics (bar and column charts with CSS Grid)

#### JavaScript (`app.js`)
- **Progressive enhancement**: works without JS, but adds click-toggle and Escape for dropdowns
- **Dropdown wiring**: connects `.nav-toggle` and `.account-toggle` buttons
- **Form confirmations**: destructive actions ask for confirmation before submit
- **Row adders**: prescription and invoice forms let you add repeating medicine/item rows
- **Security**: Content Security Policy blocks inline scripts (`script-src 'self'` only)

### Pages & Views
- **Login page** (`login.jsp`): Split SaaS layout, brand panel + form panel
- **Dashboard**: Role-aware summary of recent activity
- **Patients**: List, search, register new, view history and insurance
- **Doctors**: Roster with availability scheduling
- **Appointments**: Prevent double-booking via database constraint + app logic
- **Prescriptions**: Issued medicines linked to appointments and patients
- **Invoices & Billing**: Auto-generated from visits, tax calculation, payment status
- **Reports**: Analytics with CSS-only charts (revenue, appointments, top medicines)
- **Staff Accounts**: Admin panel for user management

---

## Backend Architecture

### Technology Stack
- **Language**: Java 25 (targets Java 17+)
- **Servlet Container**: Apache Tomcat 10.1.60 (Jakarta Servlet 6.0 / JSP 3.1)
- **Database**: MySQL 8.4 (with H2 for unit tests)
- **Connection Pool**: HikariCP (10 max connections)
- **Build Tool**: Apache Maven 3.9.16

### Key Backend Components

#### Authentication & Security
- **Session management**: `SessionUser.signIn()` creates new session with 30-minute timeout
- **CSRF protection**: `CsrfToken` generates 32-byte random tokens, constant-time comparison
- **Password hashing**: PBKDF2-SHA256 with 210,000 iterations, per-password salt, Base64 encoded
- **Role-based access control**: `AuthFilter` checks user role against endpoint permissions
- **Login failure**: Single message "Incorrect username or password" prevents username enumeration

#### Data Access Layer (DAOs)
- **UserDAO**: Manage staff accounts, password validation, last-login tracking
- **PatientDAO**: Store patient records with medical history, allergies, insurance info
- **DoctorDAO**: Manage doctor roster with availability (days of week, time slots, room number)
- **AppointmentDAO**: Book appointments with double-booking prevention
- **PrescriptionDAO**: Link medicines to doctors and patients, atomic transactions
- **InvoiceDAO**: Generate invoices with tax, payment status, line items
- **ReportDAO**: Aggregate data for analytics (5-6 month trends, top medicines, etc.)

**N+1 Query Prevention**: `InvoiceDAO.findAll()` loads 100+ invoices in 2 queries (header + items), not 101+

#### Invoice System (Atomic Transactions)
- **Invoice numbering**: Dedicated `invoice_sequence` table with FOR UPDATE lock (no reuse after deletion)
- **Tax calculation**: Per-invoice, rounded to 2 decimal places before summing
- **Totals always derived**: Never stored — computed from line items at query time
- **Status tracking**: UNPAID, PAID, CANCELLED (prevents negative money)

#### Prescription System
- **Atomic insert**: Prescription + medicines (items) committed together or rolled back
- **Doctor-scoped queries**: Doctor sees only their own prescriptions via `findByDoctor(doctorId)`
- **Appointment linking**: Prescription ties to specific appointment (nullable for standalone prescriptions)

#### Session Lifecycle (`AppLifecycleListener`)
- **On startup**: Calls `UserBootstrap` to create default staff accounts (admin, dr.menon, reception)
- **Deferred pool init**: HikariCP set to delay connection errors to first query (handles DB startup lag)
- **On undeploy**: Closes `HikariDataSource` cleanly

#### Configuration (`AppConfig`)
Resolves settings in order:
1. **Environment variables** (e.g., `DB_URL`, `DB_USERNAME`)
2. **JVM system properties** (e.g., `-Ddb.url=...`)
3. **Bundled defaults** in `application.properties`

This allows **one WAR** to run unchanged in:
- Local development (localhost:3306)
- Docker (MySQL container on 3307)
- Jenkins/Tomcat (any MySQL reachable from the app)

#### Testing Strategy
- **84 unit tests** covering DAOs, security, business logic, model calculations
- **H2 MySQL-mode database**: Each test gets a throwaway DB (H2 + atomic increment = isolation)
- **JaCoCo code coverage**: ~85% covered, report published by Jenkins
- **Parametrized tests**: Invoice tax, prescription medicines, appointment conflicts all tested at boundaries

---

## Source Code Management

### Git Configuration
- **Local repository**: `C:\Users\saish\OneDrive\Desktop\Hospital management system`
- **Git user**: Sai Shashank Kurella
- **Email**: `202742188+Sai-shashank-1006@users.noreply.github.com` (GitHub's noreply address, hides real email)
- **Branch strategy**: Single `main` branch (all work commits to main, Jenkins polls it)

### GitHub Repository
- **URL**: https://github.com/Sai-shashank-1006/Hospital-Management-System
- **Visibility**: Public
- **Key files tracked**:
  - Source code (`src/main/java`, `src/test/java`)
  - Web resources (`src/main/webapp`: JSP, CSS, JS, fonts)
  - Build config (`pom.xml`, `Jenkinsfile`)
  - Docker config (`Dockerfile`, `docker-compose.yml`, `src/main/docker/`)
  - Documentation (`README.md`, this file)
  - Database schemas (`src/main/docker/mysql/`)

### Commit History (Recent)

| Commit | Message | What Changed |
|--------|---------|--------------|
| `b34c5a4` | Add one-click demo launcher | Created `run-for-demo.bat` for easy presentation startup |
| `7dc4d50` | Keep account controls inside navbar | Fixed overflow bug, moved Sign out to dropdown panel |
| `0ffd0d7` | Fix Jenkins pipeline so it compiles and deploys | Removed hardcoded paths, used `tools` block, fixed branch detection |
| `67b8be4` | Fix Jenkins pipeline: absolute paths | Fixed Maven/JDK path issues on Windows Jenkins |
| `656d64d` | Replace logo with MediFlow blob mark | New blue gradient blob with M and teal arc |
| `a4d7353` | Rebuild interface: MediFlow branding, claymorphism | Full UI redesign with new colors, dropdowns, brand |
| `10a8043` | Add authentication, billing, prescriptions, analytics | Core feature set implementation |

---

## Build System

### Maven Configuration (`pom.xml`)

**Key Dependencies**:
- `jakarta.servlet:jakarta.servlet-api:6.0.0` — Servlet 6.0 (Jakarta EE)
- `jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.0` — JSTL for JSP
- `org.junit.jupiter:junit-jupiter:5.10.3` — JUnit 5 for testing
- `com.h2database:h2:2.4.232` — H2 database for unit tests
- `com.zaxxer:HikariCP:5.1.0` — Connection pooling
- `mysql:mysql-connector-java:8.0.33` — MySQL JDBC driver
- `org.jacoco:jacoco-maven-plugin:0.8.13` — Code coverage

**Build Phases**:
1. **Clean**: Remove `target/` directory
2. **Compile**: Javac on source code
3. **Test**: Run 84 unit tests with H2 databases
4. **Package**: Create `hms.war` WAR file (~7.7 MB)

**Output**: `target/hms.war` — ready to deploy to any Servlet 6.0 container

### Local Build Command
```bash
mvn clean package
```
Takes ~2 minutes, produces WAR and test report.

---

## CI/CD Pipeline

### Jenkins Setup

**Jenkins URL**: http://localhost:8080/  
**Job Name**: Hospital-Management-System  
**Job Type**: Pipeline (Declarative + Groovy)

#### Pipeline Stages

```
Checkout → Build → Test → Package → Deploy to Tomcat → Smoke test → Promote
                                    (only on main)     (only on main)  (only on main)
```

**1. Checkout** — Clone from GitHub (main branch)
- Records the commit SHA for build history traceability

**2. Build** — `mvn clean compile`
- Compiles source code, fails if there are errors
- Does NOT package yet (avoids deploying a broken WAR)

**3. Test** — `mvn test`
- Runs 84 unit tests
- Publishes JUnit report to Jenkins
- Publishes JaCoCo code coverage report (HTML)
- Fails the build if any test fails

**4. Package** — `mvn package -DskipTests`
- Builds the WAR now that tests passed
- Archives `hms.war` as a build artifact (kept in Jenkins history)

**5. Deploy to Tomcat** — (main branch only)
- Saves the previous WAR (for rollback)
- Uses Jenkins credential `tomcat-manager` (deployer user)
- Calls Tomcat Manager API: `curl --upload-file hms.war ... /manager/text/deploy`
- Deploys to http://localhost:9090/hms/

**6. Smoke Test** — (main branch only)
- Polls `/hms/health` endpoint (max 20 attempts, 3 sec intervals = 60 sec timeout)
- Waits for `"status":"UP"` JSON response
- Fails the build if the app doesn't report healthy
- If it fails here, the rollback stage runs

**7. Promote** — (main branch only)
- If smoke test passed, saves this WAR as the "known-good" version
- Used as the rollback target if a future deployment fails its smoke test

**Post-Failure Rollback** (if smoke test failed):
- Redeploys the previous known-good WAR
- Re-runs smoke test to confirm recovery
- If recovery fails: alerts the developer ("MANUAL INTERVENTION NEEDED")

#### Triggers

**Polling every 5 minutes**:
```groovy
triggers {
  pollSCM('H/5 * * * *')
}
```

When you push to GitHub, Jenkins polls within 5 minutes, detects the new commit, and builds automatically. (Webhooks would be faster but require GitHub to reach localhost:8080.)

#### Jenkins Configuration

**Tools** (Manage Jenkins > Tools):
- **JDK 'jdk17'**: `C:\Program Files\Java\jdk-25.0.4`
- **Maven 'maven3'**: `C:\tools\apache-maven-3.9.16`

**Credentials** (Manage Jenkins > Credentials):
- **tomcat-manager**: Username `deployer`, password (stored securely, read from Tomcat config)

**Plugins**:
- Pipeline, Git, JUnit, JaCoCo/HTML Publisher, Timestamper, Workspace Cleanup

---

## Containerization

### Docker Compose Setup (`docker-compose.yml`)

**Two services**:

#### 1. MySQL (`hms-mysql`)
```yaml
image: mysql:8.4
ports:
  - "3307:3306"  # Published on 3307 to avoid conflict with local MySQL on 3306
environment:
  MYSQL_ROOT_PASSWORD: root123
  MYSQL_DATABASE: hospital_db
  MYSQL_USER: hms_user
  MYSQL_PASSWORD: hms_password
volumes:
  - ./src/main/docker/mysql:/docker-entrypoint-initdb.d  # Init scripts
  - mysql_data:/var/lib/mysql  # Persist data across restarts
```

**Init scripts**:
- `01-schema.sql`: Creates all tables (users, patients, doctors, appointments, invoices, prescriptions, etc.)
- `02-seed.sql`: Populates initial data (sample patients, doctors, appointments)

**Healthcheck**: Queries `SELECT 1 FROM invoice_sequence LIMIT 1` to confirm the app's schema is ready (not just the MySQL daemon).

#### 2. App (`hms-app`)
```yaml
build: .  # Builds from Dockerfile
ports:
  - "8090:8080"  # Tomcat inside container runs on 8080, published as 8090
environment:
  DB_URL: jdbc:mysql://hms-mysql:3306/hospital_db?...
  DB_USERNAME: hms_user
  DB_PASSWORD: hms_password
depends_on:
  hms-mysql:
    condition: service_healthy  # Waits for MySQL health check before starting
```

### Dockerfile (Multi-stage)

**Stage 1 — Builder**:
```dockerfile
FROM maven:3.9.16-eclipse-temurin-17
COPY . /app
WORKDIR /app
RUN mvn clean package -DskipTests
```
- Builds the WAR inside a Maven container

**Stage 2 — Runtime**:
```dockerfile
FROM tomcat:10.1.60-jdk17
COPY --from=builder /app/target/hms.war $CATALINA_HOME/webapps/hms.war
EXPOSE 8080
```
- Copies the WAR to Tomcat's `webapps/` directory
- Tomcat auto-deploys it on startup

**Result**: ~900 MB image (tomcat:10 base + JDK 17 + compiled app)

---

## Deployment

### Deployment Targets

#### 1. Docker Compose (Development/Demo)
```bash
docker compose up -d
```
- Starts MySQL and Tomcat containers
- App runs on http://localhost:8090/hms/
- Database persists in named volume `mysql_data`

#### 2. Standalone Tomcat (Production/Jenkins Target)
```bash
# Port 9090 (set in conf/server.xml)
C:\tools\apache-tomcat-10.1.60\bin\startup.bat
```
- Deployment via Jenkins using Tomcat Manager API
- App runs on http://localhost:9090/hms/
- Database: configured via environment variables in `setenv.bat`:
  ```batch
  set "DB_URL=jdbc:mysql://localhost:3307/hospital_db?..."
  set "DB_USERNAME=hms_user"
  set "DB_PASSWORD=hms_password"
  ```

### Deployment Credentials

**Tomcat Manager User** (`conf/tomcat-users.xml`):
```xml
<user username="deployer" password="(random)" roles="manager-script"/>
```
- Jenkins uses this credential to upload/deploy WARs
- Password stored securely in Jenkins credentials, never in source code

### Health Checks

Both deployments expose `/hms/health` endpoint:
```json
{
  "status": "UP",
  "database": "UP",
  "detail": "connected"
}
```
- Jenkins smoke test polls this endpoint
- Docker compose healthcheck validates the init scripts ran
- Clients can monitor app status

---

## How to Run

### Prerequisites

1. **Java 17+**: `C:\Program Files\Java\jdk-25.0.4`
2. **Docker Desktop**: Running (includes Docker Engine and Docker Compose)
3. **Git**: For cloning/pushing code
4. **Maven 3.9.16**: `C:\tools\apache-maven-3.9.16` (for local builds)
5. **Tomcat 10.1.60**: `C:\tools\apache-tomcat-10.1.60` (for Jenkins deployments)

### Quick Start (For Demos)

**Double-click the desktop shortcut**:
```
MediFlow Demo.lnk
```

This runs `scripts/run-for-demo.bat`, which:
1. Starts Docker Desktop
2. Launches `docker compose up -d`
3. Starts Tomcat (`startup.bat`)
4. Opens the app in your browser (http://localhost:8090/hms/)

**Result**: App is running, ready to demo. Sign in with:
- Username: `admin`
- Password: `admin123`

### Manual Startup

**Docker (development)**:
```bash
cd "Hospital management system"
docker compose up -d
# App appears on http://localhost:8090/hms/ in ~30 seconds
```

**Tomcat (Jenkins-deployed copy)**:
```bash
# Windows
C:\tools\apache-tomcat-10.1.60\bin\startup.bat
# macOS/Linux
/path/to/tomcat/bin/startup.sh

# App appears on http://localhost:9090/hms/
```

**Shutdown**:
```bash
docker compose down          # Stop Docker containers
# Close Tomcat console window or: shutdown.bat
```

### Local Development Build

```bash
cd "Hospital management system"
mvn clean package
# Creates target/hms.war (~7.7 MB)
# Runs all 84 tests
```

---

## Architecture Summary

```
┌──────────────────────────────────────────────────────────────┐
│                        Developer                              │
│  - Writes code (Java, JSP, CSS, JS)                           │
│  - Commits to GitHub                                          │
└─────────────────────────┬──────────────────────────────────────┘
                          │
                          │ git push
                          ↓
┌──────────────────────────────────────────────────────────────┐
│                    GitHub Repository                         │
│  - Public repo (anyone can see code)                          │
│  - Triggers Jenkins polling (every 5 min)                     │
└─────────────────────────┬──────────────────────────────────────┘
                          │
                          │ New commit detected
                          ↓
┌──────────────────────────────────────────────────────────────┐
│                    Jenkins CI/CD                              │
│  1. Checkout from GitHub                                      │
│  2. mvn compile (build)                                       │
│  3. mvn test (84 tests with H2)                               │
│  4. mvn package (creates WAR)                                 │
│  5. Deploy to Tomcat 9090 (if main branch)                    │
│  6. Smoke test /hms/health                                    │
│  7. Promote (save rollback WAR)                               │
└─────────────────────────┬──────────────────────────────────────┘
                          │
                 ┌────────┴────────┐
                 ↓                 ↓
    ┌─────────────────────┐  ┌──────────────────┐
    │  Tomcat (9090)      │  │  Docker (8090)   │
    │  - Jenkins deploys  │  │  - Dev/demo use  │
    │  - Production WAR   │  │  - MySQL in      │
    │                     │  │    container     │
    └─────────────────────┘  └──────────────────┘
             │                        │
             └────────────┬───────────┘
                          ↓
                   Both serve:
              http://localhost:9090/hms/
              http://localhost:8090/hms/
              (Same code, different instances)
```

---

## Key Takeaways

✅ **Frontend**: Claymorphism design, responsive, accessible dropdowns, progressive enhancement  
✅ **Backend**: Secure (PBKDF2, CSRF, role-based access), transactional data (invoices, prescriptions)  
✅ **Build**: Maven manages dependencies and testing (84 tests)  
✅ **CI/CD**: Jenkins auto-builds on GitHub push, runs tests, deploys if green, rolls back if smoke test fails  
✅ **Containerization**: Docker Compose for easy local dev, Dockerfile for production images  
✅ **Deployment**: One WAR runs in Docker, Tomcat, and Jenkins — configuration via environment variables  
✅ **Version Control**: Git tracks all code, scripts, and infra; GitHub is the source of truth  

**Result**: A hospital app that can be **built, tested, and deployed automatically**, with the ability to **rollback** if anything goes wrong — all triggered by a simple `git push`.

---

*Generated: 24 September 2026*
