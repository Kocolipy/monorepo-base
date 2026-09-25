# Graph Report - monorepo-base-issue-11-audit-foundation  (2026-09-25)

## Corpus Check
- 201 files · ~107,844 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: (none) 10, .example 1, .properties 1)

## Summary
- 1877 nodes · 4622 edges · 123 communities (97 shown, 26 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 598 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `94c514df`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- AuthControllerTests.java
- Backend API Contract (OpenAPI 3.1)
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
- SpaFrontendTests
- EC2Instance
- DBInstance RDS PostgreSQL
- route-guards.tsx
- infra/ Is Deployment Material Not An App
- auth-context-value.ts
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
- .save
- SessionController
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
- org.junit.jupiter.api.AfterEach
- CapturedLog
- components.json
- org.junit.jupiter.api.Test
- AuditTrailServiceTests.java
- dependencies
- .revokeAll
- vite.config.ts
- engines
- overrides
- @testing-library/jest-dom
- AuditAppendOnlyIntegrationTests.java
- UserCounterService
- BackendApplication.java
- EcsLogFormatTests
- verify.sh
- jakarta.servlet.http.HttpServletRequest
- RedisSessionRevocationIntegrationTests
- UserCounter
- AuditTrail
- AuthControllerTests
- RecordingCounterService
- AuthController.java
- AdminAccountController.java
- RedisSessionRevocationIntegrationTests.java
- RFC requirements and implications
- RefusalTimingEquivalenceTests
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
- UserCounter.java
- Frontend Architecture Doc
- PrefixPasswordEncoder
- AuditOperation
- CONTEXT
- AuditRefusalReason
- hashmap
- AbsoluteSessionLifetimePolicyTests
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- org.springframework.jdbc.core.JdbcTemplate
- 3. ECS-structured logging with redaction enforced structurally

## God Nodes (most connected - your core abstractions)
1. `Account` - 58 edges
2. `AccountAdministrationServiceTests` - 50 edges
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

## Communities (123 total, 26 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 1 - "AuthControllerTests.java"
Cohesion: 0.12
Nodes (24): assertthatnoexception, authentication, authenticationmanager, AccountService, LoginAttemptService, LoginService, AccountRepository, LockoutPolicy (+16 more)

### Community 2 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.16
Nodes (11): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, getCount operation, getCurrentUser operation, getSession operation, incrementCount operation, login operation, logout operation (+3 more)

### Community 3 - "showcase.tsx"
Cohesion: 0.23
Nodes (16): One-Way Import Direction Through the Layers, components/ui Is a Package Placeholder, Button(), ButtonProps, buttonVariants, Card(), CardContent(), CardDescription() (+8 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.11
Nodes (22): Colors Come From index.css Tokens, Vite Full-Reloads on Any Watched HTML Write, Frontend Testing Guide, Arch Suite Reads Sources Through node:fs, toHaveTextContent Is a Substring Match, Coverage Excludes Are Listed, Not Globbed, E2E Flakiness Rules, Plant the Violation to Prove a Rule Fails (+14 more)

### Community 5 - "api.ts"
Cohesion: 0.15
Nodes (18): decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, SessionRequest, SessionResult (+10 more)

### Community 6 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 7 - "LogContextTests"
Cohesion: 0.20
Nodes (4): Override, LogContext, Scope, LogContextTests

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
Cohesion: 0.14
Nodes (11): AuditRetentionService, AuditRetentionScheduleConfig, Override, AuditEventRetention, AuditRetentionPolicy, AuditRetentionPolicyTests, crontask, crontrigger (+3 more)

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "uuid"
Cohesion: 0.12
Nodes (14): arraylist, AuditEventJpaRepository, UserCounterEntity, UserCounterJpaRepository, UserCounterPersistenceAdapter, list, lockmodetype, optional (+6 more)

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

### Community 24 - "SpaFrontendTests"
Cohesion: 0.09
Nodes (15): Override, SpaErrorViewResolver, SpaRoutes, SpaFrontendTests, ReservedServerPaths, SpaRoutesTests, SpaShell, org.junit.jupiter.api.Nested (+7 more)

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

### Community 31 - "showcase.test.tsx"
Cohesion: 0.20
Nodes (4): decodeCount(), Showcase(), apiFetchMock, auth

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
Cohesion: 0.10
Nodes (10): AccountSeed, Override, AccountServiceTests, 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision (+2 more)

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
Cohesion: 0.13
Nodes (6): AccountAdministrationServiceTests, InMemoryAccountSessions, Override, PendingCommit, Consequences, Context

### Community 57 - "SessionController"
Cohesion: 0.24
Nodes (7): deleteSession operation, SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession, org.springframework.web.bind.annotation.DeleteMapping

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "assertthat"
Cohesion: 0.09
Nodes (24): applicationconversionservice, assertthat, assertthatcode, assertthatthrownby, atomicinteger, authenticationexception, AccountRole, ADMIN (+16 more)

### Community 63 - "AccountSummary"
Cohesion: 0.22
Nodes (6): AccountSummary, AdminAccountControllerTests, Override, RecordingService, java.security.Principal, org.springframework.web.bind.annotation.PostMapping

### Community 64 - "accounts.tsx"
Cohesion: 0.17
Nodes (11): AccountAction, Accounts(), actionFailure(), AdminAccount, decodeAccount(), decodeAccounts(), formatDate(), formatInstant() (+3 more)

### Community 65 - "App.tsx"
Cohesion: 0.14
Nodes (12): Frontend SPA Entry HTML, App(), AuthProvider(), GuestRoute(), ProtectedRoute(), frontend_src_index, container, Login() (+4 more)

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 67 - "Account"
Cohesion: 0.15
Nodes (6): AccountAdministrationService, Account, InMemoryAccountRepository, Override, comparator, objects

### Community 68 - "SecurityConfig.java"
Cohesion: 0.08
Nodes (25): accountseed, argon2passwordencoder, authenticationentrypoint, AuditRetentionPolicyConfig, AbsoluteSessionLifetimeFilter, AccountSeedConfig, LoginLockoutConfig, SecurityConfig (+17 more)

### Community 69 - ".recordFailure"
Cohesion: 0.16
Nodes (4): Override, Recorded, RecordingAuditTrail, LoginAttemptServiceTests

### Community 70 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.09
Nodes (22): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Error contract (+14 more)

### Community 71 - "org.junit.jupiter.api.AfterEach"
Cohesion: 0.06
Nodes (22): HttpAuditRequestContext, Override, AfterCommit, AfterCommitAdapter, Override, HttpAuditRequestContextTests, AfterCommitAdapterTests, RequestIdFilterTests (+14 more)

### Community 72 - "CapturedLog"
Cohesion: 0.14
Nodes (10): AuditRetentionServiceTests, CountingRetention, Override, CapturedLog, Override, ch.qos.logback.classic.Logger, ch.qos.logback.classic.spi.ILoggingEvent, ch.qos.logback.core.read.ListAppender (+2 more)

### Community 73 - "components.json"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

### Community 74 - "org.junit.jupiter.api.Test"
Cohesion: 0.10
Nodes (5): AuditRetentionStartupTests, AbsoluteSessionLifetimeFilterTests, SecurityConfigTests, AdminAccountEndpointTests, org.junit.jupiter.api.Test

### Community 75 - "AuditTrailServiceTests.java"
Cohesion: 0.15
Nodes (13): AuditLockoutLift, EXPIRY, UNLOCK, AuditRequest, AuditRequestContext, OperationalAlerts, handlermapping, org.springframework.transaction.PlatformTransactionManager (+5 more)

### Community 76 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 78 - "vite.config.ts"
Cohesion: 0.33
Nodes (5): ref_node_url, @tailwindcss/vite, vite, vite-plugin-compression2, @vitejs/plugin-react-swc

### Community 79 - "engines"
Cohesion: 0.67
Nodes (3): engines, node, npm

### Community 82 - "AuditAppendOnlyIntegrationTests.java"
Cohesion: 0.09
Nodes (46): autowired, SessionRegistryConfiguration, SessionRegistryConfiguration, SessionRegistryConfiguration, classpathresource, containsstring, cookie, csrftoken (+38 more)

### Community 83 - "UserCounterService"
Cohesion: 0.24
Nodes (3): UserCounterService, UserCounterRepository, UserCounterServiceTests

### Community 84 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 85 - "EcsLogFormatTests"
Cohesion: 0.30
Nodes (3): EcsLogFormatTests, ResultActions, tools.jackson.databind.JsonNode

### Community 87 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.20
Nodes (12): Override, Override, RequestIdFilter, httpsession, ioexception, jakarta.servlet.FilterChain, jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse (+4 more)

### Community 88 - "RedisSessionRevocationIntegrationTests"
Cohesion: 0.13
Nodes (13): AccountSessions, AccountSessionsAdapter, Override, AccountSessionsAdapterTests, IndexedSessions, Override, RedisSessionRevocationIntegrationTests, collectors (+5 more)

### Community 89 - "UserCounter"
Cohesion: 0.22
Nodes (3): UserCounter, Override, UserCounterTests

### Community 92 - "RecordingCounterService"
Cohesion: 0.27
Nodes (3): Override, RecordingCounterService, UserCounterControllerTests

### Community 93 - "AuthController.java"
Cohesion: 0.27
Nodes (8): LoginOutcome, AuthController, UserResponse, cookievalue, org.springframework.security.core.Authentication, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy, org.springframework.security.web.context.SecurityContextRepository, org.springframework.session.web.http.CookieSerializer

### Community 94 - "AdminAccountController.java"
Cohesion: 0.10
Nodes (17): UnknownAccountException, UnsafeAccountChangeException, AdminAccountController, UserCounterController, ProbeController, httpstatus, notblank, org.springframework.web.bind.annotation.ExceptionHandler (+9 more)

### Community 95 - "RedisSessionRevocationIntegrationTests.java"
Cohesion: 0.11
Nodes (17): AccountSeedConfigTests, BackendApplicationTests, ContainerTestConfiguration, classmode, javax.sql.DataSource, org.springframework.boot.test.context.SpringBootTest, org.springframework.boot.testcontainers.service.connection.ServiceConnection, org.springframework.context.annotation.Import (+9 more)

### Community 96 - "RFC requirements and implications"
Cohesion: 0.20
Nodes (10): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+2 more)

### Community 97 - "RefusalTimingEquivalenceTests"
Cohesion: 0.27
Nodes (3): CountingPasswordEncoder, Override, RefusalTimingEquivalenceTests

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
Cohesion: 0.40
Nodes (3): AuditTrailService, Override, org.springframework.transaction.annotation.Transactional

### Community 104 - "Query contract"
Cohesion: 0.25
Nodes (8): Search, filtering, sorting and projection, Attribute projection, Filtering, Pagination, POST search, Query contract, Sorting, count()

### Community 105 - "Domain and authority model"
Cohesion: 0.25
Nodes (8): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, Protected recovery resources, SCIM User replaces Account

### Community 106 - "AuditEventEntity"
Cohesion: 0.10
Nodes (10): AuditOutcome, FAILURE, SUCCESS, AuditEventEntity, column, enumerated, enumtype, id (+2 more)

### Community 107 - "AuditEvent"
Cohesion: 0.27
Nodes (5): AuditEvent, AuditEventRepository, AuditEventPersistenceAdapter, Override, RecordingRepository

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
Cohesion: 0.10
Nodes (3): AuditAppendOnlyIntegrationTests, AuditEventRecordingIntegrationTests, ResultActions

### Community 112 - "UserCounter.java"
Cohesion: 0.20
Nodes (9): getHealth operation, JSESSIONID Session Cookie Security Scheme, Backend Runtime Configuration, Actuator Health and Info Exposure, JSESSIONID Cookie Attributes, PostgreSQL Datasource, Redis Session Namespace backend:session, Backend Test Configuration (+1 more)

### Community 113 - "Frontend Architecture Doc"
Cohesion: 0.40
Nodes (6): Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline, Vitest Deliberately Omits the Tailwind Vite Plugin

### Community 115 - "AuditOperation"
Cohesion: 0.12
Nodes (13): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, LOCKOUT_LIFT, LOCKOUT_SET, LOGIN_FAILURE, LOGIN_SUCCESS, LOGOUT (+5 more)

### Community 116 - "CONTEXT"
Cohesion: 0.29
Nodes (6): Accounts and identity provisioning, CONTEXT, Current account model, Request paths, SCIM target model, Sessions

### Community 117 - "AuditRefusalReason"
Cohesion: 0.33
Nodes (6): AuditRefusalReason, ACCOUNT_DISABLED, ACCOUNT_LOCKED, BAD_CREDENTIALS, OTHER, UNKNOWN_ACCOUNT

### Community 121 - "4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal"
Cohesion: 0.33
Nodes (5): 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal, Alternatives considered, Consequences, Context, Status

### Community 122 - "org.springframework.jdbc.core.JdbcTemplate"
Cohesion: 0.38
Nodes (4): AuditEventRetentionAdapter, Override, org.springframework.jdbc.core.JdbcTemplate, timestamp

### Community 124 - "3. ECS-structured logging with redaction enforced structurally"
Cohesion: 0.33
Nodes (5): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status

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
- **Why does `Backend API Contract (OpenAPI 3.1)` connect `Backend API Contract (OpenAPI 3.1)` to `UserCounter.java`, `SessionController`?**
  _High betweenness centrality (0.211) - this node is a cross-community bridge._
- **Why does `login operation` connect `Backend API Contract (OpenAPI 3.1)` to `auth.helpers.ts`, `api.ts`?**
  _High betweenness centrality (0.137) - this node is a cross-community bridge._
- **Why does `Shared Playwright storageState for Auth` connect `auth.helpers.ts` to `Backend API Contract (OpenAPI 3.1)`?**
  _High betweenness centrality (0.113) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _344 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AuthControllerTests.java` be split into smaller, more focused modules?**
  _Cohesion score 0.11522048364153627 - nodes in this community are weakly interconnected._