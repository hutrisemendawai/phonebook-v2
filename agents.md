# AGENTS.md — Phonebook

Guidance for AI coding agents working in this repository.

## What this project is

A Spring Boot web application for managing a company phonebook. It tracks employees
(`User`), their phone `Extension` records (multiple per person, of different types),
which `Company` and `Location` they belong to, plus login accounts (`AppUser`).

UI is server-rendered with Thymeleaf. Auth is form-based Spring Security. Data lives
in MySQL. User data can also be imported from Excel (Apache POI).

## Tech stack

- Java 21
- Spring Boot 3.5.5
- Spring MVC + Thymeleaf (server-rendered HTML)
- Spring Data JPA / Hibernate
- Spring Security + `thymeleaf-extras-springsecurity6`
- MySQL (`mysql-connector-j`)
- Apache POI (`poi-ooxml` 5.2.5) for Excel import/template
- Lombok
- Maven (use the included `mvnw` / `mvnw.cmd` wrapper)

## Build & run

```bash
# On Windows use mvnw.cmd; on Unix use mvnw
mvnw spring-boot:run
mvnw test
mvnw clean package
```

App runs on `http://localhost:8080` by default.

Database is configured in `src/main/resources/application.properties`:

- DB: `jdbc:mysql://localhost:3306/phonebook`
- `spring.jpa.hibernate.ddl-auto=update` (schema is auto-created/updated, never drops)
- `spring.jpa.show-sql=true`
- DevTools auto-restart + LiveReload are enabled

## Package layout

Base package: `com.pb.phonebook`

| Package | Purpose |
| --- | --- |
| `entity` | JPA entities |
| `repository` | Spring Data JPA repositories |
| `service` | Business logic (interface + `*ServiceImpl`) |
| `dto` | Request/view transfer objects |
| `controller` | MVC controllers |
| `config` | Security config + one-time `CommandLineRunner` migrations |

Thymeleaf templates live in `src/main/resources/templates/`, grouped by feature:

```
templates/
  home/index.html
  login.html
  register.html
  guide/index.html
  users/       (list, create, edit, import, analog-ancol)
  companies/   (list, create, edit)
  locations/   (list, create, edit)
```

## Domain model

### `User` (table `users`)

The main phonebook entry. Important details:

- **Manual ID, no `@GeneratedValue`.** The ID is assigned by the service so deleted
  IDs can be reused. `User` implements `Persistable<Long>` with a transient
  `newEntity` flag; `isNew()` returns that flag. When creating a user, build it with
  `.newEntity(true)` so JPA inserts instead of updates.
- `name` (required), `email` (optional)
- `company` → `Company` (`ManyToOne`, nullable)
- `location` → `Location` (`ManyToOne`, nullable)
- `extensions` → `Extension` (`OneToMany`, `cascade = ALL`, `orphanRemoval = true`,
  `@Builder.Default` initialized to empty list)

### `Extension` (table `extensions`)

- `type` (free-form, e.g. `IPPBX`, `Cisco`, `PABX`, `Zoom`)
- `number`
- `user` → `User` (`ManyToOne`, required)

### `Company` (table `companies`)

- `name` (unique, required)
- `code` (unique, required)

### `Location` (table `locations`)

- `name`, `region`, `subregion` (all required)
- `extCode` (optional)

### `AppUser` (table `users_login`)

Login account, separate from `User`:

- `username` (unique), `password` (BCrypt), `role` (`USER` or `ADMIN`)

## Layer conventions

Follow these patterns when adding features.

### Entities

- Use `@Entity`, Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.
- No public constructors in hand-written code; rely on Lombok.
- One JPA entity per table; no DTOs leaking into entities.

### Repositories

- Extend `JpaRepository`.
- Use JPQL `@Query` for non-trivial queries with `@Param`.
- Derived query methods (`findByEmailIgnoreCase`, `findByNameIgnoreCase`, etc.)
  are fine for simple lookups.

### Services

- Expose an interface (e.g. `UserService`) and implement it in `*ServiceImpl`.
- Use `@Service` + Lombok `@RequiredArgsConstructor` for constructor injection
  (never field `@Autowired`).
- Keep private `mapToDto(...)` / `mapToEntity(...)` mappers inside the impl.
- Annotate mutating/write methods with `@Transactional` where appropriate.
- Return DTOs from services; controllers never touch entities directly.

### Controllers

- `@Controller` (MVC, returns Thymeleaf view names), not `@RestController`.
- Use `@RequiredArgsConstructor` injection.
- Flash messages go through `RedirectAttributes` with keys `successMessage` and
  `errorMessage`.
- Use `@ModelAttribute("name")` for form binding; return `redirect:/...` after
  mutations (Post/Redirect/Get).

## Search, filtering, sorting & pagination

`GET /users` supports:

- `keyword` — matches name, email, company name, location name, subregion, extension
  number, or extension type (case-insensitive `LIKE`).
- `subregion` — filters locations by subregion.
- `locationId` — filters by exact location.
- `sortField` (`name`, `email`, `company`, `location`) + `sortDir` (`asc`/`desc`).

Pagination is **in-memory**: `UserServiceImpl.searchUsersPaginated` calls the
non-paginated `searchUsers`, deduplicates by user ID preserving sort order, then wraps
a sublist in `PageImpl`. Page size is 15. If you add heavy search features, be aware
this is not DB-level pagination.

## Duplicate handling & extension merge

When creating/importing a user, the system detects an "exact duplicate" by the
combination `name + company + location + subregion` (see
`UserRepository.findExactDuplicate`). If found:

- Extensions are merged (deduplicated by `type|number`, case-insensitive).
- Existing email is kept if the new one is empty.
- Otherwise a new user is created.

The same merge logic is used by the Excel import (`UserImportService`).

## Excel import

`UserImportService` both generates the import template and parses uploads. Columns:

```
Name, Email, Company, Location, Subregion, Extension Type, Extension Number
```

- Rows are grouped by `name + company + location + subregion` and merged into one user
  with multiple extensions.
- Company/Location are resolved by name (must already exist in DB); unknown values are
  recorded as row errors in `ImportResultDto`.
- Max upload size is 10MB (configured in `application.properties`).

## Security rules

`SecurityConfig` uses Spring Security 6 lambda DSL. Access rules are **order-sensitive**:

1. Public pages: `/`, `/home`, `/login`, `/panduan`, `/user-analog-ancol`, static
   resources, `/register/**`, `/favicon.ico`, `/error`.
2. `ADMIN`-only user mutations: `/users/new`, `/users/edit/**`, `/users/delete/**`,
   `/users/import/**`, and `POST /users`.
3. `ADMIN`-only sections: `/companies/**`, `/locations/**`.
4. **Public read** access to `GET /users`, `/users/`, `/users/**` — anyone can view the
   phonebook list.
5. Everything else requires authentication.

Passwords are BCrypt via `PasswordEncoder`. Self-registration always sets role `USER`;
admin accounts must be created manually (DB or a protected admin endpoint).

When changing routes, keep the admin rules **before** the public `GET /users/**` rule,
or they will be swallowed by it.

## One-time migration runners

There are `CommandLineRunner` beans in `config/` gated by flags. They run destructive,
one-shot DB changes and are disabled by default. Keep them disabled in normal runs.

- `ExtensionMigrationRunner` — `app.migrate-extensions=true`: merges legacy split rows
  (e.g. "Hasan Cisco") into one `User` with multiple `Extension`s, then drops the old
  `users.extension` column.
- `DepartmentCleanupRunner` — `app.remove-department=true`: drops `users.department_id`
  and the `departments` table.
- `LocationReloadRunner` — `app.reload-locations=true`: replaces all locations from
  `src/main/resources/data/locations.csv` and **clears every user's location
  assignment** (users must be reassigned manually afterward).

All three log their progress with `@Slf4j`. Always require/mention a DB backup before
running these.

## Extension types

Extension `type` is not an enum. The distinct types present in the DB drive the UI
dropdown; if none exist yet, it falls back to `IPPBX, Cisco, PABX, Zoom`. The fallback
constant is duplicated in `UserController` and `UserImportService` — update both if the
defaults change.

## Style & gotchas

- Code comments are currently a mix of Indonesian and English; match surrounding style
  when editing.
- Lombok `@Builder` + `@Builder.Default` is used for collections (e.g.
  `User.extensions`) to avoid NPEs.
- `Company.name` and `Company.code` are `unique = true`; handle
  `DataIntegrityViolationException` if you add validation around duplicates.
- Keep DTO <-> entity mapping private in service impls.
- There is only a basic `@SpringBootTest` context test; no meaningful test suite yet.
