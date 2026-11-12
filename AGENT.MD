# TournUp architecture

Kotlin / Java 21, Spring Boot MVC, Spring Security, JPA, PostgreSQL,
Flyway migrations, and Hibernate Envers auditing. Gradle Kotlin DSL builds the project.

## Application structure

Base package: `src/main/kotlin/com/abeesmall/tournup/`.
Domain code is organised by responsibility, then resource (`season`, `tournament`):

| Directory | Responsibility |
| --- | --- |
| `controller/` | HTTP routes, request validation, exception mapping, response status |
| `service/` | Transactional domain operations, projections, shared query/response helpers |
| `model/` | JPA entities and persistence invariants; no UI annotations |
| `repository/` | Persistence queries, including latest audit reads |
| `dto/` | Request/response contracts and UI field policies |
| `manifest/` | Resource registrations, field catalogues, and view definitions |
| `ui/` | Shared manifest resolver, resource registry, and published layout handling |
| `validation/` | Shared validation annotations and constraints |
| `auth/`, `security/` | Authentication adapters, sessions, CSRF, and permission rules |
| `audit/` | Envers revisions and actor attribution |
| `catalog/` | Tenant, domain, and database-binding lookup in a separate catalog database |
| `tenancy/` | Tenant resolution, connection pools, Hibernate routing, and migrations |
| `credentials/` | Database credential resolution, including the OpenBao adapter |

## Key boundaries

- Tenant data lives in separate databases accessed through dedicated database roles.
  Resolve the authenticated tenant's catalog binding before selecting its connections.
- Credential references identify secrets; credentials are resolved separately from catalog metadata.
- The backend supplies UI manifests. Resource registrations declare operations and views;
  the shared resolver combines them with DTO field policies and request context.
- The feature application (currently `tournament-manager`) is distinct from the web client.
- Record responses use `manifest`, `item`, and `included`; collections use `manifest`,
  `items`, `pagination`, and `included`. Reference selectors use lightweight cursor lookups.
- Audit metadata is permission-controlled. Enrich write responses after the service
  transaction commits so the Envers revision exists.

## Other key paths

- `src/main/resources/db/migration/`: tenant database migrations.
- `src/main/resources/db/catalog/`: catalog database migrations.
- `src/main/resources/db/seed/`: explicit seed SQL.
- `src/main/resources/application*.properties`: Spring profile configuration.
- `src/test/kotlin/`: tests mirroring application packages; controller tests use doubles,
  while PostgreSQL persistence tests use Testcontainers.
- `src/tools/`: one-shot Kotlin tools, including the `seedTournaments` Gradle task.
- `examples/api/`: readable API contract samples.
- `compose.dev.yaml`: local PostgreSQL; run the application natively for development.
- `compose.yaml`, `compose.openbao-preview.yaml`, `deploy/`: container deployment and OpenBao Agent configuration.
- `.github/workflows/build.yml`: CI build, tests, image build, and registry publishing.

## Working conventions

Reuse shared resource/query/response infrastructure when adding models. Keep domain
rules explicit, UI metadata off ORM entities, and permission checks on the server.
Do not add compatibility aliases unless requested. Keep changes small and reviewable.
Never commit secrets or print local credential files.

Compile with `./gradlew testClasses`; run tests with `./gradlew test` or select suites
using `--tests '<fully.qualified.TestClass>'`. PostgreSQL integration tests require Docker.
