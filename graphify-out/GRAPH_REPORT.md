# Graph Report - monorepo-base  (2026-09-29)

## Corpus Check
- 372 files · ~233,518 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: (none) 9, .example 1, .properties 1)

## Summary
- 3880 nodes · 12802 edges · 186 communities (128 shown, 58 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 1478 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e8d51224`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- vitest
- .sessionsOf
- .seed
- accounts.tsx
- auth.helpers.ts
- http.ts
- Workflow
- ScimConnectorLifecycleIntegrationTests
- .require
- devDependencies
- stryker.config.json
- compilerOptions
- Frontend Local Semgrep Ruleset
- package.json
- scripts
- tools.jackson.databind.JsonNode
- compilerOptions
- ScimUserPatchReaderTests.java
- lib.sh
- AGENTS.md
- deploy.sh
- ScimGroupProvisioningIntegrationTests
- mvnw
- EC2Instance
- org.junit.jupiter.params.ParameterizedTest
- ScimGroupTests
- DBInstance RDS PostgreSQL
- App.tsx
- infra/ Is Deployment Material Not An App
- .given
- tsconfig.test.json
- org.springframework.context.annotation.Bean
- Domain Documentation Guide
- GitHub Issue Tracker Guide
- IdentityAdministrationServiceTests
- PIT Scoped To Touched Tests
- Graphify Runner Agent
- ScimGroupServiceTests
- cleanup.sh
- get-vpc-info.sh
- Backend Semgrep Baseline Gate
- Graphify Runner
- Frontend Project Structure
- ScimUserProvisioningIntegrationTests
- prettier.config.mjs
- dev.sh
- integration-test.sh
- semgrep.sh
- CLAUDE.md
- ScimConnectorEntity
- bootstrap.sh
- package.sh
- Test Static index.html Stub
- com.example:backend
- AuditRetentionPolicy
- IdentityAdministrationService
- .ofUser
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh
- .servesSpaShell
- ScimUserServiceTests
- ConnectorTokenSecretTests
- auth-context-value.ts
- ArchitectureTest.java
- accountseed
- AuditRetentionStartupTests.java
- LoginAttemptServiceTests
- SCIM 2.0 account-management specification plan
- org.junit.jupiter.api.AfterEach
- AuditRetentionServiceTests
- ConnectorAdministrationService
- AuditAppendOnlyIntegrationTests
- ScimPasswordHistoryEntity
- Backend API Contract (OpenAPI 3.1)
- RFC requirements and implications
- ScimUserAttributesTests
- LogContextTests
- ScimBearerAuthenticationFilterTests
- CapturedLog
- ScimConditionalWriteIntegrationTests
- AWS CloudFormation Deployment Guide
- BackendApplication.java
- ScimDeletionIntegrationTests
- verify.sh
- list
- jakarta.persistence.Entity
- jakarta.servlet.http.HttpServletRequest
- AuditTrail
- Baseline Full Extensive Test Levels
- UserCounterService
- ScimUserEdit
- ScimGroupController.java
- ScimSecurityChainOrderTests.java
- SCIM 2.0 account-management research
- uuid
- ScimUser
- ScimConditionalWriteIntegrationTests.java
- AuthenticatedConnector
- Delivery plan
- bcryptpasswordencoder
- org.springframework.transaction.annotation.Transactional
- RequestIdFilterTests
- .scimType
- ScimUserPatchOperationTests
- AuditEvent
- SecurityConfig.java
- Credential and cryptographic policy
- .created
- Spring Session In Redis
- ScimConnectorTokenEntity
- ScimUserEntity
- SpaFrontendTests
- AuditOperation
- AdminAccountEndpointTests
- org.junit.jupiter.api.Test
- AuthControllerTests.java
- EcsLogCapture
- RecordingAuditTrail
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- AuditUserAttribute
- InMemoryScimExternalIdRepository
- ScimOffsetPage
- ScimConnector
- ScimGroupPersistenceAdapter
- ScimDiscoveryIntegrationTests
- LockoutHasNoDurationTests.java
- org.springframework.http.ResponseEntity
- components.json
- ScimPatchRefusedException
- 1. Count login attempts on the login path
- Domain and authority model
- ScimConnectorTokenTests
- .handle
- .of
- AuthControllerTests
- Key
- EcsLogFormatTests
- UserCounter
- ScimEmail
- AuditEventRetention
- ScimGroup
- ScimGroupController
- dependencies
- Frontend Architecture Doc
- .overlapEnd
- ScimSchemas
- IdentitySummary
- filterchainproxy
- mockfilterchain
- mockhttpservletresponse
- recordcomponent
- TextAttribute
- ScimUserPatchReaderTests
- ScimLoginStateTests
- Kiro: graphify enforcement
- org.springframework.data.jpa.repository.Query
- ScimGroupService
- MutableClock
- Query contract
- ScimAttributeProjection
- .ofIfMatch
- Definition of Done
- Write semantics
- CountingPasswordEncoder
- transactiondefinition
- transactionstatus
- CountingPasswordEncoder
- HttpAuditRequestContextTests.java
- ScimUserPatchOperation
- drivermanager
- inmemoryuserdetailsmanager
- Condition
- ConnectorTokenSecretTests.java
- pit-monitor-kiro.md
- ScimConnectorToken
- CountingPasswordEncoder
- .requiresWriteScope
- .of
- Kind
- AuditEventEntity
- RecordingTransactionManager
- ScimUserProfileTests
- AbsoluteSessionLifetimePolicyTests

## God Nodes (most connected - your core abstractions)
1. `ScimUser` - 109 edges
2. `ScimGroupProvisioningIntegrationTests` - 77 edges
3. `ScimGroup` - 72 edges
4. `IdentityAdministrationServiceTests` - 65 edges
5. `ScimConditionalWriteIntegrationTests` - 64 edges
6. `AuditTrail` - 62 edges
7. `ScimDeletionIntegrationTests` - 60 edges
8. `AuditOperation` - 59 edges
9. `RecordingAuditTrail` - 55 edges
10. `AuthenticatedConnector` - 53 edges

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

## Communities (186 total, 58 thin omitted)

### Community 0 - "vitest"
Cohesion: 0.15
Nodes (14): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+6 more)

### Community 2 - ".seed"
Cohesion: 0.16
Nodes (7): ScimSeedService, SeededIdentity, ScimSeedConfig, ScimSeedServiceTests, org.springframework.boot.ApplicationRunner, seededidentity, value

### Community 3 - "accounts.tsx"
Cohesion: 0.16
Nodes (24): One-Way Import Direction Through the Layers, components/ui Is a Package Placeholder, SessionResult, Button(), ButtonProps, buttonVariants, Card(), CardContent() (+16 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.17
Nodes (13): Every Playwright Spec Needs a testMatch, Shared Playwright storageState for Auth, ADMIN_CREDENTIALS, captureSessionCookie(), expireSession(), login(), loginAs(), postAdminAction() (+5 more)

### Community 5 - "http.ts"
Cohesion: 0.10
Nodes (25): getCurrentUser operation, login operation, logout operation, decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock (+17 more)

### Community 6 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

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
Nodes (36): engines, node, npm, name, overrides, qs, packageManager, private (+28 more)

### Community 14 - "scripts"
Cohesion: 0.10
Nodes (20): scripts, analyze, build, dev, format, format:check, lint, preview (+12 more)

### Community 15 - "tools.jackson.databind.JsonNode"
Cohesion: 0.08
Nodes (13): RemoveAllMembers, ReplaceMembers, ScimGroupPatchOperation, SetDisplayName, Override, ScimUserReplacement, ScimGroupRequestReader, ScimUserRequestReader (+5 more)

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "ScimUserPatchReaderTests.java"
Cohesion: 0.10
Nodes (29): addemails, ScimName, AddEmails, MergeName, NamePart, FAMILY_NAME, FORMATTED, GIVEN_NAME (+21 more)

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
Cohesion: 0.14
Nodes (16): No Parent-Relative Paths From An App, SPA Build Contract, with-frontend Maven Profile, Backend Serves SPA And Forwards Routes, Claim: No Router Data Layer Or Auth, No .env Required In Frontend, Frontend Technology Stack, EC2Instance (+8 more)

### Community 24 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.10
Nodes (7): LockoutPolicyTests, ReservedServerPaths, SpaRoutesTests, SpaShell, org.junit.jupiter.api.Nested, org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource

### Community 26 - "DBInstance RDS PostgreSQL"
Cohesion: 0.29
Nodes (8): docs/openapi.yaml API Contract, Postgres Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, DBInstance RDS PostgreSQL, DBSubnetGroup

### Community 27 - "App.tsx"
Cohesion: 0.11
Nodes (21): Frontend SPA Entry HTML, AuthRole, AuthProvider(), AuthStatus, useAuth(), GuestRoute(), ProtectedRoute(), SessionRoute() (+13 more)

### Community 28 - "infra/ Is Deployment Material Not An App"
Cohesion: 0.29
Nodes (7): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, CONTEXT.md Domain Glossary, gh CLI Conventions, GitHub Issues As Issue Tracker, PRs As Request Surface Flag

### Community 29 - ".given"
Cohesion: 0.14
Nodes (3): Override, LoginIdentityServiceTests, org.springframework.security.core.userdetails.UserDetails

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "org.springframework.context.annotation.Bean"
Cohesion: 0.09
Nodes (19): atomicinteger, authenticationexception, LoginLockoutConfig, SecurityConfig, LockoutPolicy, SessionRegistryConfiguration, SessionRegistryConfiguration, SecurityConfigPasswordEncoderTests (+11 more)

### Community 32 - "Domain Documentation Guide"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 33 - "GitHub Issue Tracker Guide"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 36 - "Graphify Runner Agent"
Cohesion: 0.40
Nodes (5): Graphify Runner Agent, Recorded Interpreter Guard, Graph Shrink Refusal, Graphify Refresh Before Commit, CLAUDE.md Graphify Override

### Community 37 - "ScimGroupServiceTests"
Cohesion: 0.18
Nodes (5): NewScimGroup, AddMembers, RemoveMembers, ScimGroupReplacement, ScimGroupServiceTests

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

### Community 50 - "ScimConnectorEntity"
Cohesion: 0.22
Nodes (4): ScimConnectorEntity, ScimConnectorJpaRepository, Override, ScimConnectorPersistenceAdapter

### Community 56 - "AuditRetentionPolicy"
Cohesion: 0.14
Nodes (12): AuditRetentionService, AuditRetentionPolicyConfig, AuditRetentionScheduleConfig, Override, AuditRetentionPolicy, AuditRetentionPolicyTests, crontask, crontrigger (+4 more)

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - ".servesSpaShell"
Cohesion: 0.17
Nodes (7): SpaRoutes, SpaRoutesScimNamespaceTests, Accounts and identity provisioning, CONTEXT, Current account model, Request paths, Sessions

### Community 63 - "ScimUserServiceTests"
Cohesion: 0.14
Nodes (4): Revocation, ScimUserServiceTests, InMemoryScimPasswordHistoryRepository, Override

### Community 64 - "ConnectorTokenSecretTests"
Cohesion: 0.11
Nodes (8): ConnectorTokenDigest, Override, Minted, Presented, ConnectorTokenSecretTests, java.security.MessageDigest, nosuchalgorithmexception, standardcharsets

### Community 65 - "auth-context-value.ts"
Cohesion: 0.11
Nodes (12): App(), AuthUser, AuthContext, AuthContextState, AuthContextValue, AdminAccount, apiFetchMock, auth (+4 more)

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 68 - "AuditRetentionStartupTests.java"
Cohesion: 0.15
Nodes (5): applicationconversionservice, AuditRetentionStartupTests, PasswordNormalizationTests, org.springframework.boot.test.context.runner.ApplicationContextRunner, propertysourcesplaceholderconfigurer

### Community 70 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.09
Nodes (22): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Goals (+14 more)

### Community 71 - "org.junit.jupiter.api.AfterEach"
Cohesion: 0.09
Nodes (13): AfterCommitAdapter, Override, AfterCommitAdapterTests, 2. Revoke a disabled account's sessions after the commit, Alternatives considered, Consequences, Context, Decision (+5 more)

### Community 72 - "AuditRetentionServiceTests"
Cohesion: 0.24
Nodes (4): AuditRetentionServiceTests, CountingRetention, Override, Override

### Community 73 - "ConnectorAdministrationService"
Cohesion: 0.08
Nodes (8): ConnectorAdministrationService, ConnectorAuthenticationService, ConnectorSummary, ConnectorTokenSummary, ScimConnectorRepository, ScimConnectorTokenRepository, ConnectorAdministrationServiceTests, java.security.SecureRandom

### Community 74 - "AuditAppendOnlyIntegrationTests"
Cohesion: 0.09
Nodes (3): AuditAppendOnlyIntegrationTests, AuditEventRecordingIntegrationTests, ResultActions

### Community 75 - "ScimPasswordHistoryEntity"
Cohesion: 0.31
Nodes (4): ScimPasswordHistoryEntity, ScimPasswordHistoryJpaRepository, Override, ScimPasswordHistoryPersistenceAdapter

### Community 76 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.12
Nodes (19): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, deleteSession operation, getHealth operation, getSession operation, JSESSIONID Session Cookie Security Scheme, updateSession operation, SessionController (+11 more)

### Community 77 - "RFC requirements and implications"
Cohesion: 0.22
Nodes (9): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+1 more)

### Community 78 - "ScimUserAttributesTests"
Cohesion: 0.05
Nodes (8): ScimDiscovery, ScimGroupAttributes, Attribute, ScimUserAttributes, ScimDiscoveryTests, SuppressWarnings, ScimUserAttributesTests, Search, filtering, sorting and projection

### Community 79 - "LogContextTests"
Cohesion: 0.16
Nodes (5): Override, LogContext, Scope, LogContextTests, mdc

### Community 81 - "CapturedLog"
Cohesion: 0.25
Nodes (6): CapturedLog, ch.qos.logback.classic.Logger, ch.qos.logback.classic.spi.ILoggingEvent, ch.qos.logback.core.read.ListAppender, function, keyvaluepair

### Community 83 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.25
Nodes (9): ALBListener, ALBTargetGroup, TargetGroupAttachment, ALB To EC2 To RDS And Redis Topology, AWS CloudFormation Deployment Guide, Stack Parameters Reference, Existing VPC Prerequisite, Infra Troubleshooting Runbook (+1 more)

### Community 84 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 85 - "ScimDeletionIntegrationTests"
Cohesion: 0.09
Nodes (10): AccountSessionsAdapter, Override, AccountSessionsAdapterTests, IndexedSessions, Override, RedisSessionRevocationIntegrationTests, ScimDeletionIntegrationTests, org.springframework.session.FindByIndexNameSessionRepository (+2 more)

### Community 87 - "list"
Cohesion: 0.06
Nodes (31): arraylist, DuplicateDisplayNameException, PasswordHistoryPolicy, PasswordReusedException, ProtectedResourceException, ScimExternalIdRepository, ScimPasswordHistoryRepository, ScimResourceType (+23 more)

### Community 88 - "jakarta.persistence.Entity"
Cohesion: 0.12
Nodes (20): ScimExternalIdEntity, cascadetype, collectiontable, column, elementcollection, embedded, embeddedid, enumerated (+12 more)

### Community 89 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.08
Nodes (21): AbsoluteSessionLifetimeFilter, Override, AbsoluteSessionLifetimePolicy, Override, Override, ScimBearerAuthenticationFilter, ScimBearerChallenge, ScimReleaseGate (+13 more)

### Community 91 - "Baseline Full Extensive Test Levels"
Cohesion: 0.25
Nodes (8): ArchUnit Baseline Gate, Always ./mvnw Never Bare mvn, Trace Before You Delete, Frontend Local Semgrep Ruleset, Baseline Full Extensive Test Levels, The mutate Flag Replaces the Array, It Does Not Narrow It, Image Tag Plus Digest Pinning, Toolchain Pin Table

### Community 92 - "UserCounterService"
Cohesion: 0.10
Nodes (10): getCount operation, incrementCount operation, resetCount operation, UserCounterService, CountResponse, UserCounterController, UserCounterRepository, Override (+2 more)

### Community 93 - "ScimUserEdit"
Cohesion: 0.13
Nodes (5): Override, ScimPasswordChange, ScimUserEdit, Override, SetPassword

### Community 94 - "ScimGroupController.java"
Cohesion: 0.07
Nodes (34): authenticationprincipal, UnknownIdentityException, UnsafeIdentityChangeException, AdminAccountController, IssuedConnectorToken, UnknownConnectorException, AdminConnectorController, CreateConnectorRequest (+26 more)

### Community 95 - "ScimSecurityChainOrderTests.java"
Cohesion: 0.16
Nodes (11): anonymousauthenticationfilter, ScimSecurityChainOrderTests, basicauthenticationfilter, csrffilter, disableencodeurlfilter, exceptiontranslationfilter, logoutfilter, org.assertj.core.api.SoftAssertions (+3 more)

### Community 96 - "SCIM 2.0 account-management research"
Cohesion: 0.22
Nodes (7): Executive finding, Existing application seams, Primary sources, Recommended implementation order, Resolved RFC decisions, SCIM 2.0 account-management research, Testing strategy

### Community 97 - "uuid"
Cohesion: 0.07
Nodes (22): arrays, assertthat, assertthatthrownby, AuditGroupAttribute, DISPLAY_NAME, MEMBERS, ConnectorTokenPolicy, ScimGroupMember (+14 more)

### Community 98 - "ScimUser"
Cohesion: 0.07
Nodes (17): DuplicateUserNameException, NormalizedUserName, ReservedResourceName, ADMIN_GROUP, BOOTSTRAP_ADMIN, ScimLoginState, ScimPageRequest, ScimUser (+9 more)

### Community 99 - "ScimConditionalWriteIntegrationTests.java"
Cohesion: 0.07
Nodes (82): autowired, RequestIdFilter, ConnectorTokenScope, READ_ONLY, READ_WRITE, BackendApplicationTests, ContainerTestConfiguration, UserCounterServiceTests (+74 more)

### Community 100 - "AuthenticatedConnector"
Cohesion: 0.09
Nodes (11): NewScimUser, ScimUserListing, ScimUserResource, ScimUserService, AuthenticatedConnector, Cause, DEACTIVATED, DELETED (+3 more)

### Community 101 - "Delivery plan"
Cohesion: 0.20
Nodes (10): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 0a — Permanent lockout, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 4 — Groups and Admin authority, Slice 5 — Complete query protocol (+2 more)

### Community 103 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.19
Nodes (3): AuditTrailService, Override, org.springframework.transaction.annotation.Transactional

### Community 105 - ".scimType"
Cohesion: 0.18
Nodes (9): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status, Error contract, Log formatting, Operational telemetry (+1 more)

### Community 106 - "ScimUserPatchOperationTests"
Cohesion: 0.16
Nodes (6): ScimEmailFilter, EmailUpdate, RemoveEmailPart, RemoveEmails, UpdateEmails, ScimUserPatchOperationTests

### Community 107 - "AuditEvent"
Cohesion: 0.15
Nodes (7): AuditEvent, AuditEventRepository, AuditOutcome, FAILURE, SUCCESS, AuditRequestContext, RecordingRepository

### Community 108 - "SecurityConfig.java"
Cohesion: 0.09
Nodes (20): argon2passwordencoder, authenticationentrypoint, authorizationfilter, ScimSecurityConfig, PasswordNormalization, changesessionidauthenticationstrategy, cookiecsrftokenrepository, daoauthenticationprovider (+12 more)

### Community 109 - "Credential and cryptographic policy"
Cohesion: 0.25
Nodes (8): Credential and cryptographic policy, Hashing and primitives, Key rotation and storage, Lockout policy, Password policy, Response headers, Session lifetime, Uniform authentication timing

### Community 111 - "Spring Session In Redis"
Cohesion: 0.33
Nodes (7): Backend Architecture Boundaries, Redis Compose Service, Spring Session In Redis, Flag ADR Conflicts Explicitly, docs/adr Decision Records, RedisCluster ElastiCache, RedisSubnetGroup

### Community 112 - "ScimConnectorTokenEntity"
Cohesion: 0.16
Nodes (4): ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 113 - "ScimUserEntity"
Cohesion: 0.08
Nodes (7): ScimGroupEntity, ScimLoginStateValue, ScimResourceEntity, ScimUserEmailValue, ScimUserEntity, jakarta.persistence.Embeddable, serializable

### Community 114 - "SpaFrontendTests"
Cohesion: 0.19
Nodes (7): Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.stereotype.Component, org.springframework.web.servlet.ModelAndView, requestdispatcher

### Community 115 - "AuditOperation"
Cohesion: 0.08
Nodes (25): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+17 more)

### Community 117 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (9): RefusalTimingEquivalenceTests, AbsoluteSessionLifetimeFilterTests, SecurityConfigTests, Connectors, Tokens, CompositeKeyContractTests, ExternalIdAlias, GroupMembership (+1 more)

### Community 118 - "AuthControllerTests.java"
Cohesion: 0.05
Nodes (40): assertthatcode, assertthatnoexception, authentication, authenticationmanager, AuditAdministrativeRefusal, LAST_ENABLED_ADMINISTRATOR, PROTECTED_RESOURCE, SELF_DISABLE (+32 more)

### Community 119 - "EcsLogCapture"
Cohesion: 0.20
Nodes (7): EcsLogCapture, Override, bytearrayoutputstream, ch.qos.logback.classic.LoggerContext, ch.qos.logback.core.OutputStreamAppender, org.springframework.core.env.Environment, structuredlogencoder

### Community 120 - "RecordingAuditTrail"
Cohesion: 0.14
Nodes (8): AuditScimRefusal, INVALID_VALUE, MUTABILITY, NO_TARGET, UNIQUENESS, Override, Recorded, RecordingAuditTrail

### Community 121 - "4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal"
Cohesion: 0.25
Nodes (6): 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal, Alternatives considered, Consequences, Context, Decision, Status

### Community 122 - "AuditUserAttribute"
Cohesion: 0.17
Nodes (10): AuditUserAttribute, ACTIVE, DISPLAY_NAME, EMAILS, LOCALE, NAME, PASSWORD, PREFERRED_LANGUAGE (+2 more)

### Community 123 - "InMemoryScimExternalIdRepository"
Cohesion: 0.43
Nodes (3): Alias, InMemoryScimExternalIdRepository, Override

### Community 124 - "ScimOffsetPage"
Cohesion: 0.18
Nodes (5): Override, ScimOffsetPage, ScimOffsetPageTests, org.springframework.data.domain.Pageable, org.springframework.data.domain.Sort

### Community 125 - "ScimConnector"
Cohesion: 0.19
Nodes (4): ScimConnector, ConnectorAuthenticationServiceTests, InMemoryScimConnectorRepository, Override

### Community 126 - "ScimGroupPersistenceAdapter"
Cohesion: 0.16
Nodes (3): ScimGroupJpaRepository, Override, ScimGroupPersistenceAdapter

### Community 127 - "ScimDiscoveryIntegrationTests"
Cohesion: 0.19
Nodes (3): ScimConditionalWrites, ScimDiscoveryIntegrationTests, org.springframework.test.web.servlet.request.RequestPostProcessor

### Community 128 - "LockoutHasNoDurationTests.java"
Cohesion: 0.21
Nodes (5): LockoutHasNoDurationTests, files, java.lang.reflect.RecordComponent, org.junit.jupiter.params.provider.MethodSource, path

### Community 129 - "org.springframework.http.ResponseEntity"
Cohesion: 0.14
Nodes (6): ScimDiscoveryController, ScimUserController, ScimUserRenderer, ProbeController, org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.GetMapping

### Community 130 - "components.json"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

### Community 131 - "ScimPatchRefusedException"
Cohesion: 0.38
Nodes (4): Reason, MUTABILITY, NO_TARGET, ScimPatchRefusedException

### Community 132 - "1. Count login attempts on the login path"
Cohesion: 0.29
Nodes (6): 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision, Status

### Community 133 - "Domain and authority model"
Cohesion: 0.29
Nodes (7): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, SCIM User replaces Account

### Community 135 - ".handle"
Cohesion: 0.14
Nodes (7): ScimErrorException, ScimExceptionHandler, InvalidPreconditionException, PreconditionFailedException, PreconditionRequiredException, org.springframework.http.HttpStatus, org.springframework.web.bind.annotation.RestControllerAdvice

### Community 136 - ".of"
Cohesion: 0.15
Nodes (3): NormalizedDisplayName, NormalizedDisplayNameTests, NormalizedUserNameTests

### Community 137 - "AuthControllerTests"
Cohesion: 0.15
Nodes (9): LoginOutcome, AuthController, LoginRequest, UserResponse, AuthControllerTests, org.springframework.security.core.Authentication, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy, org.springframework.security.web.context.SecurityContextRepository (+1 more)

### Community 140 - "UserCounter"
Cohesion: 0.16
Nodes (5): UserCounter, UserCounterEntity, Override, UserCounterPersistenceAdapter, UserCounterTests

### Community 142 - "AuditEventRetention"
Cohesion: 0.29
Nodes (3): AuditEventRetention, AuditEventRetentionAdapter, Override

### Community 143 - "ScimGroup"
Cohesion: 0.17
Nodes (3): ScimGroup, InMemoryScimGroupRepository, Override

### Community 145 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 146 - "Frontend Architecture Doc"
Cohesion: 0.16
Nodes (15): Colors Come From index.css Tokens, Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, Vite Full-Reloads on Any Watched HTML Write, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline, Vitest Deliberately Omits the Tailwind Vite Plugin (+7 more)

### Community 147 - ".overlapEnd"
Cohesion: 0.18
Nodes (3): ConnectorTokenPolicyTests, Lifetime, RotationOverlap

### Community 149 - "IdentitySummary"
Cohesion: 0.28
Nodes (4): IdentitySummary, AdminAccountControllerTests, Override, RecordingService

### Community 154 - "TextAttribute"
Cohesion: 0.33
Nodes (6): TextAttribute, DISPLAY_NAME, LOCALE, PREFERRED_LANGUAGE, TIMEZONE, USER_NAME

### Community 155 - "ScimUserPatchReaderTests"
Cohesion: 0.15
Nodes (4): RemoveActive, SetActive, SetText, ScimUserPatchReaderTests

### Community 157 - "Kiro: graphify enforcement"
Cohesion: 0.40
Nodes (4): graphify-runner, Kiro: graphify enforcement, The hooks, When the refresh fails

### Community 158 - "org.springframework.data.jpa.repository.Query"
Cohesion: 0.07
Nodes (19): UserCounterJpaRepository, ScimGroupReference, ScimGroupMemberEntity, Override, ScimGroupMemberId, ScimExternalIdJpaRepository, Override, ScimExternalIdPersistenceAdapter (+11 more)

### Community 159 - "ScimGroupService"
Cohesion: 0.14
Nodes (4): ScimGroupListing, ScimGroupResource, ScimGroupService, Tombstone

### Community 161 - "Query contract"
Cohesion: 0.33
Nodes (6): Attribute projection, Filtering, Pagination, POST search, Query contract, Sorting

### Community 162 - "ScimAttributeProjection"
Cohesion: 0.24
Nodes (5): SuppressWarnings, Kind, GROUP, USER, ScimAttributeProjection

### Community 163 - ".ofIfMatch"
Cohesion: 0.23
Nodes (3): FakeSaltedEncoder, Override, ScimVersionPreconditionTests

### Community 164 - "Definition of Done"
Cohesion: 0.40
Nodes (5): Backend gates, Contract and behavior, Definition of Done, Documentation and graph, Frontend gates

### Community 165 - "Write semantics"
Cohesion: 0.40
Nodes (5): DELETE and tombstones, PATCH, POST, PUT, Write semantics

### Community 170 - "HttpAuditRequestContextTests.java"
Cohesion: 0.20
Nodes (8): AuditRequest, HttpAuditRequestContext, Override, HttpAuditRequestContextTests, handlermapping, org.springframework.mock.web.MockHttpServletRequest, requestcontextholder, servletrequestattributes

### Community 171 - "ScimUserPatchOperation"
Cohesion: 0.16
Nodes (12): Op, ADD, REMOVE, REPLACE, Path, ScimUserPatchReader, RemovePassword, RemoveText (+4 more)

### Community 174 - "Condition"
Cohesion: 0.25
Nodes (5): Condition, ScimEmailPart, PRIMARY, TYPE, VALUE

### Community 179 - "ScimConnectorToken"
Cohesion: 0.18
Nodes (9): ConnectorTokenSecret, ScimConnectorToken, InMemoryScimConnectorTokenRepository, Override, dispatchertype, org.springframework.mock.web.MockFilterChain, org.springframework.mock.web.MockHttpServletResponse, securerandom (+1 more)

### Community 189 - "Kind"
Cohesion: 0.50
Nodes (4): Kind, CLEAR, SET, UNCHANGED

### Community 192 - "AuditEventEntity"
Cohesion: 0.39
Nodes (4): AuditEventJpaRepository, AuditEventPersistenceAdapter, Override, AuditEventEntity

### Community 193 - "RecordingTransactionManager"
Cohesion: 0.52
Nodes (4): Override, RecordingTransactionManager, org.springframework.transaction.TransactionDefinition, org.springframework.transaction.TransactionStatus

## Ambiguous Edges - Review These
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **400 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+395 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 709 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **58 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Backend API Contract (OpenAPI 3.1)` connect `Backend API Contract (OpenAPI 3.1)` to `UserCounterService`, `http.ts`?**
  _High betweenness centrality (0.154) - this node is a cross-community bridge._
- **Why does `login operation` connect `http.ts` to `Backend API Contract (OpenAPI 3.1)`, `auth.helpers.ts`?**
  _High betweenness centrality (0.093) - this node is a cross-community bridge._
- **Why does `Shared Playwright storageState for Auth` connect `auth.helpers.ts` to `Frontend Architecture Doc`, `http.ts`?**
  _High betweenness centrality (0.067) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _400 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `http.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.10476190476190476 - nodes in this community are weakly interconnected._