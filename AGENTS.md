# AGENTS.md — ShopNow

E-commerce app. Monorepo: React frontend + Spring Boot backend.

## Project layout
- `/frontend` — React (JavaScript), Vite
- `/backend` — Java 17+, Spring Boot, Maven
- Keep frontend and backend changes in separate commits where possible.

## Commands
Frontend (run inside `/frontend`):
- Install: `npm ci`
- Dev server: `npm run dev`
- Test: `npm test`
- Lint: `npm run lint`
- Build: `npm run build`

Backend (run inside `/backend`):
- Run: `./mvnw spring-boot:run`
- Test: `./mvnw test`
- Build: `./mvnw clean package`

Always run the relevant test + lint/build command before saying a task is done.

## Frontend conventions (React + JavaScript)
- Functional components and hooks only; no class components.
- One component per file, PascalCase filenames (`ProductCard.jsx`); hooks in `hooks/` named `useXxx`.
- Use PropTypes on every component that takes props.
- All API calls go through a single module (`src/api/`); never call `fetch`/`axios` directly inside components.
- Handle loading, error and empty states for every data-fetching screen.
- Keep state local unless shared; use Context (or the existing store) for cart/auth only.
- Never store tokens or secrets in code; read config from `import.meta.env.VITE_*`.
- Accessible by default: semantic HTML, alt text, labels on inputs.

## Backend conventions (Java + Spring Boot)
- Layering: `controller` → `service` → `repository`. No business logic in controllers, no repository calls from controllers.
- Use DTOs for request/response; never expose JPA entities in API responses.
- Validate input with `jakarta.validation` (`@Valid`, `@NotBlank`, etc.).
- Central error handling with `@RestControllerAdvice`; return consistent error JSON and correct HTTP status codes.
- Constructor injection only (no field `@Autowired`).
- Config in `application.yml` with profiles (`dev`, `prod`); secrets via environment variables, never committed.
- Database changes go through migrations (Flyway/Liquibase); never edit schema by hand or rely on `ddl-auto=update` in prod.
- Passwords hashed (BCrypt); protected endpoints secured with Spring Security; never log passwords, tokens or payment data.
- Paginate list endpoints (products, orders); avoid N+1 queries.

## API contract
- REST, JSON, base path `/api/v1/...`.
- If an endpoint changes, update the frontend API module in the same task and note it in the summary.
- Money values: use `BigDecimal` on the backend, send as string/decimal, never floating-point math in the UI.

## Testing
- Backend: JUnit 5 + Mockito for services; `@WebMvcTest` for controllers.
- Frontend: Vitest/Jest + React Testing Library; test behavior, not implementation.
- New feature or bug fix = at least one test.

## Boundaries
- Do not edit `.env*`, lockfiles by hand, or build output (`/dist`, `/target`).
- Ask before adding a new dependency or changing the DB schema.
- Don't delete or rewrite existing tests to make them pass.
- Don't make unrelated refactors while doing a task.

## Definition of done
- Code compiles/builds, tests pass, lint is clean.
- No leftover `console.log`, commented-out code or TODOs without a reason.
- Short summary of what changed and anything I should verify manually.

