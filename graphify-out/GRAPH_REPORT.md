# Graph Report - monorepo-base  (2026-09-30)

## Corpus Check
- 439 files · ~286,548 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 4653 nodes · 15496 edges · 221 communities (154 shown, 67 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 2130 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f97ea5dd`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.junit.jupiter.api.Test
- ScimGroup
- IdentityAdministrationServiceTests
- AuditTrail
- DormancyPolicy
- .advanceBy
- ScimGroupServiceTests
- ScimUser
- .require
- Attribute
- InMemoryAccountSessions
- org.springframework.security.web.csrf.CsrfTokenRepository
- ScimUserServiceTests
- ScimGroupProvisioningIntegrationTests
- org.springframework.data.jpa.repository.Query
- ScimAttributeProjectionTests
- org.junit.jupiter.params.ParameterizedTest
- org.springframework.boot.test.context.SpringBootTest
- ScimQuery
- AuditTrailServiceTests
- AuthenticatedConnector
- AuditTrailServiceTests.java
- UserCounter
- AuditAppendOnlyIntegrationTests
- ScimFilterPath
- org.springframework.transaction.annotation.Transactional
- AuthController
- .given
- .get
- ScimFilterParserTests
- ScimUserPatchOperationTests
- RecordingAuditTrail
- OperationalTelemetryIntegrationTests
- jakarta.servlet.http.HttpServletRequest
- ScimConditionalWriteIntegrationTests
- ScimDeletionIntegrationTests
- ScimPageRequest
- ConnectorTokenSecretTests
- .toDomain
- .status
- AWS CloudFormation Deployment Guide
- ScimUserService
- ScimUserServiceTests.java
- ScimResourceType
- .seed
- .created
- AuditOperation
- IdentitySummary
- ScimConnectorToken
- org.springframework.context.annotation.Bean
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
- .created
- jakarta.persistence.Entity
- ScimRequestObservationConventionTests
- ScimConnector
- http.ts
- LoginLockoutTests
- LogContextTests
- ScimConnectorTokenEntity
- ScimExternalIdEntity
- ScimDiscoveryIntegrationTests
- AuditUserAttribute
- ScimUserPatchReader
- ScimExceptionHandler.java
- SCIM 2.0 account-management specification plan
- compilerOptions
- AdminConnectorController
- ScimLoginStateTests
- PasswordChangeLifecycleIntegrationTests
- .of
- scripts
- ScimDiscovery
- ScimUserPatchReaderTests
- Testing Guide
- IndexedSessions
- SpaFrontendTests
- PasswordPolicyTests
- RefusalTimingEquivalenceTests
- components.json
- auth.helpers.ts
- include
- ScheduledJobMetrics
- accounts.tsx
- showcase.tsx
- lib.sh
- org.springframework.mock.web.MockHttpServletRequest
- ScimExceptionHandlerMetricTests.java
- .handle
- ScimConnectorEntity
- ScimUserAttributesTests
- stryker.config.json
- Backend
- SessionController
- ScimResourceEntity
- org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
- .ofIfMatch
- Architecture
- auth-context-value.ts
- sources.ts
- AGENTS.md
- AbsoluteSessionLifetimePolicy
- ScimSearchController.java
- .of
- dependencies
- deploy.sh
- ScimUserAttributes
- ScimUserRequestReader
- org.junit.jupiter.api.AfterEach
- AGENTS.md — frontend
- Rule
- AfterCommitAdapterTests
- ConnectorAuthenticationServiceTests
- .advertised
- ScimConnectorTokenTests
- ScimSecurityChainOrderTests.java
- RFC requirements and implications
- package.json
- showcase.test.tsx
- mutate
- mvnw
- AuditAdministrativeRefusal
- .requiresWriteScope
- RecordingCounterService
- RequestIdFilterTests
- Delivery plan
- front-end
- AGENTS.md — backend
- .render
- ScimPasswordChange
- SCIM 2.0 account-management research
- Credential and cryptographic policy
- ignorePatterns
- ManagementSessionConfiguration.java
- Attribute
- ScimTombstoneRepository
- Domain and authority model
- monorepo-base
- ScimGroupMemberId
- RecordingTransactionManager
- MockFilterChain
- ScimAuditFilterShapesTests.java
- org.junit.jupiter.params.provider.Arguments
- CONTEXT
- 1. Count login attempts on the login path
- 2. Revoke a disabled account's sessions after the commit
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- 5. Serialize scheduled jobs on per-job lock rows
- ScimGroupAttributes
- ArchitectureTest
- org.springframework.mock.web.MockHttpServletResponse
- 3. ECS-structured logging with redaction enforced structurally
- Domain Docs
- Issue tracker: GitHub
- Query contract
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

## God Nodes (most connected - your core abstractions)
1. `ScimUser` - 148 edges
2. `AuditTrail` - 86 edges
3. `ScimGroup` - 79 edges
4. `IdentityAdministrationServiceTests` - 77 edges
5. `ScimGroupProvisioningIntegrationTests` - 77 edges
6. `RecordingAuditTrail` - 75 edges
7. `ScimFilterPath` - 74 edges
8. `AuditOperation` - 71 edges
9. `ScimUserRepository` - 71 edges
10. `ScimResourceType` - 69 edges

## Surprising Connections (you probably didn't know these)
- `schemaAttributes()` --references--> `Attribute`  [EXTRACTED]
  backend/src/main/java/com/example/backend/scim/controller/ScimAttributeProjection.java → backend/src/main/java/com/example/backend/scim/controller/ScimUserAttributes.java
- `AuditAppendOnlyIntegrationTests` --references--> `AuditRetentionService`  [EXTRACTED]
  backend/src/test/java/com/example/backend/audit/AuditAppendOnlyIntegrationTests.java → backend/src/main/java/com/example/backend/audit/application/AuditRetentionService.java
- `AuditTrailService` --references--> `AuditEventRepository`  [EXTRACTED]
  backend/src/main/java/com/example/backend/audit/application/AuditTrailService.java → backend/src/main/java/com/example/backend/audit/domain/AuditEventRepository.java
- `AuditTrailService` --references--> `AuditRequestContext`  [EXTRACTED]
  backend/src/main/java/com/example/backend/audit/application/AuditTrailService.java → backend/src/main/java/com/example/backend/audit/domain/AuditRequestContext.java
- `AuditTrailService` --implements--> `AuditTrail`  [EXTRACTED]
  backend/src/main/java/com/example/backend/audit/application/AuditTrailService.java → backend/src/main/java/com/example/backend/audit/domain/AuditTrail.java

## Import Cycles
- None detected.

## Communities (221 total, 67 thin omitted)

### Community 0 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (17): AuditRetentionStartupTests, AuditRetentionPolicyTests, DormancyPolicyStartupTests, Connectors, Tokens, ConnectorTokenPolicyTests, Lifetime, RotationOverlap (+9 more)

### Community 1 - "ScimGroup"
Cohesion: 0.06
Nodes (15): DuplicateDisplayNameException, ProtectedResourceException, ofStoredValue(), ReservedResourceName, ADMIN_GROUP, BOOTSTRAP_ADMIN, ScimGroup, ScimGroupMember (+7 more)

### Community 3 - "AuditTrail"
Cohesion: 0.06
Nodes (22): AuditTrail, AfterCommit, LoginAttemptService, LoginIdentityService, LoginService, PasswordChangeService, ScimUserSessionRevocation, PasswordEncoder (+14 more)

### Community 4 - "DormancyPolicy"
Cohesion: 0.05
Nodes (34): AuditRetentionService, AuditRetentionPolicyConfig, AuditRetentionScheduleConfig, Override, AuditEventRetention, AuditRetentionPolicy, LoggingOperationalAlerts, AuditEventRetentionAdapter (+26 more)

### Community 5 - ".advanceBy"
Cohesion: 0.08
Nodes (7): DormancyRun, DormantAuthorityRevocationServiceTests, InactivityDeactivationServiceTests, InactivityGovernanceIntegrationTests, ThrowingRunnable, Override, FunctionalInterface

### Community 6 - "ScimGroupServiceTests"
Cohesion: 0.10
Nodes (6): NewScimGroup, AddMembers, RemoveMembers, ScimGroupReplacement, ScimGroupService, ScimGroupServiceTests

### Community 7 - "ScimUser"
Cohesion: 0.07
Nodes (6): DuplicateUserNameException, ScimLoginState, ScimUser, PasswordChangeGraceServiceTests, InMemoryScimUserRepository, Override

### Community 8 - ".require"
Cohesion: 0.08
Nodes (3): Override, LoginAttemptServiceTests, ScimUserSessionRevocationTests

### Community 9 - "Attribute"
Cohesion: 0.03
Nodes (54): And, Attribute, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE (+46 more)

### Community 10 - "InMemoryAccountSessions"
Cohesion: 0.09
Nodes (19): ScheduledJob, DORMANT_AUTHORITY_REVOCATION, INACTIVITY_DEACTIVATION, PASSWORD_CHANGE_GRACE_DEACTIVATION, LogEvent, ConnectorTokenPolicy, NormalizedUserName, InMemoryAccountSessions (+11 more)

### Community 11 - "org.springframework.security.web.csrf.CsrfTokenRepository"
Cohesion: 0.06
Nodes (22): ScimSecurityConfig, PasswordNormalization, LockoutHasNoDurationTests, BackendApplicationTests, EcsLogCapture, Override, EcsLogFormatTests, MockHttpSession (+14 more)

### Community 12 - "ScimUserServiceTests"
Cohesion: 0.09
Nodes (7): Override, ScimUserReplacement, ScimVersionPrecondition, ScimUserReplacementTests, Revocation, ScimUserServiceTests, Override

### Community 14 - "org.springframework.data.jpa.repository.Query"
Cohesion: 0.08
Nodes (9): ScimGroupMemberJpaRepository, ScimGroupMemberRow, ScimResourceJpaRepository, ScimUserJpaRepository, Override, ScimUserPersistenceAdapter, org.springframework.data.domain.Sort, org.springframework.data.jpa.repository.Modifying (+1 more)

### Community 15 - "ScimAttributeProjectionTests"
Cohesion: 0.09
Nodes (7): SuppressWarnings, Kind, GROUP, USER, ScimAttributeProjection, SuppressWarnings, ScimAttributeProjectionTests

### Community 16 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.07
Nodes (10): SpaRoutes, LockoutPolicyTests, ScimPageRequestTests, SpaRoutesScimNamespaceTests, ReservedServerPaths, SpaRoutesTests, SpaShell, org.junit.jupiter.params.ParameterizedTest (+2 more)

### Community 17 - "org.springframework.boot.test.context.SpringBootTest"
Cohesion: 0.19
Nodes (28): AccountSessionsAdapter, RequestIdFilter, ConnectorAdministrationService, ConnectorTokenScope, READ_ONLY, READ_WRITE, ScimTombstonePersistenceAdapter, RedisSessionRevocationIntegrationTests (+20 more)

### Community 18 - "ScimQuery"
Cohesion: 0.09
Nodes (8): Hit, Result, ScimQuery, ScimSort, Override, ScimSortTests, ScimQuerySqlTests, Override

### Community 20 - "AuthenticatedConnector"
Cohesion: 0.12
Nodes (10): ScimGroupListing, ScimGroupResource, ScimGroupController, ScimGroupRenderer, ScimUserController, AuthenticatedConnector, org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.DeleteMapping (+2 more)

### Community 21 - "AuditTrailServiceTests.java"
Cohesion: 0.07
Nodes (27): AuditEvent, AuditEventRepository, AuditGroupAttribute, DISPLAY_NAME, MEMBERS, AuditOutcome, FAILURE, SUCCESS (+19 more)

### Community 22 - "UserCounter"
Cohesion: 0.08
Nodes (9): UserCounterService, UserCounter, UserCounterRepository, UserCounterEntity, UserCounterJpaRepository, Override, UserCounterPersistenceAdapter, UserCounterTests (+1 more)

### Community 23 - "AuditAppendOnlyIntegrationTests"
Cohesion: 0.08
Nodes (9): AuditRetentionServiceTests, CountingRetention, Override, AuditAppendOnlyIntegrationTests, CapturedLog, Override, ch.qos.logback.classic.Logger, ch.qos.logback.classic.spi.ILoggingEvent (+1 more)

### Community 24 - "ScimFilterPath"
Cohesion: 0.07
Nodes (46): of(), parent(), ScimFilterPath, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE (+38 more)

### Community 25 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.16
Nodes (3): AuditTrailService, Override, org.springframework.transaction.annotation.Transactional

### Community 26 - "AuthController"
Cohesion: 0.09
Nodes (17): CurrentPasswordRejectedException, ForbiddenIdentityChangeException, UnknownIdentityException, UnsafeIdentityChangeException, AdminAccountController, AuthController, UnknownConnectorException, ScimDiscoveryController (+9 more)

### Community 27 - ".given"
Cohesion: 0.12
Nodes (5): Override, LoginIdentityServiceTests, PasswordChangeServiceTests, ScimSearchServiceTests, org.springframework.security.core.userdetails.UserDetails

### Community 30 - "ScimUserPatchOperationTests"
Cohesion: 0.11
Nodes (9): Condition, ScimEmailFilter, AddEmails, EmailUpdate, RemoveEmailPart, RemoveEmails, UpdateEmails, ScimUserPatchOperationTests (+1 more)

### Community 31 - "RecordingAuditTrail"
Cohesion: 0.14
Nodes (3): Override, Recorded, RecordingAuditTrail

### Community 32 - "OperationalTelemetryIntegrationTests"
Cohesion: 0.10
Nodes (14): Builder, ManagementPortIntegrationTests, Builder, SuppressWarnings, OperationalTelemetryIntegrationTests, Session, CookieManager, java.net.CookieManager (+6 more)

### Community 33 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.09
Nodes (15): Override, ChangePasswordRequest, Override, Override, Override, ScimBearerAuthenticationFilter, ScimBearerChallenge, Override (+7 more)

### Community 36 - "ScimPageRequest"
Cohesion: 0.10
Nodes (6): ScimUserListing, ScimQueryRequest, InvalidScimQueryException, token(), ScimPageRequest, ScimQueryRequestTests

### Community 37 - "ConnectorTokenSecretTests"
Cohesion: 0.10
Nodes (7): ConnectorTokenDigest, Override, Minted, Presented, ConnectorTokenSecretTests, java.security.MessageDigest, java.security.SecureRandom

### Community 38 - ".toDomain"
Cohesion: 0.09
Nodes (4): ScimLoginStateValue, ScimUserEmailValue, ScimUserEntity, jakarta.persistence.Embeddable

### Community 39 - ".status"
Cohesion: 0.11
Nodes (5): MockHttpSession, ProbeController, SecurityConfigTests, AdminAccountEndpointTests, MockHttpSession

### Community 40 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.05
Nodes (36): 1. Get VPC Info, 2. Deploy, 3. Get Private Key (if CloudFormation created it), 4. Deploy Application, 5. Test, Quick Start, Alerts, Architecture (+28 more)

### Community 41 - "ScimUserService"
Cohesion: 0.10
Nodes (8): ScimUserResource, ScimUserService, ScimExternalIdRepository, ScimGroupReference, Reason, MUTABILITY, NO_TARGET, ScimPatchRefusedException

### Community 42 - "ScimUserServiceTests.java"
Cohesion: 0.14
Nodes (23): ScimEmailPart, PRIMARY, TYPE, VALUE, MergeName, NamePart, FAMILY_NAME, FORMATTED (+15 more)

### Community 43 - "ScimResourceType"
Cohesion: 0.13
Nodes (9): ScimResourceType, GROUP, USER, ScimQueryPersistenceAdapter, Comparison, ScimQuerySql, Override, NamedParameterJdbcTemplate (+1 more)

### Community 44 - ".seed"
Cohesion: 0.12
Nodes (7): ScimSeedService, SeededIdentity, ScimSeedConfig, CountingPasswordEncoder, Override, ScimSeedServiceTests, org.springframework.boot.ApplicationRunner

### Community 46 - "AuditOperation"
Cohesion: 0.06
Nodes (31): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+23 more)

### Community 47 - "IdentitySummary"
Cohesion: 0.16
Nodes (8): IdentitySummary, CountResponse, UserCounterController, AdminAccountControllerTests, Override, RecordingService, java.security.Principal, org.springframework.web.bind.annotation.PostMapping

### Community 48 - "ScimConnectorToken"
Cohesion: 0.12
Nodes (4): ScimConnectorToken, ScimConnectorTokenRepository, InMemoryScimConnectorTokenRepository, Override

### Community 49 - "org.springframework.context.annotation.Bean"
Cohesion: 0.11
Nodes (16): ScimReleaseGate, ScimReleaseGateFilter, SecureRandom, SessionRegistryConfiguration, SessionRegistryConfiguration, SessionRegistryConfiguration, DormancyTestClockConfiguration, GenericContainer (+8 more)

### Community 51 - "compilerOptions"
Cohesion: 0.07
Nodes (29): compilerOptions, allowImportingTsExtensions, baseUrl, isolatedModules, jsx, lib, module, moduleDetection (+21 more)

### Community 52 - "tools.jackson.databind.JsonNode"
Cohesion: 0.23
Nodes (7): RemoveAllMembers, ReplaceMembers, ScimGroupPatchOperation, SetDisplayName, ScimGroupRequestReader, java.util.regex.Pattern, tools.jackson.databind.JsonNode

### Community 54 - "ScimFilterParser"
Cohesion: 0.23
Nodes (4): InvalidScimFilterException, Comparison, ResolvedPath, ScimFilterParser

### Community 56 - "AuthControllerTests"
Cohesion: 0.21
Nodes (5): LoginRequest, UserResponse, AuthControllerTests, Cookie, MockHttpServletResponse

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

### Community 63 - "ScimEmail"
Cohesion: 0.18
Nodes (5): NewScimUser, ScimEmail, ScimName, ScimUserProfile, ScimEmailTests

### Community 64 - "AuditEventRecordingIntegrationTests"
Cohesion: 0.22
Nodes (3): AuditEventRecordingIntegrationTests, MockHttpSession, ResultActions

### Community 65 - "ScimBearerAuthenticationFilterTests"
Cohesion: 0.20
Nodes (3): MockHttpServletRequest, ScimBearerAuthenticationFilterTests, org.springframework.security.core.Authentication

### Community 66 - "App.tsx"
Cohesion: 0.13
Nodes (17): App(), AuthRole, AuthProvider(), AuthStatus, useAuth(), GuestRoute(), ProtectedRoute(), SessionRoute() (+9 more)

### Community 68 - "jakarta.persistence.Entity"
Cohesion: 0.15
Nodes (9): ScimGroupMemberEntity, ScimPasswordHistoryEntity, ScimPasswordHistoryJpaRepository, Override, ScimPasswordHistoryPersistenceAdapter, jakarta.persistence.Entity, jakarta.persistence.Table, org.hibernate.annotations.DynamicUpdate (+1 more)

### Community 69 - "ScimRequestObservationConventionTests"
Cohesion: 0.17
Nodes (7): Override, ScimRequestObservationConvention, ServerRequestObservationContext, ScimRequestObservationConventionTests, io.micrometer.common.KeyValues, org.springframework.http.server.observation.DefaultServerRequestObservationConvention, org.springframework.http.server.observation.ServerRequestObservationContext

### Community 70 - "ScimConnector"
Cohesion: 0.15
Nodes (5): ConnectorAuthenticationService, ScimConnector, ScimConnectorRepository, InMemoryScimConnectorRepository, Override

### Community 71 - "http.ts"
Cohesion: 0.16
Nodes (16): decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, SessionRequest, ApiDecoder (+8 more)

### Community 73 - "LogContextTests"
Cohesion: 0.16
Nodes (4): Override, LogContext, Scope, LogContextTests

### Community 74 - "ScimConnectorTokenEntity"
Cohesion: 0.16
Nodes (4): ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 75 - "ScimExternalIdEntity"
Cohesion: 0.12
Nodes (8): Override, Key, ScimExternalIdEntity, ScimExternalIdJpaRepository, Override, ScimExternalIdPersistenceAdapter, jakarta.persistence.IdClass, ScimExternalIdEntity.Key

### Community 76 - "ScimDiscoveryIntegrationTests"
Cohesion: 0.16
Nodes (4): ScimConditionalWrites, org.junit.jupiter.api.AfterEach, ScimDiscoveryIntegrationTests, org.springframework.test.web.servlet.request.RequestPostProcessor

### Community 77 - "AuditUserAttribute"
Cohesion: 0.08
Nodes (19): AuditPasswordChangeRefusal, ACCOUNT_DISABLED, ACCOUNT_LOCKED, BAD_CURRENT_PASSWORD, CONTAINS_USER_NAME, REUSED, TOO_LONG, TOO_SHORT (+11 more)

### Community 78 - "ScimUserPatchReader"
Cohesion: 0.22
Nodes (6): Op, ADD, REMOVE, REPLACE, Path, ScimUserPatchReader

### Community 79 - "ScimExceptionHandler.java"
Cohesion: 0.10
Nodes (8): ScimExceptionHandler, InvalidPreconditionException, PasswordHistoryPolicy, PasswordReusedException, PreconditionFailedException, PreconditionRequiredException, ScimExceptionHandlerMetricTests, org.springframework.web.bind.annotation.RestControllerAdvice

### Community 80 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.09
Nodes (22): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Error contract (+14 more)

### Community 81 - "compilerOptions"
Cohesion: 0.09
Nodes (21): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+13 more)

### Community 82 - "AdminConnectorController"
Cohesion: 0.16
Nodes (7): ConnectorSummary, ConnectorTokenSummary, IssuedConnectorToken, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest, RotateTokenRequest

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

### Community 93 - "RefusalTimingEquivalenceTests"
Cohesion: 0.16
Nodes (5): CountingPasswordEncoder, Override, CountingPasswordEncoder, Override, RefusalTimingEquivalenceTests

### Community 94 - "components.json"
Cohesion: 0.11
Nodes (17): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+9 more)

### Community 95 - "auth.helpers.ts"
Cohesion: 0.20
Nodes (10): ADMIN_CREDENTIALS, captureSessionCookie(), expireSession(), login(), loginAs(), postAdminAction(), resetCounterViaApi(), SESSION_COOKIE (+2 more)

### Community 96 - "include"
Cohesion: 0.11
Nodes (16): compilerOptions, types, exclude, extends, include, node, src/**/*.test.ts, src/**/*.test.tsx (+8 more)

### Community 97 - "ScheduledJobMetrics"
Cohesion: 0.25
Nodes (5): ScheduledJobMetrics, ScheduledJobMetricsTests, io.micrometer.core.instrument.Counter, io.micrometer.core.instrument.MeterRegistry, io.micrometer.core.instrument.simple.SimpleMeterRegistry

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
Cohesion: 0.27
Nodes (5): HttpAuditRequestContext, Override, HttpAuditRequestContextTests, MockHttpServletRequest, org.springframework.mock.web.MockHttpServletRequest

### Community 102 - "ScimExceptionHandlerMetricTests.java"
Cohesion: 0.20
Nodes (4): MetricTag, MockHttpServletRequest, ServerRequestObservationContext, MetricTagTests

### Community 104 - "ScimConnectorEntity"
Cohesion: 0.23
Nodes (4): ScimConnectorEntity, ScimConnectorJpaRepository, Override, ScimConnectorPersistenceAdapter

### Community 106 - "stryker.config.json"
Cohesion: 0.12
Nodes (15): cleanTempDir, concurrency, coverageAnalysis, htmlReporter, fileName, jsonReporter, fileName, packageManager (+7 more)

### Community 107 - "Backend"
Cohesion: 0.13
Nodes (14): Audit trail database roles, Audit trail retention, Backend, Build and test, Bundle a frontend, Configuration, Inactivity governance, Log in (+6 more)

### Community 108 - "SessionController"
Cohesion: 0.27
Nodes (5): SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession

### Community 109 - "ScimResourceEntity"
Cohesion: 0.24
Nodes (3): ScimGroupEntity, ScimResourceEntity, ScimGroupJpaRepository

### Community 110 - "org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder"
Cohesion: 0.33
Nodes (3): org.junit.jupiter.api.AfterEach, ScimEndToEndIntegrationTests, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

### Community 111 - ".ofIfMatch"
Cohesion: 0.21
Nodes (3): FakeSaltedEncoder, Override, ScimVersionPreconditionTests

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
Cohesion: 0.23
Nodes (3): AbsoluteSessionLifetimeFilter, AbsoluteSessionLifetimePolicy, AbsoluteSessionLifetimePolicyTests

### Community 117 - "ScimSearchController.java"
Cohesion: 0.19
Nodes (5): ScimListedResource, ScimSearchListing, Search, ScimSearchController, ScimSearchRenderer

### Community 119 - "dependencies"
Cohesion: 0.15
Nodes (13): class-variance-authority, clsx, dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom (+5 more)

### Community 120 - "deploy.sh"
Cohesion: 0.42
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 121 - "ScimUserAttributes"
Cohesion: 0.23
Nodes (4): alwaysReturned(), projectableNames(), schemaAttributes(), ScimUserAttributes

### Community 124 - "AGENTS.md — frontend"
Cohesion: 0.17
Nodes (11): AGENTS.md — frontend, Architecture, Backend contract, Baseline gate, Commands, Component library, Conditional gates, Fallow (+3 more)

### Community 125 - "Rule"
Cohesion: 0.24
Nodes (7): PasswordPolicyViolationException, PasswordRuleViolation, Rule, CONTAINS_USER_NAME, REUSED, TOO_LONG, TOO_SHORT

### Community 126 - "AfterCommitAdapterTests"
Cohesion: 0.25
Nodes (3): AfterCommitAdapter, Override, AfterCommitAdapterTests

### Community 130 - "ScimSecurityChainOrderTests.java"
Cohesion: 0.29
Nodes (4): MockHttpServletRequest, ScimSecurityChainOrderTests, org.assertj.core.api.SoftAssertions, org.springframework.security.web.FilterChainProxy

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

### Community 136 - "AuditAdministrativeRefusal"
Cohesion: 0.22
Nodes (6): AuditAdministrativeRefusal, CREDENTIALLESS_TARGET, LAST_ENABLED_ADMINISTRATOR, PROTECTED_RESOURCE, SELF_DISABLE, SELF_TARGET

### Community 138 - "RecordingCounterService"
Cohesion: 0.31
Nodes (3): Override, RecordingCounterService, UserCounterControllerTests

### Community 140 - "Delivery plan"
Cohesion: 0.20
Nodes (10): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 0a — Permanent lockout, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 4 — Groups and Admin authority, Slice 5 — Complete query protocol (+2 more)

### Community 141 - "front-end"
Cohesion: 0.20
Nodes (9): Backend contract, Component library, front-end, Project structure, Scripts, Setup, Styling, Technology stack (+1 more)

### Community 142 - "AGENTS.md — backend"
Cohesion: 0.22
Nodes (8): AGENTS.md — backend, API contract, Architecture constraints, Baseline gate, Conditional gate: mutation testing, Reading a gate's result, Security-sensitive changes, Verification

### Community 144 - "ScimPasswordChange"
Cohesion: 0.22
Nodes (6): Override, Kind, CLEAR, SET, UNCHANGED, ScimPasswordChange

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

### Community 150 - "ScimTombstoneRepository"
Cohesion: 0.32
Nodes (4): ScimTombstoneRepository, InMemoryScimTombstoneRepository, Override, Tombstone

### Community 151 - "Domain and authority model"
Cohesion: 0.25
Nodes (8): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, Protected recovery resources, SCIM User replaces Account

### Community 152 - "monorepo-base"
Cohesion: 0.25
Nodes (7): Deploying to AWS, Frontend/backend integration, Layout, monorepo-base, Toolchain pins, Working on the backend, Working on the frontend

### Community 154 - "RecordingTransactionManager"
Cohesion: 0.52
Nodes (4): Override, RecordingTransactionManager, org.springframework.transaction.TransactionDefinition, org.springframework.transaction.TransactionStatus

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

## Knowledge Gaps
- **598 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+593 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **67 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ScimUser` connect `ScimUser` to `ScimGroup`, `IdentityAdministrationServiceTests`, `AuditTrail`, `DormancyPolicy`, `.advanceBy`, `ScimGroupServiceTests`, `.require`, `InMemoryAccountSessions`, `ScimUserServiceTests`, `org.springframework.data.jpa.repository.Query`, `org.springframework.boot.test.context.SpringBootTest`, `ScimQuery`, `.given`, `.toDomain`, `.status`, `ScimUserService`, `ScimUserServiceTests.java`, `.seed`, `.created`, `ScimEmail`, `AuditEventRecordingIntegrationTests`, `.created`, `.of`?**
  _High betweenness centrality (0.035) - this node is a cross-community bridge._
- **Why does `AuditTrail` connect `AuditTrail` to `ScimGroup`, `IdentityAdministrationServiceTests`, `DormancyPolicy`, `ScimGroupServiceTests`, `AuditAdministrativeRefusal`, `InMemoryAccountSessions`, `org.springframework.boot.test.context.SpringBootTest`, `AuditTrailServiceTests`, `AuditTrailServiceTests.java`, `AuditAppendOnlyIntegrationTests`, `org.springframework.transaction.annotation.Transactional`, `AuthController`, `RecordingAuditTrail`, `ScimUserService`, `.seed`, `ScimConnectorToken`, `org.junit.jupiter.api.BeforeEach`, `.created`, `AuditUserAttribute`?**
  _High betweenness centrality (0.023) - this node is a cross-community bridge._
- **Why does `AuditOperation` connect `AuditOperation` to `AuditEventRecordingIntegrationTests`, `ScimGroup`, `AuditTrail`, `DormancyPolicy`, `jakarta.persistence.Entity`, `ScimGroupServiceTests`, `.require`, `InMemoryAccountSessions`, `ScimUserServiceTests.java`, `org.springframework.boot.test.context.SpringBootTest`, `ScimConnectorLifecycleIntegrationTests`, `AuditTrailServiceTests`, `AuditTrailServiceTests.java`, `AuditAppendOnlyIntegrationTests`, `org.springframework.transaction.annotation.Transactional`, `ScimUserProvisioningIntegrationTests`, `RecordingAuditTrail`?**
  _High betweenness centrality (0.020) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _598 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.junit.jupiter.api.Test` be split into smaller, more focused modules?**
  _Cohesion score 0.03827493261455526 - nodes in this community are weakly interconnected._
- **Should `ScimGroup` be split into smaller, more focused modules?**
  _Cohesion score 0.05958485958485959 - nodes in this community are weakly interconnected._
- **Should `IdentityAdministrationServiceTests` be split into smaller, more focused modules?**
  _Cohesion score 0.07465667915106117 - nodes in this community are weakly interconnected._