# Graph Report - monorepo-base-issue-11-audit-foundation  (2026-09-25)

## Corpus Check
- 201 files · ~107,678 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: (none) 10, .example 1, .properties 1)

## Summary
- 1876 nodes · 4607 edges · 126 communities (100 shown, 26 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 594 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `9ca4d471`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- AuthControllerTests.java
- AdminAccountController.java
- showcase.tsx
- auth.helpers.ts
- api.ts
- Workflow
- LogContextTests
- AccountTests
- devDependencies
- stryker.config.json
- compilerOptions
- Frontend Local Semgrep Ruleset
- package.json
- scripts
- AuditRetentionPolicy
- compilerOptions
- uuid
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
- auth-context-value.ts
- tsconfig.test.json
- accounts.test.tsx
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
- .save
- SessionController.java
- LoginLockoutTests
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh
- assertthat
- AccountSummary
- accounts.tsx
- App.tsx
- ArchitectureTest.java
- Account
- SecurityConfig.java
- .recordFailure
- SCIM 2.0 account-management specification plan
- AfterCommit
- CapturedLog
- components.json
- org.junit.jupiter.api.Test
- AuditTrailServiceTests.java
- dependencies
- InMemoryAccountSessions
- vite.config.ts
- engines
- overrides
- @testing-library/jest-dom
- AuditAppendOnlyIntegrationTests.java
- UserCounter
- BackendApplication.java
- EcsLogFormatTests
- verify.sh
- jakarta.servlet.http.HttpServletRequest
- RedisSessionRevocationIntegrationTests
- SecurityConfigTests
- AuditTrailServiceTests
- AuthControllerTests
- RecordingCounterService
- AuthController.java
- org.springframework.web.bind.annotation.ResponseStatus
- AccountRepository
- RFC requirements and implications
- SpaFrontendTests
- EcsLogCapture
- SCIM 2.0 account-management research
- Credential and cryptographic policy
- Delivery plan
- bcryptpasswordencoder
- org.springframework.transaction.annotation.Transactional
- Query contract
- Domain and authority model
- AuditEventEntity
- AuditEvent
- Definition of Done
- Write semantics
- Supported schemas
- AuditAppendOnlyIntegrationTests
- .current
- Frontend Architecture Doc
- PrefixPasswordEncoder
- AuditOperation
- org.junit.jupiter.api.AfterEach
- .run
- hashmap
- AuditRetentionStartupTests.java
- AuditEventEntity.java
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- org.springframework.stereotype.Repository
- RequestIdFilterTests
- 3. ECS-structured logging with redaction enforced structurally
- HttpAuditRequestContextTests.java

## God Nodes (most connected - your core abstractions)
1. `Account` - 58 edges
2. `AccountAdministrationServiceTests` - 49 edges
3. `AuditAppendOnlyIntegrationTests` - 39 edges
4. `AuditEventRecordingIntegrationTests` - 35 edges
5. `AuditOperation` - 31 edges
6. `AccountRepository` - 31 edges
7. `AccountAdministrationService` - 29 edges
8. `AccountRole` - 28 edges
9. `SCIM 2.0 account-management specification plan` - 27 edges
10. `AdminAccountEndpointTests` - 26 edges

## Surprising Connections (you probably didn't know these)
- `Consequences` --references--> `AuditTrailServiceTests`  [INFERRED]
  docs/adr/0004-audit-append-failure-semantics.md → backend/src/test/java/com/example/backend/audit/application/AuditTrailServiceTests.java
- `Consequences` --references--> `LoginLockoutTests`  [INFERRED]
  docs/adr/0001-count-login-attempts-on-the-login-path.md → backend/src/test/java/com/example/backend/auth/application/LoginLockoutTests.java
- `getCurrentUser operation` --shares_data_with--> `getCurrentUser()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `logout operation` --shares_data_with--> `logout()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `Sessions` --references--> `resolveSessionRoute()`  [INFERRED]
  CONTEXT.md → frontend/src/auth/session-route.ts

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Public Request Path Through The Stack** — infra_infrastructure_alblistener, infra_infrastructure_albtargetgroup, infra_infrastructure_ec2instance [EXTRACTED 1.00]
- **Mutation Testing as the Load-Bearing-Test Doctrine on Both Sides** — frontend_docs_testing_guide_stryker_mutate_trap, frontend_docs_testing_guide_assert_exactly [INFERRED 0.85]
- **Local Compose Versus Cloud Datastores** — backend_compose_postgres_service, backend_compose_redis_service, infra_infrastructure_dbinstance, infra_infrastructure_rediscluster [INFERRED 0.85]
- **Published Credential Exposure Surface** — agents_published_credentials_warning, backend_readme_dev_default_credentials, infra_infrastructure_app_credential_parameters, backend_semgrep_rules_service_security_be_hardcoded_credential_literal [INFERRED 0.85]

## Communities (126 total, 26 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 1 - "AuthControllerTests.java"
Cohesion: 0.08
Nodes (27): assertthatcode, assertthatnoexception, authentication, authenticationmanager, AuditTrail, AccountService, LoginAttemptService, badcredentialsexception (+19 more)

### Community 2 - "AdminAccountController.java"
Cohesion: 0.24
Nodes (9): AdminAccountController, CountResponse, UserCounterController, java.security.Principal, org.springframework.web.bind.annotation.GetMapping, org.springframework.web.bind.annotation.PostMapping, org.springframework.web.bind.annotation.RequestMapping, org.springframework.web.bind.annotation.RestController (+1 more)

### Community 3 - "showcase.tsx"
Cohesion: 0.23
Nodes (17): One-Way Import Direction Through the Layers, components/ui Is a Package Placeholder, Button(), ButtonProps, buttonVariants, Card(), CardContent(), CardDescription() (+9 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.11
Nodes (22): Colors Come From index.css Tokens, Vite Full-Reloads on Any Watched HTML Write, Frontend Testing Guide, Arch Suite Reads Sources Through node:fs, toHaveTextContent Is a Substring Match, Coverage Excludes Are Listed, Not Globbed, E2E Flakiness Rules, Plant the Violation to Prove a Rule Fails (+14 more)

### Community 5 - "api.ts"
Cohesion: 0.15
Nodes (17): decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, SessionRequest, SessionResult (+9 more)

### Community 6 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 7 - "LogContextTests"
Cohesion: 0.16
Nodes (6): Override, LogContext, Scope, LogContextTests, mdc, set

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
Cohesion: 0.10
Nodes (20): scripts, analyze, build, dev, format, format:check, lint, preview (+12 more)

### Community 15 - "AuditRetentionPolicy"
Cohesion: 0.10
Nodes (17): AuditRetentionService, AuditRetentionScheduleConfig, Override, AuditEventRetention, AuditRetentionPolicy, OperationalAlerts, Override, LoggingOperationalAlerts (+9 more)

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "uuid"
Cohesion: 0.17
Nodes (7): AccountSessions, comparator, linkedhashmap, list, map, optional, uuid

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
Cohesion: 0.13
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

### Community 29 - "auth-context-value.ts"
Cohesion: 0.25
Nodes (10): AuthUser, AuthContext, AuthContextState, AuthContextValue, useAuthState(), apiFetchMock, request(), state (+2 more)

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "accounts.test.tsx"
Cohesion: 0.11
Nodes (9): App(), AdminAccount, apiFetchMock, auth, apiFetchMock, auth, @testing-library/react, @testing-library/user-event (+1 more)

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
Cohesion: 0.13
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
Cohesion: 0.15
Nodes (5): AccountJpaRepository, AccountPersistenceAdapter, Override, AccountEntity, org.springframework.data.jpa.repository.Modifying

### Community 56 - ".save"
Cohesion: 0.15
Nodes (4): AccountAdministrationServiceTests, PendingCommit, Consequences, Context

### Community 57 - "SessionController.java"
Cohesion: 0.16
Nodes (13): deleteSession operation, SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, httpstatus, jakarta.servlet.http.HttpSession, notblank (+5 more)

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "assertthat"
Cohesion: 0.13
Nodes (16): assertthat, assertthatthrownby, atomicinteger, authenticationexception, AccountRole, ADMIN, USER, LockoutPolicy (+8 more)

### Community 63 - "AccountSummary"
Cohesion: 0.28
Nodes (4): AccountSummary, AdminAccountControllerTests, Override, RecordingService

### Community 64 - "accounts.tsx"
Cohesion: 0.36
Nodes (8): AccountAction, Accounts(), actionFailure(), decodeAccount(), decodeAccounts(), formatDate(), formatInstant(), StatusCell()

### Community 65 - "App.tsx"
Cohesion: 0.15
Nodes (10): Frontend SPA Entry HTML, AuthProvider(), GuestRoute(), ProtectedRoute(), frontend_src_index, container, Login(), decodeCount() (+2 more)

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 67 - "Account"
Cohesion: 0.17
Nodes (4): AccountAdministrationService, Account, InMemoryAccountRepository, Override

### Community 68 - "SecurityConfig.java"
Cohesion: 0.05
Nodes (30): accountseed, argon2passwordencoder, authenticationentrypoint, AuditRetentionPolicyConfig, AccountSeed, AbsoluteSessionLifetimeFilter, AccountSeedConfig, LoginLockoutConfig (+22 more)

### Community 69 - ".recordFailure"
Cohesion: 0.15
Nodes (4): Override, Recorded, RecordingAuditTrail, LoginAttemptServiceTests

### Community 70 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.09
Nodes (22): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Error contract (+14 more)

### Community 71 - "AfterCommit"
Cohesion: 0.13
Nodes (11): AfterCommit, AfterCommitAdapter, Override, AfterCommitAdapterTests, 2. Revoke a disabled account's sessions after the commit, Alternatives considered, Decision, Status (+3 more)

### Community 72 - "CapturedLog"
Cohesion: 0.13
Nodes (11): AuditRetentionServiceTests, CountingRetention, Override, CapturedLog, Override, ch.qos.logback.classic.Logger, ch.qos.logback.classic.spi.ILoggingEvent, ch.qos.logback.core.read.ListAppender (+3 more)

### Community 73 - "components.json"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

### Community 74 - "org.junit.jupiter.api.Test"
Cohesion: 0.16
Nodes (3): AbsoluteSessionLifetimeFilterTests, AdminAccountEndpointTests, org.junit.jupiter.api.Test

### Community 75 - "AuditTrailServiceTests.java"
Cohesion: 0.13
Nodes (16): arraylist, AuditLockoutLift, EXPIRY, UNLOCK, AuditRefusalReason, ACCOUNT_DISABLED, ACCOUNT_LOCKED, BAD_CREDENTIALS (+8 more)

### Community 76 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 77 - "InMemoryAccountSessions"
Cohesion: 0.25
Nodes (7): SessionRegistryConfiguration, SessionRegistryConfiguration, SessionRegistryConfiguration, InMemoryAccountSessions, Override, org.springframework.boot.test.context.TestConfiguration, org.springframework.context.annotation.Primary

### Community 78 - "vite.config.ts"
Cohesion: 0.33
Nodes (5): ref_node_url, @tailwindcss/vite, vite, vite-plugin-compression2, @vitejs/plugin-react-swc

### Community 79 - "engines"
Cohesion: 0.67
Nodes (3): engines, node, npm

### Community 82 - "AuditAppendOnlyIntegrationTests.java"
Cohesion: 0.11
Nodes (38): classpathresource, containsstring, content, cookie, csrftoken, delete, files, filter (+30 more)

### Community 83 - "UserCounter"
Cohesion: 0.05
Nodes (32): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, getCount operation, getCurrentUser operation, getHealth operation, getSession operation, incrementCount operation, login operation (+24 more)

### Community 84 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 85 - "EcsLogFormatTests"
Cohesion: 0.30
Nodes (3): EcsLogFormatTests, ResultActions, tools.jackson.databind.JsonNode

### Community 87 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.19
Nodes (11): Override, Override, RequestIdFilter, httpsession, ioexception, jakarta.servlet.FilterChain, jakarta.servlet.http.HttpServletRequest, ordered (+3 more)

### Community 88 - "RedisSessionRevocationIntegrationTests"
Cohesion: 0.15
Nodes (12): AccountSessionsAdapter, Override, AccountSessionsAdapterTests, IndexedSessions, Override, RedisSessionRevocationIntegrationTests, org.springframework.session.FindByIndexNameSessionRepository, org.springframework.session.MapSession (+4 more)

### Community 91 - "AuthControllerTests"
Cohesion: 0.26
Nodes (3): LoginRequest, AuthControllerTests, jakarta.servlet.http.HttpServletResponse

### Community 92 - "RecordingCounterService"
Cohesion: 0.27
Nodes (3): Override, RecordingCounterService, UserCounterControllerTests

### Community 93 - "AuthController.java"
Cohesion: 0.28
Nodes (9): LoginOutcome, LoginService, AuthController, UserResponse, cookievalue, org.springframework.security.core.Authentication, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy, org.springframework.security.web.context.SecurityContextRepository (+1 more)

### Community 94 - "org.springframework.web.bind.annotation.ResponseStatus"
Cohesion: 0.22
Nodes (4): UnknownAccountException, UnsafeAccountChangeException, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.ResponseStatus

### Community 95 - "AccountRepository"
Cohesion: 0.16
Nodes (13): autowired, AccountRepository, AccountSeedConfigTests, BackendApplicationTests, classmode, javax.sql.DataSource, org.springframework.boot.test.context.SpringBootTest, org.springframework.context.annotation.Import (+5 more)

### Community 96 - "RFC requirements and implications"
Cohesion: 0.20
Nodes (10): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+2 more)

### Community 97 - "SpaFrontendTests"
Cohesion: 0.19
Nodes (8): Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.http.HttpStatus, org.springframework.stereotype.Component, org.springframework.web.servlet.ModelAndView, requestdispatcher

### Community 98 - "EcsLogCapture"
Cohesion: 0.18
Nodes (9): EcsLogCapture, Override, bytearrayoutputstream, ch.qos.logback.classic.LoggerContext, ch.qos.logback.core.OutputStreamAppender, org.springframework.core.env.Environment, standardcharsets, structuredlogencoder (+1 more)

### Community 99 - "SCIM 2.0 account-management research"
Cohesion: 0.22
Nodes (7): Executive finding, Existing application seams, Primary sources, Recommended implementation order, Resolved RFC decisions, SCIM 2.0 account-management research, Testing strategy

### Community 100 - "Credential and cryptographic policy"
Cohesion: 0.22
Nodes (9): Credential and cryptographic policy, Hashing and primitives, Key rotation and storage, Lockout policy, Log formatting, Password policy, Response headers, Session lifetime (+1 more)

### Community 101 - "Delivery plan"
Cohesion: 0.22
Nodes (9): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 4 — Groups and Admin authority, Slice 5 — Complete query protocol, Slice 6 — Operational Accounts page and audit (+1 more)

### Community 103 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.28
Nodes (4): AuditTrailService, Override, AuditRequestContext, org.springframework.transaction.annotation.Transactional

### Community 104 - "Query contract"
Cohesion: 0.25
Nodes (8): Search, filtering, sorting and projection, Attribute projection, Filtering, Pagination, POST search, Query contract, Sorting, count()

### Community 105 - "Domain and authority model"
Cohesion: 0.25
Nodes (8): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, Protected recovery resources, SCIM User replaces Account

### Community 106 - "AuditEventEntity"
Cohesion: 0.12
Nodes (4): AuditOutcome, FAILURE, SUCCESS, AuditEventEntity

### Community 107 - "AuditEvent"
Cohesion: 0.22
Nodes (7): AuditEvent, AuditEventRepository, AuditEventJpaRepository, AuditEventPersistenceAdapter, Override, RecordingRepository, org.springframework.data.jpa.repository.JpaRepository

### Community 108 - "Definition of Done"
Cohesion: 0.40
Nodes (5): Backend gates, Contract and behavior, Definition of Done, Documentation and graph, Frontend gates

### Community 109 - "Write semantics"
Cohesion: 0.40
Nodes (5): DELETE and tombstones, PATCH, POST, PUT, Write semantics

### Community 110 - "Supported schemas"
Cohesion: 0.67
Nodes (3): Group, Supported schemas, User

### Community 111 - "AuditAppendOnlyIntegrationTests"
Cohesion: 0.09
Nodes (7): AuditAppendOnlyIntegrationTests, AuditEventRecordingIntegrationTests, ResultActions, ContainerTestConfiguration, org.springframework.boot.testcontainers.service.connection.ServiceConnection, org.testcontainers.containers.PostgreSQLContainer, org.testcontainers.utility.DockerImageName

### Community 112 - ".current"
Cohesion: 0.31
Nodes (4): HttpAuditRequestContext, Override, HttpAuditRequestContextTests, org.springframework.mock.web.MockHttpServletRequest

### Community 113 - "Frontend Architecture Doc"
Cohesion: 0.40
Nodes (6): Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline, Vitest Deliberately Omits the Tailwind Vite Plugin

### Community 115 - "AuditOperation"
Cohesion: 0.17
Nodes (10): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, LOCKOUT_LIFT, LOCKOUT_SET, LOGIN_FAILURE, LOGIN_SUCCESS, LOGOUT (+2 more)

### Community 116 - "org.junit.jupiter.api.AfterEach"
Cohesion: 0.20
Nodes (4): dispatchertype, mockfilterchain, org.junit.jupiter.api.AfterEach, webutils

### Community 119 - "AuditRetentionStartupTests.java"
Cohesion: 0.25
Nodes (4): applicationconversionservice, AuditRetentionStartupTests, org.springframework.boot.test.context.runner.ApplicationContextRunner, propertysourcesplaceholderconfigurer

### Community 120 - "AuditEventEntity.java"
Cohesion: 0.42
Nodes (6): column, enumerated, enumtype, id, jakarta.persistence.Entity, jakarta.persistence.Table

### Community 121 - "4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal"
Cohesion: 0.25
Nodes (6): 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal, Alternatives considered, Consequences, Context, Decision, Status

### Community 122 - "org.springframework.stereotype.Repository"
Cohesion: 0.36
Nodes (5): AuditEventRetentionAdapter, Override, org.springframework.jdbc.core.JdbcTemplate, org.springframework.stereotype.Repository, timestamp

### Community 124 - "3. ECS-structured logging with redaction enforced structurally"
Cohesion: 0.33
Nodes (5): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status

### Community 125 - "HttpAuditRequestContextTests.java"
Cohesion: 0.60
Nodes (3): handlermapping, requestcontextholder, servletrequestattributes

## Ambiguous Edges - Review These
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **344 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+339 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 552 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **26 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Backend API Contract (OpenAPI 3.1)` connect `UserCounter` to `SessionController.java`?**
  _High betweenness centrality (0.220) - this node is a cross-community bridge._
- **Why does `login operation` connect `UserCounter` to `auth.helpers.ts`, `api.ts`?**
  _High betweenness centrality (0.139) - this node is a cross-community bridge._
- **Why does `Shared Playwright storageState for Auth` connect `auth.helpers.ts` to `UserCounter`?**
  _High betweenness centrality (0.112) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _344 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AuthControllerTests.java` be split into smaller, more focused modules?**
  _Cohesion score 0.08076923076923077 - nodes in this community are weakly interconnected._