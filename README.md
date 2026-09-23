# Hospital Management System

A Java web application for managing patients, doctors and appointments, built and
deployed through a DevOps pipeline: **GitHub → Jenkins → Maven → Apache Tomcat**.

The application is a classic Servlet/JSP web app packaged as a WAR, backed by MySQL.

---

## Contents

- [Features](#features)
- [Technology stack](#technology-stack)
- [Project layout](#project-layout)
- [Quick start with Docker](#quick-start-with-docker)
- [Port allocation](#port-allocation)
- [Running without Docker](#running-without-docker)
- [Configuration](#configuration)
- [Building and testing](#building-and-testing)
- [The Jenkins pipeline](#the-jenkins-pipeline)
- [Setting up Jenkins](#setting-up-jenkins)
- [Troubleshooting](#troubleshooting)

---

## Features

| Area | What it does |
|---|---|
| **Authentication** | Username/password sign-in with PBKDF2-hashed passwords, session timeout and CSRF protection. |
| **Role-based access** | Three roles — Administrator, Doctor, Receptionist — enforced on every request. |
| **Dashboard** | Counts and upcoming bookings, tailored to the signed-in role. |
| **Patients** | Register, edit and delete patients, including insurance and medical history; search by name, phone or email. |
| **Doctors** | Consulting roster with fees, room, weekly schedule and availability. |
| **Appointments** | Book, reschedule, complete and cancel. Prevents double-booking a doctor for the same slot. |
| **Prescriptions** | Record diagnosis and medicines per consultation. Doctors see only their own. |
| **Billing** | Invoices with line items, tax and status. Generate one from an appointment to pre-fill the consultation fee. |
| **Reports** | Operational and clinical analytics: monthly trends, busiest doctors, revenue, patient demographics, top medicines. |
| **Staff accounts** | Administrators create and manage sign-in accounts. |
| **Health endpoint** | `GET /hms/health` returns JSON reporting application and database status. The pipeline uses it as a post-deployment smoke test. |

Server-side validation covers required fields, phone and email format, future-dated
births, negative fees, past-dated bookings, double-booking, invalid consulting
windows, incomplete insurance records, and empty prescriptions or invoices.

### Roles

| Role | May reach |
|---|---|
| **Administrator** | Everything, including staff accounts and the doctor roster. |
| **Doctor** | Patients, appointments, prescriptions, reports. Not billing, the roster, or accounts. Sees only their own prescriptions. |
| **Receptionist** | Patients, appointments, billing, reports. Not prescriptions — prescribing is a medical act. |

Roles are defined once in [`Role.java`](src/main/java/com/hms/model/Role.java). The
navigation menu and the request filter both ask the same `canAccess` method, so a
hidden menu item and a blocked URL can never disagree. Hiding a link is a
convenience; [`AuthFilter`](src/main/java/com/hms/web/AuthFilter.java) is the control.

### Default accounts

On first start, when the users table is empty, three accounts are created:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Administrator |
| `dr.menon` | `doctor123` | Doctor |
| `reception` | `reception123` | Receptionist |

> **These are development credentials.** Change them before the system holds real
> data. Set `HMS_BOOTSTRAP_ADMIN_PASSWORD` to choose the administrator password, or
> `HMS_BOOTSTRAP=false` to skip seeding entirely. Once any account exists, the
> bootstrap never runs again.

### Security measures

| Measure | Where |
|---|---|
| Passwords hashed with PBKDF2-HMAC-SHA256, per-password salt, 210,000 iterations | `PasswordHasher` |
| Constant-time hash and token comparison | `PasswordHasher`, `CsrfToken` |
| Session fixation prevented — a new session is issued on sign-in | `SessionUser.signIn` |
| CSRF token required on every state-changing request | `AuthFilter` |
| Identical error for unknown user, wrong password and disabled account | `LoginServlet` |
| SQL injection prevented — every query is a parameterised `PreparedStatement` | all DAOs |
| Output escaped through JSTL `<c:out>` | all views |
| CSP, `X-Frame-Options`, `nosniff`, `Referrer-Policy` | `SecurityHeadersFilter` |
| Authenticated pages marked no-store, so Back after sign-out shows nothing | `AuthFilter` |

---

## Technology stack

| Layer | Choice |
|---|---|
| Language | Java 17 |
| Web | Jakarta Servlet 6.0 + JSP/JSTL (Jakarta EE 10) |
| Database | MySQL 8.4, accessed with JDBC |
| Connection pool | HikariCP |
| Build | Maven (WAR packaging) |
| Tests | JUnit 5, H2 in MySQL-compatibility mode |
| Coverage | JaCoCo |
| CI/CD | Jenkins declarative pipeline |
| Runtime | Apache Tomcat 10.1 |

> **Why Jakarta, not `javax`?** Tomcat 10 and later implement Jakarta EE, where the
> servlet packages were renamed from `javax.servlet` to `jakarta.servlet`. Code written
> for Tomcat 9 will not run on Tomcat 10 without that change.

---

## Project layout

```
.
├── Jenkinsfile                  CI/CD pipeline definition
├── Dockerfile                   Multi-stage build: Maven -> Tomcat
├── docker-compose.yml           Local stack (MySQL + app, optional Jenkins)
├── pom.xml                      Maven build
├── docker/mysql/init/           01-schema.sql, 02-seed.sql - run on first MySQL startup
├── scripts/                     Database backup and restore
├── docs/PROJECT-REPORT.md       Written project report
└── src/
    ├── main/
    │   ├── java/com/hms/
    │   │   ├── config/          Settings resolution (env -> system property -> file)
    │   │   ├── db/              HikariCP connection pool
    │   │   ├── model/           Patient, Doctor, Appointment, Prescription, Invoice, User, Role
    │   │   ├── dao/             JDBC data access
    │   │   ├── security/        Password hashing, CSRF, session, account bootstrap
    │   │   └── web/             Servlets and filters
    │   ├── resources/           application.properties
    │   └── webapp/
    │       ├── WEB-INF/views/   JSP views (not directly reachable by URL)
    │       ├── WEB-INF/web.xml  Error pages and session config
    │       ├── css/
    │       └── js/app.js        Progressive enhancement only
    └── test/                    JUnit 5 tests + H2 schema
```

---

## Database backups

`mysqldump` with `--single-transaction`, so the application keeps serving while the
backup runs.

```bash
./scripts/backup-db.sh                      # Linux / macOS
.\scripts\backup-db.ps1                     # Windows
./scripts/restore-db.sh backups/hospital_db-20260923-020000.sql.gz
```

Backups are timestamped, gzipped, and pruned after `RETENTION_DAYS` (default 14).
The script checks the dump is a plausible size before declaring success, because a
dump that failed midway can still leave a small, valid-looking file.

Schedule it nightly:

```bash
# Linux
0 2 * * * cd /opt/hms && ./scripts/backup-db.sh >> /var/log/hms-backup.log 2>&1
```
```powershell
# Windows
schtasks /create /tn "HMS backup" /tr "powershell -File C:\hms\scripts\backup-db.ps1" /sc daily /st 02:00
```

### Database migrations

`docker/mysql/init/` only runs against an **empty** data volume, so it is the
first-install path, not an upgrade path. Once a database holds real data, schema
changes go in `db/migrations/` as numbered, forward-only files
(`V2__add_referrals.sql`), applied in order and never edited once released. Keep
`01-schema.sql` in step so a fresh install and a migrated one end up identical.

---

## Quick start with Docker

The fastest way to see the application running. Requires Docker Desktop.

```bash
docker compose up -d --build
```

Then open **<http://localhost:8090/hms/>**.

> **Why 8090 and not 8080?** Jenkins also defaults to port 8080, and two services
> cannot share one. The app is published on 8090 so both can run at once. Override
> with `APP_PORT=9000 docker compose up -d` if that port is taken too.
> See [Port allocation](#port-allocation).

The database is created and seeded automatically with five patients, five doctors
and five appointments, so the dashboard is populated on first load.

Useful commands:

```bash
docker compose logs -f app      # follow application logs
docker compose ps               # container status and health
docker compose down             # stop, keeping data
docker compose down -v          # stop and wipe the database (re-runs the seed script)
```

MySQL is also published on **port 3307** if you want to inspect it with a client:

```bash
mysql -h 127.0.0.1 -P 3307 -u hms_user -phms_password hospital_db
```

> The seed script in `docker/mysql/init/` only runs when the data volume is empty.
> After editing it, run `docker compose down -v` to see the changes.

---

## Port allocation

Jenkins and Tomcat both default to **8080**, so a machine running this project end to
end needs them separated. The layout this project assumes:

| Service | Port | Set where |
|---|---|---|
| Jenkins | `8080` | Its installed default. |
| Docker app stack | `8090` | `APP_PORT` in `docker-compose.yml`. |
| Standalone Tomcat (the pipeline's deploy target) | `9090` | `<Connector port="9090">` in `<tomcat>/conf/server.xml`. |
| MySQL (Docker) | `3307` | `docker-compose.yml`, to avoid a local MySQL on 3306. |
| Jenkins in Docker (`ci` profile) | `8081` | `docker-compose.yml`. |

To change the standalone Tomcat port, edit the HTTP connector in
`<tomcat>/conf/server.xml` and restart it:

```xml
<Connector port="9090" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" />
```

If you change it, update `TOMCAT_URL` and `HEALTH_URL` in the `environment` block of
the [`Jenkinsfile`](Jenkinsfile) to match.

To find what is holding a port:

```bash
# Windows
netstat -ano | findstr :8080
# Linux / macOS
lsof -i :8080
```

---

## Running without Docker

You will need JDK 17+, Maven 3.9+, MySQL 8, and Tomcat 10.1+.

**1. Create the database**

```bash
mysql -u root -p < docker/mysql/init/01-schema.sql
```

**2. Create the application's MySQL user**

```sql
CREATE USER 'hms_user'@'localhost' IDENTIFIED BY 'hms_password';
GRANT ALL PRIVILEGES ON hospital_db.* TO 'hms_user'@'localhost';
FLUSH PRIVILEGES;
```

**3. Build the WAR**

```bash
mvn clean package
```

**4. Deploy it**

Copy `target/hms.war` into `<tomcat>/webapps/`, then start Tomcat:

```bash
# Linux / macOS
<tomcat>/bin/startup.sh
# Windows
<tomcat>\bin\startup.bat
```

The application is at `http://localhost:<tomcat-port>/hms/` — 8080 on a default Tomcat,
or 9090 if you moved the connector to keep clear of Jenkins as described under
[Port allocation](#port-allocation).

---

## Configuration

Settings resolve in this order, first match wins:

1. **Environment variable** — `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
2. **JVM system property** — `-Ddb.url=...`
3. **`src/main/resources/application.properties`** — the packaged defaults

| Property | Environment variable | Default |
|---|---|---|
| `db.url` | `DB_URL` | `jdbc:mysql://localhost:3306/hospital_db?...` |
| `db.username` | `DB_USERNAME` | `hms_user` |
| `db.password` | `DB_PASSWORD` | `hms_password` |
| `db.pool.maxSize` | `DB_POOL_MAX_SIZE` | `10` |

This is what lets one WAR run unchanged in local, Docker and Jenkins-deployed
environments — only the environment differs, never the artifact.

> **Security note.** The credentials in this repository are development defaults.
> For anything real, inject them as environment variables (or Jenkins credentials)
> and keep them out of version control.

---

## Building and testing

```bash
mvn clean package     # compile, test, and build target/hms.war
mvn test              # tests only
```

The 83 tests exercise the DAO layer against **H2 running in MySQL-compatibility
mode**, plus the password hashing, access rules and billing arithmetic. That means
`mvn test` needs no database server — important for CI, where provisioning MySQL
just to run unit tests would make every build slower and more fragile. The same SQL
the application issues against MySQL is what the tests run.

| Suite | Tests | Covers |
|---|---:|---|
| `PatientDAOTest` | 9 | CRUD, search, null handling, ordering |
| `DoctorDAOTest` | 6 | CRUD, availability filtering, fee defaults |
| `AppointmentDAOTest` | 8 | Conflicts, cancelled slots, cascade deletes |
| `PrescriptionDAOTest` | 9 | Header + medicines in one transaction, per-doctor scoping |
| `InvoiceDAOTest` | 13 | Numbering, totals, status, cascades |
| `UserDAOTest` | 11 | Accounts, password changes, admin counting |
| `InvoiceTest` | 8 | Money arithmetic and rounding |
| `RoleTest` | 7 | Access rules per role |
| `PasswordHasherTest` | 7 | Hashing, salting, malformed input |
| `AppConfigTest` | 5 | Settings resolution order |

Coverage report after a build: `target/site/jacoco/index.html`.

---

## The Jenkins pipeline

Defined in [`Jenkinsfile`](Jenkinsfile). Every shell step goes through a helper
that picks `bat` or `sh`, so the pipeline runs on a Windows or Linux agent.

| Stage | What it does | Runs on |
|---|---|---|
| **Checkout** | Pulls the commit from GitHub, records its SHA on the build. | every branch |
| **Build** | `mvn clean compile`. | every branch |
| **Test** | `mvn test`, publishes JUnit results and the JaCoCo report. | every branch |
| **Package** | `mvn package -DskipTests`, archives `hms.war` as a build artifact. | every branch |
| **Deploy to Tomcat** | Saves the currently-live WAR, then uploads the new one via the Tomcat Manager API. | `main` only |
| **Smoke test** | Polls `/hms/health` up to 20 times until it reports `UP`. | `main` only |
| **Promote** | Records this WAR as the known-good version to roll back to. | `main` only |
| **Rollback** (on failure) | Redeploys the last known-good WAR if a deployment failed its smoke test. | `main` only |

### Rollback

The deploy stage copies the currently-live WAR aside *before* replacing it. Without
that there is nothing to roll back **to**, which is why a rollback stage bolted onto
the end of a pipeline usually cannot work. If the smoke test then fails, the `post`
block redeploys the saved WAR and re-checks health.

Rollback runs only when a deployment actually reached the server and then failed —
a compile or test failure never got that far, so redeploying over a healthy server
would be wrong. The first ever build has nothing saved and says so.

Four deliberate choices worth noting:

- **Compile and test run before packaging.** A failing test can never produce a
  WAR that might be deployed by accident.
- **Only `main` deploys.** Feature branches get the full build-and-test feedback
  without touching the running server.
- **Deployment is verified, not assumed.** Tomcat's deploy call returns before the
  application has finished starting, and an app can unpack successfully but still
  fail to reach MySQL. The smoke test polls the health endpoint, which checks the
  database connection, so a broken deployment fails the build instead of going unnoticed.

---

## Setting up Jenkins

**1. Tools** — *Manage Jenkins → Tools*

| Type | Name | Notes |
|---|---|---|
| JDK | `jdk17` | Install automatically, or point at a local JDK 17+ |
| Maven | `maven3` | Install automatically (3.9.x) |

The names must match the `tools` block in the Jenkinsfile exactly.

**2. Tomcat manager user** — add to `<tomcat>/conf/tomcat-users.xml` and restart Tomcat:

```xml
<role rolename="manager-script"/>
<user username="deployer" password="CHANGE_ME" roles="manager-script"/>
```

`manager-script` is the role that grants access to the text API the pipeline calls.
It is separate from `manager-gui`, which is the web console.

**3. Credentials** — *Manage Jenkins → Credentials → Global → Add*

| Kind | ID | Value |
|---|---|---|
| Username with password | `tomcat-manager` | the `deployer` user above |

**4. Create the job**

*New Item → Pipeline*, then under *Pipeline*:

- Definition: **Pipeline script from SCM**
- SCM: **Git**, repository URL: your GitHub repository
- Branch: `*/main`
- Script Path: `Jenkinsfile`

**5. Trigger builds from GitHub**

- In the job: tick **GitHub hook trigger for GITScm polling**.
- On GitHub: *Settings → Webhooks → Add webhook*, payload URL
  `http://<your-jenkins-host>/github-webhook/`, content type `application/json`,
  event: *Just the push event*.

Jenkins must be reachable from GitHub for the webhook to arrive. If it runs on
your own machine, either expose it with a tunnel or switch the job to
**Poll SCM** (`H/5 * * * *`) instead.

**Trying the pipeline locally?** `docker compose --profile ci up -d jenkins` starts
Jenkins on <http://localhost:8081> with the setup wizard disabled.

---

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `/hms/health` returns `"status":"DOWN"` | The app is running but cannot reach MySQL. Check `DB_URL`, the user's grants, and that MySQL is accepting connections. The `detail` field names the underlying error. |
| `Communications link failure` on first start | MySQL was still initialising. The compose file waits for the healthcheck; outside Docker, start MySQL first. |
| `404` on `/hms/` | The WAR is not deployed, or deployed under a different context path. The path comes from the WAR name, set by `<finalName>` in `pom.xml`. |
| Jenkins: `mvn: not found` | The Maven tool is not named `maven3` in *Manage Jenkins → Tools*, or the `tools` block was removed. |
| Jenkins deploy returns `403` | The user lacks `manager-script`, or Tomcat's manager app restricts remote addresses. Both are configured on the Tomcat side. |
| `bind: address already in use` on startup | Another service holds the port — most often Jenkins on 8080. See [Port allocation](#port-allocation). |
| Jenkins deploy hits Jenkins itself | `TOMCAT_URL` still points at 8080, where Jenkins is running. Point it at the Tomcat port. |
| `ClassNotFoundException: javax.servlet.*` | Code or a dependency is written for Tomcat 9. This project targets Tomcat 10+ and the `jakarta.*` namespace. |
| Seed data did not change after editing the SQL | The init script only runs on an empty volume: `docker compose down -v && docker compose up -d`. |
