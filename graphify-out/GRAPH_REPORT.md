# Graph Report - monorepo-base-issue-23  (2026-09-30)

## Corpus Check
- 408 files · ~261,993 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 16 file(s) not represented in the graph (top: (none) 10, .example 1, .properties 1)

## Summary
- 4490 nodes · 14973 edges · 200 communities (136 shown, 64 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 1699 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `35aa574c`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- .sessionsOf
- Attribute
- accounts.tsx
- auth.helpers.ts
- http.ts
- Workflow
- ScimConnectorLifecycleIntegrationTests
- ScimFilterParserTests
- devDependencies
- stryker.config.json
- compilerOptions
- Frontend Local Semgrep Ruleset
- package.json
- scripts
- .readCreate
- compilerOptions
- ScimUserServiceTests.java
- lib.sh
- AGENTS.md
- deploy.sh
- ScimGroupProvisioningIntegrationTests
- mvnw
- EC2Instance
- org.junit.jupiter.params.ParameterizedTest
- ScimGroupPersistenceAdapter
- DBInstance RDS PostgreSQL
- App.tsx
- infra/ Is Deployment Material Not An App
- .given
- tsconfig.test.json
- .require
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
- AuditRetentionServiceTests.java
- ScimQuerySql
- ScimAttributeProjectionTests
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh
- ScimQuerySql.java
- ScimUserServiceTests
- ConnectorTokenSecretTests
- auth-context-value.ts
- ArchitectureTest.java
- accountseed
- RedisSessionRevocationIntegrationTests.java
- LoginAttemptServiceTests
- SCIM 2.0 account-management specification plan
- ScimSecurityChainOrderTests.java
- org.springframework.web.bind.annotation.GetMapping
- ConnectorAdministrationService
- AuditAppendOnlyIntegrationTests
- ScimRequestObservationConventionTests
- Backend API Contract (OpenAPI 3.1)
- AuditRetentionPolicy
- ScimUserAttributesTests
- LogContextTests
- ScimBearerAuthenticationFilterTests
- Attribute
- ScimConditionalWriteIntegrationTests
- AWS CloudFormation Deployment Guide
- BackendApplication.java
- ScimDeletionIntegrationTests
- verify.sh
- uuid
- jakarta.persistence.Entity
- jakarta.servlet.http.HttpServletRequest
- AuditTrail
- Baseline Full Extensive Test Levels
- org.springframework.web.bind.annotation.PostMapping
- ScimUserPatchOperation
- AuthController.java
- tools.jackson.databind.JsonNode
- SCIM 2.0 account-management research
- assertthat
- ScimUser
- ScimConditionalWriteIntegrationTests.java
- ScimUserService
- RFC requirements and implications
- bcryptpasswordencoder
- org.springframework.transaction.annotation.Transactional
- RequestIdFilterTests
- ScimAttributeProjection
- ScimUserPatchOperationTests
- AuditTrailServiceTests.java
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
- org.springframework.stereotype.Service
- org.junit.jupiter.params.provider.CsvSource
- RecordingAuditTrail
- AuditEventRecordingIntegrationTests
- AuditUserAttribute
- Tokens
- IndexedSessions
- ScimConnector
- ScimResourceEntity
- .get
- ScimQueryProtocolIntegrationTests
- AuthenticatedConnector
- components.json
- ScimPatchRefusedException
- .of
- Domain and authority model
- .seed
- .handle
- .of
- AuthControllerTests
- ScimExternalIdEntity
- EcsLogFormatTests
- UserCounter
- ScimEmail
- AuditEventRetentionAdapter.java
- ScimGroup
- ScimGroupController
- dependencies
- Frontend Architecture Doc
- .of
- ScimSchemas
- IdentitySummary
- filterchainproxy
- mockfilterchain
- mockhttpservletresponse
- recordcomponent
- ScimQueryVocabulary.java
- ScimUserPatchReaderTests
- org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
- Kiro: graphify enforcement
- org.springframework.data.jpa.repository.Query
- ScimResourceType
- .invalidValue
- ScimFilterParser
- UserCounterService
- .ofIfMatch
- ScheduledJobMetrics
- org.junit.jupiter.params.provider.Arguments
- ScimLoginStateTests
- transactiondefinition
- transactionstatus
- ScimUserEmailValue
- org.junit.jupiter.api.AfterEach
- ScimUserPatchReader
- drivermanager
- inmemoryuserdetailsmanager
- IdentityAdministrationService
- ScimGroupService
- FakeSaltedEncoder
- OperationalTelemetryIntegrationTests.java
- SessionController
- ScimConnectorToken
- EcsLogCapture
- ScimName
- AuditRetentionStartupTests.java
- .fromSearchRequest
- ManagementSessionConfiguration.java
- .requiresWriteScope
- LockoutHasNoDurationTests.java
- Delivery plan
- .summarize
- RefusalTimingEquivalenceTests
- 1. Count login attempts on the login path
- CountingPasswordEncoder
- AuditOutcome
- SetPassword
- .of
- CountingPasswordEncoder
- Query contract
- Definition of Done
- Write semantics
- ScimGroupReference

## God Nodes (most connected - your core abstractions)
1. `ScimUser` - 110 edges
2. `ScimFilterPath` - 77 edges
3. `ScimGroupProvisioningIntegrationTests` - 77 edges
4. `ScimGroup` - 74 edges
5. `ScimResourceType` - 72 edges
6. `AuditTrail` - 66 edges
7. `AuthenticatedConnector` - 65 edges
8. `IdentityAdministrationServiceTests` - 65 edges
9. `ScimConditionalWriteIntegrationTests` - 64 edges
10. `ScimQueryProtocolIntegrationTests` - 63 edges

## Surprising Connections (you probably didn't know these)
- `Consequences` --references--> `AuditTrailServiceTests`  [INFERRED]
  docs/adr/0004-audit-append-failure-semantics.md → backend/src/test/java/com/example/backend/audit/application/AuditTrailServiceTests.java
- `Consequences` --references--> `LoginLockoutTests`  [INFERRED]
  docs/adr/0001-count-login-attempts-on-the-login-path.md → backend/src/test/java/com/example/backend/auth/application/LoginLockoutTests.java
- `getCurrentUser operation` --shares_data_with--> `getCurrentUser()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
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

## Communities (200 total, 64 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 2 - "Attribute"
Cohesion: 0.03
Nodes (53): And, Attribute, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE (+45 more)

### Community 3 - "accounts.tsx"
Cohesion: 0.13
Nodes (30): components/ui Is a Package Placeholder, useAuth(), useAuthState(), SessionResult, useSessionRequest(), Button(), ButtonProps, buttonVariants (+22 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.17
Nodes (13): Every Playwright Spec Needs a testMatch, Shared Playwright storageState for Auth, ADMIN_CREDENTIALS, captureSessionCookie(), expireSession(), login(), loginAs(), postAdminAction() (+5 more)

### Community 5 - "http.ts"
Cohesion: 0.16
Nodes (16): decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, SessionRequest, ApiDecoder (+8 more)

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

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "ScimUserServiceTests.java"
Cohesion: 0.13
Nodes (24): addemails, ScimEmailPart, PRIMARY, TYPE, VALUE, RemoveName, RemovePassword, RemoveText (+16 more)

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
Cohesion: 0.06
Nodes (11): assertthatcode, attributeref, SpaRoutes, LockoutPolicyTests, SpaRoutesScimNamespaceTests, ReservedServerPaths, SpaRoutesTests, SpaShell (+3 more)

### Community 25 - "ScimGroupPersistenceAdapter"
Cohesion: 0.15
Nodes (4): ScimGroupMemberJpaRepository, ScimGroupMemberRow, Override, ScimGroupPersistenceAdapter

### Community 26 - "DBInstance RDS PostgreSQL"
Cohesion: 0.29
Nodes (8): docs/openapi.yaml API Contract, Postgres Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, DBInstance RDS PostgreSQL, DBSubnetGroup

### Community 27 - "App.tsx"
Cohesion: 0.15
Nodes (15): CONTEXT, Request paths, Sessions, AuthRole, AuthStatus, GuestRoute(), ProtectedRoute(), SessionRoute() (+7 more)

### Community 28 - "infra/ Is Deployment Material Not An App"
Cohesion: 0.29
Nodes (7): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, CONTEXT.md Domain Glossary, gh CLI Conventions, GitHub Issues As Issue Tracker, PRs As Request Surface Flag

### Community 29 - ".given"
Cohesion: 0.15
Nodes (3): Override, LoginIdentityServiceTests, org.springframework.security.core.userdetails.UserDetails

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

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
Cohesion: 0.14
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

### Community 56 - "AuditRetentionServiceTests.java"
Cohesion: 0.14
Nodes (11): AuditRetentionServiceTests, CountingRetention, Override, CapturedLog, Override, ch.qos.logback.classic.Level, ch.qos.logback.classic.Logger, ch.qos.logback.classic.spi.ILoggingEvent (+3 more)

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "ScimQuerySql.java"
Cohesion: 0.11
Nodes (32): and, And, AttributeRef, Comparison, Not, Operator, CO, EQ (+24 more)

### Community 63 - "ScimUserServiceTests"
Cohesion: 0.13
Nodes (5): Revocation, ScimUserServiceTests, Override, InMemoryScimPasswordHistoryRepository, Override

### Community 64 - "ConnectorTokenSecretTests"
Cohesion: 0.10
Nodes (9): ConnectorTokenDigest, Override, ConnectorTokenSecret, Minted, Presented, ConnectorTokenSecretTests, java.security.MessageDigest, nosuchalgorithmexception (+1 more)

### Community 65 - "auth-context-value.ts"
Cohesion: 0.09
Nodes (17): App(), AuthUser, AuthProvider(), AuthContext, AuthContextState, AuthContextValue, apiFetchMock, request() (+9 more)

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 68 - "RedisSessionRevocationIntegrationTests.java"
Cohesion: 0.19
Nodes (8): AccountSessionsAdapter, RedisSessionRevocationIntegrationTests, org.springframework.session.FindByIndexNameSessionRepository, org.springframework.session.Session, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, org.testcontainers.containers.GenericContainer, transactiontemplate

### Community 70 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.10
Nodes (21): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Error contract (+13 more)

### Community 71 - "ScimSecurityChainOrderTests.java"
Cohesion: 0.11
Nodes (18): anonymousauthenticationfilter, BackendApplicationTests, ScimSecurityChainOrderTests, basicauthenticationfilter, classmode, csrffilter, disableencodeurlfilter, exceptiontranslationfilter (+10 more)

### Community 72 - "org.springframework.web.bind.annotation.GetMapping"
Cohesion: 0.14
Nodes (5): ScimDiscovery, ScimDiscoveryController, ProbeController, ScimDiscoveryTests, org.springframework.web.bind.annotation.GetMapping

### Community 73 - "ConnectorAdministrationService"
Cohesion: 0.10
Nodes (6): ConnectorAdministrationService, ConnectorAuthenticationService, IssuedConnectorToken, ScimConnectorRepository, ScimConnectorTokenRepository, java.security.SecureRandom

### Community 75 - "ScimRequestObservationConventionTests"
Cohesion: 0.17
Nodes (7): Override, ScimRequestObservationConvention, ScimRequestObservationConventionTests, io.micrometer.common.KeyValues, keyvalue, org.springframework.http.server.observation.DefaultServerRequestObservationConvention, org.springframework.http.server.observation.ServerRequestObservationContext

### Community 76 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.12
Nodes (17): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, getCurrentUser operation, getHealth operation, getSession operation, incrementCount operation, login operation, logout operation (+9 more)

### Community 77 - "AuditRetentionPolicy"
Cohesion: 0.14
Nodes (11): AuditRetentionService, AuditRetentionScheduleConfig, Override, AuditEventRetention, AuditRetentionPolicy, AuditRetentionPolicyTests, crontask, crontrigger (+3 more)

### Community 78 - "ScimUserAttributesTests"
Cohesion: 0.07
Nodes (5): ScimGroupAttributes, Attribute, ScimUserAttributes, SuppressWarnings, ScimUserAttributesTests

### Community 79 - "LogContextTests"
Cohesion: 0.19
Nodes (4): Override, LogContext, Scope, LogContextTests

### Community 80 - "ScimBearerAuthenticationFilterTests"
Cohesion: 0.19
Nodes (3): LoginOutcome, ScimBearerAuthenticationFilterTests, org.springframework.security.core.Authentication

### Community 81 - "Attribute"
Cohesion: 0.13
Nodes (9): Attribute, Kind, BOOLEAN, COMPLEX, DATE_TIME, REFERENCE, STRING, ScimQueryVocabulary (+1 more)

### Community 83 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.25
Nodes (9): ALBListener, ALBTargetGroup, TargetGroupAttachment, ALB To EC2 To RDS And Redis Topology, AWS CloudFormation Deployment Guide, Stack Parameters Reference, Existing VPC Prerequisite, Infra Troubleshooting Runbook (+1 more)

### Community 84 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 87 - "uuid"
Cohesion: 0.05
Nodes (31): arraylist, authenticationprincipal, ScimGroupListing, ScimListedResource, ScimSearchListing, PasswordHistoryPolicy, ScimExternalIdRepository, ScimPasswordHistoryRepository (+23 more)

### Community 88 - "jakarta.persistence.Entity"
Cohesion: 0.10
Nodes (22): ScimGroupMemberEntity, ScimPasswordHistoryEntity, ScimPasswordHistoryJpaRepository, Override, ScimPasswordHistoryPersistenceAdapter, cascadetype, collectiontable, column (+14 more)

### Community 89 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.09
Nodes (21): AbsoluteSessionLifetimeFilter, Override, AbsoluteSessionLifetimePolicy, Override, Override, ScimBearerAuthenticationFilter, ScimBearerChallenge, ScimReleaseGate (+13 more)

### Community 90 - "AuditTrail"
Cohesion: 0.09
Nodes (4): AuditFilterShape, AuditTrail, AuditTrailServiceTests, RecordingRepository

### Community 91 - "Baseline Full Extensive Test Levels"
Cohesion: 0.25
Nodes (8): ArchUnit Baseline Gate, Always ./mvnw Never Bare mvn, Trace Before You Delete, Frontend Local Semgrep Ruleset, Baseline Full Extensive Test Levels, The mutate Flag Replaces the Array, It Does Not Narrow It, Image Tag Plus Digest Pinning, Toolchain Pin Table

### Community 92 - "org.springframework.web.bind.annotation.PostMapping"
Cohesion: 0.16
Nodes (9): CountResponse, UserCounterController, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest, RotateTokenRequest, UserCounterControllerTests, java.security.Principal (+1 more)

### Community 93 - "ScimUserPatchOperation"
Cohesion: 0.15
Nodes (5): Override, ScimPasswordChange, ScimUserEdit, Override, ScimUserPatchOperation

### Community 94 - "AuthController.java"
Cohesion: 0.09
Nodes (25): UnknownIdentityException, UnsafeIdentityChangeException, AdminAccountController, AuthController, UnknownConnectorException, InvalidConnectorTokenLifetimeException, cachecontrol, cookievalue (+17 more)

### Community 95 - "tools.jackson.databind.JsonNode"
Cohesion: 0.27
Nodes (6): RemoveAllMembers, ReplaceMembers, ScimGroupPatchOperation, SetDisplayName, ScimGroupRequestReader, tools.jackson.databind.JsonNode

### Community 96 - "SCIM 2.0 account-management research"
Cohesion: 0.25
Nodes (6): Executive finding, Primary sources, Recommended implementation order, Resolved RFC decisions, SCIM 2.0 account-management research, Testing strategy

### Community 97 - "assertthat"
Cohesion: 0.06
Nodes (38): arrays, assertthat, assertthatnoexception, assertthatthrownby, atomicinteger, atomicreference, authentication, authenticationmanager (+30 more)

### Community 98 - "ScimUser"
Cohesion: 0.08
Nodes (11): DuplicateUserNameException, ReservedResourceName, ADMIN_GROUP, BOOTSTRAP_ADMIN, ScimLoginState, ScimUser, ScimUserRepository, Override (+3 more)

### Community 99 - "ScimConditionalWriteIntegrationTests.java"
Cohesion: 0.09
Nodes (70): arraynode, authenticationexception, autowired, RequestIdFilter, ConnectorTokenScope, READ_ONLY, READ_WRITE, ContainerTestConfiguration (+62 more)

### Community 100 - "ScimUserService"
Cohesion: 0.08
Nodes (15): NewScimUser, ScimUserListing, Override, ScimUserReplacement, ScimUserResource, ScimUserService, ScimUserProfile, Cause (+7 more)

### Community 101 - "RFC requirements and implications"
Cohesion: 0.15
Nodes (15): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+7 more)

### Community 103 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.18
Nodes (4): AuditTrailService, Override, AuditEvent, org.springframework.transaction.annotation.Transactional

### Community 105 - "ScimAttributeProjection"
Cohesion: 0.19
Nodes (6): SuppressWarnings, Kind, GROUP, USER, ScimAttributeProjection, Search

### Community 106 - "ScimUserPatchOperationTests"
Cohesion: 0.16
Nodes (7): Condition, ScimEmailFilter, EmailUpdate, RemoveEmailPart, RemoveEmails, UpdateEmails, ScimUserPatchOperationTests

### Community 107 - "AuditTrailServiceTests.java"
Cohesion: 0.09
Nodes (22): AuditAdministrativeRefusal, LAST_ENABLED_ADMINISTRATOR, PROTECTED_RESOURCE, SELF_DISABLE, AuditGroupAttribute, DISPLAY_NAME, MEMBERS, AuditRefusalReason (+14 more)

### Community 108 - "SecurityConfig.java"
Cohesion: 0.06
Nodes (36): argon2passwordencoder, authenticationentrypoint, authorizationfilter, AuditRetentionPolicyConfig, LoginLockoutConfig, ScimSecurityConfig, ScimSeedConfig, PasswordNormalization (+28 more)

### Community 109 - "Credential and cryptographic policy"
Cohesion: 0.22
Nodes (9): Credential and cryptographic policy, Hashing and primitives, Key rotation and storage, Lockout policy, Log formatting, Password policy, Response headers, Session lifetime (+1 more)

### Community 111 - "Spring Session In Redis"
Cohesion: 0.33
Nodes (7): Backend Architecture Boundaries, Redis Compose Service, Spring Session In Redis, Flag ADR Conflicts Explicitly, docs/adr Decision Records, RedisCluster ElastiCache, RedisSubnetGroup

### Community 112 - "ScimConnectorTokenEntity"
Cohesion: 0.15
Nodes (4): ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 114 - "SpaFrontendTests"
Cohesion: 0.19
Nodes (7): Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.stereotype.Component, org.springframework.web.servlet.ModelAndView, requestdispatcher

### Community 115 - "AuditOperation"
Cohesion: 0.06
Nodes (32): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+24 more)

### Community 117 - "org.junit.jupiter.api.Test"
Cohesion: 0.05
Nodes (12): AbsoluteSessionLifetimeFilterTests, SecurityConfigTests, AbsoluteSessionLifetimePolicyTests, Connectors, ConnectorTokenPolicyTests, Lifetime, RotationOverlap, PasswordNormalizationTests (+4 more)

### Community 118 - "org.springframework.stereotype.Service"
Cohesion: 0.08
Nodes (19): AfterCommit, LoginAttemptService, LoginIdentityService, LoginService, ScimUserSessionRevocation, SecurityConfig, AccountSessions, ScimUserSessions (+11 more)

### Community 119 - "org.junit.jupiter.params.provider.CsvSource"
Cohesion: 0.16
Nodes (3): ScimQueryRequestTests, ScimPageRequestTests, org.junit.jupiter.params.provider.CsvSource

### Community 120 - "RecordingAuditTrail"
Cohesion: 0.14
Nodes (8): AuditScimRefusal, INVALID_VALUE, MUTABILITY, NO_TARGET, UNIQUENESS, Override, Recorded, RecordingAuditTrail

### Community 122 - "AuditUserAttribute"
Cohesion: 0.15
Nodes (10): AuditUserAttribute, ACTIVE, DISPLAY_NAME, EMAILS, LOCALE, NAME, PASSWORD, PREFERRED_LANGUAGE (+2 more)

### Community 123 - "Tokens"
Cohesion: 0.11
Nodes (3): ConnectorAdministrationServiceTests, Tokens, ScimConnectorTokenTests

### Community 124 - "IndexedSessions"
Cohesion: 0.28
Nodes (5): Override, AccountSessionsAdapterTests, IndexedSessions, Override, org.springframework.session.MapSession

### Community 125 - "ScimConnector"
Cohesion: 0.19
Nodes (4): ScimConnector, ConnectorAuthenticationServiceTests, InMemoryScimConnectorRepository, Override

### Community 126 - "ScimResourceEntity"
Cohesion: 0.25
Nodes (3): ScimGroupEntity, ScimResourceEntity, ScimGroupJpaRepository

### Community 129 - "AuthenticatedConnector"
Cohesion: 0.15
Nodes (5): ScimSearchRenderer, ScimUserController, ScimUserRenderer, AuthenticatedConnector, org.springframework.http.ResponseEntity

### Community 130 - "components.json"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

### Community 131 - "ScimPatchRefusedException"
Cohesion: 0.36
Nodes (4): Reason, MUTABILITY, NO_TARGET, ScimPatchRefusedException

### Community 133 - "Domain and authority model"
Cohesion: 0.25
Nodes (8): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, Protected recovery resources, SCIM User replaces Account

### Community 134 - ".seed"
Cohesion: 0.21
Nodes (3): ScimSeedService, SeededIdentity, ScimSeedServiceTests

### Community 135 - ".handle"
Cohesion: 0.10
Nodes (11): ScimErrorException, ScimExceptionHandler, InvalidPreconditionException, PasswordReusedException, PreconditionFailedException, PreconditionRequiredException, Accounts and identity provisioning, Current account model (+3 more)

### Community 136 - ".of"
Cohesion: 0.26
Nodes (3): NormalizedDisplayName, NormalizedDisplayNameTests, normalizer

### Community 137 - "AuthControllerTests"
Cohesion: 0.19
Nodes (3): LoginRequest, UserResponse, AuthControllerTests

### Community 138 - "ScimExternalIdEntity"
Cohesion: 0.13
Nodes (8): Override, Key, ScimExternalIdEntity, ScimExternalIdJpaRepository, Override, ScimExternalIdPersistenceAdapter, jakarta.persistence.IdClass, ScimExternalIdEntity.Key

### Community 139 - "EcsLogFormatTests"
Cohesion: 0.18
Nodes (7): EcsLogFormatTests, ResultActions, 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status

### Community 140 - "UserCounter"
Cohesion: 0.16
Nodes (5): UserCounter, UserCounterEntity, Override, UserCounterPersistenceAdapter, UserCounterTests

### Community 142 - "AuditEventRetentionAdapter.java"
Cohesion: 0.33
Nodes (3): AuditEventRetentionAdapter, Override, timestamp

### Community 143 - "ScimGroup"
Cohesion: 0.07
Nodes (8): DuplicateDisplayNameException, ScimGroup, ScimGroupMember, ScimGroupRepository, UnknownGroupMemberException, ScimGroupTests, InMemoryScimGroupRepository, Override

### Community 144 - "ScimGroupController"
Cohesion: 0.24
Nodes (3): ScimGroupController, ScimGroupRenderer, org.springframework.web.bind.annotation.PatchMapping

### Community 145 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 146 - "Frontend Architecture Doc"
Cohesion: 0.11
Nodes (20): Colors Come From index.css Tokens, Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, Vite Full-Reloads on Any Watched HTML Write, One-Way Import Direction Through the Layers, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline (+12 more)

### Community 147 - ".of"
Cohesion: 0.19
Nodes (3): Attribute, ScimAuditFilterShapes, ScimAuditFilterShapesTests

### Community 149 - "IdentitySummary"
Cohesion: 0.16
Nodes (10): IdentitySummary, AdminAccountControllerTests, Override, RecordingService, 2. Revoke a disabled account's sessions after the commit, Alternatives considered, Consequences, Context (+2 more)

### Community 154 - "ScimQueryVocabulary.java"
Cohesion: 0.04
Nodes (76): active, ScimFilterPath, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE (+68 more)

### Community 155 - "ScimUserPatchReaderTests"
Cohesion: 0.16
Nodes (5): AddEmails, RemoveActive, SetActive, SetText, ScimUserPatchReaderTests

### Community 156 - "org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder"
Cohesion: 0.26
Nodes (4): ScimConditionalWrites, ScimEndToEndIntegrationTests, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder, org.springframework.test.web.servlet.request.RequestPostProcessor

### Community 157 - "Kiro: graphify enforcement"
Cohesion: 0.40
Nodes (4): graphify-runner, Kiro: graphify enforcement, The hooks, When the refresh fails

### Community 158 - "org.springframework.data.jpa.repository.Query"
Cohesion: 0.21
Nodes (10): UserCounterJpaRepository, ScimResourceJpaRepository, ScimUserJpaRepository, collection, lockmodetype, org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.jpa.repository.Lock, org.springframework.data.jpa.repository.Modifying (+2 more)

### Community 159 - "ScimResourceType"
Cohesion: 0.06
Nodes (28): ScimSearchService, ScimSearchController, ProtectedResourceException, ScimPageRequest, Hit, Result, ScimQuery, ScimQueryRepository (+20 more)

### Community 161 - "ScimFilterParser"
Cohesion: 0.19
Nodes (4): ResolvedPath, ScimFilterParser, Search, filtering, sorting and projection, Filtering

### Community 162 - "UserCounterService"
Cohesion: 0.15
Nodes (5): getCount operation, UserCounterService, UserCounterRepository, Override, RecordingCounterService

### Community 163 - ".ofIfMatch"
Cohesion: 0.21
Nodes (4): ScimVersionPreconditionTests, InMemoryScimTombstoneRepository, Override, Tombstone

### Community 164 - "ScheduledJobMetrics"
Cohesion: 0.20
Nodes (8): atomiclong, ScheduledJobMetrics, ScheduledJobMetricsTests, io.micrometer.core.instrument.Counter, io.micrometer.core.instrument.MeterRegistry, io.micrometer.core.instrument.simple.SimpleMeterRegistry, timegauge, timeunit

### Community 169 - "ScimUserEmailValue"
Cohesion: 0.15
Nodes (5): Override, ScimGroupMemberId, ScimUserEmailValue, jakarta.persistence.Embeddable, serializable

### Community 170 - "org.junit.jupiter.api.AfterEach"
Cohesion: 0.05
Nodes (24): HttpAuditRequestContext, Override, AfterCommitAdapter, Override, MetricTag, InvalidScimFilterException, HttpAuditRequestContextTests, AfterCommitAdapterTests (+16 more)

### Community 171 - "ScimUserPatchReader"
Cohesion: 0.19
Nodes (6): Op, ADD, REMOVE, REPLACE, Path, ScimUserPatchReader

### Community 177 - "OperationalTelemetryIntegrationTests.java"
Cohesion: 0.08
Nodes (21): Builder, ManagementPortIntegrationTests, Builder, SuppressWarnings, OperationalTelemetryIntegrationTests, Session, cookiepolicy, httpcookie (+13 more)

### Community 178 - "SessionController"
Cohesion: 0.22
Nodes (8): deleteSession operation, updateSession operation, SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession, org.springframework.web.bind.annotation.PutMapping

### Community 179 - "ScimConnectorToken"
Cohesion: 0.33
Nodes (3): ScimConnectorToken, InMemoryScimConnectorTokenRepository, Override

### Community 180 - "EcsLogCapture"
Cohesion: 0.21
Nodes (6): EcsLogCapture, Override, bytearrayoutputstream, ch.qos.logback.classic.LoggerContext, ch.qos.logback.core.OutputStreamAppender, structuredlogencoder

### Community 181 - "ScimName"
Cohesion: 0.18
Nodes (10): ScimName, MergeName, NamePart, FAMILY_NAME, FORMATTED, GIVEN_NAME, HONORIFIC_PREFIX, HONORIFIC_SUFFIX (+2 more)

### Community 182 - "AuditRetentionStartupTests.java"
Cohesion: 0.22
Nodes (4): applicationconversionservice, AuditRetentionStartupTests, org.springframework.boot.test.context.runner.ApplicationContextRunner, propertysourcesplaceholderconfigurer

### Community 184 - "ManagementSessionConfiguration.java"
Cohesion: 0.33
Nodes (7): abstracthttpsessionapplicationinitializer, ManagementSessionConfiguration, managementporttype, org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort, org.springframework.boot.autoconfigure.condition.ConditionalOnClass, org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean, org.springframework.session.web.http.SessionRepositoryFilter

### Community 186 - "LockoutHasNoDurationTests.java"
Cohesion: 0.31
Nodes (5): LockoutHasNoDurationTests, files, java.lang.reflect.RecordComponent, org.springframework.core.env.Environment, path

### Community 187 - "Delivery plan"
Cohesion: 0.22
Nodes (9): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 0a — Permanent lockout, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 5 — Complete query protocol, Slice 6 — Operational Accounts page and audit (+1 more)

### Community 190 - "1. Count login attempts on the login path"
Cohesion: 0.29
Nodes (6): 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision, Status

### Community 192 - "AuditOutcome"
Cohesion: 0.18
Nodes (8): AuditEventRepository, AuditOutcome, FAILURE, SUCCESS, AuditEventJpaRepository, AuditEventPersistenceAdapter, Override, AuditEventEntity

### Community 196 - "Query contract"
Cohesion: 0.40
Nodes (5): Attribute projection, Pagination, POST search, Query contract, Sorting

### Community 197 - "Definition of Done"
Cohesion: 0.40
Nodes (5): Backend gates, Contract and behavior, Definition of Done, Documentation and graph, Frontend gates

### Community 198 - "Write semantics"
Cohesion: 0.40
Nodes (5): DELETE and tombstones, PATCH, POST, PUT, Write semantics

## Ambiguous Edges - Review These
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **453 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+448 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 835 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **64 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Backend API Contract (OpenAPI 3.1)` connect `Backend API Contract (OpenAPI 3.1)` to `UserCounterService`, `SessionController`?**
  _High betweenness centrality (0.110) - this node is a cross-community bridge._
- **Why does `login operation` connect `Backend API Contract (OpenAPI 3.1)` to `auth.helpers.ts`, `http.ts`?**
  _High betweenness centrality (0.072) - this node is a cross-community bridge._
- **Why does `Shared Playwright storageState for Auth` connect `auth.helpers.ts` to `Frontend Architecture Doc`, `Backend API Contract (OpenAPI 3.1)`?**
  _High betweenness centrality (0.054) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _453 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Attribute` be split into smaller, more focused modules?**
  _Cohesion score 0.03373015873015873 - nodes in this community are weakly interconnected._