# Graph Report - monorepo-base  (2026-09-24)

## Corpus Check
- 143 files · ~70,855 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: (none) 9, .example 1, .properties 1)

## Summary
- 1270 nodes · 2818 edges · 91 communities (71 shown, 20 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 410 edges (avg confidence: 0.83)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d72e3c59`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- UserCounter
- SecurityConfigTests.java
- showcase.tsx
- auth.helpers.ts
- api.ts
- Workflow
- Backend API Contract (OpenAPI 3.1)
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
- App.tsx
- infra/ Is Deployment Material Not An App
- accounts.test.tsx
- tsconfig.test.json
- SessionController
- Domain Documentation Guide
- GitHub Issue Tracker Guide
- Baseline Full Extensive Test Levels
- PIT Scoped To Touched Tests
- Graphify Runner Agent
- UserCounterService
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
- AccountPersistenceAdapter
- bootstrap.sh
- package.sh
- Test Static index.html Stub
- com.example:backend
- org.junit.jupiter.api.Test
- SecurityConfig.java
- org.springframework.web.bind.annotation.ResponseStatus
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh
- AccountRole
- UserCounterEntity
- accounts.tsx
- AccountEntity
- ArchitectureTest.java
- Account
- AdminAccountController.java
- SpaFrontendTests
- AccountService.java
- AccountPersistenceAdapter.java
- MutableClock
- eslint.config.js
- AuthControllerTests
- PrefixPasswordEncoder
- dependencies
- AdminAccountEndpointTests
- vite.config.ts
- engines
- overrides
- @testing-library/jest-dom
- AuthControllerTests.java
- RecordingCounterService
- SecurityConfigTests
- auth-context-value.ts
- AuthController.java
- org.springframework.boot.test.context.SpringBootTest
- .logout
- main.tsx
- .inMemoryAccountSessions

## God Nodes (most connected - your core abstractions)
1. `Account` - 47 edges
2. `AccountAdministrationServiceTests` - 37 edges
3. `AccountRole` - 28 edges
4. `AccountSummary` - 23 edges
5. `AccountRepository` - 23 edges
6. `SecurityConfigTests` - 23 edges
7. `AccountTests` - 22 edges
8. `InMemoryAccountRepository` - 21 edges
9. `AdminAccountEndpointTests` - 21 edges
10. `AuthControllerTests` - 21 edges

## Surprising Connections (you probably didn't know these)
- `Top recommendation` --references--> `AccountSummary`  [INFERRED]
  architecture-review.md → backend/src/main/java/com/example/backend/auth/application/AccountSummary.java
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

## Communities (91 total, 20 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 1 - "UserCounter"
Cohesion: 0.22
Nodes (4): UserCounter, Override, UserCounterPersistenceAdapter, UserCounterTests

### Community 2 - "SecurityConfigTests.java"
Cohesion: 0.11
Nodes (24): containsstring, content, cookie, csrftoken, filter, filterchainproxy, forwardedurl, get (+16 more)

### Community 3 - "showcase.tsx"
Cohesion: 0.23
Nodes (16): One-Way Import Direction Through the Layers, components/ui Is a Package Placeholder, Button(), ButtonProps, buttonVariants, Card(), CardContent(), CardDescription() (+8 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.05
Nodes (46): Colors Come From index.css Tokens, aliases, components, hooks, lib, ui, utils, iconLibrary (+38 more)

### Community 5 - "api.ts"
Cohesion: 0.19
Nodes (17): decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, SessionRequest, SessionResult (+9 more)

### Community 6 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 7 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.17
Nodes (13): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, getCurrentUser operation, getHealth operation, getSession operation, login operation, logout operation, JSESSIONID Session Cookie Security Scheme (+5 more)

### Community 8 - "AccountTests"
Cohesion: 0.09
Nodes (12): 03 — The runtime configuration surface has no module at all — Strong (count grew again), 06 — A template whose identity has no seam — Speculative (unchanged), 09 — The login path must remember to count its own attempts — Worth exploring (unchanged), 10 — The role vocabulary is authored on both sides of the contract — Speculative (authors grew), 14 — Account status is enforced only at authentication — Worth exploring (new), Architecture review — deepening opportunities, Open candidates, Top recommendation (+4 more)

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
Cohesion: 0.14
Nodes (13): 04 — The Counter slice is seven modules over a two-field row — Worth exploring (contrast sharpened again), 12 — The account listing row is authored five times — Strong (new), AccountSummary, AdminAccountController, AdminAccountResponse, ArchitectureTest, AdminAccountControllerTests, Override (+5 more)

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "IndexedSessions"
Cohesion: 0.12
Nodes (15): arraylist, AccountSessions, AccountSessionsAdapter, Override, AccountSessionsAdapterTests, IndexedSessions, Override, collectors (+7 more)

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
Cohesion: 0.14
Nodes (11): SpaRoutes, ReservedServerPaths, SpaRoutesTests, SpaShell, Accounts and roles, CONTEXT, Request paths, Sessions (+3 more)

### Community 25 - "EC2Instance"
Cohesion: 0.14
Nodes (16): No Parent-Relative Paths From An App, SPA Build Contract, with-frontend Maven Profile, Backend Serves SPA And Forwards Routes, Claim: No Router Data Layer Or Auth, No .env Required In Frontend, Frontend Technology Stack, EC2Instance (+8 more)

### Community 26 - "DBInstance RDS PostgreSQL"
Cohesion: 0.29
Nodes (8): docs/openapi.yaml API Contract, Postgres Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, DBInstance RDS PostgreSQL, DBSubnetGroup

### Community 27 - "App.tsx"
Cohesion: 0.18
Nodes (14): AuthRole, AuthProvider(), AuthStatus, GuestRoute(), ProtectedRoute(), SessionRoute(), DEFAULT_DESTINATION, LOGIN_PATH (+6 more)

### Community 28 - "infra/ Is Deployment Material Not An App"
Cohesion: 0.29
Nodes (7): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, CONTEXT.md Domain Glossary, gh CLI Conventions, GitHub Issues As Issue Tracker, PRs As Request Surface Flag

### Community 29 - "accounts.test.tsx"
Cohesion: 0.09
Nodes (8): App(), apiFetchMock, auth, apiFetchMock, auth, @testing-library/react, @testing-library/user-event, vitest

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "SessionController"
Cohesion: 0.35
Nodes (5): SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession

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

### Community 37 - "UserCounterService"
Cohesion: 0.13
Nodes (10): getCount operation, incrementCount operation, resetCount operation, UserCounterService, CountResponse, UserCounterRepository, UserCounterServiceTests, UserCounterControllerTests (+2 more)

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

### Community 50 - "AccountPersistenceAdapter"
Cohesion: 0.16
Nodes (10): AccountJpaRepository, AccountPersistenceAdapter, Override, UserCounterJpaRepository, lockmodetype, org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.jpa.repository.Lock, org.springframework.data.jpa.repository.Modifying (+2 more)

### Community 56 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (17): AccountAdministrationService, Override, AccountAdministrationServiceTests, AccountServiceTests, LoginAttemptServiceTests, LoginLockoutTests, InMemoryAccountSessions, Override (+9 more)

### Community 57 - "SecurityConfig.java"
Cohesion: 0.12
Nodes (17): authenticationentrypoint, LoginLockoutConfig, SecurityConfig, bcryptpasswordencoder, changesessionidauthenticationstrategy, cookiecsrftokenrepository, daoauthenticationprovider, httpmethod (+9 more)

### Community 58 - "org.springframework.web.bind.annotation.ResponseStatus"
Cohesion: 0.22
Nodes (4): UnknownAccountException, UnsafeAccountChangeException, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.ResponseStatus

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "AccountRole"
Cohesion: 0.20
Nodes (10): assertthat, assertthatthrownby, AccountRole, ADMIN, USER, duration, instant, lockedexception (+2 more)

### Community 63 - "UserCounterEntity"
Cohesion: 0.21
Nodes (7): UserCounterEntity, column, enumerated, enumtype, id, jakarta.persistence.Entity, jakarta.persistence.Table

### Community 64 - "accounts.tsx"
Cohesion: 0.22
Nodes (12): 13 — Schema evolution has no module — Strong (new), useAuth(), AccountAction, Accounts(), actionFailure(), AdminAccount, decodeAccount(), decodeAccounts() (+4 more)

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.11
Nodes (17): classes, component, configuration, controller, entity, entitymanager, generalcodingrules, importoption (+9 more)

### Community 67 - "Account"
Cohesion: 0.17
Nodes (8): Account, AccountRepository, InMemoryAccountRepository, Override, comparator, hashmap, list, objects

### Community 68 - "AdminAccountController.java"
Cohesion: 0.20
Nodes (11): UserCounterController, httpstatus, notblank, org.springframework.web.bind.annotation.GetMapping, org.springframework.web.bind.annotation.PutMapping, org.springframework.web.bind.annotation.RequestMapping, org.springframework.web.bind.annotation.RestController, pathvariable (+3 more)

### Community 69 - "SpaFrontendTests"
Cohesion: 0.18
Nodes (8): Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.http.HttpStatus, org.springframework.test.web.servlet.MockMvc, org.springframework.web.servlet.ModelAndView, requestdispatcher

### Community 70 - "AccountService.java"
Cohesion: 0.15
Nodes (14): accountseed, 08 — Seeded accounts are identified by position, not by role — Strong (regressed), Assessment: was the work done?, What the new code adds to the board, AccountSeed, AccountService, AccountSeedConfig, AccountSeedConfigTests (+6 more)

### Community 71 - "AccountPersistenceAdapter.java"
Cohesion: 0.22
Nodes (5): PostgreSQL Datasource, Backend Test Configuration, H2 In-Memory Test Datasource, optional, org.springframework.stereotype.Repository

### Community 72 - "MutableClock"
Cohesion: 0.25
Nodes (5): Override, MutableClock, clock, zoneid, zoneoffset

### Community 73 - "eslint.config.js"
Cohesion: 0.33
Nodes (5): @eslint/js, eslint-plugin-react-hooks, eslint-plugin-react-refresh, globals, typescript-eslint

### Community 74 - "AuthControllerTests"
Cohesion: 0.27
Nodes (3): LoginRequest, AuthControllerTests, org.junit.jupiter.api.AfterEach

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
Cohesion: 0.11
Nodes (19): assertthatcode, assertthatnoexception, authentication, authenticationexception, authenticationmanager, LoginAttemptService, LoginService, LockoutPolicy (+11 more)

### Community 84 - "SecurityConfigTests"
Cohesion: 0.14
Nodes (3): 11 — Two administrative namespaces, one of them dead — Strong (new), ProbeController, SecurityConfigTests

### Community 85 - "auth-context-value.ts"
Cohesion: 0.25
Nodes (10): AuthUser, AuthContext, AuthContextState, AuthContextValue, useAuthState(), apiFetchMock, request(), state (+2 more)

### Community 86 - "AuthController.java"
Cohesion: 0.29
Nodes (7): AuthController, UserResponse, cookievalue, org.springframework.security.core.Authentication, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy, org.springframework.security.web.csrf.CsrfTokenRepository, org.springframework.session.web.http.CookieSerializer

### Community 87 - "org.springframework.boot.test.context.SpringBootTest"
Cohesion: 0.36
Nodes (6): autowired, BackendApplicationTests, classmode, org.springframework.boot.test.context.SpringBootTest, org.springframework.security.web.SecurityFilterChain, org.springframework.test.annotation.DirtiesContext

### Community 88 - ".logout"
Cohesion: 0.36
Nodes (4): deleteSession operation, jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse, org.springframework.web.bind.annotation.DeleteMapping

### Community 89 - "main.tsx"
Cohesion: 0.40
Nodes (4): Frontend SPA Entry HTML, frontend_src_index, container, react-dom

### Community 90 - ".inMemoryAccountSessions"
Cohesion: 0.50
Nodes (3): SessionRegistryConfiguration, org.springframework.boot.test.context.TestConfiguration, org.springframework.context.annotation.Primary

## Ambiguous Edges - Review These
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **252 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `USER` (+247 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 397 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **20 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `AdminAccount` connect `accounts.tsx` to `App.tsx`, `accounts.test.tsx`, `AccountSummary`?**
  _High betweenness centrality (0.179) - this node is a cross-community bridge._
- **Why does `13 — Schema evolution has no module — Strong (new)` connect `accounts.tsx` to `AccountTests`, `AccountService.java`, `AccountSummary`?**
  _High betweenness centrality (0.118) - this node is a cross-community bridge._
- **Why does `AccountSummary` connect `AccountSummary` to `accounts.tsx`, `AdminAccountController.java`, `AccountTests`, `org.junit.jupiter.api.Test`, `AccountRole`?**
  _High betweenness centrality (0.118) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _252 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SecurityConfigTests.java` be split into smaller, more focused modules?**
  _Cohesion score 0.11396011396011396 - nodes in this community are weakly interconnected._