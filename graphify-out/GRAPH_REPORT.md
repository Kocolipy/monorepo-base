# Graph Report - monorepo-base  (2026-09-25)

## Corpus Check
- 143 files · ~70,173 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: (none) 9, .example 1, .properties 1)

## Summary
- 1274 nodes · 2825 edges · 84 communities (67 shown, 17 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 410 edges (avg confidence: 0.83)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d431d67c`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- AccountRepository
- AdminAccountEndpointTests.java
- showcase.tsx
- auth.helpers.ts
- api.ts
- Workflow
- SessionController.java
- AccountTests
- devDependencies
- stryker.config.json
- compilerOptions
- Frontend Local Semgrep Ruleset
- package.json
- scripts
- AccountSummary
- compilerOptions
- IndexedSessions
- lib.sh
- AGENTS.md
- deploy.sh
- Kiro: graphify enforcement
- mvnw
- AWS CloudFormation Deployment Guide
- .servesSpaShell
- EC2Instance
- DBInstance RDS PostgreSQL
- route-guards.tsx
- infra/ Is Deployment Material Not An App
- showcase.test.tsx
- tsconfig.test.json
- auth-context-value.ts
- Domain Documentation Guide
- GitHub Issue Tracker Guide
- Baseline Full Extensive Test Levels
- PIT Scoped To Touched Tests
- Graphify Runner Agent
- UserCounter
- cleanup.sh
- get-vpc-info.sh
- Backend Semgrep Baseline Gate
- Graphify Runner
- Frontend Project Structure
- Spring Session In Redis
- prettier.config.mjs
- dev.sh
- integration-test.sh
- semgrep.sh
- CLAUDE.md
- AccountEntity
- bootstrap.sh
- package.sh
- Test Static index.html Stub
- com.example:backend
- org.junit.jupiter.api.Test
- SecurityConfig.java
- 1. Count login attempts on the login path
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh
- AccountRole
- SessionControllerTests.java
- accounts.tsx
- ArchitectureTest.java
- Account
- SpaFrontendTests
- AccountService
- InMemoryAccountRepository.java
- MutableClock
- eslint.config.js
- AccountServiceTests
- dependencies
- vite.config.ts
- engines
- overrides
- @testing-library/jest-dom
- AuthControllerTests.java
- SecurityConfigTests
- use-session-request.test.tsx
- org.springframework.boot.test.context.SpringBootTest
- App.tsx
- .inMemoryAccountSessions

## God Nodes (most connected - your core abstractions)
1. `Account` - 47 edges
2. `AccountAdministrationServiceTests` - 39 edges
3. `AccountRole` - 28 edges
4. `AdminAccountEndpointTests` - 24 edges
5. `SecurityConfigTests` - 23 edges
6. `AccountTests` - 22 edges
7. `AccountSummary` - 21 edges
8. `AccountRepository` - 21 edges
9. `AuthControllerTests` - 21 edges
10. `InMemoryAccountRepository` - 20 edges

## Surprising Connections (you probably didn't know these)
- `Consequences` --references--> `LoginLockoutTests`  [INFERRED]
  docs/adr/0001-count-login-attempts-on-the-login-path.md → backend/src/test/java/com/example/backend/auth/application/LoginLockoutTests.java
- `getCurrentUser operation` --shares_data_with--> `getCurrentUser()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `logout operation` --shares_data_with--> `logout()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `Sessions` --references--> `resolveSessionRoute()`  [INFERRED]
  CONTEXT.md → frontend/src/auth/session-route.ts
- `Frontend Local Semgrep Ruleset` --semantically_similar_to--> `Backend Semgrep Baseline Gate`  [INFERRED] [semantically similar]
  frontend/AGENTS.md → backend/AGENTS.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Public Request Path Through The Stack** — infra_infrastructure_alblistener, infra_infrastructure_albtargetgroup, infra_infrastructure_ec2instance [EXTRACTED 1.00]
- **Mutation Testing as the Load-Bearing-Test Doctrine on Both Sides** — frontend_docs_testing_guide_stryker_mutate_trap, frontend_docs_testing_guide_assert_exactly [INFERRED 0.85]
- **Local Compose Versus Cloud Datastores** — backend_compose_postgres_service, backend_compose_redis_service, infra_infrastructure_dbinstance, infra_infrastructure_rediscluster [INFERRED 0.85]
- **Published Credential Exposure Surface** — agents_published_credentials_warning, backend_readme_dev_default_credentials, infra_infrastructure_app_credential_parameters, backend_semgrep_rules_service_security_be_hardcoded_credential_literal [INFERRED 0.85]

## Communities (84 total, 17 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 1 - "AccountRepository"
Cohesion: 0.19
Nodes (3): AccountRepository, AccountSeedConfigTests, org.springframework.security.crypto.password.PasswordEncoder

### Community 2 - "AdminAccountEndpointTests.java"
Cohesion: 0.12
Nodes (23): containsstring, content, cookie, csrftoken, filter, filterchainproxy, forwardedurl, get (+15 more)

### Community 3 - "showcase.tsx"
Cohesion: 0.26
Nodes (14): One-Way Import Direction Through the Layers, components/ui Is a Package Placeholder, Button(), ButtonProps, buttonVariants, Card(), CardContent(), CardDescription() (+6 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.05
Nodes (46): Colors Come From index.css Tokens, aliases, components, hooks, lib, ui, utils, iconLibrary (+38 more)

### Community 5 - "api.ts"
Cohesion: 0.17
Nodes (18): decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, useAuthState(), SessionRequest (+10 more)

### Community 6 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 7 - "SessionController.java"
Cohesion: 0.09
Nodes (25): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, deleteSession operation, getCurrentUser operation, getHealth operation, getSession operation, login operation, logout operation (+17 more)

### Community 9 - "devDependencies"
Cohesion: 0.07
Nodes (29): devDependencies, dependency-cruiser, eslint, @eslint/js, eslint-plugin-react-hooks, eslint-plugin-react-refresh, fallow, globals (+21 more)

### Community 10 - "stryker.config.json"
Cohesion: 0.07
Nodes (26): cleanTempDir, clearTextReporter, allowColor, maxTestsToLog, _comment_mutate, concurrency, coverageAnalysis, htmlReporter (+18 more)

### Community 11 - "compilerOptions"
Cohesion: 0.09
Nodes (21): compilerOptions, allowImportingTsExtensions, baseUrl, isolatedModules, jsx, lib, module, moduleDetection (+13 more)

### Community 12 - "Frontend Local Semgrep Ruleset"
Cohesion: 0.25
Nodes (8): Frontend Local Semgrep Ruleset, fe-dangerously-set-inner-html, fe-document-write, fe-eval-or-dynamic-function, fe-hardcoded-credential, fe-inner-html-assignment, fe-target-blank-without-noopener, Local Ruleset Keeps the Scan Offline and Deterministic

### Community 13 - "package.json"
Cohesion: 0.09
Nodes (22): name, packageManager, private, type, version, class-variance-authority, clsx, dependency-cruiser (+14 more)

### Community 14 - "scripts"
Cohesion: 0.11
Nodes (19): scripts, analyze, build, dev, format, format:check, lint, preview (+11 more)

### Community 15 - "AccountSummary"
Cohesion: 0.07
Nodes (25): 03 — The runtime configuration surface has no module at all — Strong (new load-bearing keys), 04 — The Counter slice is seven modules over a two-field row — Worth exploring (contrast sharpened again), 06 — A template whose identity has no seam — Speculative (unchanged), 10 — The role vocabulary is authored on both sides of the contract — Speculative (unchanged), 12 — The account row is still copied for the wire — Strong (narrowed), 13 — Schema evolution has no module — Strong (scope halved, core untouched), Architecture review — deepening opportunities, Open candidates (+17 more)

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "IndexedSessions"
Cohesion: 0.14
Nodes (14): arraylist, AccountSessionsAdapter, Override, AccountSessionsAdapterTests, IndexedSessions, Override, collectors, linkedhashmap (+6 more)

### Community 18 - "lib.sh"
Cohesion: 0.24
Nodes (14): die(), load_backend_env(), log(), pinned_version(), port_holder(), require_cmd(), require_docker(), require_maven() (+6 more)

### Community 19 - "AGENTS.md"
Cohesion: 0.12
Nodes (14): Agent, Agent documentation, Build and validation, Environment, Frontend/backend integration, graphify, Ignore rules, Layout (+6 more)

### Community 20 - "deploy.sh"
Cohesion: 0.42
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 21 - "Kiro: graphify enforcement"
Cohesion: 0.40
Nodes (4): graphify-runner, Kiro: graphify enforcement, The hooks, When the refresh fails

### Community 22 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 23 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.25
Nodes (9): ALBListener, ALBTargetGroup, TargetGroupAttachment, ALB To EC2 To RDS And Redis Topology, AWS CloudFormation Deployment Guide, Stack Parameters Reference, Existing VPC Prerequisite, Infra Troubleshooting Runbook (+1 more)

### Community 24 - ".servesSpaShell"
Cohesion: 0.12
Nodes (13): SpaRoutes, ReservedServerPaths, SpaRoutesTests, SpaShell, Accounts and identity provisioning, CONTEXT, Current account model, Request paths (+5 more)

### Community 25 - "EC2Instance"
Cohesion: 0.14
Nodes (16): No Parent-Relative Paths From An App, SPA Build Contract, with-frontend Maven Profile, Backend Serves SPA And Forwards Routes, Claim: No Router Data Layer Or Auth, No .env Required In Frontend, Frontend Technology Stack, EC2Instance (+8 more)

### Community 26 - "DBInstance RDS PostgreSQL"
Cohesion: 0.29
Nodes (8): docs/openapi.yaml API Contract, Postgres Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, DBInstance RDS PostgreSQL, DBSubnetGroup

### Community 27 - "route-guards.tsx"
Cohesion: 0.23
Nodes (11): AuthRole, AuthStatus, useAuth(), SessionRoute(), DEFAULT_DESTINATION, LOGIN_PATH, resolveSessionRoute(), SessionRequirement (+3 more)

### Community 28 - "infra/ Is Deployment Material Not An App"
Cohesion: 0.29
Nodes (7): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, CONTEXT.md Domain Glossary, gh CLI Conventions, GitHub Issues As Issue Tracker, PRs As Request Surface Flag

### Community 29 - "showcase.test.tsx"
Cohesion: 0.13
Nodes (5): apiFetchMock, auth, @testing-library/react, @testing-library/user-event, vitest

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "auth-context-value.ts"
Cohesion: 0.22
Nodes (7): AuthUser, AuthContext, AuthContextState, AuthContextValue, AdminAccount, apiFetchMock, auth

### Community 32 - "Domain Documentation Guide"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 33 - "GitHub Issue Tracker Guide"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 34 - "Baseline Full Extensive Test Levels"
Cohesion: 0.25
Nodes (8): ArchUnit Baseline Gate, Always ./mvnw Never Bare mvn, Trace Before You Delete, Frontend Local Semgrep Ruleset, Baseline Full Extensive Test Levels, The mutate Flag Replaces the Array, It Does Not Narrow It, Image Tag Plus Digest Pinning, Toolchain Pin Table

### Community 36 - "Graphify Runner Agent"
Cohesion: 0.40
Nodes (5): Graphify Runner Agent, Recorded Interpreter Guard, Graph Shrink Refusal, Graphify Refresh Before Commit, CLAUDE.md Graphify Override

### Community 37 - "UserCounter"
Cohesion: 0.05
Nodes (26): getCount operation, incrementCount operation, resetCount operation, UserCounterService, CountResponse, UserCounterController, UserCounter, UserCounterRepository (+18 more)

### Community 38 - "cleanup.sh"
Cohesion: 0.70
Nodes (4): print_error(), print_info(), print_warn(), cleanup.sh script

### Community 39 - "get-vpc-info.sh"
Cohesion: 0.70
Nodes (4): print_header(), print_info(), print_warn(), get-vpc-info.sh script

### Community 40 - "Backend Semgrep Baseline Gate"
Cohesion: 0.18
Nodes (12): Long-Gate Sentinel And Log Pattern, Published Default Credentials Warning, Security-Sensitive Change Policy, Backend Semgrep Baseline Gate, Development Default Credentials, be-authorize-any-request-permit-all, be-cors-wildcard-origin, be-csrf-disabled (+4 more)

### Community 41 - "Graphify Runner"
Cohesion: 0.50
Nodes (3): Graphify Runner, Reporting, Steps

### Community 42 - "Frontend Project Structure"
Cohesion: 0.67
Nodes (3): shadcn Placeholder Primitives, src/ Dependency Direction Rules, Frontend Project Structure

### Community 43 - "Spring Session In Redis"
Cohesion: 0.33
Nodes (7): Backend Architecture Boundaries, Redis Compose Service, Spring Session In Redis, Flag ADR Conflicts Explicitly, docs/adr Decision Records, RedisCluster ElastiCache, RedisSubnetGroup

### Community 50 - "AccountEntity"
Cohesion: 0.10
Nodes (15): AccountJpaRepository, AccountPersistenceAdapter, Override, AccountEntity, column, enumerated, enumtype, id (+7 more)

### Community 56 - "org.junit.jupiter.api.Test"
Cohesion: 0.07
Nodes (16): ~~09 — The login path must remember to count its own attempts~~ ✅ `d72e3c5`, ~~11 — Two administrative namespaces, one of them dead~~ ✅ `c74b917` + `fa2da47`, ~~14 — Account status is enforced only at authentication~~ ✅ `d431d67`, ~~15 — The indexed-session fact is authored five times, one of them wrongly~~ ✅ (uncommitted), 16 — Session revocation is a non-transactional write inside a transaction — Worth exploring (new), What shipped, and how well, AccountAdministrationService, AccountSessions (+8 more)

### Community 57 - "SecurityConfig.java"
Cohesion: 0.07
Nodes (33): authenticationentrypoint, authenticationexception, LoginService, LoginLockoutConfig, SecurityConfig, AuthController, LoginRequest, UserResponse (+25 more)

### Community 58 - "1. Count login attempts on the login path"
Cohesion: 0.29
Nodes (6): 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision, Status

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "AccountRole"
Cohesion: 0.19
Nodes (11): assertthat, assertthatthrownby, AccountRole, ADMIN, USER, LockoutPolicy, duration, instant (+3 more)

### Community 63 - "SessionControllerTests.java"
Cohesion: 0.40
Nodes (4): assertthatcode, chronounit, mockhttpservletrequest, mockhttpsession

### Community 64 - "accounts.tsx"
Cohesion: 0.36
Nodes (8): AccountAction, Accounts(), actionFailure(), decodeAccount(), decodeAccounts(), formatDate(), formatInstant(), StatusCell()

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.10
Nodes (21): ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, configuration, controller, entity (+13 more)

### Community 67 - "Account"
Cohesion: 0.24
Nodes (3): Account, InMemoryAccountRepository, Override

### Community 69 - "SpaFrontendTests"
Cohesion: 0.20
Nodes (7): Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.http.HttpStatus, org.springframework.web.servlet.ModelAndView, requestdispatcher

### Community 70 - "AccountService"
Cohesion: 0.36
Nodes (7): accountseed, 08 — Seeded accounts are identified by position, not by role — Strong (narrowed), AccountSeed, AccountService, AccountSeedConfig, org.springframework.boot.ApplicationRunner, value

### Community 71 - "InMemoryAccountRepository.java"
Cohesion: 0.32
Nodes (5): comparator, hashmap, list, objects, optional

### Community 72 - "MutableClock"
Cohesion: 0.29
Nodes (4): Override, MutableClock, zoneid, zoneoffset

### Community 73 - "eslint.config.js"
Cohesion: 0.33
Nodes (5): @eslint/js, eslint-plugin-react-hooks, eslint-plugin-react-refresh, globals, typescript-eslint

### Community 75 - "AccountServiceTests"
Cohesion: 0.24
Nodes (4): Override, AccountServiceTests, Override, PrefixPasswordEncoder

### Community 76 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 78 - "vite.config.ts"
Cohesion: 0.33
Nodes (5): ref_node_url, @tailwindcss/vite, vite, vite-plugin-compression2, @vitejs/plugin-react-swc

### Community 79 - "engines"
Cohesion: 0.67
Nodes (3): engines, node, npm

### Community 82 - "AuthControllerTests.java"
Cohesion: 0.14
Nodes (15): assertthatnoexception, authentication, authenticationmanager, LoginAttemptService, badcredentialsexception, clock, defaultcookieserializer, inmemoryuserdetailsmanager (+7 more)

### Community 84 - "SecurityConfigTests"
Cohesion: 0.12
Nodes (5): ProbeController, SecurityConfigTests, org.springframework.mock.web.MockHttpSession, org.springframework.test.web.servlet.MockMvc, org.springframework.web.bind.annotation.GetMapping

### Community 85 - "use-session-request.test.tsx"
Cohesion: 0.28
Nodes (7): apiFetchMock, request(), state, wrapper(), useSessionRequest(), decodeCount(), Showcase()

### Community 87 - "org.springframework.boot.test.context.SpringBootTest"
Cohesion: 0.36
Nodes (6): autowired, BackendApplicationTests, classmode, org.springframework.boot.test.context.SpringBootTest, org.springframework.security.web.SecurityFilterChain, org.springframework.test.annotation.DirtiesContext

### Community 89 - "App.tsx"
Cohesion: 0.17
Nodes (10): Frontend SPA Entry HTML, App(), AuthProvider(), GuestRoute(), ProtectedRoute(), frontend_src_index, container, Login() (+2 more)

### Community 90 - ".inMemoryAccountSessions"
Cohesion: 0.50
Nodes (3): SessionRegistryConfiguration, org.springframework.boot.test.context.TestConfiguration, org.springframework.context.annotation.Primary

## Ambiguous Edges - Review These
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **254 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `USER` (+249 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 399 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **17 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Backend API Contract (OpenAPI 3.1)` connect `SessionController.java` to `UserCounter`?**
  _High betweenness centrality (0.285) - this node is a cross-community bridge._
- **Why does `login operation` connect `SessionController.java` to `auth.helpers.ts`, `api.ts`?**
  _High betweenness centrality (0.171) - this node is a cross-community bridge._
- **Why does `Shared Playwright storageState for Auth` connect `auth.helpers.ts` to `SessionController.java`?**
  _High betweenness centrality (0.131) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `AccountAdministrationServiceTests` (e.g. with `~~14 — Account status is enforced only at authentication~~ ✅ `d431d67`` and `16 — Session revocation is a non-transactional write inside a transaction — Worth exploring (new)`) actually correct?**
  _`AccountAdministrationServiceTests` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _254 weakly-connected nodes found - possible documentation gaps or missing edges._