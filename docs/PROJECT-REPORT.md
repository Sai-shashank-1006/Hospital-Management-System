# Hospital Management System
### A DevOps-Based Web Application Using GitHub, Jenkins, Maven & Apache Tomcat

---

## Abstract

This project implements a Hospital Management System as a Java web application and,
more importantly, delivers it through an automated DevOps pipeline. The application
manages patients, doctors and appointments using Java Servlets and JSP, backed by a
MySQL database, and is packaged by Maven into a deployable WAR archive.

The emphasis of the project is the delivery process rather than the application alone.
A change pushed to GitHub triggers a Jenkins pipeline that compiles the source, runs an
automated test suite, packages the artifact, deploys it to Apache Tomcat, and then
verifies that the deployed application is genuinely running and able to reach its
database. Any failure along that path stops the pipeline and fails the build, so a
defective change cannot reach the running server unnoticed.

---

## 1. Introduction

### 1.1 Background

Hospitals handle large volumes of record-keeping: patient registration, doctor rosters,
and appointment scheduling. Handled on paper or in spreadsheets, these tasks are slow,
error-prone and hard to search. A web-based system centralises the data and makes it
available to staff from any machine on the network.

### 1.2 The problem this project addresses

Building such an application is only part of the work. In traditional practice, a
developer builds the software on their own machine and hands over an archive to be
installed manually. This creates well-known problems:

- **"It works on my machine."** Differences between the developer's environment and
  the server produce failures that appear only after deployment.
- **Manual deployment is error-prone.** Copying files by hand invites mistakes and is
  difficult to repeat identically.
- **Defects are found late.** Without automated testing on every change, a regression
  may not surface until someone happens to exercise that feature.
- **No audit trail.** When something breaks, it is unclear which change caused it.

DevOps addresses these problems by automating the path from source code to running
software, and by making that path repeatable and observable.

### 1.3 Objectives

1. Develop a functional Hospital Management System with Servlets, JSP and MySQL.
2. Manage the source code in a Git repository hosted on GitHub.
3. Automate compilation, dependency management and packaging with Maven.
4. Build an automated test suite that runs on every change.
5. Construct a Jenkins pipeline that builds, tests, packages and deploys automatically.
6. Deploy the resulting artifact to Apache Tomcat without manual intervention.
7. Verify each deployment automatically rather than assuming it succeeded.

### 1.4 Scope

The system covers patient records, the doctor roster, and appointment scheduling,
together with the full build-and-deployment pipeline. Authentication, billing,
pharmacy and laboratory modules are outside the scope; Section 11 discusses these as
future extensions.

---

## 2. Literature and background

### 2.1 What DevOps means here

DevOps is a set of practices that shorten the time between writing a change and having
it running reliably, by automating the steps in between. Three practices are central to
this project:

- **Continuous Integration (CI)** — every change is merged into a shared repository and
  automatically built and tested. Problems are found within minutes.
- **Continuous Delivery/Deployment (CD)** — a change that passes its checks is deployed
  automatically, so deployment becomes routine rather than an event.
- **Infrastructure as Code** — the environment is described in files kept in version
  control (here, the `Dockerfile` and `docker-compose.yml`), so it can be recreated
  identically by anyone.

### 2.2 The tools and why each was chosen

| Tool | Role | Why this tool |
|---|---|---|
| **Git / GitHub** | Version control, change history, pipeline trigger | The de facto standard; GitHub's webhooks let a push start a build automatically. |
| **Maven** | Build, dependency management, packaging | Declarative: `pom.xml` fully describes the build, so it behaves identically on a developer machine and on the CI agent. |
| **Jenkins** | Orchestrates the pipeline | Open-source, self-hostable, and its declarative pipeline lives in the repository as `Jenkinsfile`, so the build process is version-controlled alongside the code. |
| **Apache Tomcat** | Servlet container / runtime | The reference implementation for Servlets and JSP, with a Manager API that supports scripted deployment. |
| **MySQL** | Relational database | Widely deployed, strong support for the referential integrity this data model needs. |
| **JUnit 5 + H2** | Automated testing | H2 in MySQL-compatibility mode lets the test suite run without a database server. |
| **Docker** | Reproducible local environment | Brings up the full stack identically on any machine. |

---

## 3. System analysis

### 3.1 Existing system

Manual or spreadsheet-based record-keeping: data is duplicated across files, searching
is slow, appointment clashes are detected only by human attention, and there is no
enforced consistency between related records.

### 3.2 Proposed system

A centralised web application with a relational database enforcing consistency at the
storage layer. Specifically:

- Records are stored once and searchable instantly.
- Foreign-key constraints guarantee an appointment always refers to a real patient and
  a real doctor.
- Double-booking is prevented by an explicit check before an appointment is saved.
- Input validation rejects malformed data before it reaches the database.

### 3.3 Functional requirements

| ID | Requirement |
|---|---|
| FR-1 | Register, view, edit and delete patient records. |
| FR-2 | Search patients by name, phone number or email. |
| FR-3 | Maintain a doctor roster including specialization, fee and availability. |
| FR-4 | Book an appointment between a patient and an available doctor. |
| FR-5 | Reject a booking that would double-book a doctor at the same time. |
| FR-6 | Mark appointments completed or cancelled. |
| FR-7 | Display summary counts and upcoming appointments on a dashboard. |
| FR-8 | Expose a health endpoint reporting application and database status. |

### 3.4 Non-functional requirements

| ID | Requirement |
|---|---|
| NFR-1 | Deployment must be automated and repeatable. |
| NFR-2 | Every change must be built and tested automatically. |
| NFR-3 | The same artifact must run in any environment, configured externally. |
| NFR-4 | Database credentials must not be hard-coded in the deployed artifact. |
| NFR-5 | The interface must be usable on desktop and tablet screens. |

---

## 4. System design

### 4.1 Architecture

The application follows a layered MVC structure. Each layer depends only on the one
below it, which is what makes the data access layer testable in isolation.

```
        Browser
           │  HTTP
           ▼
┌──────────────────────┐
│  Servlets  (control) │   request handling, validation, dispatch
├──────────────────────┤
│  JSP views   (view)  │   presentation only, no business logic
├──────────────────────┤
│  DAOs       (model)  │   SQL, parameterised statements
├──────────────────────┤
│  HikariCP pool       │   pooled JDBC connections
└──────────┬───────────┘
           ▼
        MySQL
```

A request for `/patients/` reaches `PatientServlet`, which calls `PatientDAO` for the
data, places the result on the request, and forwards to `patients.jsp` for rendering.

The JSP views live under `WEB-INF/`, which the servlet container does not serve
directly. A view can therefore only be reached through a servlet, guaranteeing it is
never rendered without the data it expects.

### 4.2 Database design

Three tables, with appointments referencing the other two.

```
┌──────────────────┐         ┌────────────────────┐        ┌──────────────────┐
│    patients      │         │    appointments    │        │     doctors      │
├──────────────────┤         ├────────────────────┤        ├──────────────────┤
│ id          PK   │────┐    │ id             PK  │   ┌────│ id          PK   │
│ full_name        │    └───<│ patient_id     FK  │   │    │ full_name        │
│ gender           │         │ doctor_id      FK  │>──┘    │ specialization   │
│ date_of_birth    │         │ appointment_time   │        │ phone            │
│ phone            │         │ reason             │        │ email            │
│ email            │         │ status             │        │ consultation_fee │
│ address          │         │ created_at         │        │ available        │
│ blood_group      │         └────────────────────┘        │ created_at       │
│ created_at       │                                       └──────────────────┘
└──────────────────┘
```

**Relationships.** One patient has many appointments; one doctor has many appointments.
Both foreign keys use `ON DELETE CASCADE`, so removing a patient or doctor removes
their appointments rather than leaving rows pointing at records that no longer exist.
The user interface warns before such a delete.

**Indexes.** Beyond the primary keys, `idx_appointments_doctor_time` on
`(doctor_id, appointment_time)` supports the double-booking check, which is the most
frequent query on the table.

### 4.3 Module description

| Module | Responsibility |
|---|---|
| `config.AppConfig` | Resolves settings: environment variable, then system property, then bundled file. |
| `db.DataSourceProvider` | Creates and owns the HikariCP connection pool. |
| `model` | Plain Java objects: `Patient`, `Doctor`, `Appointment`. |
| `dao` | All SQL. Each DAO takes a `DataSource` through its constructor, which is what allows tests to supply H2 instead of MySQL. |
| `web` | Servlets: request parsing, validation, dispatch to views. |
| `web.HealthServlet` | Reports application and database status as JSON for the pipeline's smoke test. |
| `web.AppLifecycleListener` | Closes the connection pool on undeploy, so redeploying does not leak connections. |

---

## 5. Implementation notes

### 5.1 Configuration that follows the environment, not the artifact

A principle of continuous delivery is that the *same* artifact is promoted through
environments; only configuration differs. `AppConfig` resolves each setting from, in
order: an environment variable, a JVM system property, then the packaged defaults.

The same `hms.war` therefore runs against a developer's local MySQL, the Docker stack,
and a deployed server, with no rebuild — the environment supplies `DB_URL`,
`DB_USERNAME` and `DB_PASSWORD`. It also keeps real credentials out of the repository,
satisfying NFR-3 and NFR-4.

### 5.2 Connection pooling

Opening a database connection is expensive. HikariCP maintains a pool that is reused
across requests. The pool is created once, lazily, and closed by a
`ServletContextListener` on shutdown — without this, each redeployment during
development would leave MySQL connections stranded until they timed out.

### 5.3 Preventing SQL injection

Every query uses `PreparedStatement` with bound parameters, never string concatenation.
A patient searching for `'; DROP TABLE patients; --` searches for that literal text
rather than executing it, because the driver sends the value separately from the SQL.

### 5.4 Preventing double-booking

Before an appointment is saved, `AppointmentDAO.hasConflict` checks whether the doctor
already holds a non-cancelled appointment at that exact time. Two details matter:

- **Cancelled appointments do not block a slot** — a cancelled booking should free the
  time it held.
- **An edit excludes its own row** — otherwise saving an appointment without changing
  its time would report a conflict with itself.

Both behaviours are covered by tests.

### 5.5 Output escaping

All user-supplied values are rendered through JSTL's `<c:out>`, which escapes HTML.
A patient whose name contains `<script>` is displayed as text rather than executed,
preventing stored cross-site scripting.

---

## 6. Testing

### 6.1 Strategy

28 automated tests run on every build. They target the DAO layer, where the SQL and the
data rules live, because that is where a defect is both most likely and most costly.

The tests run against **H2 in MySQL-compatibility mode** rather than a real MySQL
server. This is a deliberate trade-off:

- **Benefit:** `mvn test` needs no database server, so the Jenkins agent requires no
  MySQL provisioning. Builds are faster and cannot fail for environmental reasons.
- **Cost:** H2 is not MySQL. Behaviour that differs between them — vendor-specific
  functions, exact type coercion — would not be caught.
- **Mitigation:** the test schema mirrors the production schema, including the foreign
  keys, and the DAOs issue exactly the same SQL in both cases. The post-deployment
  smoke test then exercises the real MySQL connection.

### 6.2 Coverage

| Suite | Tests | What it verifies |
|---|---|---|
| `PatientDAOTest` | 9 | CRUD, null date of birth, case-insensitive search across three columns, blank-term search, counts, ordering. |
| `DoctorDAOTest` | 6 | CRUD, null fee defaulting to zero, availability filtering, roster ordering. |
| `AppointmentDAOTest` | 8 | Joined names, conflict detection, cancelled slots freeing up, self-exclusion on edit, upcoming ordering and limit, status counts, cascade delete. |
| `AppConfigTest` | 5 | Environment-variable name mapping, resolution order, numeric fallbacks. |

Test result:

```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- AppConfigTest
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- AppointmentDAOTest
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- DoctorDAOTest
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- PatientDAOTest

BUILD SUCCESS
```

JaCoCo produces a coverage report at `target/site/jacoco/index.html`, published by
Jenkins on each build.

---

## 7. The CI/CD pipeline

### 7.1 Flow

```
Developer                GitHub               Jenkins                      Tomcat
    │                      │                     │                           │
    │  git push            │                     │                           │
    ├─────────────────────>│                     │                           │
    │                      │  webhook            │                           │
    │                      ├────────────────────>│                           │
    │                      │                     │ 1. Checkout               │
    │                      │                     │ 2. Build    (compile)     │
    │                      │                     │ 3. Test     (JUnit)       │
    │                      │                     │ 4. Package  (WAR)         │
    │                      │                     │ 5. Deploy ───────────────>│
    │                      │                     │ 6. Smoke test <───────────┤
    │   build result       │                     │                           │
    │<─────────────────────┴─────────────────────┤                           │
```

### 7.2 Stages

| Stage | Command / action | Fails the build when |
|---|---|---|
| Checkout | `checkout scm` | The repository or branch is unreachable. |
| Build | `mvn clean compile` | The code does not compile. |
| Test | `mvn test` | Any test fails. |
| Package | `mvn package -DskipTests` | Packaging fails. WAR is archived. |
| Deploy | `curl --upload-file` to Tomcat Manager | Tomcat rejects the upload (auth, permissions). |
| Smoke test | Polls `/hms/health` up to 20 times | The app does not report `UP` within 60 seconds. |

### 7.3 Design decisions in the pipeline

**Compiling and testing before packaging.** Packaging is a separate, later stage so
that a failing test can never produce a WAR that might be deployed by mistake.

**Deploying only from `main`.** Feature branches receive the full build-and-test
feedback, but do not touch the running server. Deployment is gated on the branch, not
on the developer remembering.

**Verifying the deployment rather than assuming it.** This is the stage that most often
gets omitted, and it is the most valuable. Tomcat's deploy call returns as soon as the
WAR is accepted — before the application has finished starting. An application can also
deploy perfectly and still be broken, for instance if it cannot reach the database.

The `HealthServlet` therefore opens a real database connection and validates it. The
pipeline polls that endpoint until it reports `UP`, or fails the build after 60 seconds
and prints the last response. A deployment that unpacks but cannot work is caught by the
pipeline rather than by a user.

**Credentials handled through Jenkins.** The Tomcat account is stored in Jenkins'
credential store and injected as masked environment variables, referenced with native
shell syntax so the password is never interpolated into the build log.

**Platform-independent steps.** Every shell step dispatches through a helper that picks
`bat` or `sh` based on `isUnix()`, so the same pipeline runs on a Windows or Linux agent.

---

## 8. Containerisation

`docker-compose.yml` brings up MySQL and the application together, giving an identical
environment on any machine.

The `Dockerfile` uses a **multi-stage build**: the first stage builds the WAR with
Maven, the second copies only that artifact onto a Tomcat image. Maven and the
downloaded dependencies never reach the final image, which keeps it substantially
smaller and removes build tooling from the runtime.

Two details worth noting:

- `pom.xml` is copied before the source so that Docker's layer cache only re-downloads
  dependencies when the dependency list actually changes, not on every source edit.
- The application container waits on MySQL's healthcheck rather than merely on its
  start, because the container is running well before the database is ready to accept
  connections.

---

## 9. Results

The completed system provides:

- A working Hospital Management System covering patients, doctors and appointments.
- Automated build and packaging via Maven.
- 28 automated tests, all passing, run on every change.
- A Jenkins pipeline taking a GitHub commit through to a verified Tomcat deployment
  with no manual step.
- A containerised environment reproducible on any machine with Docker.

Measured against the objectives in Section 1.3, all seven were met.

---

## 10. Challenges encountered

**The `javax` to `jakarta` namespace change.** Tomcat 10 implements Jakarta EE, in
which the servlet packages were renamed from `javax.servlet` to `jakarta.servlet`. Most
Servlet/JSP tutorials predate this and will not compile against Tomcat 10. The project
targets the `jakarta.*` namespace throughout, including the JSTL implementation, which
has its own Jakarta-compatible release.

**Testing database code without a database.** Requiring MySQL on the build agent would
have made builds slower and prone to environmental failure. Running H2 in
MySQL-compatibility mode, with DAOs accepting a `DataSource` through the constructor,
allowed the real SQL to be tested without a server. Section 6.1 discusses the limits of
this approach.

**Deployments that appear to succeed.** An early version of the pipeline ended at the
deploy stage and reported success, even though the application was not yet serving
requests. Tomcat acknowledges a WAR upload before the context has started. Adding the
health endpoint and a polling smoke test made the difference between "the file was
uploaded" and "the application is running and can reach its database".

**Container startup ordering.** The application initially failed on first run because
Tomcat started while MySQL was still initialising. Docker's `depends_on` alone only
waits for the container to start, not for the service inside it to be ready; a
healthcheck with `condition: service_healthy` was required.

---

## 11. Future enhancements

| Enhancement | Rationale |
|---|---|
| Authentication and role-based access | Patient data is sensitive; the current system has no login. This is the most important next step. |
| Staged environments | Deploy to a staging server first, promoting to production only after checks pass. |
| Automated rollback | Redeploy the previous archived WAR automatically when a smoke test fails. |
| Integration tests against real MySQL | Using Testcontainers, closing the H2/MySQL behavioural gap. |
| Static analysis and dependency scanning | SonarQube and OWASP Dependency-Check as pipeline stages. |
| Billing, pharmacy and lab modules | Broader hospital coverage. |
| Medical history per patient | Currently only appointments are recorded, not outcomes. |

---

## 12. Conclusion

This project set out to build a Hospital Management System and, more significantly, to
deliver it through an automated DevOps pipeline. Both were achieved.

The application demonstrates layered web development with Servlets, JSP and JDBC,
including parameterised queries, connection pooling, server-side validation and output
escaping. The pipeline demonstrates the practices that make delivery repeatable:
version control as the single source of truth, automated testing as a gate rather than
an afterthought, one artifact configured by its environment, and deployment verified
rather than assumed.

The most instructive part of the work was the last of these. It is straightforward to
write a pipeline that copies a file to a server and reports success. Establishing that
the deployed application is genuinely running, and able to reach its database, required
a health endpoint, a polling smoke test, and an understanding of why Tomcat returns from
a deployment before the application is ready. That distinction — between a step
completing and a system working — is the practical core of what DevOps automation is for.

---

## 13. References

1. Apache Maven Project — <https://maven.apache.org/guides/>
2. Jenkins User Documentation, Pipeline — <https://www.jenkins.io/doc/book/pipeline/>
3. Apache Tomcat 10.1 Documentation, Manager App HOW-TO — <https://tomcat.apache.org/tomcat-10.1-doc/manager-howto.html>
4. Jakarta Servlet Specification 6.0 — <https://jakarta.ee/specifications/servlet/6.0/>
5. MySQL 8.4 Reference Manual — <https://dev.mysql.com/doc/refman/8.4/en/>
6. HikariCP — <https://github.com/brettwooldridge/HikariCP>
7. JUnit 5 User Guide — <https://junit.org/junit5/docs/current/user-guide/>
8. Docker Documentation, Multi-stage builds — <https://docs.docker.com/build/building/multi-stage/>
9. Humble, J. and Farley, D., *Continuous Delivery*, Addison-Wesley, 2010.
10. OWASP SQL Injection Prevention Cheat Sheet — <https://cheatsheetseries.owasp.org/>

---

## Appendix A — Running the project

**With Docker (recommended):**

```bash
docker compose up -d --build
# http://localhost:8090/hms/
```

**Without Docker:** requires JDK 17+, Maven, MySQL 8 and Tomcat 10.1+.

```bash
mysql -u root -p < docker/mysql/init/01-schema.sql
mvn clean package
cp target/hms.war <tomcat>/webapps/
```

Full setup instructions, including Jenkins configuration, are in the repository
[`README.md`](../README.md).

## Appendix B — Repository layout

```
Jenkinsfile                CI/CD pipeline definition
Dockerfile                 Multi-stage build: Maven -> Tomcat
docker-compose.yml         Local stack (MySQL + app, optional Jenkins)
pom.xml                    Maven build definition
docker/mysql/init/         Schema and seed data
docs/PROJECT-REPORT.md     This report
src/main/java/com/hms/     config, db, model, dao, web
src/main/webapp/           JSP views, web.xml, stylesheet
src/test/java/com/hms/     JUnit 5 test suite
```
