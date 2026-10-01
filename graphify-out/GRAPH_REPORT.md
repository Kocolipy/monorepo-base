# Graph Report - monorepo-base-no-grace  (2026-10-01)

## Corpus Check
- 471 files · ~309,788 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 16 file(s) not represented in the graph (top: (none) 10, .example 1, .properties 1)

## Summary
- 5154 nodes · 17994 edges · 204 communities (155 shown, 49 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 2265 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d0ff2a99`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.junit.jupiter.api.Test
- InMemoryScimGroupRepository
- IdentityAdministrationServiceTests
- ScimUserResource
- connectors.tsx
- .advanceBy
- .of
- .toDomain
- .changePassword
- Attribute
- AuditTrail
- SecurityConfig
- ScimUserServiceTests
- ScimGroupProvisioningIntegrationTests
- org.springframework.data.jpa.repository.Query
- ScimAttributeProjectionTests
- org.junit.jupiter.params.provider.ValueSource
- org.junit.jupiter.api.BeforeEach
- ScimQuery
- AuditTrailServiceTests
- AuthenticatedConnector
- AuditTrailServiceTests.java
- UserCounter
- AuditAppendOnlyIntegrationTests
- ScimQueryVocabulary.java
- org.springframework.transaction.annotation.Transactional
- ScimGroupController.java
- .given
- org.junit.jupiter.params.ParameterizedTest
- ScimFilterParserTests
- ScimUserPatchOperationTests
- RecordingAuditTrail
- OperationalTelemetryIntegrationTests
- jakarta.servlet.http.HttpServletRequest
- .created
- ScimDeletionIntegrationTests
- ProbeController
- ConnectorTokenSecretTests
- jakarta.persistence.Entity
- PasswordChangeLifecycleIntegrationTests
- AWS CloudFormation Deployment Guide
- SecurityConfig.java
- ScimUserPatchReaderTests.java
- ScimResourceType
- .seed
- .created
- AuditOperation
- IdentitySummary
- SeededIdentity
- AuditEventQuery
- ScimConnectorLifecycleIntegrationTests
- compilerOptions
- tools.jackson.databind.JsonNode
- .readCreate
- ScimFilterParser
- ScimUserProfile
- AuthControllerTests
- ScimQuerySql.java
- Attribute
- ScimUserProvisioningIntegrationTests
- devDependencies
- Workflow
- ScimEmail
- AdminConnectorController
- ScimBearerAuthenticationFilterTests
- App.tsx
- ScimVersionPrecondition
- AuditTrailService.java
- ScimRequestObservationConventionTests
- ScimConnector
- apiFetch
- ScimUserPatchReader
- LogContextTests
- ScimConnectorTokenEntity
- ScimExternalIdEntity
- .get
- AuditUserAttribute
- .handle
- ScimUser
- SCIM 2.0 account-management specification plan
- compilerOptions
- .require
- AdminAccountEndpointTests
- InactivityGovernanceIntegrationTests
- .of
- scripts
- ScimUserAttributesTests
- ScimUserPatchReaderTests
- Rule
- IndexedSessions
- SpaFrontendTests
- ScimLoginStateTests
- ScimConnectorToken
- components.json
- auth.helpers.ts
- tsconfig.test.json
- ScheduledJobMetrics
- ScimUserPersistenceAdapter
- accounts.tsx
- lib.sh
- org.springframework.mock.web.MockHttpServletRequest
- MetricTagTests
- AuditListingEndToEndIntegrationTests
- .of
- ScimExceptionHandlerMetricTests.java
- stryker.config.json
- Backend
- SessionController
- EcsLogCapture
- ScimEndToEndIntegrationTests
- FakeSaltedEncoder
- The layers
- auth-context-value.ts
- sources.ts
- AGENTS.md
- .violation
- ScimQuerySqlTests
- ScimGroup
- AuditRetentionService
- deploy.sh
- ScimConnectorEntity
- ScimResourceEntity
- org.springframework.context.annotation.Bean
- AGENTS.md — frontend
- AuditPasswordChangeRefusal
- ScimSeedIntegrationTests
- org.springframework.context.annotation.Configuration
- .isProtectedFromWrites
- ScimAttributeProjection
- DormancyPolicyStartupTests.java
- RFC requirements and implications
- package.json
- .fromSearchRequest
- ManagementPortIntegrationTests.java
- mvnw
- accounts-admin.spec.ts
- EcsLogFormatTests
- java.security.Principal
- change-password.spec.ts
- Delivery plan
- front-end
- AGENTS.md — backend
- org.springframework.http.ResponseEntity
- ScimUserRequestReader
- SCIM 2.0 account-management research
- Credential and cryptographic policy
- ScimPatchRefusedException
- ManagementSessionConfiguration.java
- .sampleTraffic
- AfterCommitAdapterTests
- Quick Start
- monorepo-base
- Testing Guide
- assertthat
- AuditRetentionPolicy
- AuditAdministrativeRefusal
- ScimPasswordHistoryPersistenceAdapter
- CONTEXT
- Common Tasks
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- org.springframework.stereotype.Component
- 5. Serialize scheduled jobs on per-job lock rows
- AuditEventReadAdapter.java
- RecordingTransactionManager
- ArchitectureTest.java
- CapturedLog
- Domain and authority model
- Domain Docs
- Issue tracker: GitHub
- Query contract
- CountingPasswordEncoder
- Definition of Done
- Write semantics
- 1. Count login attempts on the login path
- cleanup.sh
- get-vpc-info.sh
- Kiro: graphify enforcement
- dev-stop.sh
- E2E
- Graphify Runner
- 3. ECS-structured logging with redaction enforced structurally
- ScimSchemas
- AuditEventRetentionAdapter.java
- ScimEmailPart
- .the_string_form_says_whether_a_password_was_sent_and_never_what_it_was
- dev.sh
- integration-test.sh
- semgrep.sh
- verify.sh
- CLAUDE.md
- ConnectorTokenDigest.java
- prettier.config.mjs
- graphify-guard.sh
- graphify-refresh.sh
- bootstrap.sh
- package.sh
- com.example:backend
- uuid
- LockoutHasNoDurationTests
- .of
- accounts.test.tsx

## God Nodes (most connected - your core abstractions)
1. `ScimUser` - 142 edges
2. `ScimGroup` - 81 edges
3. `AuditTrail` - 79 edges
4. `ScimGroupProvisioningIntegrationTests` - 78 edges
5. `ScimFilterPath` - 77 edges
6. `AuditOperation` - 76 edges
7. `ScimUserRepository` - 76 edges
8. `ScimResourceType` - 72 edges
9. `RecordingAuditTrail` - 69 edges
10. `AuthenticatedConnector` - 67 edges

## Surprising Connections (you probably didn't know these)
- `Decision` --references--> `ScheduledJobLock`  [INFERRED]
  docs/adr/0005-serialize-scheduled-jobs-on-per-job-lock-rows.md → backend/src/main/java/com/example/backend/auth/domain/ScheduledJobLock.java
- `Consequences` --references--> `AuditTrailServiceTests`  [INFERRED]
  docs/adr/0004-audit-append-failure-semantics.md → backend/src/test/java/com/example/backend/audit/application/AuditTrailServiceTests.java
- `Consequences` --references--> `LoginLockoutTests`  [INFERRED]
  docs/adr/0001-count-login-attempts-on-the-login-path.md → backend/src/test/java/com/example/backend/auth/application/LoginLockoutTests.java
- `Alerts` --references--> `OperationalTelemetryIntegrationTests`  [INFERRED]
  infra/README.md → backend/src/test/java/com/example/backend/observability/OperationalTelemetryIntegrationTests.java
- `Sessions` --references--> `resolveSessionRoute()`  [INFERRED]
  CONTEXT.md → frontend/src/auth/session-route.ts

## Import Cycles
- None detected.

## Communities (204 total, 49 thin omitted)

### Community 0 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (13): AuditEventQueryTests, AbsoluteSessionLifetimeFilterTests, RequestIdFilterTests, ConnectorAdministrationServiceTests, Connectors, Tokens, ConnectorTokenPolicyTests, Lifetime (+5 more)

### Community 1 - "InMemoryScimGroupRepository"
Cohesion: 0.16
Nodes (4): InMemoryScimGroupRepository, Override, Override, Result

### Community 2 - "IdentityAdministrationServiceTests"
Cohesion: 0.09
Nodes (3): IdentityAdministrationService, DirectGroup, IdentityAdministrationServiceTests

### Community 3 - "ScimUserResource"
Cohesion: 0.16
Nodes (6): ScimListedResource, ScimUserListing, ScimUserResource, ScimUserRenderer, ScimGroupReference, datetimeformatter

### Community 4 - "connectors.tsx"
Cohesion: 0.11
Nodes (26): Connector, connectorPath(), CONNECTORS_PATH, ConnectorToken, decodeJson(), DirectGroup, formatDate(), formatInstant() (+18 more)

### Community 6 - ".of"
Cohesion: 0.14
Nodes (5): NewScimGroup, AddMembers, RemoveMembers, ScimGroupReplacement, ScimGroupServiceTests

### Community 7 - ".toDomain"
Cohesion: 0.09
Nodes (3): ScimLoginStateValue, ScimUserEmailValue, ScimUserEntity

### Community 8 - ".changePassword"
Cohesion: 0.08
Nodes (3): Override, PasswordChangeServiceTests, ScimUserSessionRevocationTests

### Community 9 - "Attribute"
Cohesion: 0.04
Nodes (54): And, Attribute, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE (+46 more)

### Community 10 - "AuditTrail"
Cohesion: 0.06
Nodes (49): Architecture constraints, AuditTrail, LoggingOperationalAlerts, AfterCommit, DormantAuthorityRevocationService, InactivityDeactivationService, LoginAttemptService, LoginIdentityService (+41 more)

### Community 11 - "SecurityConfig"
Cohesion: 0.10
Nodes (7): SecurityConfig, CountingPasswordEncoder, Override, CountingPasswordEncoder, Override, RefusalTimingEquivalenceTests, SecurityConfigPasswordEncoderTests

### Community 12 - "ScimUserServiceTests"
Cohesion: 0.09
Nodes (8): NewScimUser, ScimUserReplacement, Revocation, ScimUserServiceTests, Override, InMemoryScimTombstoneRepository, Override, Tombstone

### Community 13 - "ScimGroupProvisioningIntegrationTests"
Cohesion: 0.07
Nodes (6): Alias, InMemoryScimExternalIdRepository, Override, ScimConditionalWriteIntegrationTests, ScimGroupProvisioningIntegrationTests, org.springframework.test.web.servlet.MvcResult

### Community 14 - "org.springframework.data.jpa.repository.Query"
Cohesion: 0.12
Nodes (12): UserCounterJpaRepository, ScimGroupMemberJpaRepository, ScimGroupMemberRow, ScimResourceJpaRepository, ScimUserJpaRepository, collection, lockmodetype, org.springframework.data.jpa.repository.JpaRepository (+4 more)

### Community 16 - "org.junit.jupiter.params.provider.ValueSource"
Cohesion: 0.05
Nodes (11): ScimQueryRequestTests, LockoutPolicyTests, ScimUserProfileTests, ScimVersionPreconditionTests, ScimWriteScopeRuleTests, SpaRoutesScimNamespaceTests, ReservedServerPaths, SpaRoutesTests (+3 more)

### Community 17 - "org.junit.jupiter.api.BeforeEach"
Cohesion: 0.09
Nodes (82): arraynode, autowired, RequestIdFilter, ConnectorAdministrationService, AuditListingIntegrationTests, Seeded, DormancyTestClockConfiguration, SelfReadIntegrationTests (+74 more)

### Community 18 - "ScimQuery"
Cohesion: 0.11
Nodes (8): Hit, Result, ScimQuery, ScimSort, Override, Result, ScimSearchServiceTests, ScimSortTests

### Community 20 - "AuthenticatedConnector"
Cohesion: 0.12
Nodes (5): ScimGroupListing, ScimGroupResource, ScimGroupController, ScimGroupRenderer, AuthenticatedConnector

### Community 21 - "AuditTrailServiceTests.java"
Cohesion: 0.12
Nodes (14): AuditRefusalReason, ACCOUNT_DISABLED, ACCOUNT_LOCKED, BAD_CREDENTIALS, OTHER, UNKNOWN_ACCOUNT, AccountSessionsAdapter, SeededBootstrapAdminTestConfiguration (+6 more)

### Community 22 - "UserCounter"
Cohesion: 0.09
Nodes (7): UserCounterService, UserCounter, UserCounterRepository, UserCounterEntity, Override, UserCounterPersistenceAdapter, UserCounterTests

### Community 23 - "AuditAppendOnlyIntegrationTests"
Cohesion: 0.09
Nodes (3): AuditAppendOnlyIntegrationTests, AuditEventRecordingIntegrationTests, ResultActions

### Community 24 - "ScimQueryVocabulary.java"
Cohesion: 0.04
Nodes (77): active, ScimFilterPath, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE (+69 more)

### Community 25 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.16
Nodes (4): AuditTrailService, Override, Override, org.springframework.transaction.annotation.Transactional

### Community 26 - "ScimGroupController.java"
Cohesion: 0.07
Nodes (34): authenticationprincipal, AdminAuditController, InvalidAuditQueryException, CurrentPasswordRejectedException, ForbiddenIdentityChangeException, UnknownIdentityException, UnknownSessionIdentityException, UnsafeIdentityChangeException (+26 more)

### Community 27 - ".given"
Cohesion: 0.09
Nodes (6): Override, Group, SelfRecord, LoginIdentityServiceTests, SelfReadServiceTests, org.springframework.security.core.userdetails.UserDetails

### Community 28 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.10
Nodes (6): ScimPageRequestTests, ScimQueryProtocolIntegrationTests, org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.Arguments, org.junit.jupiter.params.provider.CsvSource, org.junit.jupiter.params.provider.MethodSource

### Community 30 - "ScimUserPatchOperationTests"
Cohesion: 0.13
Nodes (8): Condition, ScimEmailFilter, AddEmails, EmailUpdate, RemoveEmailPart, RemoveEmails, UpdateEmails, ScimUserPatchOperationTests

### Community 31 - "RecordingAuditTrail"
Cohesion: 0.12
Nodes (8): AuditScimRefusal, INVALID_VALUE, MUTABILITY, NO_TARGET, UNIQUENESS, Override, Recorded, RecordingAuditTrail

### Community 33 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.08
Nodes (20): AbsoluteSessionLifetimeFilter, Override, AbsoluteSessionLifetimePolicy, Override, ConnectorAuthenticationService, Override, ScimBearerAuthenticationFilter, ScimBearerChallenge (+12 more)

### Community 37 - "ConnectorTokenSecretTests"
Cohesion: 0.12
Nodes (5): ConnectorTokenDigest, Override, Minted, Presented, ConnectorTokenSecretTests

### Community 38 - "jakarta.persistence.Entity"
Cohesion: 0.09
Nodes (23): ScimGroupMemberEntity, Override, ScimGroupMemberId, ScimPasswordHistoryEntity, cascadetype, collectiontable, column, elementcollection (+15 more)

### Community 40 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.11
Nodes (18): Architecture, AWS CloudFormation Deployment Guide, Can't Access via ALB, Can't Retrieve SSH Key, CloudFormation Creates Key (Recommended), Database Connection Error, Deploy Application, Deployment (+10 more)

### Community 41 - "SecurityConfig.java"
Cohesion: 0.06
Nodes (34): anonymousauthenticationfilter, argon2passwordencoder, authenticationentrypoint, authorizationfilter, SCIM release gate, ScimReleaseGate, ScimReleaseGateFilter, ScimSecurityConfig (+26 more)

### Community 42 - "ScimUserPatchReaderTests.java"
Cohesion: 0.11
Nodes (33): addemails, ScimName, MergeName, NamePart, FAMILY_NAME, FORMATTED, GIVEN_NAME, HONORIFIC_PREFIX (+25 more)

### Community 43 - "ScimResourceType"
Cohesion: 0.15
Nodes (5): ScimResourceType, GROUP, USER, ScimQuerySql, Override

### Community 46 - "AuditOperation"
Cohesion: 0.06
Nodes (30): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+22 more)

### Community 47 - "IdentitySummary"
Cohesion: 0.12
Nodes (11): GroupSummary, IdentitySummary, AdminAccountControllerTests, Override, RecordingService, deletemapping, java.lang.reflect.Method, modifier (+3 more)

### Community 48 - "SeededIdentity"
Cohesion: 0.29
Nodes (4): SeededIdentity, ScimSeedConfig, org.springframework.boot.ApplicationRunner, seededidentity

### Community 49 - "AuditEventQuery"
Cohesion: 0.09
Nodes (13): AuditEventListingService, AuditEventPage, AuditEventQuery, AuditEventReader, AuditEventReadAdapter, Override, AuditEventListingServiceTests, AdminAuditControllerTests (+5 more)

### Community 51 - "compilerOptions"
Cohesion: 0.09
Nodes (21): compilerOptions, allowImportingTsExtensions, baseUrl, isolatedModules, jsx, lib, module, moduleDetection (+13 more)

### Community 52 - "tools.jackson.databind.JsonNode"
Cohesion: 0.23
Nodes (7): RemoveAllMembers, ReplaceMembers, ScimGroupPatchOperation, SetDisplayName, ScimGroupRequestReader, java.util.regex.Pattern, tools.jackson.databind.JsonNode

### Community 54 - "ScimFilterParser"
Cohesion: 0.19
Nodes (4): ResolvedPath, ScimFilterParser, Search, filtering, sorting and projection, Filtering

### Community 55 - "ScimUserProfile"
Cohesion: 0.11
Nodes (6): Override, ScimPasswordChange, ScimUserEdit, Override, SetPassword, ScimUserProfile

### Community 56 - "AuthControllerTests"
Cohesion: 0.09
Nodes (13): LoginOutcome, PasswordPolicyViolationException, AuthController, ChangePasswordRequest, Override, LoginRequest, PasswordRuleViolation, UserResponse (+5 more)

### Community 57 - "ScimQuerySql.java"
Cohesion: 0.12
Nodes (28): and, And, AttributeRef, Comparison, Not, Operator, CO, EQ (+20 more)

### Community 58 - "Attribute"
Cohesion: 0.11
Nodes (14): Kind, CLEAR, SET, UNCHANGED, Attribute, Kind, BOOLEAN, COMPLEX (+6 more)

### Community 60 - "devDependencies"
Cohesion: 0.07
Nodes (29): devDependencies, dependency-cruiser, eslint, @eslint/js, eslint-plugin-react-hooks, eslint-plugin-react-refresh, fallow, globals (+21 more)

### Community 61 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 64 - "AdminConnectorController"
Cohesion: 0.12
Nodes (9): ConnectorSummary, ConnectorTokenSummary, IssuedConnectorToken, UnknownConnectorException, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest, RotateTokenRequest (+1 more)

### Community 66 - "App.tsx"
Cohesion: 0.10
Nodes (33): Architecture, Backend contract, Routing, Why `auth/` is its own folder and not a page, AuthRole, AuthProvider(), AuthStatus, useAuth() (+25 more)

### Community 67 - "ScimVersionPrecondition"
Cohesion: 0.18
Nodes (7): Cause, ADMIN_MEMBERSHIP_REMOVED, DEACTIVATED, DELETED, PASSWORD_CHANGED, USER_NAME_CHANGED, ScimVersionPrecondition

### Community 68 - "AuditTrailService.java"
Cohesion: 0.11
Nodes (15): AuditEvent, AuditEventRepository, AuditGroupAttribute, DISPLAY_NAME, MEMBERS, AuditOutcome, FAILURE, SUCCESS (+7 more)

### Community 69 - "ScimRequestObservationConventionTests"
Cohesion: 0.16
Nodes (7): Override, ScimRequestObservationConvention, ScimRequestObservationConventionTests, io.micrometer.common.KeyValues, keyvalue, org.springframework.http.server.observation.DefaultServerRequestObservationConvention, org.springframework.http.server.observation.ServerRequestObservationContext

### Community 70 - "ScimConnector"
Cohesion: 0.19
Nodes (4): ScimConnector, ConnectorAuthenticationServiceTests, InMemoryScimConnectorRepository, Override

### Community 71 - "apiFetch"
Cohesion: 0.10
Nodes (24): Why every request goes through `lib/http.ts`, Unit-testing a module that calls the API, decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN (+16 more)

### Community 72 - "ScimUserPatchReader"
Cohesion: 0.22
Nodes (6): Op, ADD, REMOVE, REPLACE, Path, ScimUserPatchReader

### Community 73 - "LogContextTests"
Cohesion: 0.19
Nodes (4): Override, LogContext, Scope, LogContextTests

### Community 74 - "ScimConnectorTokenEntity"
Cohesion: 0.15
Nodes (4): ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 75 - "ScimExternalIdEntity"
Cohesion: 0.13
Nodes (8): Override, Key, ScimExternalIdEntity, ScimExternalIdJpaRepository, Override, ScimExternalIdPersistenceAdapter, jakarta.persistence.IdClass, ScimExternalIdEntity.Key

### Community 76 - ".get"
Cohesion: 0.13
Nodes (3): SelfControllerTests, org.junit.jupiter.api.AfterEach, ScimDiscoveryIntegrationTests

### Community 77 - "AuditUserAttribute"
Cohesion: 0.14
Nodes (11): AuditUserAttribute, ACTIVE, DISPLAY_NAME, EMAILS, GROUPS, LOCALE, NAME, PASSWORD (+3 more)

### Community 78 - ".handle"
Cohesion: 0.13
Nodes (7): ScimErrorException, ScimExceptionHandler, InvalidPreconditionException, PreconditionFailedException, PreconditionRequiredException, org.springframework.http.HttpStatus, org.springframework.web.bind.annotation.RestControllerAdvice

### Community 79 - "ScimUser"
Cohesion: 0.06
Nodes (39): arrays, assertthatnoexception, atomicinteger, authentication, authenticationexception, authenticationmanager, DuplicateUserNameException, LockoutPolicy (+31 more)

### Community 80 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.10
Nodes (21): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Error contract (+13 more)

### Community 81 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 82 - ".require"
Cohesion: 0.09
Nodes (3): LoginAttemptServiceTests, LoginLockoutTests, org.springframework.security.core.AuthenticationException

### Community 84 - "InactivityGovernanceIntegrationTests"
Cohesion: 0.08
Nodes (8): DormancyRun, InactivityDeactivationServiceTests, InactivityGovernanceIntegrationTests, ThrowingRunnable, Override, InMemoryScheduledJobLock, Override, FunctionalInterface

### Community 86 - "scripts"
Cohesion: 0.10
Nodes (20): scripts, analyze, build, dev, format, format:check, lint, preview (+12 more)

### Community 87 - "ScimUserAttributesTests"
Cohesion: 0.05
Nodes (8): ScimDiscovery, ScimGroupAttributes, Attribute, ScimUserAttributes, ScimDiscoveryTests, SuppressWarnings, ScimUserAttributesTests, Attribute projection

### Community 89 - "Rule"
Cohesion: 0.25
Nodes (6): Rule, CONTAINS_USER_NAME, REUSED, TOO_LONG, TOO_SHORT, Semgrep

### Community 90 - "IndexedSessions"
Cohesion: 0.28
Nodes (5): Override, AccountSessionsAdapterTests, IndexedSessions, Override, org.springframework.session.MapSession

### Community 91 - "SpaFrontendTests"
Cohesion: 0.23
Nodes (3): Override, SpaFrontendTests, org.springframework.web.servlet.ModelAndView

### Community 93 - "ScimConnectorToken"
Cohesion: 0.19
Nodes (4): ScimConnectorToken, ScimConnectorTokenTests, InMemoryScimConnectorTokenRepository, Override

### Community 94 - "components.json"
Cohesion: 0.11
Nodes (17): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+9 more)

### Community 95 - "auth.helpers.ts"
Cohesion: 0.19
Nodes (12): Forcing a refused request, ADMIN_CREDENTIALS, captureSessionCookie(), csrfHeader(), expireSession(), login(), loginAs(), postAdminAction() (+4 more)

### Community 96 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 97 - "ScheduledJobMetrics"
Cohesion: 0.20
Nodes (7): atomiclong, ScheduledJobMetrics, ScheduledJobMetricsTests, io.micrometer.core.instrument.Counter, io.micrometer.core.instrument.MeterRegistry, io.micrometer.core.instrument.simple.SimpleMeterRegistry, timegauge

### Community 99 - "accounts.tsx"
Cohesion: 0.13
Nodes (27): Component library, Component library, Button(), ButtonProps, buttonVariants, Card(), CardContent(), CardDescription() (+19 more)

### Community 100 - "lib.sh"
Cohesion: 0.24
Nodes (14): die(), load_backend_env(), log(), pinned_version(), port_holder(), require_cmd(), require_docker(), require_maven() (+6 more)

### Community 101 - "org.springframework.mock.web.MockHttpServletRequest"
Cohesion: 0.28
Nodes (4): HttpAuditRequestContext, Override, HttpAuditRequestContextTests, org.springframework.mock.web.MockHttpServletRequest

### Community 103 - "AuditListingEndToEndIntegrationTests"
Cohesion: 0.18
Nodes (4): AuditListingEndToEndIntegrationTests, ClockedSession, Override, jakarta.servlet.ServletContext

### Community 104 - ".of"
Cohesion: 0.18
Nodes (4): Attribute, ScimAuditFilterShapes, ScimAuditFilterShapesTests, org.junit.jupiter.params.provider.EnumSource

### Community 105 - "ScimExceptionHandlerMetricTests.java"
Cohesion: 0.16
Nodes (10): AuditRequest, MetricTag, InvalidScimFilterException, ScimExceptionHandlerMetricTests, handlermapping, mockhttpservletresponse, requestcontextholder, serverhttpobservationfilter (+2 more)

### Community 106 - "stryker.config.json"
Cohesion: 0.07
Nodes (26): cleanTempDir, clearTextReporter, allowColor, maxTestsToLog, _comment_mutate, concurrency, coverageAnalysis, htmlReporter (+18 more)

### Community 107 - "Backend"
Cohesion: 0.14
Nodes (13): Audit trail database roles, Audit trail retention, Backend, Build and test, Bundle a frontend, Configuration, Inactivity governance, Log in (+5 more)

### Community 108 - "SessionController"
Cohesion: 0.33
Nodes (5): SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession

### Community 109 - "EcsLogCapture"
Cohesion: 0.18
Nodes (7): EcsLogCapture, Override, bytearrayoutputstream, ch.qos.logback.classic.LoggerContext, ch.qos.logback.core.OutputStreamAppender, org.springframework.core.env.Environment, structuredlogencoder

### Community 112 - "The layers"
Cohesion: 0.11
Nodes (16): BackendApplication, Architecture, Dev-server reloads, Styling and the token pipeline, The `@/` alias, The layers, What is deliberately absent, Why `components/ui/` is fenced off (+8 more)

### Community 113 - "auth-context-value.ts"
Cohesion: 0.11
Nodes (23): AuthUser, changePassword(), classifyRejection(), decodeRuleMessage(), PasswordChangeOutcome, UserResponse, api, CONFINED (+15 more)

### Community 114 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 115 - "AGENTS.md"
Cohesion: 0.15
Nodes (11): Agent, Build and validation, Documentation, Environment, Frontend/backend integration, graphify, Ignore rules, Layout (+3 more)

### Community 118 - "ScimGroup"
Cohesion: 0.10
Nodes (4): DuplicateDisplayNameException, ScimGroup, Override, ScimGroupPersistenceAdapter

### Community 119 - "AuditRetentionService"
Cohesion: 0.23
Nodes (5): AuditRetentionService, AuditEventRetention, AuditRetentionServiceTests, CountingRetention, Override

### Community 120 - "deploy.sh"
Cohesion: 0.42
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 121 - "ScimConnectorEntity"
Cohesion: 0.23
Nodes (4): ScimConnectorEntity, ScimConnectorJpaRepository, Override, ScimConnectorPersistenceAdapter

### Community 122 - "ScimResourceEntity"
Cohesion: 0.23
Nodes (3): ScimGroupEntity, ScimResourceEntity, ScimGroupJpaRepository

### Community 123 - "org.springframework.context.annotation.Bean"
Cohesion: 0.11
Nodes (13): DormancyPolicyConfig, LoginLockoutConfig, SessionRegistryConfiguration, SessionRegistryConfiguration, SessionRegistryConfiguration, chronounit, org.springframework.boot.testcontainers.service.connection.ServiceConnection, org.springframework.context.annotation.Bean (+5 more)

### Community 124 - "AGENTS.md — frontend"
Cohesion: 0.22
Nodes (8): AGENTS.md — frontend, Baseline gate, Commands, Conditional gates, Fallow, Semgrep, Testing, TypeScript

### Community 125 - "AuditPasswordChangeRefusal"
Cohesion: 0.22
Nodes (8): AuditPasswordChangeRefusal, ACCOUNT_DISABLED, ACCOUNT_LOCKED, BAD_CURRENT_PASSWORD, CONTAINS_USER_NAME, REUSED, TOO_LONG, TOO_SHORT

### Community 126 - "ScimSeedIntegrationTests"
Cohesion: 0.23
Nodes (4): ScimSeedLock, ScimSeedLockAdapter, ScimSeedIntegrationTests, propagation

### Community 127 - "org.springframework.context.annotation.Configuration"
Cohesion: 0.24
Nodes (9): AuditRetentionScheduleConfig, Override, Override, crontask, crontrigger, org.springframework.context.annotation.Configuration, org.springframework.scheduling.annotation.EnableScheduling, org.springframework.scheduling.annotation.SchedulingConfigurer (+1 more)

### Community 129 - "ScimAttributeProjection"
Cohesion: 0.16
Nodes (7): SuppressWarnings, Kind, GROUP, USER, ScimAttributeProjection, Search, ScimSearchRenderer

### Community 130 - "DormancyPolicyStartupTests.java"
Cohesion: 0.16
Nodes (6): applicationconversionservice, AuditRetentionStartupTests, DormancyPolicyStartupTests, org.springframework.boot.test.context.runner.ApplicationContextRunner, propertysourcesplaceholderconfigurer, systemenvironmentpropertysource

### Community 131 - "RFC requirements and implications"
Cohesion: 0.15
Nodes (15): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+7 more)

### Community 132 - "package.json"
Cohesion: 0.05
Nodes (43): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge, engines (+35 more)

### Community 134 - "ManagementPortIntegrationTests.java"
Cohesion: 0.20
Nodes (10): Builder, ManagementPortIntegrationTests, cookiepolicy, httpcookie, httprequest, java.net.http.HttpClient, localmanagementport, localserverport (+2 more)

### Community 135 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 136 - "accounts-admin.spec.ts"
Cohesion: 0.23
Nodes (11): anonymousApi(), cleanUp(), deprovision(), groupsTable(), openAccounts(), RUN, scimApi(), settle() (+3 more)

### Community 138 - "java.security.Principal"
Cohesion: 0.19
Nodes (6): CountResponse, UserCounterController, Override, RecordingCounterService, UserCounterControllerTests, java.security.Principal

### Community 139 - "change-password.spec.ts"
Cohesion: 0.21
Nodes (8): adminRequest(), anonymousApi(), deprovision(), expectSignedOutThenSignIn(), provision(), Provisioned, RUN, scimApi()

### Community 140 - "Delivery plan"
Cohesion: 0.22
Nodes (9): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 0a — Permanent lockout, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 5 — Complete query protocol, Slice 6 — Operational Accounts page and audit (+1 more)

### Community 141 - "front-end"
Cohesion: 0.22
Nodes (8): Backend contract, front-end, Project structure, Scripts, Setup, Styling, Technology stack, Testing

### Community 142 - "AGENTS.md — backend"
Cohesion: 0.25
Nodes (7): AGENTS.md — backend, API contract, Baseline gate, Conditional gate: mutation testing, Reading a gate's result, Security-sensitive changes, Verification

### Community 143 - "org.springframework.http.ResponseEntity"
Cohesion: 0.23
Nodes (3): ScimDiscoveryController, ScimUserController, org.springframework.http.ResponseEntity

### Community 145 - "SCIM 2.0 account-management research"
Cohesion: 0.22
Nodes (7): Executive finding, Existing application seams, Primary sources, Recommended implementation order, Resolved RFC decisions, SCIM 2.0 account-management research, Testing strategy

### Community 146 - "Credential and cryptographic policy"
Cohesion: 0.22
Nodes (9): Credential and cryptographic policy, Hashing and primitives, Key rotation and storage, Lockout policy, Log formatting, Password policy, Response headers, Session lifetime (+1 more)

### Community 147 - "ScimPatchRefusedException"
Cohesion: 0.36
Nodes (4): Reason, MUTABILITY, NO_TARGET, ScimPatchRefusedException

### Community 148 - "ManagementSessionConfiguration.java"
Cohesion: 0.19
Nodes (12): abstracthttpsessionapplicationinitializer, ManagementSessionConfiguration, Alerts, Keep it off the internet, Operational telemetry, Scraping, Who can read it, managementporttype (+4 more)

### Community 149 - ".sampleTraffic"
Cohesion: 0.29
Nodes (4): Builder, Session, java.net.CookieManager, java.net.http.HttpResponse

### Community 150 - "AfterCommitAdapterTests"
Cohesion: 0.17
Nodes (9): AfterCommitAdapter, Override, AfterCommitAdapterTests, 2. Revoke a disabled account's sessions after the commit, Alternatives considered, Consequences, Context, Decision (+1 more)

### Community 151 - "Quick Start"
Cohesion: 0.25
Nodes (6): 1. Get VPC Info, 2. Deploy, 3. Get Private Key (if CloudFormation created it), 4. Deploy Application, 5. Test, Quick Start

### Community 152 - "monorepo-base"
Cohesion: 0.25
Nodes (7): Deploying to AWS, Frontend/backend integration, Layout, monorepo-base, Toolchain pins, Working on the backend, Working on the frontend

### Community 153 - "Testing Guide"
Cohesion: 0.18
Nodes (10): Architecture tests, Assert exactly, not loosely, Coverage excludes — why the list is explicit, Fallow, Mutation testing, Prove a new rule fails, Running the suites, Testing Guide (+2 more)

### Community 154 - "assertthat"
Cohesion: 0.05
Nodes (32): assertthat, assertthatcode, assertthatthrownby, atomicreference, attributeref, ConnectorTokenPolicy, ConnectorTokenScope, READ_ONLY (+24 more)

### Community 155 - "AuditRetentionPolicy"
Cohesion: 0.29
Nodes (3): AuditRetentionPolicyConfig, AuditRetentionPolicy, AuditRetentionPolicyTests

### Community 156 - "AuditAdministrativeRefusal"
Cohesion: 0.25
Nodes (6): AuditAdministrativeRefusal, CREDENTIALLESS_TARGET, LAST_ENABLED_ADMINISTRATOR, PROTECTED_RESOURCE, SELF_DISABLE, SELF_TARGET

### Community 157 - "ScimPasswordHistoryPersistenceAdapter"
Cohesion: 0.48
Nodes (3): ScimPasswordHistoryJpaRepository, Override, ScimPasswordHistoryPersistenceAdapter

### Community 158 - "CONTEXT"
Cohesion: 0.29
Nodes (6): Accounts and identity provisioning, CONTEXT, Current account model, Request paths, SCIM target model, Sessions

### Community 159 - "Common Tasks"
Cohesion: 0.29
Nodes (7): Check ALB Target Health, Common Tasks, Delete Stack, Inspect the Stack, Restart Application, Update Application, View Logs

### Community 160 - "4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal"
Cohesion: 0.25
Nodes (6): 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal, Alternatives considered, Consequences, Context, Decision, Status

### Community 161 - "org.springframework.stereotype.Component"
Cohesion: 0.32
Nodes (6): SpaErrorViewResolver, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.stereotype.Component, requestdispatcher, transactionsynchronization, transactionsynchronizationmanager

### Community 162 - "5. Serialize scheduled jobs on per-job lock rows"
Cohesion: 0.29
Nodes (6): 5. Serialize scheduled jobs on per-job lock rows, Alternatives considered, Consequences, Context, Decision, Status

### Community 163 - "AuditEventReadAdapter.java"
Cohesion: 0.14
Nodes (10): ScimQueryPersistenceAdapter, BackendApplicationTests, classmode, java.sql.Timestamp, javax.sql.DataSource, org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate, org.springframework.test.annotation.DirtiesContext, sqlexception (+2 more)

### Community 164 - "RecordingTransactionManager"
Cohesion: 0.43
Nodes (4): Override, RecordingTransactionManager, org.springframework.transaction.TransactionDefinition, org.springframework.transaction.TransactionStatus

### Community 165 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 166 - "CapturedLog"
Cohesion: 0.18
Nodes (7): CapturedLog, Override, ch.qos.logback.classic.Logger, ch.qos.logback.classic.spi.ILoggingEvent, ch.qos.logback.core.read.ListAppender, function, keyvaluepair

### Community 167 - "Domain and authority model"
Cohesion: 0.25
Nodes (8): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, Protected recovery resources, SCIM User replaces Account

### Community 168 - "Domain Docs"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 169 - "Issue tracker: GitHub"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 170 - "Query contract"
Cohesion: 0.50
Nodes (4): Pagination, POST search, Query contract, Sorting

### Community 171 - "CountingPasswordEncoder"
Cohesion: 0.33
Nodes (3): CountingPasswordEncoder, Override, RecordingSeedLock

### Community 172 - "Definition of Done"
Cohesion: 0.40
Nodes (5): Backend gates, Contract and behavior, Definition of Done, Documentation and graph, Frontend gates

### Community 173 - "Write semantics"
Cohesion: 0.40
Nodes (5): DELETE and tombstones, PATCH, POST, PUT, Write semantics

### Community 174 - "1. Count login attempts on the login path"
Cohesion: 0.29
Nodes (6): 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision, Status

### Community 175 - "cleanup.sh"
Cohesion: 0.70
Nodes (4): print_error(), print_info(), print_warn(), cleanup.sh script

### Community 176 - "get-vpc-info.sh"
Cohesion: 0.70
Nodes (4): print_header(), print_info(), print_warn(), get-vpc-info.sh script

### Community 177 - "Kiro: graphify enforcement"
Cohesion: 0.40
Nodes (4): graphify-runner, Kiro: graphify enforcement, The hooks, When the refresh fails

### Community 178 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 179 - "E2E"
Cohesion: 0.29
Nodes (6): Calling the API from a spec, E2E, Flakiness — the rules that keep these tests green, Routing a new spec, Sharing backend state under `fullyParallel`, resetCounterViaApi()

### Community 180 - "Graphify Runner"
Cohesion: 0.50
Nodes (3): Graphify Runner, Reporting, Steps

### Community 181 - "3. ECS-structured logging with redaction enforced structurally"
Cohesion: 0.33
Nodes (5): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status

### Community 184 - "ScimEmailPart"
Cohesion: 0.40
Nodes (4): ScimEmailPart, PRIMARY, TYPE, VALUE

### Community 191 - "ConnectorTokenDigest.java"
Cohesion: 0.50
Nodes (3): java.security.MessageDigest, nosuchalgorithmexception, standardcharsets

### Community 221 - "uuid"
Cohesion: 0.05
Nodes (25): arraylist, ScimSearchListing, PasswordHistoryPolicy, ScimConnectorRepository, ScimConnectorTokenRepository, ScimPageRequest, UnknownGroupMemberException, ScimTombstonePersistenceAdapter (+17 more)

### Community 233 - "accounts.test.tsx"
Cohesion: 0.08
Nodes (10): CONFINED, GroupRow, UserRow, apiFetchMock, auth, Result, apiFetchMock, auth (+2 more)

## Knowledge Gaps
- **537 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+532 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 945 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **49 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Why `auth/` is its own folder and not a page` connect `App.tsx` to `The layers`, `InactivityGovernanceIntegrationTests`, `apiFetch`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Why does `ScimUser` connect `ScimUser` to `.isProtectedFromWrites`, `InMemoryScimGroupRepository`, `IdentityAdministrationServiceTests`, `ScimUserResource`, `.advanceBy`, `.of`, `.toDomain`, `.changePassword`, `AuditTrail`, `SecurityConfig`, `ScimUserServiceTests`, `org.junit.jupiter.api.BeforeEach`, `ScimQuery`, `AuthenticatedConnector`, `AuditTrailServiceTests.java`, `UserCounter`, `AuditAppendOnlyIntegrationTests`, `assertthat`, `.given`, `.seed`, `.created`, `SeededIdentity`, `ScimUserProfile`, `ScimVersionPrecondition`, `.get`, `.require`, `AdminAccountEndpointTests`, `InactivityGovernanceIntegrationTests`, `.of`, `uuid`, `ScimUserPersistenceAdapter`, `ScimGroup`?**
  _High betweenness centrality (0.034) - this node is a cross-community bridge._
- **Why does `Architecture` connect `App.tsx` to `accounts.tsx`, `AGENTS.md — frontend`, `apiFetch`, `InactivityGovernanceIntegrationTests`?**
  _High betweenness centrality (0.032) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _537 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.junit.jupiter.api.Test` be split into smaller, more focused modules?**
  _Cohesion score 0.04122861265718408 - nodes in this community are weakly interconnected._
- **Should `IdentityAdministrationServiceTests` be split into smaller, more focused modules?**
  _Cohesion score 0.09325396825396826 - nodes in this community are weakly interconnected._
- **Should `connectors.tsx` be split into smaller, more focused modules?**
  _Cohesion score 0.10793650793650794 - nodes in this community are weakly interconnected._