# Graph Report - monorepo-base-issue-30  (2026-09-26)

## Corpus Check
- 297 files · ~161,751 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: (none) 10, .example 1, .properties 1)

## Summary
- 2843 nodes · 8052 edges · 154 communities (111 shown, 43 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 909 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `2786def6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- AuthControllerTests.java
- ScimConnectorLifecycleIntegrationTests
- accounts.tsx
- auth.helpers.ts
- api.ts
- Workflow
- LogContextTests
- .sessionsOf
- devDependencies
- stryker.config.json
- compilerOptions
- Frontend Local Semgrep Ruleset
- package.json
- scripts
- .readCreate
- compilerOptions
- uuid
- lib.sh
- AGENTS.md
- deploy.sh
- org.junit.jupiter.api.Test
- mvnw
- AWS CloudFormation Deployment Guide
- org.junit.jupiter.params.ParameterizedTest
- EC2Instance
- DBInstance RDS PostgreSQL
- App.tsx
- infra/ Is Deployment Material Not An App
- auth-context-value.ts
- tsconfig.test.json
- SecurityConfig.java
- Domain Documentation Guide
- GitHub Issue Tracker Guide
- Baseline Full Extensive Test Levels
- PIT Scoped To Touched Tests
- Graphify Runner Agent
- .findByUsername
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
- Backend API Contract (OpenAPI 3.1)
- .of
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh
- assertthat
- AccountSummary
- ConnectorTokenSecretTests
- accounts.test.tsx
- ArchitectureTest.java
- org.springframework.context.annotation.Bean
- SecurityConfig
- .require
- SCIM 2.0 account-management specification plan
- org.junit.jupiter.api.AfterEach
- CapturedLog
- ConnectorAdministrationService
- .status
- AuditTrailServiceTests.java
- dependencies
- java.security.Principal
- ScimUserAttributesTests
- ScimUserProvisioningIntegrationTests
- ScimBearerAuthenticationFilterTests
- AuditRetentionPolicy
- AuditAppendOnlyIntegrationTests.java
- AccountService
- BackendApplication.java
- jakarta.persistence.Entity
- verify.sh
- ScimBearerAuthenticationFilterTests.java
- RedisSessionRevocationIntegrationTests
- RequestIdFilterTests
- AuditTrail
- jakarta.servlet.http.HttpServletRequest
- .getCount
- org.springframework.web.bind.annotation.GetMapping
- AuthController.java
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
- ScimConnectorTokenEntity
- ScimUserEntity
- PrefixPasswordEncoder
- AuditOperation
- ContainerTestConfiguration.java
- .overlapEnd
- hashmap
- AbsoluteSessionLifetimePolicy
- RecordingAuditTrail
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- ScimSecurityChainOrderTests.java
- PermanentLockoutMigrationTests
- ScimOffsetPage
- ScimConnector
- ScimConnectorEntity
- ScimDiscoveryIntegrationTests
- Account
- ScimPageRequest
- EcsLogFormatTests
- .scimType
- AuditTrailService.java
- ConnectorAuthenticationServiceTests
- ScimConnectorTokenTests
- .of
- .requiresWriteScope
- AuditRetentionStartupTests.java
- AdminConnectorController
- ScimConnectorToken
- ScimUserEntity.java
- ScimExceptionHandler.java
- ScimUserEmailValue
- ScimUserProfileTests
- CONTEXT
- .identifies
- main.tsx
- AuditEventRetentionAdapter.java
- ScimSchemas
- InvalidConnectorTokenLifetimeException
- filterchainproxy
- mockfilterchain
- mockhttpservletresponse
- recordcomponent

## God Nodes (most connected - your core abstractions)
1. `Account` - 63 edges
2. `AccountAdministrationServiceTests` - 54 edges
3. `AuditOperation` - 46 edges
4. `ScimConnectorLifecycleIntegrationTests` - 46 edges
5. `ScimUserProvisioningIntegrationTests` - 42 edges
6. `AuditAppendOnlyIntegrationTests` - 41 edges
7. `AuditTrail` - 39 edges
8. `AccountRepository` - 36 edges
9. `ScimConnectorToken` - 36 edges
10. `AuditEventRecordingIntegrationTests` - 35 edges

## Surprising Connections (you probably didn't know these)
- `Consequences` --references--> `AuditTrailServiceTests`  [INFERRED]
  docs/adr/0004-audit-append-failure-semantics.md → backend/src/test/java/com/example/backend/audit/application/AuditTrailServiceTests.java
- `Consequences` --references--> `LoginLockoutTests`  [INFERRED]
  docs/adr/0001-count-login-attempts-on-the-login-path.md → backend/src/test/java/com/example/backend/auth/application/LoginLockoutTests.java
- `Sessions` --references--> `resolveSessionRoute()`  [INFERRED]
  CONTEXT.md → frontend/src/auth/session-route.ts
- `Frontend Local Semgrep Ruleset` --semantically_similar_to--> `Backend Semgrep Baseline Gate`  [INFERRED] [semantically similar]
  frontend/AGENTS.md → backend/AGENTS.md
- `Decision` --references--> `AfterCommit`  [INFERRED]
  docs/adr/0002-revoke-sessions-after-commit.md → backend/src/main/java/com/example/backend/auth/application/AfterCommit.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Public Request Path Through The Stack** — infra_infrastructure_alblistener, infra_infrastructure_albtargetgroup, infra_infrastructure_ec2instance [EXTRACTED 1.00]
- **Mutation Testing as the Load-Bearing-Test Doctrine on Both Sides** — frontend_docs_testing_guide_stryker_mutate_trap, frontend_docs_testing_guide_assert_exactly [INFERRED 0.85]
- **Local Compose Versus Cloud Datastores** — backend_compose_postgres_service, backend_compose_redis_service, infra_infrastructure_dbinstance, infra_infrastructure_rediscluster [INFERRED 0.85]
- **Published Credential Exposure Surface** — agents_published_credentials_warning, backend_readme_dev_default_credentials, infra_infrastructure_app_credential_parameters, backend_semgrep_rules_service_security_be_hardcoded_credential_literal [INFERRED 0.85]

## Communities (154 total, 43 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 1 - "AuthControllerTests.java"
Cohesion: 0.09
Nodes (27): assertthatnoexception, authentication, authenticationmanager, AfterCommit, LoginAttemptService, LoginService, AccountRepository, AccountSessions (+19 more)

### Community 2 - "ScimConnectorLifecycleIntegrationTests"
Cohesion: 0.05
Nodes (13): ScimUserResource, ScimUserService, NormalizedUserName, ScimExternalIdRepository, ScimUser, ScimUserRepository, NormalizedUserNameTests, ScimUserTests (+5 more)

### Community 3 - "accounts.tsx"
Cohesion: 0.13
Nodes (27): components/ui Is a Package Placeholder, useAuth(), useAuthState(), useSessionRequest(), Button(), ButtonProps, buttonVariants, Card() (+19 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.05
Nodes (46): Colors Come From index.css Tokens, aliases, components, hooks, lib, ui, utils, iconLibrary (+38 more)

### Community 5 - "api.ts"
Cohesion: 0.15
Nodes (20): getCurrentUser operation, login operation, logout operation, decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock (+12 more)

### Community 6 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 7 - "LogContextTests"
Cohesion: 0.20
Nodes (4): Override, LogContext, Scope, LogContextTests

### Community 8 - ".sessionsOf"
Cohesion: 0.22
Nodes (3): Override, PendingCommit, Consequences

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
Cohesion: 0.06
Nodes (35): engines, node, npm, name, overrides, qs, packageManager, private (+27 more)

### Community 14 - "scripts"
Cohesion: 0.10
Nodes (20): scripts, analyze, build, dev, format, format:check, lint, preview (+12 more)

### Community 15 - ".readCreate"
Cohesion: 0.07
Nodes (15): NewScimUser, ScimErrorException, ScimUserRequestReader, ScimEmail, ScimName, ScimResourceType, GROUP, USER (+7 more)

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "uuid"
Cohesion: 0.08
Nodes (18): arraylist, ScimConnectorRepository, collectors, comparator, linkedhashmap, linkedhashset, list, locale (+10 more)

### Community 18 - "lib.sh"
Cohesion: 0.24
Nodes (14): die(), load_backend_env(), log(), pinned_version(), port_holder(), require_cmd(), require_docker(), require_maven() (+6 more)

### Community 19 - "AGENTS.md"
Cohesion: 0.12
Nodes (14): Agent, Agent documentation, Build and validation, Environment, Frontend/backend integration, graphify, Ignore rules, Layout (+6 more)

### Community 20 - "deploy.sh"
Cohesion: 0.42
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 21 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (5): AbsoluteSessionLifetimeFilterTests, AccountTests, Connectors, Tokens, org.junit.jupiter.api.Test

### Community 22 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 23 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.25
Nodes (9): ALBListener, ALBTargetGroup, TargetGroupAttachment, ALB To EC2 To RDS And Redis Topology, AWS CloudFormation Deployment Guide, Stack Parameters Reference, Existing VPC Prerequisite, Infra Troubleshooting Runbook (+1 more)

### Community 24 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.13
Nodes (8): SpaRoutes, SpaRoutesScimNamespaceTests, ReservedServerPaths, SpaRoutesTests, SpaShell, org.junit.jupiter.api.Nested, org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource

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

### Community 29 - "auth-context-value.ts"
Cohesion: 0.28
Nodes (9): AuthUser, AuthContext, AuthContextState, AuthContextValue, apiFetchMock, request(), state, wrapper() (+1 more)

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "SecurityConfig.java"
Cohesion: 0.12
Nodes (17): argon2passwordencoder, authenticationentrypoint, authorizationfilter, changesessionidauthenticationstrategy, cookiecsrftokenrepository, daoauthenticationprovider, delegatingpasswordencoder, httpmethod (+9 more)

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

### Community 37 - ".findByUsername"
Cohesion: 0.10
Nodes (9): Override, AccountServiceTests, 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision, Status (+1 more)

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
Cohesion: 0.07
Nodes (15): AccountJpaRepository, AccountPersistenceAdapter, Override, AccountEntity, Override, Key, ScimExternalIdEntity, ScimExternalIdJpaRepository (+7 more)

### Community 56 - ".save"
Cohesion: 0.11
Nodes (3): AccountAdministrationService, AccountAdministrationServiceTests, Context

### Community 57 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.18
Nodes (12): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, deleteSession operation, getSession operation, incrementCount operation, updateSession operation, SessionController, SessionResponse (+4 more)

### Community 58 - ".of"
Cohesion: 0.09
Nodes (6): SuppressWarnings, ScimAttributeProjection, ScimUserController, ScimUserRenderer, SuppressWarnings, ScimAttributeProjectionTests

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "assertthat"
Cohesion: 0.10
Nodes (17): assertthat, assertthatcode, assertthatthrownby, atomicinteger, authenticationexception, AccountRole, ADMIN, USER (+9 more)

### Community 63 - "AccountSummary"
Cohesion: 0.28
Nodes (4): AccountSummary, AdminAccountControllerTests, Override, RecordingService

### Community 64 - "ConnectorTokenSecretTests"
Cohesion: 0.09
Nodes (12): arrays, ConnectorTokenDigest, Override, Minted, Presented, ConnectorTokenSecretTests, base64, hashset (+4 more)

### Community 65 - "accounts.test.tsx"
Cohesion: 0.09
Nodes (9): App(), AdminAccount, apiFetchMock, auth, apiFetchMock, auth, @testing-library/react, @testing-library/user-event (+1 more)

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 67 - "org.springframework.context.annotation.Bean"
Cohesion: 0.18
Nodes (10): accountseed, AuditRetentionPolicyConfig, AccountSeed, AccountSeedConfig, LoginLockoutConfig, ScimSecurityConfig, org.springframework.boot.ApplicationRunner, org.springframework.context.annotation.Bean (+2 more)

### Community 68 - "SecurityConfig"
Cohesion: 0.28
Nodes (3): SecurityConfig, SecurityConfigPasswordEncoderTests, org.springframework.security.core.userdetails.UserDetailsService

### Community 70 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.10
Nodes (20): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Goals (+12 more)

### Community 71 - "org.junit.jupiter.api.AfterEach"
Cohesion: 0.05
Nodes (26): AuditRequest, HttpAuditRequestContext, Override, AfterCommitAdapter, Override, Override, SpaErrorViewResolver, HttpAuditRequestContextTests (+18 more)

### Community 72 - "CapturedLog"
Cohesion: 0.14
Nodes (10): AuditRetentionServiceTests, CountingRetention, Override, CapturedLog, Override, ch.qos.logback.classic.Logger, ch.qos.logback.classic.spi.ILoggingEvent, ch.qos.logback.core.read.ListAppender (+2 more)

### Community 73 - "ConnectorAdministrationService"
Cohesion: 0.13
Nodes (5): ConnectorAdministrationService, ConnectorSummary, ConnectorTokenSummary, ScimConnectorTokenRepository, ConnectorAdministrationServiceTests

### Community 74 - ".status"
Cohesion: 0.08
Nodes (5): AuditEventRecordingIntegrationTests, ResultActions, ProbeController, SecurityConfigTests, AdminAccountEndpointTests

### Community 75 - "AuditTrailServiceTests.java"
Cohesion: 0.18
Nodes (9): AuditRefusalReason, ACCOUNT_DISABLED, ACCOUNT_LOCKED, BAD_CREDENTIALS, OTHER, UNKNOWN_ACCOUNT, simpletransactionstatus, transactionstatus (+1 more)

### Community 76 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 77 - "java.security.Principal"
Cohesion: 0.15
Nodes (7): UnknownAccountException, UnsafeAccountChangeException, AdminAccountController, java.security.Principal, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.PostMapping, org.springframework.web.bind.annotation.ResponseStatus

### Community 78 - "ScimUserAttributesTests"
Cohesion: 0.10
Nodes (4): Attribute, ScimUserAttributes, SuppressWarnings, ScimUserAttributesTests

### Community 81 - "AuditRetentionPolicy"
Cohesion: 0.14
Nodes (12): AuditRetentionService, AuditRetentionScheduleConfig, Override, AuditEventRetention, AuditRetentionPolicy, AuditRetentionPolicyTests, crontask, crontrigger (+4 more)

### Community 82 - "AuditAppendOnlyIntegrationTests.java"
Cohesion: 0.09
Nodes (59): autowired, RequestIdFilter, SessionRegistryConfiguration, SessionRegistryConfiguration, SessionRegistryConfiguration, InMemoryAccountSessions, Override, ContainerTestConfiguration (+51 more)

### Community 83 - "AccountService"
Cohesion: 0.07
Nodes (21): getHealth operation, resetCount operation, JSESSIONID Session Cookie Security Scheme, AccountService, UserCounterService, UserCounter, UserCounterRepository, UserCounterEntity (+13 more)

### Community 84 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 85 - "jakarta.persistence.Entity"
Cohesion: 0.37
Nodes (7): column, enumerated, enumtype, id, jakarta.persistence.Entity, jakarta.persistence.Table, serializable

### Community 87 - "ScimBearerAuthenticationFilterTests.java"
Cohesion: 0.09
Nodes (19): ConnectorAuthenticationService, AuthenticatedConnector, ConnectorTokenScope, READ_ONLY, READ_WRITE, ConnectorTokenSecret, ScimWriteScopeRule, dispatchertype (+11 more)

### Community 88 - "RedisSessionRevocationIntegrationTests"
Cohesion: 0.14
Nodes (12): AccountSessionsAdapter, Override, AccountSessionsAdapterTests, IndexedSessions, Override, RedisSessionRevocationIntegrationTests, org.springframework.session.FindByIndexNameSessionRepository, org.springframework.session.MapSession (+4 more)

### Community 91 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.06
Nodes (25): LoginOutcome, Override, AuthController, LoginRequest, UserResponse, Override, Override, ScimBearerAuthenticationFilter (+17 more)

### Community 92 - ".getCount"
Cohesion: 0.20
Nodes (5): getCount operation, CountResponse, Override, RecordingCounterService, UserCounterControllerTests

### Community 93 - "org.springframework.web.bind.annotation.GetMapping"
Cohesion: 0.18
Nodes (5): ScimDiscovery, ScimDiscoveryController, ScimDiscoveryTests, org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.GetMapping

### Community 94 - "AuthController.java"
Cohesion: 0.12
Nodes (19): authenticationprincipal, UserCounterController, UnknownConnectorException, cachecontrol, cookievalue, httpstatus, notblank, notnull (+11 more)

### Community 95 - "RedisSessionRevocationIntegrationTests.java"
Cohesion: 0.10
Nodes (15): LockoutHasNoDurationTests, BackendApplicationTests, classmode, drivermanager, files, java.lang.reflect.RecordComponent, javax.sql.DataSource, org.junit.jupiter.params.provider.MethodSource (+7 more)

### Community 96 - "RFC requirements and implications"
Cohesion: 0.20
Nodes (10): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+2 more)

### Community 97 - "RefusalTimingEquivalenceTests"
Cohesion: 0.27
Nodes (3): CountingPasswordEncoder, Override, RefusalTimingEquivalenceTests

### Community 98 - "EcsLogCapture"
Cohesion: 0.18
Nodes (8): EcsLogCapture, Override, bytearrayoutputstream, ch.qos.logback.classic.LoggerContext, ch.qos.logback.core.OutputStreamAppender, org.springframework.core.env.Environment, structuredlogencoder, tools.jackson.databind.json.JsonMapper

### Community 99 - "SCIM 2.0 account-management research"
Cohesion: 0.22
Nodes (7): Executive finding, Existing application seams, Primary sources, Recommended implementation order, Resolved RFC decisions, SCIM 2.0 account-management research, Testing strategy

### Community 100 - "Credential and cryptographic policy"
Cohesion: 0.25
Nodes (8): Credential and cryptographic policy, Hashing and primitives, Key rotation and storage, Lockout policy, Password policy, Response headers, Session lifetime, Uniform authentication timing

### Community 101 - "Delivery plan"
Cohesion: 0.20
Nodes (10): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 0a — Permanent lockout, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 4 — Groups and Admin authority, Slice 5 — Complete query protocol (+2 more)

### Community 103 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.36
Nodes (3): AuditTrailService, Override, org.springframework.transaction.annotation.Transactional

### Community 104 - "Query contract"
Cohesion: 0.25
Nodes (8): Search, filtering, sorting and projection, Attribute projection, Filtering, Pagination, POST search, Query contract, Sorting, count()

### Community 105 - "Domain and authority model"
Cohesion: 0.29
Nodes (7): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, SCIM User replaces Account

### Community 106 - "AuditEventEntity"
Cohesion: 0.11
Nodes (4): AuditOutcome, FAILURE, SUCCESS, AuditEventEntity

### Community 107 - "AuditEvent"
Cohesion: 0.20
Nodes (6): AuditEvent, AuditEventRepository, AuditEventJpaRepository, AuditEventPersistenceAdapter, Override, RecordingRepository

### Community 108 - "Definition of Done"
Cohesion: 0.40
Nodes (5): Backend gates, Contract and behavior, Definition of Done, Documentation and graph, Frontend gates

### Community 109 - "Write semantics"
Cohesion: 0.40
Nodes (5): DELETE and tombstones, PATCH, POST, PUT, Write semantics

### Community 110 - "Supported schemas"
Cohesion: 0.67
Nodes (3): Group, Supported schemas, User

### Community 112 - "ScimConnectorTokenEntity"
Cohesion: 0.16
Nodes (4): ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 115 - "AuditOperation"
Cohesion: 0.09
Nodes (20): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+12 more)

### Community 117 - ".overlapEnd"
Cohesion: 0.18
Nodes (3): ConnectorTokenPolicyTests, Lifetime, RotationOverlap

### Community 119 - "AbsoluteSessionLifetimePolicy"
Cohesion: 0.27
Nodes (3): AbsoluteSessionLifetimeFilter, AbsoluteSessionLifetimePolicy, AbsoluteSessionLifetimePolicyTests

### Community 120 - "RecordingAuditTrail"
Cohesion: 0.27
Nodes (3): Override, Recorded, RecordingAuditTrail

### Community 121 - "4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal"
Cohesion: 0.25
Nodes (6): 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal, Alternatives considered, Consequences, Context, Decision, Status

### Community 122 - "ScimSecurityChainOrderTests.java"
Cohesion: 0.16
Nodes (11): anonymousauthenticationfilter, ScimSecurityChainOrderTests, basicauthenticationfilter, csrffilter, disableencodeurlfilter, exceptiontranslationfilter, logoutfilter, org.assertj.core.api.SoftAssertions (+3 more)

### Community 124 - "ScimOffsetPage"
Cohesion: 0.33
Nodes (4): Override, ScimOffsetPage, org.springframework.data.domain.Pageable, org.springframework.data.domain.Sort

### Community 125 - "ScimConnector"
Cohesion: 0.24
Nodes (3): ScimConnector, InMemoryScimConnectorRepository, Override

### Community 126 - "ScimConnectorEntity"
Cohesion: 0.23
Nodes (4): ScimConnectorEntity, ScimConnectorJpaRepository, Override, ScimConnectorPersistenceAdapter

### Community 128 - "Account"
Cohesion: 0.31
Nodes (3): Account, InMemoryAccountRepository, Override

### Community 129 - "ScimPageRequest"
Cohesion: 0.23
Nodes (5): ScimUserListing, ScimPageRequest, ScimUserJpaRepository, Override, ScimUserPersistenceAdapter

### Community 131 - ".scimType"
Cohesion: 0.17
Nodes (9): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status, Error contract, Log formatting, Operational telemetry (+1 more)

### Community 132 - "AuditTrailService.java"
Cohesion: 0.22
Nodes (6): AuditRequestContext, AuditScimRefusal, UNIQUENESS, org.springframework.transaction.PlatformTransactionManager, org.springframework.transaction.support.TransactionTemplate, transactiondefinition

### Community 137 - "AuditRetentionStartupTests.java"
Cohesion: 0.25
Nodes (4): applicationconversionservice, AuditRetentionStartupTests, org.springframework.boot.test.context.runner.ApplicationContextRunner, propertysourcesplaceholderconfigurer

### Community 138 - "AdminConnectorController"
Cohesion: 0.44
Nodes (5): IssuedConnectorToken, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest, RotateTokenRequest

### Community 139 - "ScimConnectorToken"
Cohesion: 0.47
Nodes (3): ScimConnectorToken, InMemoryScimConnectorTokenRepository, Override

### Community 140 - "ScimUserEntity.java"
Cohesion: 0.22
Nodes (8): cascadetype, collectiontable, elementcollection, fetchtype, joincolumn, mapsid, onetoone, ordercolumn

### Community 141 - "ScimExceptionHandler.java"
Cohesion: 0.36
Nodes (3): ScimExceptionHandler, DuplicateUserNameException, org.springframework.web.bind.annotation.RestControllerAdvice

### Community 144 - "CONTEXT"
Cohesion: 0.29
Nodes (6): Accounts and identity provisioning, CONTEXT, Current account model, Request paths, SCIM target model, Sessions

### Community 146 - "main.tsx"
Cohesion: 0.33
Nodes (5): One-Way Import Direction Through the Layers, Frontend SPA Entry HTML, frontend_src_index, container, react-dom

## Ambiguous Edges - Review These
- `login operation` → `Shared Playwright storageState for Auth`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to

## Knowledge Gaps
- **348 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+343 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 627 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **43 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `login operation` and `Shared Playwright storageState for Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Backend API Contract (OpenAPI 3.1)` connect `Backend API Contract (OpenAPI 3.1)` to `AccountService`, `.getCount`, `api.ts`?**
  _High betweenness centrality (0.168) - this node is a cross-community bridge._
- **Why does `login operation` connect `api.ts` to `Backend API Contract (OpenAPI 3.1)`, `auth.helpers.ts`?**
  _High betweenness centrality (0.114) - this node is a cross-community bridge._
- **Why does `Shared Playwright storageState for Auth` connect `auth.helpers.ts` to `api.ts`?**
  _High betweenness centrality (0.092) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _348 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AuthControllerTests.java` be split into smaller, more focused modules?**
  _Cohesion score 0.09468599033816426 - nodes in this community are weakly interconnected._