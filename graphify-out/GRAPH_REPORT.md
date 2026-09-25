# Graph Report - monorepo-base  (2026-09-25)

## Corpus Check
- 164 files · ~94,764 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: (none) 9, .example 1, .properties 1)

## Summary
- 1545 nodes · 3424 edges · 110 communities (85 shown, 25 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 469 edges (avg confidence: 0.83)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `db6fadce`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- AuthControllerTests.java
- AccountAdministrationService
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
- AccountAdministrationService.java
- compilerOptions
- auth-context-value.ts
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
- use-session-request.test.tsx
- tsconfig.test.json
- showcase.test.tsx
- Domain Documentation Guide
- GitHub Issue Tracker Guide
- Baseline Full Extensive Test Levels
- PIT Scoped To Touched Tests
- Graphify Runner Agent
- AccountServiceTests
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
- AccountAdministrationServiceTests
- SessionController.java
- .require
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh
- AccountRole
- UserCounterEntity
- accounts.tsx
- AccountPersistenceAdapter
- AccountSummary
- Account
- SecurityConfig.java
- UserCounter
- SCIM 2.0 account-management specification plan
- RequestIdFilterTests.java
- components.json
- Frontend Testing Guide
- AdminAccountEndpointTests
- PrefixPasswordEncoder
- dependencies
- AccountSessions
- vite.config.ts
- engines
- overrides
- @testing-library/jest-dom
- AdminAccountEndpointTests.java
- UserCounterService
- CONTEXT
- EcsLogCapture
- Frontend Architecture Doc
- jakarta.servlet.http.HttpServletRequest
- SpaFrontendTests
- org.junit.jupiter.api.Test
- AuthController.java
- AuthControllerTests
- java.security.Principal
- Shared Playwright storageState for Auth
- AdminAccountController.java
- tailwind
- RFC requirements and implications
- UserCounter.java
- authentication.spec.ts
- SCIM 2.0 account-management research
- Credential and cryptographic policy
- Delivery plan
- bcryptpasswordencoder
- Query contract
- Domain and authority model
- 3. ECS-structured logging with redaction enforced structurally
- Definition of Done
- Write semantics
- Supported schemas
- mockhttpservletrequest

## God Nodes (most connected - your core abstractions)
1. `Account` - 48 edges
2. `AccountAdministrationServiceTests` - 44 edges
3. `AccountRole` - 27 edges
4. `SCIM 2.0 account-management specification plan` - 27 edges
5. `AdminAccountEndpointTests` - 25 edges
6. `AccountSummary` - 24 edges
7. `AccountAdministrationService` - 23 edges
8. `SecurityConfigTests` - 23 edges
9. `AccountTests` - 23 edges
10. `InMemoryAccountRepository` - 22 edges

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

## Communities (110 total, 25 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 1 - "AuthControllerTests.java"
Cohesion: 0.15
Nodes (13): assertthatnoexception, authentication, authenticationmanager, LoginAttemptService, LockoutPolicy, badcredentialsexception, clock, content (+5 more)

### Community 3 - "showcase.tsx"
Cohesion: 0.23
Nodes (16): One-Way Import Direction Through the Layers, components/ui Is a Package Placeholder, Button(), ButtonProps, buttonVariants, Card(), CardContent(), CardDescription() (+8 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.25
Nodes (7): ADMIN_CREDENTIALS, captureSessionCookie(), expireSession(), postAdminAction(), SESSION_COOKIE, submitLoginViaApi(), @playwright/test

### Community 5 - "api.ts"
Cohesion: 0.17
Nodes (18): decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, useAuthState(), SessionRequest (+10 more)

### Community 6 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 7 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.18
Nodes (12): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, getCurrentUser operation, getHealth operation, getSession operation, logout operation, JSESSIONID Session Cookie Security Scheme, updateSession operation (+4 more)

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
Nodes (25): name, packageManager, private, type, version, class-variance-authority, dependency-cruiser, eslint (+17 more)

### Community 14 - "scripts"
Cohesion: 0.11
Nodes (19): scripts, analyze, build, dev, format, format:check, lint, preview (+11 more)

### Community 15 - "AccountAdministrationService.java"
Cohesion: 0.22
Nodes (8): authenticationexception, LoginService, LogEvent, AccountSeedConfigTests, loggerfactory, org.slf4j.Logger, org.springframework.security.authentication.AuthenticationManager, usernamepasswordauthenticationtoken

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "auth-context-value.ts"
Cohesion: 0.24
Nodes (6): AuthUser, AuthContext, AuthContextState, AuthContextValue, apiFetchMock, auth

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
Cohesion: 0.19
Nodes (7): SpaRoutes, ReservedServerPaths, SpaRoutesTests, SpaShell, org.junit.jupiter.api.Nested, org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource

### Community 25 - "EC2Instance"
Cohesion: 0.14
Nodes (16): No Parent-Relative Paths From An App, SPA Build Contract, with-frontend Maven Profile, Backend Serves SPA And Forwards Routes, Claim: No Router Data Layer Or Auth, No .env Required In Frontend, Frontend Technology Stack, EC2Instance (+8 more)

### Community 26 - "DBInstance RDS PostgreSQL"
Cohesion: 0.29
Nodes (8): docs/openapi.yaml API Contract, Postgres Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, DBInstance RDS PostgreSQL, DBSubnetGroup

### Community 27 - "App.tsx"
Cohesion: 0.15
Nodes (16): AuthRole, AuthProvider(), AuthStatus, useAuth(), GuestRoute(), ProtectedRoute(), SessionRoute(), DEFAULT_DESTINATION (+8 more)

### Community 28 - "infra/ Is Deployment Material Not An App"
Cohesion: 0.29
Nodes (7): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, CONTEXT.md Domain Glossary, gh CLI Conventions, GitHub Issues As Issue Tracker, PRs As Request Surface Flag

### Community 29 - "use-session-request.test.tsx"
Cohesion: 0.28
Nodes (7): apiFetchMock, request(), state, wrapper(), useSessionRequest(), decodeCount(), Showcase()

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "showcase.test.tsx"
Cohesion: 0.10
Nodes (10): Frontend SPA Entry HTML, App(), frontend_src_index, container, apiFetchMock, auth, react-dom, @testing-library/react (+2 more)

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

### Community 37 - "AccountServiceTests"
Cohesion: 0.14
Nodes (8): Override, AccountServiceTests, 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision, Status

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
Cohesion: 0.18
Nodes (7): AccountEntity, column, enumerated, enumtype, id, jakarta.persistence.Entity, jakarta.persistence.Table

### Community 56 - "AccountAdministrationServiceTests"
Cohesion: 0.17
Nodes (5): AccountAdministrationServiceTests, Override, PendingCommit, Consequences, Context

### Community 57 - "SessionController.java"
Cohesion: 0.18
Nodes (12): deleteSession operation, SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession, notblank, org.springframework.web.bind.annotation.DeleteMapping (+4 more)

### Community 58 - ".require"
Cohesion: 0.13
Nodes (5): ~~09 — The login path must remember to count its own attempts~~ ✅ `d72e3c5`, What shipped, and how well, LoginAttemptServiceTests, LoginLockoutTests, org.springframework.security.core.AuthenticationException

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "AccountRole"
Cohesion: 0.13
Nodes (18): assertthat, assertthatcode, assertthatthrownby, atomicinteger, AccountRole, ADMIN, USER, Override (+10 more)

### Community 63 - "UserCounterEntity"
Cohesion: 0.18
Nodes (7): UserCounterEntity, UserCounterJpaRepository, Override, UserCounterPersistenceAdapter, lockmodetype, org.springframework.data.jpa.repository.Lock, org.springframework.stereotype.Repository

### Community 64 - "accounts.tsx"
Cohesion: 0.36
Nodes (8): AccountAction, Accounts(), actionFailure(), decodeAccount(), decodeAccounts(), formatDate(), formatInstant(), StatusCell()

### Community 65 - "AccountPersistenceAdapter"
Cohesion: 0.21
Nodes (7): AccountJpaRepository, AccountPersistenceAdapter, Override, org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.jpa.repository.Modifying, org.springframework.data.jpa.repository.Query, param

### Community 66 - "AccountSummary"
Cohesion: 0.05
Nodes (38): 03 — The runtime configuration surface has no module at all — Strong (new load-bearing keys), 04 — The Counter slice is seven modules over a two-field row — Worth exploring (contrast sharpened again), 06 — A template whose identity has no seam — Speculative (unchanged), 10 — The role vocabulary is authored on both sides of the contract — Speculative (unchanged), ~~12 — The account row is still copied for the wire~~ ✅ (uncommitted), 13 — Schema evolution has no module — Strong (scope halved, core untouched), Architecture review — deepening opportunities, Open candidates (+30 more)

### Community 67 - "Account"
Cohesion: 0.11
Nodes (13): Account, AccountRepository, InMemoryAccountRepository, Override, comparator, hashmap, list, objects (+5 more)

### Community 68 - "SecurityConfig.java"
Cohesion: 0.05
Nodes (34): accountseed, 08 — Seeded accounts are identified by position, not by role — Strong (narrowed), argon2passwordencoder, authenticationentrypoint, AccountSeed, AccountService, AbsoluteSessionLifetimeFilter, AccountSeedConfig (+26 more)

### Community 70 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.09
Nodes (22): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Error contract (+14 more)

### Community 71 - "RequestIdFilterTests.java"
Cohesion: 0.05
Nodes (23): arraylist, AfterCommit, AfterCommitAdapter, Override, Override, LogContext, Scope, AfterCommitAdapterTests (+15 more)

### Community 72 - "components.json"
Cohesion: 0.18
Nodes (10): aliases, components, hooks, lib, ui, iconLibrary, rsc, $schema (+2 more)

### Community 73 - "Frontend Testing Guide"
Cohesion: 0.25
Nodes (9): Colors Come From index.css Tokens, Vite Full-Reloads on Any Watched HTML Write, Frontend Testing Guide, Arch Suite Reads Sources Through node:fs, toHaveTextContent Is a Substring Match, Coverage Excludes Are Listed, Not Globbed, E2E Flakiness Rules, Plant the Violation to Prove a Rule Fails (+1 more)

### Community 76 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 77 - "AccountSessions"
Cohesion: 0.28
Nodes (5): ~~14 — Account status is enforced only at authentication~~ ✅ `d431d67`, 16 — Session revocation is a non-transactional write inside a transaction — Worth exploring (new), AccountSessions, InMemoryAccountSessions, Override

### Community 78 - "vite.config.ts"
Cohesion: 0.33
Nodes (5): ref_node_url, @tailwindcss/vite, vite, vite-plugin-compression2, @vitejs/plugin-react-swc

### Community 79 - "engines"
Cohesion: 0.67
Nodes (3): engines, node, npm

### Community 82 - "AdminAccountEndpointTests.java"
Cohesion: 0.09
Nodes (39): autowired, BackendApplicationTests, EcsLogFormatTests, classmode, classpathresource, containsstring, cookie, csrftoken (+31 more)

### Community 83 - "UserCounterService"
Cohesion: 0.20
Nodes (5): getCount operation, UserCounterService, UserCounterRepository, UserCounterServiceTests, org.springframework.stereotype.Service

### Community 84 - "CONTEXT"
Cohesion: 0.29
Nodes (6): Accounts and identity provisioning, CONTEXT, Current account model, Request paths, SCIM target model, Sessions

### Community 85 - "EcsLogCapture"
Cohesion: 0.12
Nodes (13): EcsLogCapture, Override, bytearrayoutputstream, ch.qos.logback.classic.Logger, ch.qos.logback.classic.LoggerContext, ch.qos.logback.classic.spi.ILoggingEvent, ch.qos.logback.core.OutputStreamAppender, org.springframework.core.env.Environment (+5 more)

### Community 86 - "Frontend Architecture Doc"
Cohesion: 0.29
Nodes (8): utils, Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, The @/ Alias Declared in Four Agreeing Places, Tailwind v4 CSS-First Token Pipeline, Vitest Deliberately Omits the Tailwind Vite Plugin

### Community 87 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.18
Nodes (12): Override, Override, RequestIdFilter, ioexception, jakarta.servlet.FilterChain, jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse, ordered (+4 more)

### Community 88 - "SpaFrontendTests"
Cohesion: 0.09
Nodes (19): AccountSessionsAdapter, Override, Override, SpaErrorViewResolver, AccountSessionsAdapterTests, IndexedSessions, Override, SpaFrontendTests (+11 more)

### Community 89 - "org.junit.jupiter.api.Test"
Cohesion: 0.13
Nodes (5): ~~11 — Two administrative namespaces, one of them dead~~ ✅ `c74b917` + `fa2da47`, AbsoluteSessionLifetimeFilterTests, ProbeController, SecurityConfigTests, org.junit.jupiter.api.Test

### Community 90 - "AuthController.java"
Cohesion: 0.29
Nodes (7): AuthController, UserResponse, cookievalue, org.springframework.security.core.Authentication, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy, org.springframework.security.web.context.SecurityContextRepository, org.springframework.session.web.http.CookieSerializer

### Community 92 - "java.security.Principal"
Cohesion: 0.16
Nodes (11): incrementCount operation, resetCount operation, CountResponse, UserCounterController, Override, RecordingCounterService, UserCounterControllerTests, java.security.Principal (+3 more)

### Community 93 - "Shared Playwright storageState for Auth"
Cohesion: 0.40
Nodes (5): login operation, Shared Playwright storageState for Auth, login(), loginAs(), submitLogin()

### Community 94 - "AdminAccountController.java"
Cohesion: 0.15
Nodes (8): UnknownAccountException, UnsafeAccountChangeException, AdminAccountController, httpstatus, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.PostMapping, org.springframework.web.bind.annotation.ResponseStatus, pathvariable

### Community 95 - "tailwind"
Cohesion: 0.33
Nodes (6): tailwind, baseColor, config, css, cssVariables, prefix

### Community 96 - "RFC requirements and implications"
Cohesion: 0.20
Nodes (10): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+2 more)

### Community 97 - "UserCounter.java"
Cohesion: 0.50
Nodes (3): PostgreSQL Datasource, Backend Test Configuration, H2 In-Memory Test Datasource

### Community 99 - "SCIM 2.0 account-management research"
Cohesion: 0.22
Nodes (7): Executive finding, Existing application seams, Primary sources, Recommended implementation order, Resolved RFC decisions, SCIM 2.0 account-management research, Testing strategy

### Community 100 - "Credential and cryptographic policy"
Cohesion: 0.22
Nodes (9): Credential and cryptographic policy, Hashing and primitives, Key rotation and storage, Lockout policy, Log formatting, Password policy, Response headers, Session lifetime (+1 more)

### Community 101 - "Delivery plan"
Cohesion: 0.22
Nodes (9): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 4 — Groups and Admin authority, Slice 5 — Complete query protocol, Slice 6 — Operational Accounts page and audit (+1 more)

### Community 104 - "Query contract"
Cohesion: 0.25
Nodes (8): Search, filtering, sorting and projection, Attribute projection, Filtering, Pagination, POST search, Query contract, Sorting, count()

### Community 105 - "Domain and authority model"
Cohesion: 0.25
Nodes (8): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, Protected recovery resources, SCIM User replaces Account

### Community 106 - "3. ECS-structured logging with redaction enforced structurally"
Cohesion: 0.33
Nodes (5): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status

### Community 108 - "Definition of Done"
Cohesion: 0.40
Nodes (5): Backend gates, Contract and behavior, Definition of Done, Documentation and graph, Frontend gates

### Community 109 - "Write semantics"
Cohesion: 0.40
Nodes (5): DELETE and tombstones, PATCH, POST, PUT, Write semantics

### Community 110 - "Supported schemas"
Cohesion: 0.67
Nodes (3): Group, Supported schemas, User

## Ambiguous Edges - Review These
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **327 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `USER` (+322 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 493 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **25 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `~~12 — The account row is still copied for the wire~~ ✅ (uncommitted)` connect `AccountSummary` to `AccountAdministrationServiceTests`, `.require`?**
  _High betweenness centrality (0.175) - this node is a cross-community bridge._
- **Why does `AdminAccount` connect `AccountSummary` to `accounts.tsx`, `auth-context-value.ts`, `App.tsx`?**
  _High betweenness centrality (0.168) - this node is a cross-community bridge._
- **Why does `Frontend Testing Guide` connect `Frontend Testing Guide` to `authentication.spec.ts`, `Baseline Full Extensive Test Levels`, `Shared Playwright storageState for Auth`?**
  _High betweenness centrality (0.127) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AccountAdministrationServiceTests` (e.g. with `~~12 — The account row is still copied for the wire~~ ✅ (uncommitted)` and `~~14 — Account status is enforced only at authentication~~ ✅ `d431d67``) actually correct?**
  _`AccountAdministrationServiceTests` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _327 weakly-connected nodes found - possible documentation gaps or missing edges._