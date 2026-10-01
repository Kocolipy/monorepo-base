# Graph Report - monorepo-base-audit-listing  (2026-09-30)

## Corpus Check
- 460 files · ~296,912 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 4826 nodes · 16215 edges · 227 communities (169 shown, 58 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 2189 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a6a888a5`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.junit.jupiter.api.Test
- ScimGroup
- IdentityAdministrationServiceTests
- ScimUserService
- DormancyPolicy
- .advanceBy
- .of
- ScimUser
- .require
- Attribute
- MutableClock
- org.springframework.context.annotation.Bean
- ScimUserServiceTests
- ScimGroupProvisioningIntegrationTests
- org.springframework.data.jpa.repository.Query
- ScimAttributeProjectionTests
- org.junit.jupiter.params.ParameterizedTest
- org.springframework.boot.test.context.SpringBootTest
- ScimQuery
- AuditTrail
- ScimGroupService
- AuditTrailServiceTests.java
- UserCounter
- AuditAppendOnlyIntegrationTests
- ScimFilterPath
- org.springframework.transaction.annotation.Transactional
- org.springframework.web.bind.annotation.RestController
- .given
- ScimQueryProtocolIntegrationTests
- ScimFilterParserTests
- ScimUserPatchOperation
- RecordingAuditTrail
- OperationalTelemetryIntegrationTests
- jakarta.servlet.http.HttpServletRequest
- ScimConditionalWriteIntegrationTests
- ScimDeletionIntegrationTests
- .invalidValue
- ConnectorTokenSecretTests
- .toDomain
- PasswordChangeLifecycleIntegrationTests
- AWS CloudFormation Deployment Guide
- .the_string_form_says_whether_a_password_was_sent_and_never_what_it_was
- ScimEmailFilter
- ScimResourceType
- .seed
- .created
- AuditOperation
- IdentitySummary
- ScimConnectorToken
- AuditEventQuery
- ScimConnectorLifecycleIntegrationTests
- compilerOptions
- tools.jackson.databind.JsonNode
- .readCreate
- ScimFilterParser
- ScimUserEdit
- AuthControllerTests
- AttributeRef
- Attribute
- ScimUserProvisioningIntegrationTests
- devDependencies
- Workflow
- org.junit.jupiter.api.BeforeEach
- ScimEmail
- AuditEventRecordingIntegrationTests
- ScimBearerAuthenticationFilterTests
- App.tsx
- ScimUserRepository
- jakarta.persistence.Entity
- ScimRequestObservationConventionTests
- ScimConnector
- http.ts
- org.springframework.stereotype.Service
- LogContextTests
- ScimConnectorTokenEntity
- org.springframework.stereotype.Repository
- .get
- AuditUserAttribute
- .handle
- ReservedResourceName
- SCIM 2.0 account-management specification plan
- compilerOptions
- AdminConnectorController
- .sessionsOf
- InactivityGovernanceIntegrationTests
- .of
- scripts
- ScimDiscovery
- ScimUserPatchReaderTests
- Testing Guide
- IndexedSessions
- SpaFrontendTests
- AuditEventEntity
- RefusalTimingEquivalenceTests
- components.json
- auth.helpers.ts
- include
- ScheduledJobMetricsTests
- accounts.tsx
- showcase.tsx
- lib.sh
- org.springframework.mock.web.MockHttpServletRequest
- MetricTagTests
- AuditListingEndToEndIntegrationTests
- ScimConnectorEntity
- ScimUserAttributesTests
- stryker.config.json
- Backend
- SessionController
- AuditRetentionService
- org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
- FakeSaltedEncoder
- Architecture
- auth-context-value.ts
- sources.ts
- AGENTS.md
- AbsoluteSessionLifetimePolicy
- EcsLogCapture
- ScimGroupPersistenceAdapter
- dependencies
- deploy.sh
- .deactivateOverdueUsers
- org.springframework.http.ResponseEntity
- AuditRetentionServiceTests
- AGENTS.md — frontend
- .changePassword
- org.junit.jupiter.api.AfterEach
- ConnectorAuthenticationServiceTests
- SelfReadIntegrationTests
- ScimConnectorTokenTests
- AuditRetentionPolicy
- RFC requirements and implications
- package.json
- showcase.test.tsx
- mutate
- mvnw
- Operator
- .requiresWriteScope
- org.springframework.web.bind.annotation.PostMapping
- RequestIdFilterTests
- Delivery plan
- front-end
- AGENTS.md — backend
- AuthenticatedConnector
- ScimBearerAuthenticationFilter.java
- SCIM 2.0 account-management research
- Credential and cryptographic policy
- ignorePatterns
- ManagementSessionConfiguration.java
- .summarize
- AfterCommitAdapterTests
- Domain and authority model
- monorepo-base
- ScimGroupMemberId
- Cause
- MockFilterChain
- org.junit.jupiter.params.provider.CsvSource
- CapturedLog
- CONTEXT
- 1. Count login attempts on the login path
- 2. Revoke a disabled account's sessions after the commit
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- 5. Serialize scheduled jobs on per-job lock rows
- ScimUserProfileTests
- CountingPasswordEncoder
- ArchitectureTest
- EcsLogFormatTests
- 3. ECS-structured logging with redaction enforced structurally
- Domain Docs
- Issue tracker: GitHub
- Query contract
- .currentUser
- Definition of Done
- Write semantics
- reporters
- cleanup.sh
- get-vpc-info.sh
- Kiro: graphify enforcement
- dev-stop.sh
- BackendApplication
- Graphify Runner
- thresholds
- ScimSchemas
- Supported schemas
- clearTextReporter
- _comment_mutate
- dev.sh
- integration-test.sh
- semgrep.sh
- verify.sh
- CLAUDE.md
- eslint-plugin-react-refresh
- globals
- @playwright/test
- @tailwindcss/vite
- @testing-library/jest-dom
- @testing-library/react
- @testing-library/user-event
- @types/node
- @types/react
- typescript
- typescript-eslint
- vite-plugin-compression2
- vitest
- @vitest/coverage-v8
- @vitest/ui
- prettier.config.mjs
- graphify-guard.sh
- graphify-refresh.sh
- bootstrap.sh
- package.sh
- com.example:backend
- UnsafeIdentityChangeException
- AuthController
- ScimPasswordChange
- RecordingTransactionManager
- ScimQueryPersistenceAdapter.java
- ScimAuditFilterShapesTests

## God Nodes (most connected - your core abstractions)
1. `ScimUser` - 154 edges
2. `AuditTrail` - 86 edges
3. `ScimGroup` - 82 edges
4. `AuditOperation` - 78 edges
5. `IdentityAdministrationServiceTests` - 77 edges
6. `ScimGroupProvisioningIntegrationTests` - 77 edges
7. `ScimUserRepository` - 76 edges
8. `RecordingAuditTrail` - 75 edges
9. `ScimFilterPath` - 74 edges
10. `ScimResourceType` - 69 edges

## Surprising Connections (you probably didn't know these)
- `AdminAuditController` --references--> `AuditEventListingService`  [EXTRACTED]
  backend/src/main/java/com/example/backend/audit/controller/AdminAuditController.java → backend/src/main/java/com/example/backend/audit/application/AuditEventListingService.java
- `AuditRetentionService` --references--> `AuditRetentionPolicy`  [EXTRACTED]
  backend/src/main/java/com/example/backend/audit/application/AuditRetentionService.java → backend/src/main/java/com/example/backend/audit/domain/AuditRetentionPolicy.java
- `AuditAppendOnlyIntegrationTests` --references--> `AuditRetentionService`  [EXTRACTED]
  backend/src/test/java/com/example/backend/audit/AuditAppendOnlyIntegrationTests.java → backend/src/main/java/com/example/backend/audit/application/AuditRetentionService.java
- `AuditTrailService` --references--> `AuditEventRepository`  [EXTRACTED]
  backend/src/main/java/com/example/backend/audit/application/AuditTrailService.java → backend/src/main/java/com/example/backend/audit/domain/AuditEventRepository.java
- `AuditTrailService` --references--> `AuditRequestContext`  [EXTRACTED]
  backend/src/main/java/com/example/backend/audit/application/AuditTrailService.java → backend/src/main/java/com/example/backend/audit/domain/AuditRequestContext.java

## Import Cycles
- None detected.

## Communities (227 total, 58 thin omitted)

### Community 0 - "org.junit.jupiter.api.Test"
Cohesion: 0.03
Nodes (16): AuditRetentionStartupTests, AuditEventQueryTests, DormancyPolicyStartupTests, Connectors, Tokens, ConnectorTokenPolicyTests, Lifetime, RotationOverlap (+8 more)

### Community 1 - "ScimGroup"
Cohesion: 0.08
Nodes (6): DuplicateDisplayNameException, ScimGroup, UnknownGroupMemberException, ScimGroupTests, InMemoryScimGroupRepository, Override

### Community 3 - "ScimUserService"
Cohesion: 0.08
Nodes (15): ScimSearchService, ScimUserService, ScimExternalIdRepository, ScimGroupMember, ScimPageRequest, ScimQueryRepository, ScimTombstoneRepository, ScimSearchServiceTests (+7 more)

### Community 4 - "DormancyPolicy"
Cohesion: 0.10
Nodes (11): DormantAuthorityRevocationService, InactivityDeactivationService, PasswordChangeGraceService, DormancyPolicyConfig, DormancyScheduleConfig, Override, ScheduledJobLock, ScheduledJobLockAdapter (+3 more)

### Community 5 - ".advanceBy"
Cohesion: 0.12
Nodes (3): DormancyRun, DormantAuthorityRevocationServiceTests, InactivityDeactivationServiceTests

### Community 6 - ".of"
Cohesion: 0.13
Nodes (5): NewScimGroup, AddMembers, RemoveMembers, ScimGroupReplacement, ScimGroupServiceTests

### Community 7 - "ScimUser"
Cohesion: 0.08
Nodes (9): DuplicateUserNameException, ScimLoginState, ScimUser, ScimUserProfile, Override, ScimUserPersistenceAdapter, InMemoryScimUserRepository, Override (+1 more)

### Community 8 - ".require"
Cohesion: 0.08
Nodes (3): LoginAttemptServiceTests, LoginLockoutTests, org.springframework.security.core.AuthenticationException

### Community 9 - "Attribute"
Cohesion: 0.04
Nodes (44): And, Attribute, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE (+36 more)

### Community 10 - "MutableClock"
Cohesion: 0.08
Nodes (21): LoggingOperationalAlerts, ScheduledJob, DORMANT_AUTHORITY_REVOCATION, INACTIVITY_DEACTIVATION, PASSWORD_CHANGE_GRACE_DEACTIVATION, Override, LogEvent, ConnectorTokenPolicy (+13 more)

### Community 11 - "org.springframework.context.annotation.Bean"
Cohesion: 0.07
Nodes (25): LoginLockoutConfig, ScimReleaseGate, ScimSecurityConfig, PasswordNormalization, SessionRegistryConfiguration, SessionRegistryConfiguration, SessionRegistryConfiguration, DormancyTestClockConfiguration (+17 more)

### Community 12 - "ScimUserServiceTests"
Cohesion: 0.09
Nodes (7): NewScimUser, ScimUserReplacement, ScimVersionPrecondition, Revocation, ScimUserServiceTests, ScimVersionPreconditionTests, Tombstone

### Community 14 - "org.springframework.data.jpa.repository.Query"
Cohesion: 0.12
Nodes (9): ScimGroupJpaRepository, ScimGroupMemberJpaRepository, ScimGroupMemberRow, ScimResourceJpaRepository, ScimUserJpaRepository, org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.jpa.repository.Lock, org.springframework.data.jpa.repository.Modifying (+1 more)

### Community 15 - "ScimAttributeProjectionTests"
Cohesion: 0.08
Nodes (9): SuppressWarnings, Kind, GROUP, USER, ScimAttributeProjection, Search, ScimSearchRenderer, SuppressWarnings (+1 more)

### Community 16 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.07
Nodes (8): SpaRoutes, LockoutPolicyTests, SpaRoutesScimNamespaceTests, ReservedServerPaths, SpaRoutesTests, SpaShell, org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource

### Community 17 - "org.springframework.boot.test.context.SpringBootTest"
Cohesion: 0.13
Nodes (36): RequestIdFilter, ConnectorTokenScope, READ_ONLY, READ_WRITE, AuditEventReadAdapterIntegrationTests, BackendApplicationTests, ContainerTestConfiguration, UserCounterServiceTests (+28 more)

### Community 18 - "ScimQuery"
Cohesion: 0.10
Nodes (7): Hit, Result, ScimQuery, ScimSort, Override, ScimSortTests, ScimQuerySqlTests

### Community 19 - "AuditTrail"
Cohesion: 0.07
Nodes (4): AuditFilterShape, AuditTrail, AuditTrailServiceTests, RecordingRepository

### Community 20 - "ScimGroupService"
Cohesion: 0.11
Nodes (5): ScimGroupListing, ScimGroupResource, ScimGroupService, ScimGroupController, ScimGroupRenderer

### Community 21 - "AuditTrailServiceTests.java"
Cohesion: 0.05
Nodes (33): AuditAdministrativeRefusal, CREDENTIALLESS_TARGET, LAST_ENABLED_ADMINISTRATOR, PROTECTED_RESOURCE, SELF_DISABLE, SELF_TARGET, AuditEvent, AuditEventRepository (+25 more)

### Community 22 - "UserCounter"
Cohesion: 0.09
Nodes (7): UserCounter, UserCounterRepository, UserCounterEntity, UserCounterJpaRepository, Override, UserCounterPersistenceAdapter, UserCounterTests

### Community 24 - "ScimFilterPath"
Cohesion: 0.07
Nodes (46): of(), parent(), ScimFilterPath, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE (+38 more)

### Community 25 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.15
Nodes (3): AuditTrailService, Override, org.springframework.transaction.annotation.Transactional

### Community 26 - "org.springframework.web.bind.annotation.RestController"
Cohesion: 0.11
Nodes (9): AdminAuditController, InvalidAuditQueryException, ForbiddenIdentityChangeException, UnknownIdentityException, UnknownSessionIdentityException, SelfController, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.ResponseStatus (+1 more)

### Community 27 - ".given"
Cohesion: 0.09
Nodes (6): Override, Group, SelfRecord, LoginIdentityServiceTests, SelfReadServiceTests, org.springframework.security.core.userdetails.UserDetails

### Community 28 - "ScimQueryProtocolIntegrationTests"
Cohesion: 0.12
Nodes (3): ScimQueryProtocolIntegrationTests, org.junit.jupiter.params.provider.Arguments, org.junit.jupiter.params.provider.MethodSource

### Community 30 - "ScimUserPatchOperation"
Cohesion: 0.10
Nodes (26): AddEmails, EmailUpdate, MergeName, NamePart, FAMILY_NAME, FORMATTED, GIVEN_NAME, HONORIFIC_PREFIX (+18 more)

### Community 31 - "RecordingAuditTrail"
Cohesion: 0.14
Nodes (3): Override, Recorded, RecordingAuditTrail

### Community 32 - "OperationalTelemetryIntegrationTests"
Cohesion: 0.13
Nodes (8): Builder, Builder, SuppressWarnings, OperationalTelemetryIntegrationTests, Session, CookieManager, java.net.http.HttpClient, java.net.http.HttpResponse

### Community 33 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.11
Nodes (12): Override, Override, ScimBearerAuthenticationFilter, ScimBearerChallenge, Override, ScimReleaseGateFilter, jakarta.servlet.FilterChain, jakarta.servlet.http.HttpServletRequest (+4 more)

### Community 36 - ".invalidValue"
Cohesion: 0.16
Nodes (8): Op, ADD, REMOVE, REPLACE, Path, ScimUserPatchReader, ScimUserRequestReader, token()

### Community 37 - "ConnectorTokenSecretTests"
Cohesion: 0.12
Nodes (7): ConnectorTokenDigest, Override, Minted, Presented, ConnectorTokenSecretTests, java.security.MessageDigest, java.security.SecureRandom

### Community 38 - ".toDomain"
Cohesion: 0.09
Nodes (4): ScimLoginStateValue, ScimUserEmailValue, ScimUserEntity, jakarta.persistence.Embeddable

### Community 39 - "PasswordChangeLifecycleIntegrationTests"
Cohesion: 0.08
Nodes (8): MockHttpSession, ProbeController, SecurityConfigTests, AdminAccountEndpointTests, MockHttpSession, Cookie, PasswordChangeLifecycleIntegrationTests, jakarta.servlet.http.Cookie

### Community 40 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.05
Nodes (36): 1. Get VPC Info, 2. Deploy, 3. Get Private Key (if CloudFormation created it), 4. Deploy Application, 5. Test, Quick Start, Alerts, Architecture (+28 more)

### Community 42 - "ScimEmailFilter"
Cohesion: 0.15
Nodes (6): Condition, ScimEmailFilter, ScimEmailPart, PRIMARY, TYPE, VALUE

### Community 43 - "ScimResourceType"
Cohesion: 0.14
Nodes (6): ScimResourceType, GROUP, USER, Comparison, ScimQuerySql, Override

### Community 44 - ".seed"
Cohesion: 0.14
Nodes (7): ScimSeedService, SeededIdentity, ScimSeedConfig, CountingPasswordEncoder, Override, ScimSeedServiceTests, org.springframework.boot.ApplicationRunner

### Community 45 - ".created"
Cohesion: 0.07
Nodes (3): ReservedResourceNameTests, ScimLoginStateTests, ScimUserTests

### Community 46 - "AuditOperation"
Cohesion: 0.06
Nodes (31): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+23 more)

### Community 47 - "IdentitySummary"
Cohesion: 0.22
Nodes (4): IdentitySummary, AdminAccountControllerTests, Override, RecordingService

### Community 48 - "ScimConnectorToken"
Cohesion: 0.17
Nodes (6): ConnectorTokenSecret, ScimConnectorToken, ScimConnectorTokenRepository, InMemoryScimConnectorTokenRepository, Override, org.springframework.mock.web.MockHttpServletResponse

### Community 49 - "AuditEventQuery"
Cohesion: 0.08
Nodes (16): AuditEventListingService, AuditEventPage, AuditEventQuery, AuditEventReader, AuditOutcome, FAILURE, SUCCESS, AuditEventReadAdapter (+8 more)

### Community 51 - "compilerOptions"
Cohesion: 0.07
Nodes (29): compilerOptions, allowImportingTsExtensions, baseUrl, isolatedModules, jsx, lib, module, moduleDetection (+21 more)

### Community 52 - "tools.jackson.databind.JsonNode"
Cohesion: 0.14
Nodes (8): RemoveAllMembers, ReplaceMembers, ScimGroupPatchOperation, SetDisplayName, ScimGroupRequestReader, ScimQueryRequest, InvalidScimQueryException, tools.jackson.databind.JsonNode

### Community 54 - "ScimFilterParser"
Cohesion: 0.23
Nodes (4): InvalidScimFilterException, Comparison, ResolvedPath, ScimFilterParser

### Community 55 - "ScimUserEdit"
Cohesion: 0.14
Nodes (3): ScimName, ScimUserEdit, Override

### Community 56 - "AuthControllerTests"
Cohesion: 0.22
Nodes (5): ChangePasswordRequest, Override, LoginRequest, AuthControllerTests, MockHttpServletResponse

### Community 57 - "AttributeRef"
Cohesion: 0.17
Nodes (20): Attribute, ScimAuditFilterShapes, And, AttributeRef, Comparison, Not, Operator, CO (+12 more)

### Community 58 - "Attribute"
Cohesion: 0.16
Nodes (9): Attribute, Kind, BOOLEAN, COMPLEX, DATE_TIME, REFERENCE, STRING, ScimQueryVocabulary (+1 more)

### Community 60 - "devDependencies"
Cohesion: 0.07
Nodes (27): eslint, @eslint/js, eslint-plugin-react-hooks, fallow, devDependencies, dependency-cruiser, eslint, @eslint/js (+19 more)

### Community 61 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 64 - "AuditEventRecordingIntegrationTests"
Cohesion: 0.13
Nodes (6): AuditEventRecordingIntegrationTests, MockHttpSession, ResultActions, AuditListingIntegrationTests, Seeded, org.springframework.mock.web.MockHttpSession

### Community 65 - "ScimBearerAuthenticationFilterTests"
Cohesion: 0.18
Nodes (4): SecureRandom, MockHttpServletRequest, ScimBearerAuthenticationFilterTests, org.springframework.mock.web.MockFilterChain

### Community 66 - "App.tsx"
Cohesion: 0.13
Nodes (17): App(), AuthRole, AuthProvider(), AuthStatus, useAuth(), GuestRoute(), ProtectedRoute(), SessionRoute() (+9 more)

### Community 67 - "ScimUserRepository"
Cohesion: 0.07
Nodes (3): SelfReadService, ScimGroupRepository, ScimUserRepository

### Community 68 - "jakarta.persistence.Entity"
Cohesion: 0.11
Nodes (11): ScimExternalIdEntity, ScimGroupMemberEntity, ScimPasswordHistoryEntity, ScimPasswordHistoryJpaRepository, Override, ScimPasswordHistoryPersistenceAdapter, jakarta.persistence.Entity, jakarta.persistence.IdClass (+3 more)

### Community 69 - "ScimRequestObservationConventionTests"
Cohesion: 0.18
Nodes (7): Override, ScimRequestObservationConvention, ServerRequestObservationContext, ScimRequestObservationConventionTests, io.micrometer.common.KeyValues, org.springframework.http.server.observation.DefaultServerRequestObservationConvention, org.springframework.http.server.observation.ServerRequestObservationContext

### Community 70 - "ScimConnector"
Cohesion: 0.16
Nodes (5): ConnectorAuthenticationService, ScimConnector, ScimConnectorRepository, InMemoryScimConnectorRepository, Override

### Community 71 - "http.ts"
Cohesion: 0.16
Nodes (16): decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, SessionRequest, ApiDecoder (+8 more)

### Community 72 - "org.springframework.stereotype.Service"
Cohesion: 0.10
Nodes (13): AfterCommit, LoginAttemptService, LoginIdentityService, LoginService, ScimUserSessionRevocation, PasswordEncoder, SecurityConfig, AccountSessions (+5 more)

### Community 73 - "LogContextTests"
Cohesion: 0.16
Nodes (4): Override, LogContext, Scope, LogContextTests

### Community 74 - "ScimConnectorTokenEntity"
Cohesion: 0.15
Nodes (4): ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 75 - "org.springframework.stereotype.Repository"
Cohesion: 0.10
Nodes (10): AuditEventRetentionAdapter, Override, Override, Key, ScimExternalIdJpaRepository, Override, ScimExternalIdPersistenceAdapter, Override (+2 more)

### Community 76 - ".get"
Cohesion: 0.14
Nodes (4): MockHttpSession, SelfControllerTests, org.junit.jupiter.api.AfterEach, ScimDiscoveryIntegrationTests

### Community 77 - "AuditUserAttribute"
Cohesion: 0.13
Nodes (11): AuditUserAttribute, ACTIVE, DISPLAY_NAME, EMAILS, GROUPS, LOCALE, NAME, PASSWORD (+3 more)

### Community 78 - ".handle"
Cohesion: 0.17
Nodes (5): ScimErrorException, ScimExceptionHandler, InvalidPreconditionException, org.springframework.http.HttpStatus, org.springframework.web.bind.annotation.RestControllerAdvice

### Community 79 - "ReservedResourceName"
Cohesion: 0.08
Nodes (13): PasswordHistoryPolicy, PasswordReusedException, PreconditionFailedException, PreconditionRequiredException, ProtectedResourceException, ofStoredValue(), ReservedResourceName, ADMIN_GROUP (+5 more)

### Community 80 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.09
Nodes (22): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Error contract (+14 more)

### Community 81 - "compilerOptions"
Cohesion: 0.09
Nodes (21): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+13 more)

### Community 82 - "AdminConnectorController"
Cohesion: 0.16
Nodes (7): IssuedConnectorToken, UnknownConnectorException, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest, RotateTokenRequest, InvalidConnectorTokenLifetimeException

### Community 84 - "InactivityGovernanceIntegrationTests"
Cohesion: 0.17
Nodes (3): InactivityGovernanceIntegrationTests, ThrowingRunnable, FunctionalInterface

### Community 85 - ".of"
Cohesion: 0.10
Nodes (3): NormalizedDisplayName, NormalizedDisplayNameTests, NormalizedUserNameTests

### Community 86 - "scripts"
Cohesion: 0.10
Nodes (20): scripts, analyze, build, dev, format, format:check, lint, preview (+12 more)

### Community 89 - "Testing Guide"
Cohesion: 0.11
Nodes (18): Architecture tests, Assert exactly, not loosely, Calling the API from a spec, Coverage excludes — why the list is explicit, E2E, Fallow, Flakiness — the rules that keep these tests green, Forcing a refused request (+10 more)

### Community 90 - "IndexedSessions"
Cohesion: 0.22
Nodes (6): Override, AccountSessionsAdapterTests, IndexedSessions, Override, MapSession, org.springframework.session.MapSession

### Community 91 - "SpaFrontendTests"
Cohesion: 0.18
Nodes (7): ModelAndView, Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.stereotype.Component, org.springframework.web.servlet.ModelAndView

### Community 92 - "AuditEventEntity"
Cohesion: 0.39
Nodes (4): AuditEventJpaRepository, AuditEventPersistenceAdapter, Override, AuditEventEntity

### Community 93 - "RefusalTimingEquivalenceTests"
Cohesion: 0.23
Nodes (3): CountingPasswordEncoder, Override, RefusalTimingEquivalenceTests

### Community 94 - "components.json"
Cohesion: 0.11
Nodes (17): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+9 more)

### Community 95 - "auth.helpers.ts"
Cohesion: 0.20
Nodes (10): ADMIN_CREDENTIALS, captureSessionCookie(), expireSession(), login(), loginAs(), postAdminAction(), resetCounterViaApi(), SESSION_COOKIE (+2 more)

### Community 96 - "include"
Cohesion: 0.11
Nodes (16): compilerOptions, types, exclude, extends, include, node, src/**/*.test.ts, src/**/*.test.tsx (+8 more)

### Community 98 - "accounts.tsx"
Cohesion: 0.16
Nodes (10): SessionResult, AccountAction, Accounts(), actionFailure(), AdminAccount, decodeAccount(), decodeAccounts(), formatDate() (+2 more)

### Community 99 - "showcase.tsx"
Cohesion: 0.31
Nodes (11): Button(), ButtonProps, buttonVariants, Card(), CardContent(), CardDescription(), CardFooter(), CardHeader() (+3 more)

### Community 100 - "lib.sh"
Cohesion: 0.22
Nodes (12): die(), load_backend_env(), log(), require_cmd(), require_docker(), require_maven(), require_node(), require_port_free() (+4 more)

### Community 101 - "org.springframework.mock.web.MockHttpServletRequest"
Cohesion: 0.26
Nodes (5): HttpAuditRequestContext, Override, HttpAuditRequestContextTests, MockHttpServletRequest, org.springframework.mock.web.MockHttpServletRequest

### Community 102 - "MetricTagTests"
Cohesion: 0.27
Nodes (3): MockHttpServletRequest, ServerRequestObservationContext, MetricTagTests

### Community 103 - "AuditListingEndToEndIntegrationTests"
Cohesion: 0.16
Nodes (5): AuditListingEndToEndIntegrationTests, ClockedSession, Override, jakarta.servlet.ServletContext, org.springframework.test.web.servlet.request.RequestPostProcessor

### Community 104 - "ScimConnectorEntity"
Cohesion: 0.23
Nodes (4): ScimConnectorEntity, ScimConnectorJpaRepository, Override, ScimConnectorPersistenceAdapter

### Community 105 - "ScimUserAttributesTests"
Cohesion: 0.07
Nodes (8): alwaysReturned(), projectableNames(), schemaAttributes(), ScimGroupAttributes, Attribute, ScimUserAttributes, SuppressWarnings, ScimUserAttributesTests

### Community 106 - "stryker.config.json"
Cohesion: 0.12
Nodes (15): cleanTempDir, concurrency, coverageAnalysis, htmlReporter, fileName, jsonReporter, fileName, packageManager (+7 more)

### Community 107 - "Backend"
Cohesion: 0.13
Nodes (14): Audit trail database roles, Audit trail retention, Backend, Build and test, Bundle a frontend, Configuration, Inactivity governance, Log in (+6 more)

### Community 108 - "SessionController"
Cohesion: 0.22
Nodes (7): SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession, org.springframework.web.bind.annotation.DeleteMapping, org.springframework.web.bind.annotation.PutMapping

### Community 109 - "AuditRetentionService"
Cohesion: 0.17
Nodes (10): AuditRetentionService, AuditRetentionScheduleConfig, Override, AuditEventRetention, ScheduledJobMetrics, io.micrometer.core.instrument.Counter, io.micrometer.core.instrument.MeterRegistry, org.springframework.scheduling.annotation.EnableScheduling (+2 more)

### Community 110 - "org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder"
Cohesion: 0.33
Nodes (3): org.junit.jupiter.api.AfterEach, ScimEndToEndIntegrationTests, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

### Community 112 - "Architecture"
Cohesion: 0.14
Nodes (13): Architecture, Build, Dev-server reloads, Routing, Styling and the token pipeline, The `@/` alias, The layers, What is deliberately absent (+5 more)

### Community 113 - "auth-context-value.ts"
Cohesion: 0.25
Nodes (10): AuthUser, AuthContext, AuthContextState, AuthContextValue, useAuthState(), apiFetchMock, request(), state (+2 more)

### Community 114 - "sources.ts"
Cohesion: 0.20
Nodes (11): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+3 more)

### Community 115 - "AGENTS.md"
Cohesion: 0.15
Nodes (11): Agent, Build and validation, Documentation, Environment, Frontend/backend integration, graphify, Ignore rules, Layout (+3 more)

### Community 116 - "AbsoluteSessionLifetimePolicy"
Cohesion: 0.20
Nodes (4): AbsoluteSessionLifetimeFilter, Override, AbsoluteSessionLifetimePolicy, AbsoluteSessionLifetimePolicyTests

### Community 117 - "EcsLogCapture"
Cohesion: 0.24
Nodes (6): EcsLogCapture, Override, ch.qos.logback.classic.Logger, ch.qos.logback.classic.LoggerContext, ch.qos.logback.core.OutputStreamAppender, OutputStreamAppender

### Community 118 - "ScimGroupPersistenceAdapter"
Cohesion: 0.14
Nodes (4): ScimGroupEntity, ScimResourceEntity, Override, ScimGroupPersistenceAdapter

### Community 119 - "dependencies"
Cohesion: 0.15
Nodes (13): class-variance-authority, clsx, dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom (+5 more)

### Community 120 - "deploy.sh"
Cohesion: 0.42
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 122 - "org.springframework.http.ResponseEntity"
Cohesion: 0.25
Nodes (5): ScimSearchListing, ScimDiscoveryController, ScimSearchController, org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.GetMapping

### Community 123 - "AuditRetentionServiceTests"
Cohesion: 0.24
Nodes (4): AuditRetentionServiceTests, CountingRetention, Override, Override

### Community 124 - "AGENTS.md — frontend"
Cohesion: 0.17
Nodes (11): AGENTS.md — frontend, Architecture, Backend contract, Baseline gate, Commands, Component library, Conditional gates, Fallow (+3 more)

### Community 125 - ".changePassword"
Cohesion: 0.09
Nodes (13): CurrentPasswordRejectedException, PasswordChangeService, PasswordPolicyViolationException, PasswordPolicy, Rule, CONTAINS_USER_NAME, REUSED, TOO_LONG (+5 more)

### Community 126 - "org.junit.jupiter.api.AfterEach"
Cohesion: 0.12
Nodes (5): AccountSessionsAdapter, RedisSessionRevocationIntegrationTests, org.junit.jupiter.api.AfterEach, org.springframework.session.FindByIndexNameSessionRepository, org.springframework.session.Session

### Community 130 - "AuditRetentionPolicy"
Cohesion: 0.24
Nodes (3): AuditRetentionPolicyConfig, AuditRetentionPolicy, AuditRetentionPolicyTests

### Community 131 - "RFC requirements and implications"
Cohesion: 0.18
Nodes (11): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+3 more)

### Community 132 - "package.json"
Cohesion: 0.18
Nodes (10): engines, node, npm, name, overrides, qs, packageManager, private (+2 more)

### Community 133 - "showcase.test.tsx"
Cohesion: 0.18
Nodes (4): decodeCount(), Showcase(), apiFetchMock, auth

### Community 134 - "mutate"
Cohesion: 0.18
Nodes (11): !src/**/*.test.ts, !src/**/*.test.tsx, !src/**/*.testHelpers.ts, !src/**/*.testHelpers.tsx, !test/**, mutate, !src/components/ui/**, !src/**/*.d.ts (+3 more)

### Community 135 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 136 - "Operator"
Cohesion: 0.20
Nodes (10): Operator, CO, EQ, EW, GE, GT, LE, LT (+2 more)

### Community 138 - "org.springframework.web.bind.annotation.PostMapping"
Cohesion: 0.16
Nodes (10): AdminAccountController, UserCounterService, CountResponse, UserCounterController, Override, RecordingCounterService, UserCounterControllerTests, java.security.Principal (+2 more)

### Community 140 - "Delivery plan"
Cohesion: 0.20
Nodes (10): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 0a — Permanent lockout, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 4 — Groups and Admin authority, Slice 5 — Complete query protocol (+2 more)

### Community 141 - "front-end"
Cohesion: 0.20
Nodes (9): Backend contract, Component library, front-end, Project structure, Scripts, Setup, Styling, Technology stack (+1 more)

### Community 142 - "AGENTS.md — backend"
Cohesion: 0.22
Nodes (8): AGENTS.md — backend, API contract, Architecture constraints, Baseline gate, Conditional gate: mutation testing, Reading a gate's result, Security-sensitive changes, Verification

### Community 143 - "AuthenticatedConnector"
Cohesion: 0.11
Nodes (8): ScimListedResource, ScimUserListing, ScimUserResource, ScimUserController, ScimUserRenderer, AuthenticatedConnector, ScimGroupReference, org.springframework.web.bind.annotation.PatchMapping

### Community 144 - "ScimBearerAuthenticationFilter.java"
Cohesion: 0.21
Nodes (3): MetricTag, ScimWriteScopeRule, ScimExceptionHandlerMetricTests

### Community 145 - "SCIM 2.0 account-management research"
Cohesion: 0.22
Nodes (7): Executive finding, Existing application seams, Primary sources, Recommended implementation order, Resolved RFC decisions, SCIM 2.0 account-management research, Testing strategy

### Community 146 - "Credential and cryptographic policy"
Cohesion: 0.22
Nodes (9): Credential and cryptographic policy, Hashing and primitives, Key rotation and storage, Lockout policy, Log formatting, Password policy, Response headers, Session lifetime (+1 more)

### Community 147 - "ignorePatterns"
Cohesion: 0.22
Nodes (9): ignorePatterns, .agents, artifacts, .claude, coverage, dist, graphify-out, playwright-report (+1 more)

### Community 148 - "ManagementSessionConfiguration.java"
Cohesion: 0.39
Nodes (6): ManagementSessionConfiguration, DelegatingFilterProxyRegistrationBean, org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort, org.springframework.boot.autoconfigure.condition.ConditionalOnClass, org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean, org.springframework.session.web.http.SessionRepositoryFilter

### Community 150 - "AfterCommitAdapterTests"
Cohesion: 0.25
Nodes (3): AfterCommitAdapter, Override, AfterCommitAdapterTests

### Community 151 - "Domain and authority model"
Cohesion: 0.25
Nodes (8): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, Protected recovery resources, SCIM User replaces Account

### Community 152 - "monorepo-base"
Cohesion: 0.25
Nodes (7): Deploying to AWS, Frontend/backend integration, Layout, monorepo-base, Toolchain pins, Working on the backend, Working on the frontend

### Community 154 - "Cause"
Cohesion: 0.22
Nodes (6): Cause, ADMIN_MEMBERSHIP_REMOVED, DEACTIVATED, DELETED, PASSWORD_CHANGED, USER_NAME_CHANGED

### Community 156 - "org.junit.jupiter.params.provider.CsvSource"
Cohesion: 0.13
Nodes (3): ScimQueryRequestTests, ScimPageRequestTests, org.junit.jupiter.params.provider.CsvSource

### Community 157 - "CapturedLog"
Cohesion: 0.33
Nodes (3): CapturedLog, ch.qos.logback.classic.spi.ILoggingEvent, ch.qos.logback.core.read.ListAppender

### Community 158 - "CONTEXT"
Cohesion: 0.29
Nodes (6): Accounts and identity provisioning, CONTEXT, Current account model, Request paths, SCIM target model, Sessions

### Community 159 - "1. Count login attempts on the login path"
Cohesion: 0.29
Nodes (6): 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision, Status

### Community 160 - "2. Revoke a disabled account's sessions after the commit"
Cohesion: 0.29
Nodes (6): 2. Revoke a disabled account's sessions after the commit, Alternatives considered, Consequences, Context, Decision, Status

### Community 161 - "4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal"
Cohesion: 0.29
Nodes (6): 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal, Alternatives considered, Consequences, Context, Decision, Status

### Community 162 - "5. Serialize scheduled jobs on per-job lock rows"
Cohesion: 0.29
Nodes (6): 5. Serialize scheduled jobs on per-job lock rows, Alternatives considered, Consequences, Context, Decision, Status

### Community 165 - "ArchitectureTest"
Cohesion: 0.40
Nodes (5): ArchitectureTest, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, ImportOption.DoNotIncludeTests, jakarta.persistence.EntityManager

### Community 166 - "EcsLogFormatTests"
Cohesion: 0.17
Nodes (6): LockoutHasNoDurationTests, EcsLogFormatTests, MockHttpSession, ResultActions, java.lang.reflect.RecordComponent, org.springframework.core.env.Environment

### Community 167 - "3. ECS-structured logging with redaction enforced structurally"
Cohesion: 0.33
Nodes (5): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status

### Community 168 - "Domain Docs"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 169 - "Issue tracker: GitHub"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 170 - "Query contract"
Cohesion: 0.33
Nodes (6): Attribute projection, Filtering, Pagination, POST search, Query contract, Sorting

### Community 171 - ".currentUser"
Cohesion: 0.29
Nodes (3): LoginOutcome, UserResponse, org.springframework.security.core.Authentication

### Community 172 - "Definition of Done"
Cohesion: 0.40
Nodes (5): Backend gates, Contract and behavior, Definition of Done, Documentation and graph, Frontend gates

### Community 173 - "Write semantics"
Cohesion: 0.40
Nodes (5): DELETE and tombstones, PATCH, POST, PUT, Write semantics

### Community 174 - "reporters"
Cohesion: 0.40
Nodes (5): reporters, clear-text, html, json, progress

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

### Community 180 - "Graphify Runner"
Cohesion: 0.50
Nodes (3): Graphify Runner, Reporting, Steps

### Community 181 - "thresholds"
Cohesion: 0.50
Nodes (4): thresholds, break, high, low

### Community 183 - "Supported schemas"
Cohesion: 0.67
Nodes (3): Group, Supported schemas, User

### Community 184 - "clearTextReporter"
Cohesion: 0.67
Nodes (3): clearTextReporter, allowColor, maxTestsToLog

### Community 185 - "_comment_mutate"
Cohesion: 0.67
Nodes (3): _comment_mutate, src/components/ui/** is vendored placeholder code, due to be deleted when the in-house shadcn package is published. Its mutants are edits to Tailwind class strings, and killing them means pinning assertions to markup that is about to be replaced., src/main.tsx is the composition root: its only statement is a createRoot call against the real document, so every mutant is either uncoverable or a restatement of what the smoke E2E already proves.

### Community 222 - "AuthController"
Cohesion: 0.38
Nodes (5): AuthController, PasswordRuleViolation, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy, org.springframework.security.web.context.SecurityContextRepository, org.springframework.session.web.http.CookieSerializer

### Community 223 - "ScimPasswordChange"
Cohesion: 0.22
Nodes (6): Override, Kind, CLEAR, SET, UNCHANGED, ScimPasswordChange

### Community 224 - "RecordingTransactionManager"
Cohesion: 0.52
Nodes (4): Override, RecordingTransactionManager, org.springframework.transaction.TransactionDefinition, org.springframework.transaction.TransactionStatus

### Community 225 - "ScimQueryPersistenceAdapter.java"
Cohesion: 0.40
Nodes (3): NamedParameterJdbcTemplate, ScimQueryPersistenceAdapter, org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

## Knowledge Gaps
- **598 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+593 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **58 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AuditOperation` connect `AuditOperation` to `AuditEventRecordingIntegrationTests`, `ScimUserService`, `jakarta.persistence.Entity`, `.of`, `MutableClock`, `org.springframework.context.annotation.Bean`, `ReservedResourceName`, `AuditEventQuery`, `org.springframework.boot.test.context.SpringBootTest`, `ScimConnectorLifecycleIntegrationTests`, `AuditTrailServiceTests.java`, `.of`, `org.springframework.transaction.annotation.Transactional`, `org.springframework.web.bind.annotation.RestController`, `ScimUserProvisioningIntegrationTests`, `AuditEventEntity`, `RecordingAuditTrail`?**
  _High betweenness centrality (0.033) - this node is a cross-community bridge._
- **Why does `ScimUser` connect `ScimUser` to `ScimGroup`, `IdentityAdministrationServiceTests`, `ScimUserService`, `.advanceBy`, `.of`, `.require`, `MutableClock`, `org.springframework.context.annotation.Bean`, `ScimUserServiceTests`, `AuthenticatedConnector`, `org.springframework.boot.test.context.SpringBootTest`, `.given`, `.toDomain`, `.seed`, `.created`, `AuditEventRecordingIntegrationTests`, `ScimUserRepository`, `org.springframework.stereotype.Service`, `.get`, `ReservedResourceName`, `.sessionsOf`, `.deactivateOverdueUsers`, `.changePassword`, `org.junit.jupiter.api.AfterEach`?**
  _High betweenness centrality (0.026) - this node is a cross-community bridge._
- **Why does `ConnectorAdministrationService` connect `org.junit.jupiter.api.BeforeEach` to `SelfReadIntegrationTests`, `ScimUserService`, `MutableClock`, `ScimGroupProvisioningIntegrationTests`, `org.springframework.boot.test.context.SpringBootTest`, `AuditTrail`, `.summarize`, `.given`, `ScimQueryProtocolIntegrationTests`, `OperationalTelemetryIntegrationTests`, `ScimConditionalWriteIntegrationTests`, `ScimDeletionIntegrationTests`, `ConnectorTokenSecretTests`, `PasswordChangeLifecycleIntegrationTests`, `ScimConnectorToken`, `ScimUserProvisioningIntegrationTests`, `ScimUserRepository`, `ScimConnector`, `org.springframework.stereotype.Service`, `.get`, `AdminConnectorController`, `InactivityGovernanceIntegrationTests`, `org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder`?**
  _High betweenness centrality (0.022) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _598 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.junit.jupiter.api.Test` be split into smaller, more focused modules?**
  _Cohesion score 0.032253765160602424 - nodes in this community are weakly interconnected._
- **Should `ScimGroup` be split into smaller, more focused modules?**
  _Cohesion score 0.07539682539682539 - nodes in this community are weakly interconnected._
- **Should `IdentityAdministrationServiceTests` be split into smaller, more focused modules?**
  _Cohesion score 0.07365792759051186 - nodes in this community are weakly interconnected._