# Graph Report - monorepo-base-issue-30  (2026-09-26)

## Corpus Check
- 298 files · ~163,123 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: (none) 10, .example 1, .properties 1)

## Summary
- 2868 nodes · 8146 edges · 169 communities (117 shown, 52 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 925 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `acbba46c`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- org.springframework.security.crypto.password.PasswordEncoder
- AuditTrail
- accounts.tsx
- auth.helpers.ts
- api.ts
- Workflow
- ScimConnectorLifecycleIntegrationTests
- .require
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
- AccountTests
- mvnw
- EC2Instance
- org.junit.jupiter.params.ParameterizedTest
- Frontend Technology Stack
- Spring Session In Redis
- route-guards.tsx
- infra/ Is Deployment Material Not An App
- AccountRepository
- tsconfig.test.json
- SecurityConfig.java
- Domain Documentation Guide
- GitHub Issue Tracker Guide
- AuditEventRecordingIntegrationTests
- PIT Scoped To Touched Tests
- Graphify Runner Agent
- .findByUsername
- cleanup.sh
- get-vpc-info.sh
- Backend Semgrep Baseline Gate
- Graphify Runner
- Frontend Project Structure
- ScimBearerAuthenticationFilterTests.java
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
- .of
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh
- assertthat
- AccountSummary
- ConnectorTokenSecretTests
- auth-context-value.ts
- ArchitectureTest.java
- org.springframework.context.annotation.Configuration
- org.springframework.context.annotation.Bean
- LoginAttemptServiceTests
- SCIM 2.0 account-management specification plan
- org.junit.jupiter.api.AfterEach
- EcsLogFormatTests
- ConnectorAdministrationService
- .status
- .sessionsOf
- dependencies
- AdminConnectorController
- ScimUserAttributesTests
- LogContextTests
- ScimBearerAuthenticationFilterTests
- AuditRetentionPolicy
- AuditAppendOnlyIntegrationTests.java
- .increment
- BackendApplication.java
- ScimUserEntity.java
- verify.sh
- ScimUserPersistenceAdapter.java
- AccountLockoutPersistenceIntegrationTests
- jakarta.servlet.http.HttpServletResponse
- AuditTrailServiceTests
- jakarta.servlet.http.HttpServletRequest
- RecordingCounterService
- ScimUserController.java
- AuthController.java
- org.springframework.boot.test.context.SpringBootTest
- RFC requirements and implications
- .overlapEnd
- ScimPageRequest
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
- ScimUser
- AuditAppendOnlyIntegrationTests
- ConnectorTokenScope
- ScimUserEntity
- SpaFrontendTests
- AuditOperation
- .of
- org.junit.jupiter.api.Test
- hashmap
- Backend API Contract (OpenAPI 3.1)
- RecordingAuditTrail
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- ScimDiscovery
- PermanentLockoutMigrationTests
- ScimOffsetPage
- ScimConnector
- App.tsx
- ScimDiscoveryIntegrationTests
- Account
- .byId
- components.json
- .scimType
- AuditTrailService.java
- ConnectorAuthenticationServiceTests
- ScimConnectorTokenTests
- .of
- .requiresWriteScope
- AuthControllerTests
- ScimConnectorToken
- ScimConnectorTokenRepository
- UserCounter
- ScimEmail
- AccountService
- ScimUserProfileTests
- CONTEXT
- .identifies
- Frontend Testing Guide
- org.springframework.jdbc.core.JdbcTemplate
- ScimSchemas
- UserCounterEntity
- filterchainproxy
- mockfilterchain
- mockhttpservletresponse
- recordcomponent
- .summarize
- InMemoryAccountSessions
- Attribute
- RecordingTransactionManager
- ScimConnectorTokenPersistenceAdapter
- SpaRoutes
- InMemoryScimExternalIdRepository
- ScimBearerAuthenticationFilter.java
- AbsoluteSessionLifetimePolicyTests
- Frontend Architecture Doc
- ScimReleaseGate
- UnknownConnectorException
- InvalidConnectorTokenLifetimeException
- transactiondefinition
- transactionstatus

## God Nodes (most connected - your core abstractions)
1. `Account` - 65 edges
2. `AccountAdministrationServiceTests` - 54 edges
3. `AuditOperation` - 46 edges
4. `ScimConnectorLifecycleIntegrationTests` - 46 edges
5. `ScimUserProvisioningIntegrationTests` - 42 edges
6. `AuditAppendOnlyIntegrationTests` - 41 edges
7. `AuditTrail` - 39 edges
8. `AccountRepository` - 38 edges
9. `ScimConnectorToken` - 36 edges
10. `AuditEventRecordingIntegrationTests` - 35 edges

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

## Communities (169 total, 52 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 1 - "org.springframework.security.crypto.password.PasswordEncoder"
Cohesion: 0.16
Nodes (9): CountingPasswordEncoder, Override, AccountSeedConfigTests, badcredentialsexception, disabledexception, lockedexception, org.springframework.security.authentication.AuthenticationManager, org.springframework.security.crypto.password.PasswordEncoder (+1 more)

### Community 2 - "AuditTrail"
Cohesion: 0.12
Nodes (5): AuditTrail, ScimUserResource, ScimUserService, ScimExternalIdRepository, ScimUserRepository

### Community 3 - "accounts.tsx"
Cohesion: 0.15
Nodes (26): One-Way Import Direction Through the Layers, components/ui Is a Package Placeholder, useSessionRequest(), Button(), ButtonProps, buttonVariants, Card(), CardContent() (+18 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.17
Nodes (13): Every Playwright Spec Needs a testMatch, Shared Playwright storageState for Auth, ADMIN_CREDENTIALS, captureSessionCookie(), expireSession(), login(), loginAs(), postAdminAction() (+5 more)

### Community 5 - "api.ts"
Cohesion: 0.15
Nodes (17): decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, SessionRequest, SessionResult (+9 more)

### Community 6 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 7 - "ScimConnectorLifecycleIntegrationTests"
Cohesion: 0.09
Nodes (3): ScimConnectorLifecycleIntegrationTests, ScimUserProvisioningIntegrationTests, org.springframework.test.web.servlet.MvcResult

### Community 8 - ".require"
Cohesion: 0.20
Nodes (4): LoginOutcome, LoginService, LoginLockoutTests, org.springframework.security.core.AuthenticationException

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
Cohesion: 0.11
Nodes (7): ScimErrorException, ScimExceptionHandler, ScimUserRequestReader, ScimUserRequestReaderTests, org.springframework.http.HttpStatus, org.springframework.web.bind.annotation.RestControllerAdvice, tools.jackson.databind.JsonNode

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "uuid"
Cohesion: 0.09
Nodes (19): ScimExternalIdJpaRepository, Override, ScimExternalIdPersistenceAdapter, collectors, comparator, linkedhashmap, linkedhashset, list (+11 more)

### Community 18 - "lib.sh"
Cohesion: 0.24
Nodes (14): die(), load_backend_env(), log(), pinned_version(), port_holder(), require_cmd(), require_docker(), require_maven() (+6 more)

### Community 19 - "AGENTS.md"
Cohesion: 0.12
Nodes (14): Agent, Agent documentation, Build and validation, Environment, Frontend/backend integration, graphify, Ignore rules, Layout (+6 more)

### Community 20 - "deploy.sh"
Cohesion: 0.42
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 22 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 23 - "EC2Instance"
Cohesion: 0.17
Nodes (16): ALBListener, ALBTargetGroup, EC2Instance, EC2InstanceProfile, EC2KeyPair, EC2Role, TargetGroupAttachment, Infra Quickstart Flow (+8 more)

### Community 24 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.16
Nodes (7): SpaRoutesScimNamespaceTests, ReservedServerPaths, SpaRoutesTests, SpaShell, org.junit.jupiter.api.Nested, org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource

### Community 25 - "Frontend Technology Stack"
Cohesion: 0.25
Nodes (8): No Parent-Relative Paths From An App, SPA Build Contract, with-frontend Maven Profile, Backend Serves SPA And Forwards Routes, Claim: No Router Data Layer Or Auth, No .env Required In Frontend, Frontend Technology Stack, npm ci Not npm install

### Community 26 - "Spring Session In Redis"
Cohesion: 0.18
Nodes (14): docs/openapi.yaml API Contract, Postgres Compose Service, Redis Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, Spring Session In Redis (+6 more)

### Community 27 - "route-guards.tsx"
Cohesion: 0.21
Nodes (12): AuthRole, AuthStatus, useAuth(), useAuthState(), SessionRoute(), DEFAULT_DESTINATION, LOGIN_PATH, resolveSessionRoute() (+4 more)

### Community 28 - "infra/ Is Deployment Material Not An App"
Cohesion: 0.20
Nodes (10): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, Backend Architecture Boundaries, Flag ADR Conflicts Explicitly, docs/adr Decision Records, CONTEXT.md Domain Glossary, gh CLI Conventions (+2 more)

### Community 29 - "AccountRepository"
Cohesion: 0.16
Nodes (11): AccountAdministrationService, AfterCommit, LoginAttemptService, AccountRepository, AccountSessions, BootstrapAdmin, LockoutPolicy, org.springframework.security.core.userdetails.UserDetails (+3 more)

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "SecurityConfig.java"
Cohesion: 0.11
Nodes (18): argon2passwordencoder, authenticationentrypoint, authorizationfilter, ScimSecurityConfig, changesessionidauthenticationstrategy, cookiecsrftokenrepository, daoauthenticationprovider, delegatingpasswordencoder (+10 more)

### Community 32 - "Domain Documentation Guide"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 33 - "GitHub Issue Tracker Guide"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 36 - "Graphify Runner Agent"
Cohesion: 0.40
Nodes (5): Graphify Runner Agent, Recorded Interpreter Guard, Graph Shrink Refusal, Graphify Refresh Before Commit, CLAUDE.md Graphify Override

### Community 37 - ".findByUsername"
Cohesion: 0.08
Nodes (10): Override, AccountServiceTests, Override, PrefixPasswordEncoder, 1. Count login attempts on the login path, Alternatives considered, Consequences, Context (+2 more)

### Community 38 - "cleanup.sh"
Cohesion: 0.70
Nodes (4): print_error(), print_info(), print_warn(), cleanup.sh script

### Community 39 - "get-vpc-info.sh"
Cohesion: 0.70
Nodes (4): print_header(), print_info(), print_warn(), get-vpc-info.sh script

### Community 40 - "Backend Semgrep Baseline Gate"
Cohesion: 0.17
Nodes (13): Long-Gate Sentinel And Log Pattern, Published Default Credentials Warning, Security-Sensitive Change Policy, Backend Semgrep Baseline Gate, Development Default Credentials, be-authorize-any-request-permit-all, be-cors-wildcard-origin, be-csrf-disabled (+5 more)

### Community 41 - "Graphify Runner"
Cohesion: 0.50
Nodes (3): Graphify Runner, Reporting, Steps

### Community 42 - "Frontend Project Structure"
Cohesion: 0.67
Nodes (3): shadcn Placeholder Primitives, src/ Dependency Direction Rules, Frontend Project Structure

### Community 43 - "ScimBearerAuthenticationFilterTests.java"
Cohesion: 0.15
Nodes (12): AuthenticatedConnector, ConnectorTokenSecret, base64, clock, dispatchertype, grantedauthority, hashset, java.security.SecureRandom (+4 more)

### Community 50 - "AccountEntity"
Cohesion: 0.15
Nodes (5): AccountJpaRepository, AccountPersistenceAdapter, Override, AccountEntity, AccountEntityTests

### Community 57 - "SessionController"
Cohesion: 0.24
Nodes (7): deleteSession operation, SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession, org.springframework.web.bind.annotation.PutMapping

### Community 58 - ".of"
Cohesion: 0.12
Nodes (4): SuppressWarnings, ScimAttributeProjection, SuppressWarnings, ScimAttributeProjectionTests

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "assertthat"
Cohesion: 0.09
Nodes (17): applicationconversionservice, assertthat, assertthatcode, assertthatthrownby, LogEvent, ConnectorTokenPolicy, Override, MutableClock (+9 more)

### Community 63 - "AccountSummary"
Cohesion: 0.24
Nodes (4): AccountSummary, AdminAccountControllerTests, Override, RecordingService

### Community 64 - "ConnectorTokenSecretTests"
Cohesion: 0.11
Nodes (9): arrays, ConnectorTokenDigest, Override, Minted, Presented, ConnectorTokenSecretTests, java.security.MessageDigest, nosuchalgorithmexception (+1 more)

### Community 65 - "auth-context-value.ts"
Cohesion: 0.09
Nodes (17): AuthUser, AuthContext, AuthContextState, AuthContextValue, apiFetchMock, request(), state, wrapper() (+9 more)

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 67 - "org.springframework.context.annotation.Configuration"
Cohesion: 0.27
Nodes (7): accountseed, AuditRetentionPolicyConfig, AccountSeed, AccountSeedConfig, org.springframework.boot.ApplicationRunner, org.springframework.context.annotation.Configuration, value

### Community 68 - "org.springframework.context.annotation.Bean"
Cohesion: 0.20
Nodes (5): LoginLockoutConfig, SecurityConfig, SecurityConfigPasswordEncoderTests, org.springframework.context.annotation.Bean, org.springframework.security.web.context.SecurityContextRepository

### Community 70 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.10
Nodes (20): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Goals (+12 more)

### Community 71 - "org.junit.jupiter.api.AfterEach"
Cohesion: 0.07
Nodes (19): AuditRequest, HttpAuditRequestContext, Override, AfterCommitAdapter, Override, HttpAuditRequestContextTests, AfterCommitAdapterTests, 2. Revoke a disabled account's sessions after the commit (+11 more)

### Community 72 - "EcsLogFormatTests"
Cohesion: 0.07
Nodes (18): AuditRetentionServiceTests, CountingRetention, Override, CapturedLog, Override, EcsLogCapture, Override, EcsLogFormatTests (+10 more)

### Community 74 - ".status"
Cohesion: 0.12
Nodes (3): ProbeController, SecurityConfigTests, AdminAccountEndpointTests

### Community 75 - ".sessionsOf"
Cohesion: 0.22
Nodes (3): Override, PendingCommit, Consequences

### Community 76 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 77 - "AdminConnectorController"
Cohesion: 0.12
Nodes (12): UnknownAccountException, UnsafeAccountChangeException, AdminAccountController, IssuedConnectorToken, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest, RotateTokenRequest (+4 more)

### Community 78 - "ScimUserAttributesTests"
Cohesion: 0.15
Nodes (3): ScimUserAttributes, SuppressWarnings, ScimUserAttributesTests

### Community 79 - "LogContextTests"
Cohesion: 0.20
Nodes (4): Override, LogContext, Scope, LogContextTests

### Community 81 - "AuditRetentionPolicy"
Cohesion: 0.14
Nodes (12): AuditRetentionService, AuditRetentionScheduleConfig, Override, AuditEventRetention, AuditRetentionPolicy, AuditRetentionPolicyTests, crontask, crontrigger (+4 more)

### Community 82 - "AuditAppendOnlyIntegrationTests.java"
Cohesion: 0.09
Nodes (52): assertthatnoexception, authenticationmanager, autowired, RequestIdFilter, ScimReleaseGateIntegrationTests, bean, classpathresource, containsstring (+44 more)

### Community 83 - ".increment"
Cohesion: 0.20
Nodes (5): incrementCount operation, resetCount operation, CountResponse, UserCounterController, UserCounterServiceTests

### Community 84 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 85 - "ScimUserEntity.java"
Cohesion: 0.07
Nodes (25): ScimConnectorEntity, Override, Key, ScimExternalIdEntity, ScimConnectorJpaRepository, Override, ScimConnectorPersistenceAdapter, cascadetype (+17 more)

### Community 87 - "ScimUserPersistenceAdapter.java"
Cohesion: 0.12
Nodes (11): arraylist, NewScimUser, ScimName, ScimResourceType, GROUP, USER, ScimUserProfile, dataintegrityviolationexception (+3 more)

### Community 88 - "AccountLockoutPersistenceIntegrationTests"
Cohesion: 0.11
Nodes (11): AccountSessionsAdapter, Override, AccountLockoutPersistenceIntegrationTests, AccountSessionsAdapterTests, IndexedSessions, Override, RedisSessionRevocationIntegrationTests, org.springframework.session.FindByIndexNameSessionRepository (+3 more)

### Community 89 - "jakarta.servlet.http.HttpServletResponse"
Cohesion: 0.15
Nodes (9): Override, ScimBearerAuthenticationFilter, ScimBearerChallenge, jakarta.servlet.http.HttpServletResponse, graphify-runner, Kiro: graphify enforcement, The hooks, When the refresh fails (+1 more)

### Community 91 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.15
Nodes (12): AbsoluteSessionLifetimeFilter, Override, AbsoluteSessionLifetimePolicy, Override, Override, httpsession, jakarta.servlet.FilterChain, jakarta.servlet.http.HttpServletRequest (+4 more)

### Community 92 - "RecordingCounterService"
Cohesion: 0.27
Nodes (3): Override, RecordingCounterService, UserCounterControllerTests

### Community 93 - "ScimUserController.java"
Cohesion: 0.30
Nodes (7): authenticationprincipal, ScimDiscoveryController, org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.GetMapping, pathvariable, requestparam, servleturicomponentsbuilder

### Community 94 - "AuthController.java"
Cohesion: 0.21
Nodes (15): AuthController, cachecontrol, cookievalue, httpstatus, notblank, notnull, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy, org.springframework.session.web.http.CookieSerializer (+7 more)

### Community 95 - "org.springframework.boot.test.context.SpringBootTest"
Cohesion: 0.07
Nodes (33): anonymousauthenticationfilter, LockoutHasNoDurationTests, BackendApplicationTests, ContainerTestConfiguration, ScimSecurityChainOrderTests, basicauthenticationfilter, classmode, csrffilter (+25 more)

### Community 96 - "RFC requirements and implications"
Cohesion: 0.20
Nodes (10): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+2 more)

### Community 97 - ".overlapEnd"
Cohesion: 0.18
Nodes (3): ConnectorTokenPolicyTests, Lifetime, RotationOverlap

### Community 98 - "ScimPageRequest"
Cohesion: 0.16
Nodes (6): ScimUserListing, DuplicateUserNameException, ScimPageRequest, ScimUserJpaRepository, Override, ScimUserPersistenceAdapter

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
Cohesion: 0.31
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
Cohesion: 0.24
Nodes (6): AuditEvent, AuditEventRepository, AuditEventJpaRepository, AuditEventPersistenceAdapter, Override, RecordingRepository

### Community 108 - "Definition of Done"
Cohesion: 0.40
Nodes (5): Backend gates, Contract and behavior, Definition of Done, Documentation and graph, Frontend gates

### Community 109 - "Write semantics"
Cohesion: 0.40
Nodes (5): DELETE and tombstones, PATCH, POST, PUT, Write semantics

### Community 110 - "ScimUser"
Cohesion: 0.21
Nodes (5): ScimUser, ScimUserTests, Group, Supported schemas, User

### Community 112 - "ConnectorTokenScope"
Cohesion: 0.19
Nodes (4): ConnectorTokenScope, READ_ONLY, READ_WRITE, ScimConnectorTokenEntity

### Community 113 - "ScimUserEntity"
Cohesion: 0.12
Nodes (4): ScimResourceEntity, ScimUserEmailValue, ScimUserEntity, jakarta.persistence.Embeddable

### Community 114 - "SpaFrontendTests"
Cohesion: 0.19
Nodes (7): Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.stereotype.Component, org.springframework.web.servlet.ModelAndView, requestdispatcher

### Community 115 - "AuditOperation"
Cohesion: 0.12
Nodes (16): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+8 more)

### Community 117 - "org.junit.jupiter.api.Test"
Cohesion: 0.07
Nodes (8): AuditRetentionStartupTests, RefusalTimingEquivalenceTests, AbsoluteSessionLifetimeFilterTests, RequestIdFilterTests, Connectors, Tokens, org.junit.jupiter.api.Test, org.springframework.boot.test.context.runner.ApplicationContextRunner

### Community 119 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.12
Nodes (17): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, getCount operation, getCurrentUser operation, getHealth operation, getSession operation, login operation, logout operation (+9 more)

### Community 120 - "RecordingAuditTrail"
Cohesion: 0.27
Nodes (3): Override, Recorded, RecordingAuditTrail

### Community 121 - "4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal"
Cohesion: 0.25
Nodes (6): 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal, Alternatives considered, Consequences, Context, Decision, Status

### Community 124 - "ScimOffsetPage"
Cohesion: 0.33
Nodes (4): Override, ScimOffsetPage, org.springframework.data.domain.Pageable, org.springframework.data.domain.Sort

### Community 125 - "ScimConnector"
Cohesion: 0.26
Nodes (3): ScimConnector, InMemoryScimConnectorRepository, Override

### Community 126 - "App.tsx"
Cohesion: 0.18
Nodes (9): Frontend SPA Entry HTML, App(), AuthProvider(), GuestRoute(), ProtectedRoute(), frontend_src_index, container, Login() (+1 more)

### Community 128 - "Account"
Cohesion: 0.11
Nodes (19): atomicinteger, authentication, authenticationexception, AuditRefusalReason, ACCOUNT_DISABLED, ACCOUNT_LOCKED, BAD_CREDENTIALS, OTHER (+11 more)

### Community 130 - "components.json"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

### Community 131 - ".scimType"
Cohesion: 0.17
Nodes (9): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status, Error contract, Log formatting, Operational telemetry (+1 more)

### Community 132 - "AuditTrailService.java"
Cohesion: 0.17
Nodes (7): AuditRequestContext, AuditScimRefusal, UNIQUENESS, OperationalAlerts, Override, LoggingOperationalAlerts, org.springframework.transaction.PlatformTransactionManager

### Community 137 - "AuthControllerTests"
Cohesion: 0.21
Nodes (4): LoginRequest, UserResponse, AuthControllerTests, org.springframework.security.core.Authentication

### Community 138 - "ScimConnectorToken"
Cohesion: 0.36
Nodes (3): ScimConnectorToken, InMemoryScimConnectorTokenRepository, Override

### Community 139 - "ScimConnectorTokenRepository"
Cohesion: 0.23
Nodes (3): ConnectorAuthenticationService, ScimConnectorRepository, ScimConnectorTokenRepository

### Community 142 - "AccountService"
Cohesion: 0.29
Nodes (3): AccountService, UserCounterService, UserCounterRepository

### Community 144 - "CONTEXT"
Cohesion: 0.29
Nodes (6): Accounts and identity provisioning, CONTEXT, Current account model, Request paths, SCIM target model, Sessions

### Community 146 - "Frontend Testing Guide"
Cohesion: 0.14
Nodes (15): ArchUnit Baseline Gate, Always ./mvnw Never Bare mvn, Colors Come From index.css Tokens, Trace Before You Delete, Frontend Local Semgrep Ruleset, Baseline Full Extensive Test Levels, Vite Full-Reloads on Any Watched HTML Write, Frontend Testing Guide (+7 more)

### Community 147 - "org.springframework.jdbc.core.JdbcTemplate"
Cohesion: 0.47
Nodes (3): AuditEventRetentionAdapter, Override, org.springframework.jdbc.core.JdbcTemplate

### Community 149 - "UserCounterEntity"
Cohesion: 0.22
Nodes (5): UserCounterEntity, UserCounterJpaRepository, Override, UserCounterPersistenceAdapter, org.springframework.data.jpa.repository.Lock

### Community 155 - "InMemoryAccountSessions"
Cohesion: 0.24
Nodes (8): SessionRegistryConfiguration, SessionRegistryConfiguration, SessionRegistryConfiguration, InMemoryAccountSessions, Override, InMemorySessionRegistryConfiguration, org.springframework.boot.test.context.TestConfiguration, org.springframework.context.annotation.Primary

### Community 157 - "RecordingTransactionManager"
Cohesion: 0.43
Nodes (4): Override, RecordingTransactionManager, org.springframework.transaction.TransactionDefinition, org.springframework.transaction.TransactionStatus

### Community 158 - "ScimConnectorTokenPersistenceAdapter"
Cohesion: 0.29
Nodes (3): ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 160 - "InMemoryScimExternalIdRepository"
Cohesion: 0.43
Nodes (3): Alias, InMemoryScimExternalIdRepository, Override

### Community 161 - "ScimBearerAuthenticationFilter.java"
Cohesion: 0.33
Nodes (4): ScimWriteScopeRule, httpheaders, ioexception, preauthenticatedauthenticationtoken

### Community 163 - "Frontend Architecture Doc"
Cohesion: 0.40
Nodes (6): Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline, Vitest Deliberately Omits the Tailwind Vite Plugin

## Ambiguous Edges - Review These
- `login operation` → `Shared Playwright storageState for Auth`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to

## Knowledge Gaps
- **348 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+343 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 628 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **52 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `login operation` and `Shared Playwright storageState for Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Backend API Contract (OpenAPI 3.1)` connect `Backend API Contract (OpenAPI 3.1)` to `SessionController`, `.increment`?**
  _High betweenness centrality (0.172) - this node is a cross-community bridge._
- **Why does `login operation` connect `Backend API Contract (OpenAPI 3.1)` to `auth.helpers.ts`, `api.ts`?**
  _High betweenness centrality (0.119) - this node is a cross-community bridge._
- **Why does `Shared Playwright storageState for Auth` connect `auth.helpers.ts` to `Frontend Testing Guide`, `Backend API Contract (OpenAPI 3.1)`?**
  _High betweenness centrality (0.098) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _348 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AuditTrail` be split into smaller, more focused modules?**
  _Cohesion score 0.12315270935960591 - nodes in this community are weakly interconnected._