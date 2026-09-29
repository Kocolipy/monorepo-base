# Graph Report - monorepo-base-issue-16  (2026-09-29)

## Corpus Check
- 371 files · ~232,946 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: (none) 10, .example 1, .properties 1)

## Summary
- 3877 nodes · 12788 edges · 203 communities (128 shown, 75 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 1477 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `84907196`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- assertthat
- ScimUserRepository
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
- .readCreate
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
- Spring Session In Redis
- route-guards.tsx
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
- uuid
- bootstrap.sh
- package.sh
- Test Static index.html Stub
- com.example:backend
- AuditRetentionPolicy
- Backend API Contract (OpenAPI 3.1)
- .ofUser
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh
- AuditRetentionStartupTests.java
- ScimUserServiceTests
- ConnectorTokenSecretTests
- auth-context-value.ts
- ArchitectureTest.java
- accountseed
- ScimResourceType
- LoginAttemptServiceTests
- SCIM 2.0 account-management specification plan
- AfterCommitAdapterTests.java
- 3. ECS-structured logging with redaction enforced structurally
- ConnectorAdministrationService
- AuditAppendOnlyIntegrationTests
- .sessionsOf
- dependencies
- AdminConnectorController
- ScimUserAttributesTests
- LogContextTests
- ScimBearerAuthenticationFilterTests
- CapturedLog
- ScimConditionalWriteIntegrationTests
- AuditAppendOnlyIntegrationTests.java
- BackendApplication.java
- ScimDeletionIntegrationTests
- verify.sh
- ScimUserProfile
- jakarta.persistence.Entity
- jakarta.servlet.http.HttpServletRequest
- AuditTrail
- Cause
- java.security.Principal
- ScimUserEdit
- ScimGroupController.java
- ScimSecurityChainOrderTests.java
- RFC requirements and implications
- IdentityAdministrationServiceTests.java
- ScimUser
- ScimConditionalWriteIntegrationTests.java
- AuthenticatedConnector
- Delivery plan
- bcryptpasswordencoder
- org.springframework.transaction.annotation.Transactional
- Query contract
- Domain and authority model
- ScimUserPatchOperationTests
- AuditEvent
- SecurityConfig.java
- AuditEventRecordingIntegrationTests
- .created
- RedisSessionRevocationIntegrationTests.java
- ScimConnectorTokenEntity
- ScimUserEntity
- SpaFrontendTests
- AuditOperation
- .of
- org.junit.jupiter.api.Test
- ScimUserServiceTests.java
- EcsLogCapture
- RecordingAuditTrail
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- AuditUserAttribute
- IdentityAdministrationService
- ScimOffsetPage
- ScimConnector
- ScimGroup
- ScimDiscoveryIntegrationTests
- LockoutHasNoDurationTests.java
- ScimUserController
- components.json
- ScimPatchRefusedException
- ScimUserRequestReader
- tools.jackson.databind.JsonNode
- ScimConnectorTokenTests
- .handle
- .of
- AuthControllerTests
- Key
- EcsLogFormatTests
- UserCounter
- ScimEmail
- org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
- InMemoryScimGroupRepository
- ScimGroupController
- RequestIdFilterTests
- Frontend Testing Guide
- .overlapEnd
- ScimSchemas
- IdentitySummary
- filterchainproxy
- mockfilterchain
- mockhttpservletresponse
- recordcomponent
- jakarta.servlet.http.HttpServletResponse
- ScimUserPatchOperation
- ScimLoginState
- IndexedSessions
- org.springframework.data.jpa.repository.Query
- ScimGroupService
- InMemoryScimExternalIdRepository
- ScimUserPersistenceAdapter
- ScimAttributeProjection
- ScimUserReplacement
- AuditTrailServiceTests.java
- ScimUserEmailValue
- CountingPasswordEncoder
- transactiondefinition
- transactionstatus
- CountingPasswordEncoder
- .current
- ScimUserPatchReader
- drivermanager
- inmemoryuserdetailsmanager
- ScimEmailFilter
- FakeSaltedEncoder
- button.tsx
- .ofIfMatch
- .asConnector
- ScimConnectorToken
- .summarize
- org.springframework.http.ResponseEntity
- InMemoryAccountSessions
- CountingPasswordEncoder
- ConnectorAuthenticationServiceTests
- .requiresWriteScope
- App.tsx
- HttpAuditRequestContextTests.java
- .of
- ScimPasswordChange
- org.junit.jupiter.api.AfterEach
- Frontend Technology Stack
- AuditEventEntity
- RecordingTransactionManager
- ScimUserProfileTests
- RefusalTimingEquivalenceTests
- 1. Count login attempts on the login path
- 2. Revoke a disabled account's sessions after the commit
- AbsoluteSessionLifetimePolicyTests
- .normalize
- TextAttribute
- ScimExternalIdPersistenceAdapter

## God Nodes (most connected - your core abstractions)
1. `ScimUser` - 109 edges
2. `ScimGroupProvisioningIntegrationTests` - 77 edges
3. `ScimGroup` - 72 edges
4. `IdentityAdministrationServiceTests` - 65 edges
5. `ScimConditionalWriteIntegrationTests` - 64 edges
6. `AuditTrail` - 62 edges
7. `AuditOperation` - 59 edges
8. `ScimDeletionIntegrationTests` - 59 edges
9. `RecordingAuditTrail` - 55 edges
10. `AuthenticatedConnector` - 53 edges

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

## Communities (203 total, 75 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 1 - "assertthat"
Cohesion: 0.07
Nodes (15): assertthat, assertthatcode, assertthatthrownby, base64, cause, chronounit, dispatchertype, hashset (+7 more)

### Community 2 - "ScimUserRepository"
Cohesion: 0.06
Nodes (37): assertthatnoexception, atomicinteger, authentication, authenticationmanager, AfterCommit, LoginAttemptService, LoginIdentityService, LoginService (+29 more)

### Community 3 - "accounts.tsx"
Cohesion: 0.18
Nodes (22): useAuth(), useAuthState(), useSessionRequest(), Button(), Card(), CardContent(), CardDescription(), CardFooter() (+14 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.17
Nodes (13): Every Playwright Spec Needs a testMatch, Shared Playwright storageState for Auth, ADMIN_CREDENTIALS, captureSessionCookie(), expireSession(), login(), loginAs(), postAdminAction() (+5 more)

### Community 5 - "http.ts"
Cohesion: 0.15
Nodes (18): decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, SessionRequest, SessionResult (+10 more)

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
Nodes (35): engines, node, npm, name, overrides, qs, packageManager, private (+27 more)

### Community 14 - "scripts"
Cohesion: 0.10
Nodes (20): scripts, analyze, build, dev, format, format:check, lint, preview (+12 more)

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "ScimUserPatchReaderTests.java"
Cohesion: 0.11
Nodes (28): addemails, NamePart, FAMILY_NAME, FORMATTED, GIVEN_NAME, HONORIFIC_PREFIX, HONORIFIC_SUFFIX, MIDDLE_NAME (+20 more)

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
Cohesion: 0.08
Nodes (9): SpaRoutes, LockoutPolicyTests, SpaRoutesScimNamespaceTests, ReservedServerPaths, SpaRoutesTests, SpaShell, org.junit.jupiter.api.Nested, org.junit.jupiter.params.ParameterizedTest (+1 more)

### Community 26 - "Spring Session In Redis"
Cohesion: 0.18
Nodes (14): docs/openapi.yaml API Contract, Postgres Compose Service, Redis Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, Spring Session In Redis (+6 more)

### Community 27 - "route-guards.tsx"
Cohesion: 0.15
Nodes (15): Accounts and identity provisioning, CONTEXT, Current account model, Request paths, Sessions, AuthRole, AuthStatus, SessionRoute() (+7 more)

### Community 28 - "infra/ Is Deployment Material Not An App"
Cohesion: 0.20
Nodes (10): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, Backend Architecture Boundaries, Flag ADR Conflicts Explicitly, docs/adr Decision Records, CONTEXT.md Domain Glossary, gh CLI Conventions (+2 more)

### Community 29 - ".given"
Cohesion: 0.14
Nodes (3): Override, LoginIdentityServiceTests, org.springframework.security.core.userdetails.UserDetails

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "org.springframework.context.annotation.Bean"
Cohesion: 0.14
Nodes (10): AuditRetentionPolicyConfig, LoginLockoutConfig, SecurityConfig, ScimSeedConfig, SecurityConfigPasswordEncoderTests, org.springframework.boot.ApplicationRunner, org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration (+2 more)

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
Cohesion: 0.17
Nodes (5): NewScimGroup, AddMembers, RemoveMembers, ScimGroupReplacement, ScimGroupServiceTests

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

### Community 50 - "uuid"
Cohesion: 0.09
Nodes (12): ScimGroupReference, ScimConnectorJpaRepository, ScimConnectorPersistenceAdapter, ScimPasswordHistoryJpaRepository, ScimPasswordHistoryPersistenceAdapter, comparator, instant, list (+4 more)

### Community 56 - "AuditRetentionPolicy"
Cohesion: 0.14
Nodes (11): AuditRetentionService, AuditRetentionScheduleConfig, Override, AuditEventRetention, AuditRetentionPolicy, AuditRetentionPolicyTests, crontask, crontrigger (+3 more)

### Community 57 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.09
Nodes (25): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, deleteSession operation, getCount operation, getCurrentUser operation, getHealth operation, getSession operation, incrementCount operation (+17 more)

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "AuditRetentionStartupTests.java"
Cohesion: 0.25
Nodes (4): applicationconversionservice, AuditRetentionStartupTests, org.springframework.boot.test.context.runner.ApplicationContextRunner, propertysourcesplaceholderconfigurer

### Community 63 - "ScimUserServiceTests"
Cohesion: 0.15
Nodes (5): SetActive, SetPassword, Revocation, ScimUserServiceTests, Override

### Community 64 - "ConnectorTokenSecretTests"
Cohesion: 0.10
Nodes (9): ConnectorTokenDigest, Override, Minted, Presented, ConnectorTokenSecretTests, java.security.MessageDigest, java.security.SecureRandom, nosuchalgorithmexception (+1 more)

### Community 65 - "auth-context-value.ts"
Cohesion: 0.10
Nodes (16): AuthUser, AuthContext, AuthContextState, AuthContextValue, apiFetchMock, request(), state, wrapper() (+8 more)

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 68 - "ScimResourceType"
Cohesion: 0.18
Nodes (10): ScimResourceType, GROUP, USER, ScimTombstoneRepository, Override, ScimTombstonePersistenceAdapter, InMemoryScimTombstoneRepository, Override (+2 more)

### Community 70 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.05
Nodes (43): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Backend gates, Connector identity and token lifecycle, Contract and behavior (+35 more)

### Community 71 - "AfterCommitAdapterTests.java"
Cohesion: 0.23
Nodes (6): AfterCommitAdapter, Override, AfterCommitAdapterTests, transactionsynchronization, transactionsynchronizationmanager, transactionsynchronizationutils

### Community 72 - "3. ECS-structured logging with redaction enforced structurally"
Cohesion: 0.33
Nodes (5): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status

### Community 76 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 77 - "AdminConnectorController"
Cohesion: 0.12
Nodes (10): UnknownIdentityException, IssuedConnectorToken, UnknownConnectorException, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest, RotateTokenRequest, InvalidConnectorTokenLifetimeException (+2 more)

### Community 78 - "ScimUserAttributesTests"
Cohesion: 0.05
Nodes (8): ScimDiscovery, ScimGroupAttributes, Attribute, ScimUserAttributes, ScimDiscoveryTests, SuppressWarnings, ScimUserAttributesTests, Search, filtering, sorting and projection

### Community 79 - "LogContextTests"
Cohesion: 0.19
Nodes (4): Override, LogContext, Scope, LogContextTests

### Community 80 - "ScimBearerAuthenticationFilterTests"
Cohesion: 0.19
Nodes (3): LoginOutcome, ScimBearerAuthenticationFilterTests, org.springframework.security.core.Authentication

### Community 81 - "CapturedLog"
Cohesion: 0.16
Nodes (7): AuditRetentionServiceTests, CountingRetention, Override, CapturedLog, Override, ch.qos.logback.classic.spi.ILoggingEvent, ch.qos.logback.core.read.ListAppender

### Community 83 - "AuditAppendOnlyIntegrationTests.java"
Cohesion: 0.12
Nodes (45): autowired, InMemorySessionRegistryConfiguration, ScimReleaseGateDefaultIntegrationTests, ScimReleaseGateIntegrationTests, bean, classpathresource, containsstring, content (+37 more)

### Community 84 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 87 - "ScimUserProfile"
Cohesion: 0.09
Nodes (17): arraylist, DuplicateDisplayNameException, PasswordHistoryPolicy, PasswordReusedException, ScimName, ScimUserProfile, UnknownGroupMemberException, collectors (+9 more)

### Community 88 - "jakarta.persistence.Entity"
Cohesion: 0.08
Nodes (24): ScimConnectorEntity, ScimExternalIdEntity, ScimGroupMemberEntity, ScimPasswordHistoryEntity, Override, cascadetype, collectiontable, column (+16 more)

### Community 89 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.12
Nodes (16): AbsoluteSessionLifetimeFilter, Override, AbsoluteSessionLifetimePolicy, Override, ScimReleaseGate, Override, ScimReleaseGateFilter, httpsession (+8 more)

### Community 91 - "Cause"
Cohesion: 0.29
Nodes (5): Cause, DEACTIVATED, DELETED, PASSWORD_CHANGED, USER_NAME_CHANGED

### Community 92 - "java.security.Principal"
Cohesion: 0.19
Nodes (7): CountResponse, UserCounterController, Override, RecordingCounterService, UserCounterControllerTests, java.security.Principal, org.springframework.web.bind.annotation.RestController

### Community 94 - "ScimGroupController.java"
Cohesion: 0.14
Nodes (23): authenticationprincipal, AuthController, cachecontrol, cookievalue, httpstatus, notblank, notnull, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy (+15 more)

### Community 95 - "ScimSecurityChainOrderTests.java"
Cohesion: 0.11
Nodes (17): anonymousauthenticationfilter, BackendApplicationTests, ScimSecurityChainOrderTests, basicauthenticationfilter, classmode, csrffilter, disableencodeurlfilter, exceptiontranslationfilter (+9 more)

### Community 96 - "RFC requirements and implications"
Cohesion: 0.11
Nodes (16): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Executive finding, Existing application seams (+8 more)

### Community 97 - "IdentityAdministrationServiceTests.java"
Cohesion: 0.09
Nodes (21): AuditAdministrativeRefusal, LAST_ENABLED_ADMINISTRATOR, PROTECTED_RESOURCE, SELF_DISABLE, LoggingOperationalAlerts, LogEvent, ConnectorAuthenticationService, ConnectorTokenPolicy (+13 more)

### Community 98 - "ScimUser"
Cohesion: 0.08
Nodes (7): ScimGroupListing, ScimUserListing, DuplicateUserNameException, ScimPageRequest, ScimUser, InMemoryScimUserRepository, Override

### Community 99 - "ScimConditionalWriteIntegrationTests.java"
Cohesion: 0.13
Nodes (26): authenticationexception, RequestIdFilter, ConnectorTokenScope, READ_ONLY, READ_WRITE, ContainerTestConfiguration, countdownlatch, dataaccessexception (+18 more)

### Community 100 - "AuthenticatedConnector"
Cohesion: 0.15
Nodes (4): NewScimUser, ScimUserResource, ScimUserService, AuthenticatedConnector

### Community 101 - "Delivery plan"
Cohesion: 0.20
Nodes (10): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 0a — Permanent lockout, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 4 — Groups and Admin authority, Slice 5 — Complete query protocol (+2 more)

### Community 103 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.19
Nodes (3): AuditTrailService, Override, org.springframework.transaction.annotation.Transactional

### Community 104 - "Query contract"
Cohesion: 0.33
Nodes (6): Attribute projection, Filtering, Pagination, POST search, Query contract, Sorting

### Community 105 - "Domain and authority model"
Cohesion: 0.25
Nodes (8): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, Protected recovery resources, SCIM User replaces Account

### Community 106 - "ScimUserPatchOperationTests"
Cohesion: 0.17
Nodes (5): EmailUpdate, RemoveEmailPart, RemoveEmails, UpdateEmails, ScimUserPatchOperationTests

### Community 107 - "AuditEvent"
Cohesion: 0.19
Nodes (6): AuditEvent, AuditEventRepository, AuditOutcome, FAILURE, SUCCESS, RecordingRepository

### Community 108 - "SecurityConfig.java"
Cohesion: 0.10
Nodes (19): argon2passwordencoder, authenticationentrypoint, authorizationfilter, ScimSecurityConfig, PasswordNormalization, changesessionidauthenticationstrategy, cookiecsrftokenrepository, daoauthenticationprovider (+11 more)

### Community 111 - "RedisSessionRevocationIntegrationTests.java"
Cohesion: 0.16
Nodes (15): AuditRefusalReason, ACCOUNT_DISABLED, ACCOUNT_LOCKED, BAD_CREDENTIALS, OTHER, UNKNOWN_ACCOUNT, AccountSessionsAdapter, RedisSessionRevocationIntegrationTests (+7 more)

### Community 112 - "ScimConnectorTokenEntity"
Cohesion: 0.16
Nodes (4): ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 113 - "ScimUserEntity"
Cohesion: 0.11
Nodes (4): ScimGroupEntity, ScimLoginStateValue, ScimResourceEntity, ScimUserEntity

### Community 114 - "SpaFrontendTests"
Cohesion: 0.21
Nodes (7): Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.stereotype.Component, org.springframework.web.servlet.ModelAndView, requestdispatcher

### Community 115 - "AuditOperation"
Cohesion: 0.08
Nodes (25): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+17 more)

### Community 117 - "org.junit.jupiter.api.Test"
Cohesion: 0.05
Nodes (8): AbsoluteSessionLifetimeFilterTests, SecurityConfigTests, Connectors, Tokens, CompositeKeyContractTests, ExternalIdAlias, GroupMembership, org.junit.jupiter.api.Test

### Community 118 - "ScimUserServiceTests.java"
Cohesion: 0.17
Nodes (12): arrays, NormalizedUserName, ProtectedResourceException, ReservedResourceName, ADMIN_GROUP, BOOTSTRAP_ADMIN, ScimGroupMember, ScimIdentities (+4 more)

### Community 119 - "EcsLogCapture"
Cohesion: 0.20
Nodes (7): EcsLogCapture, Override, bytearrayoutputstream, ch.qos.logback.classic.Logger, ch.qos.logback.classic.LoggerContext, ch.qos.logback.core.OutputStreamAppender, structuredlogencoder

### Community 120 - "RecordingAuditTrail"
Cohesion: 0.14
Nodes (8): AuditScimRefusal, INVALID_VALUE, MUTABILITY, NO_TARGET, UNIQUENESS, Override, Recorded, RecordingAuditTrail

### Community 121 - "4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal"
Cohesion: 0.25
Nodes (6): 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal, Alternatives considered, Consequences, Context, Decision, Status

### Community 122 - "AuditUserAttribute"
Cohesion: 0.17
Nodes (10): AuditUserAttribute, ACTIVE, DISPLAY_NAME, EMAILS, LOCALE, NAME, PASSWORD, PREFERRED_LANGUAGE (+2 more)

### Community 124 - "ScimOffsetPage"
Cohesion: 0.18
Nodes (5): Override, ScimOffsetPage, ScimOffsetPageTests, org.springframework.data.domain.Pageable, org.springframework.data.domain.Sort

### Community 125 - "ScimConnector"
Cohesion: 0.20
Nodes (4): ScimConnector, Override, InMemoryScimConnectorRepository, Override

### Community 126 - "ScimGroup"
Cohesion: 0.20
Nodes (3): ScimGroup, Override, ScimGroupPersistenceAdapter

### Community 128 - "LockoutHasNoDurationTests.java"
Cohesion: 0.25
Nodes (6): LockoutHasNoDurationTests, files, java.lang.reflect.RecordComponent, org.junit.jupiter.params.provider.MethodSource, org.springframework.core.env.Environment, path

### Community 130 - "components.json"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

### Community 131 - "ScimPatchRefusedException"
Cohesion: 0.36
Nodes (4): Reason, MUTABILITY, NO_TARGET, ScimPatchRefusedException

### Community 133 - "tools.jackson.databind.JsonNode"
Cohesion: 0.23
Nodes (8): RemoveAllMembers, ReplaceMembers, ScimGroupPatchOperation, SetDisplayName, ScimGroupRequestReader, java.util.regex.Pattern, matcher, tools.jackson.databind.JsonNode

### Community 135 - ".handle"
Cohesion: 0.14
Nodes (7): ScimErrorException, ScimExceptionHandler, InvalidPreconditionException, PreconditionFailedException, PreconditionRequiredException, org.springframework.http.HttpStatus, org.springframework.web.bind.annotation.RestControllerAdvice

### Community 137 - "AuthControllerTests"
Cohesion: 0.19
Nodes (3): LoginRequest, UserResponse, AuthControllerTests

### Community 140 - "UserCounter"
Cohesion: 0.10
Nodes (7): UserCounter, UserCounterEntity, Override, UserCounterPersistenceAdapter, UserCounterServiceTests, UserCounterTests, EntityManager

### Community 142 - "org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder"
Cohesion: 0.17
Nodes (7): AuditEventRetentionAdapter, Override, ScimConditionalWrites, ScimEndToEndIntegrationTests, org.springframework.jdbc.core.JdbcTemplate, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder, org.springframework.test.web.servlet.request.RequestPostProcessor

### Community 143 - "InMemoryScimGroupRepository"
Cohesion: 0.12
Nodes (3): ScimSeedServiceTests, InMemoryScimGroupRepository, Override

### Community 146 - "Frontend Testing Guide"
Cohesion: 0.14
Nodes (15): ArchUnit Baseline Gate, Always ./mvnw Never Bare mvn, Colors Come From index.css Tokens, Trace Before You Delete, Frontend Local Semgrep Ruleset, Baseline Full Extensive Test Levels, Vite Full-Reloads on Any Watched HTML Write, Frontend Testing Guide (+7 more)

### Community 147 - ".overlapEnd"
Cohesion: 0.18
Nodes (3): ConnectorTokenPolicyTests, Lifetime, RotationOverlap

### Community 149 - "IdentitySummary"
Cohesion: 0.22
Nodes (6): IdentitySummary, AdminAccountController, AdminAccountControllerTests, Override, RecordingService, org.springframework.web.bind.annotation.PostMapping

### Community 154 - "jakarta.servlet.http.HttpServletResponse"
Cohesion: 0.15
Nodes (8): Override, ScimBearerAuthenticationFilter, ScimBearerChallenge, jakarta.servlet.http.HttpServletResponse, graphify-runner, Kiro: graphify enforcement, The hooks, When the refresh fails

### Community 155 - "ScimUserPatchOperation"
Cohesion: 0.14
Nodes (6): AddEmails, MergeName, RemoveText, ScimUserPatchOperation, SetText, ScimUserPatchReaderTests

### Community 157 - "IndexedSessions"
Cohesion: 0.28
Nodes (5): Override, AccountSessionsAdapterTests, IndexedSessions, Override, org.springframework.session.MapSession

### Community 158 - "org.springframework.data.jpa.repository.Query"
Cohesion: 0.12
Nodes (13): UserCounterJpaRepository, ScimExternalIdJpaRepository, ScimGroupJpaRepository, ScimGroupMemberJpaRepository, ScimGroupMemberRow, ScimResourceJpaRepository, collection, lockmodetype (+5 more)

### Community 159 - "ScimGroupService"
Cohesion: 0.18
Nodes (3): ScimGroupResource, ScimGroupService, ScimVersionPrecondition

### Community 161 - "ScimUserPersistenceAdapter"
Cohesion: 0.20
Nodes (3): ScimUserJpaRepository, Override, ScimUserPersistenceAdapter

### Community 162 - "ScimAttributeProjection"
Cohesion: 0.23
Nodes (5): SuppressWarnings, Kind, GROUP, USER, ScimAttributeProjection

### Community 163 - "ScimUserReplacement"
Cohesion: 0.33
Nodes (3): Override, ScimUserReplacement, ScimUserReplacementTests

### Community 164 - "AuditTrailServiceTests.java"
Cohesion: 0.24
Nodes (5): AuditGroupAttribute, DISPLAY_NAME, MEMBERS, OperationalAlerts, simpletransactionstatus

### Community 165 - "ScimUserEmailValue"
Cohesion: 0.14
Nodes (5): Override, ScimGroupMemberId, ScimUserEmailValue, jakarta.persistence.Embeddable, serializable

### Community 170 - ".current"
Cohesion: 0.31
Nodes (4): HttpAuditRequestContext, Override, HttpAuditRequestContextTests, org.springframework.mock.web.MockHttpServletRequest

### Community 171 - "ScimUserPatchReader"
Cohesion: 0.19
Nodes (9): Op, ADD, REMOVE, REPLACE, Path, ScimUserPatchReader, SCIM target model, Supported User profile (+1 more)

### Community 174 - "ScimEmailFilter"
Cohesion: 0.20
Nodes (6): Condition, ScimEmailFilter, ScimEmailPart, PRIMARY, TYPE, VALUE

### Community 176 - "button.tsx"
Cohesion: 0.16
Nodes (13): Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, One-Way Import Direction Through the Layers, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline, components/ui Is a Package Placeholder, Vitest Deliberately Omits the Tailwind Vite Plugin (+5 more)

### Community 179 - "ScimConnectorToken"
Cohesion: 0.27
Nodes (3): ScimConnectorToken, InMemoryScimConnectorTokenRepository, Override

### Community 181 - "org.springframework.http.ResponseEntity"
Cohesion: 0.35
Nodes (4): ScimDiscoveryController, ProbeController, org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.GetMapping

### Community 182 - "InMemoryAccountSessions"
Cohesion: 0.24
Nodes (7): SessionRegistryConfiguration, SessionRegistryConfiguration, SessionRegistryConfiguration, InMemoryAccountSessions, Override, org.springframework.boot.test.context.TestConfiguration, org.springframework.context.annotation.Primary

### Community 186 - "App.tsx"
Cohesion: 0.22
Nodes (8): Frontend SPA Entry HTML, App(), AuthProvider(), GuestRoute(), ProtectedRoute(), frontend_src_index, container, react-dom

### Community 187 - "HttpAuditRequestContextTests.java"
Cohesion: 0.31
Nodes (5): AuditRequest, AuditRequestContext, handlermapping, requestcontextholder, servletrequestattributes

### Community 189 - "ScimPasswordChange"
Cohesion: 0.20
Nodes (6): Override, Kind, CLEAR, SET, UNCHANGED, ScimPasswordChange

### Community 191 - "Frontend Technology Stack"
Cohesion: 0.25
Nodes (8): No Parent-Relative Paths From An App, SPA Build Contract, with-frontend Maven Profile, Backend Serves SPA And Forwards Routes, Claim: No Router Data Layer Or Auth, No .env Required In Frontend, Frontend Technology Stack, npm ci Not npm install

### Community 192 - "AuditEventEntity"
Cohesion: 0.39
Nodes (4): AuditEventJpaRepository, AuditEventPersistenceAdapter, Override, AuditEventEntity

### Community 193 - "RecordingTransactionManager"
Cohesion: 0.43
Nodes (4): Override, RecordingTransactionManager, org.springframework.transaction.TransactionDefinition, org.springframework.transaction.TransactionStatus

### Community 196 - "1. Count login attempts on the login path"
Cohesion: 0.29
Nodes (6): 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision, Status

### Community 197 - "2. Revoke a disabled account's sessions after the commit"
Cohesion: 0.29
Nodes (6): 2. Revoke a disabled account's sessions after the commit, Alternatives considered, Consequences, Context, Decision, Status

### Community 200 - "TextAttribute"
Cohesion: 0.33
Nodes (6): TextAttribute, DISPLAY_NAME, LOCALE, PREFERRED_LANGUAGE, TIMEZONE, USER_NAME

## Ambiguous Edges - Review These
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **399 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+394 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 707 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **75 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `login operation` connect `Backend API Contract (OpenAPI 3.1)` to `auth.helpers.ts`, `http.ts`?**
  _High betweenness centrality (0.095) - this node is a cross-community bridge._
- **Why does `Shared Playwright storageState for Auth` connect `auth.helpers.ts` to `Backend API Contract (OpenAPI 3.1)`, `Frontend Testing Guide`?**
  _High betweenness centrality (0.065) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _399 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `assertthat` be split into smaller, more focused modules?**
  _Cohesion score 0.07396870554765292 - nodes in this community are weakly interconnected._
- **Should `ScimUserRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.0594679186228482 - nodes in this community are weakly interconnected._