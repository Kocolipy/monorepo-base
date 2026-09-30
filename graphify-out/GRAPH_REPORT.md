# Graph Report - monorepo-base-self-read  (2026-09-30)

## Corpus Check
- 446 files · ~290,173 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 4714 nodes · 15769 edges · 222 communities (157 shown, 65 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 2156 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `2368c051`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.junit.jupiter.api.Test
- ScimGroup
- IdentityAdministrationServiceTests
- AuditTrail
- InactivityGovernanceIntegrationTests.java
- .advanceBy
- ScimGroupServiceTests
- ScimUser
- .require
- Attribute
- ConnectorAdministrationServiceTests.java
- ScimSecurityChainOrderTests.java
- ScimUserServiceTests
- ScimGroupProvisioningIntegrationTests
- org.springframework.data.jpa.repository.Query
- ScimAttributeProjectionTests
- org.junit.jupiter.params.ParameterizedTest
- org.junit.jupiter.api.BeforeEach
- ScimQuery
- AuditTrailServiceTests
- AuthenticatedConnector
- AuditTrailServiceTests.java
- UserCounter
- AuditAppendOnlyIntegrationTests
- ScimFilterPath
- org.springframework.transaction.annotation.Transactional
- AuthController.java
- .given
- ScimQueryProtocolIntegrationTests
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
- ScimUserProfile
- ScimUserPatchOperation
- ScimResourceType
- .of
- .created
- AuditOperation
- IdentitySummary
- ScimConnectorToken
- org.springframework.boot.test.context.TestConfiguration
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
- ScimEmail
- AuditEventRecordingIntegrationTests
- ScimBearerAuthenticationFilterTests
- App.tsx
- ScimSeedService
- jakarta.persistence.Entity
- ScimRequestObservationConventionTests
- ScimConnector
- http.ts
- org.springframework.context.annotation.Bean
- LogContextTests
- ScimConnectorTokenEntity
- ScimExternalIdEntity
- ScimDiscoveryIntegrationTests
- AuditUserAttribute
- .handle
- ScimUserServiceTests.java
- SCIM 2.0 account-management specification plan
- compilerOptions
- AdminConnectorController
- .sessionsOf
- PasswordChangeLifecycleIntegrationTests
- ScimUserPersistenceAdapter
- scripts
- ScimDiscovery
- ScimUserPatchReaderTests
- Testing Guide
- IndexedSessions
- SpaFrontendTests
- AuditEvent
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
- Tokens
- ScimConnectorEntity
- ScimUserAttributesTests
- stryker.config.json
- Backend
- SessionController
- ScimResourceEntity
- ScimEndToEndIntegrationTests
- FakeSaltedEncoder
- Architecture
- auth-context-value.ts
- sources.ts
- AGENTS.md
- AbsoluteSessionLifetimePolicy
- ScimAttributeProjection
- ScimGroupPersistenceAdapter
- dependencies
- deploy.sh
- .deactivateOverdueUsers
- org.springframework.web.bind.annotation.GetMapping
- .get
- AGENTS.md — frontend
- Rule
- org.junit.jupiter.api.AfterEach
- ConnectorAuthenticationServiceTests
- SelfReadIntegrationTests
- ScimConnectorTokenTests
- org.springframework.stereotype.Repository
- RFC requirements and implications
- package.json
- showcase.test.tsx
- mutate
- mvnw
- Operator
- .requiresWriteScope
- UserCounterService
- RequestIdFilterTests
- Delivery plan
- front-end
- AGENTS.md — backend
- ScimUserController
- AuditPasswordChangeRefusal
- SCIM 2.0 account-management research
- Credential and cryptographic policy
- ignorePatterns
- ManagementSessionConfiguration.java
- .summarize
- ContainerTestConfiguration.java
- Domain and authority model
- monorepo-base
- ScimGroupMemberId
- Cause
- MockFilterChain
- org.junit.jupiter.params.provider.CsvSource
- org.junit.jupiter.params.provider.Arguments
- CONTEXT
- 1. Count login attempts on the login path
- 2. Revoke a disabled account's sessions after the commit
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- 5. Serialize scheduled jobs on per-job lock rows
- ScimUserProfileTests
- CountingPasswordEncoder
- ArchitectureTest
- LockoutHasNoDurationTests
- 3. ECS-structured logging with redaction enforced structurally
- Domain Docs
- Issue tracker: GitHub
- Query contract
- CountingPasswordEncoder
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

## God Nodes (most connected - your core abstractions)
1. `ScimUser` - 154 edges
2. `AuditTrail` - 86 edges
3. `ScimGroup` - 82 edges
4. `IdentityAdministrationServiceTests` - 77 edges
5. `ScimGroupProvisioningIntegrationTests` - 77 edges
6. `RecordingAuditTrail` - 75 edges
7. `ScimFilterPath` - 74 edges
8. `ScimUserRepository` - 74 edges
9. `AuditOperation` - 71 edges
10. `ScimResourceType` - 69 edges

## Surprising Connections (you probably didn't know these)
- `AuditRetentionScheduleConfig` --references--> `AuditRetentionService`  [EXTRACTED]
  backend/src/main/java/com/example/backend/audit/config/AuditRetentionScheduleConfig.java → backend/src/main/java/com/example/backend/audit/application/AuditRetentionService.java
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

## Communities (222 total, 65 thin omitted)

### Community 0 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (16): NormalizedDisplayName, AuditRetentionStartupTests, DormancyPolicyStartupTests, Connectors, ConnectorTokenPolicyTests, Lifetime, RotationOverlap, NormalizedDisplayNameTests (+8 more)

### Community 1 - "ScimGroup"
Cohesion: 0.07
Nodes (19): DuplicateDisplayNameException, NormalizedUserName, ofStoredValue(), ReservedResourceName, ADMIN_GROUP, BOOTSTRAP_ADMIN, ScimGroup, ScimGroupMember (+11 more)

### Community 3 - "AuditTrail"
Cohesion: 0.07
Nodes (20): AuditTrail, AfterCommit, LoginAttemptService, LoginIdentityService, PasswordChangeService, ScimUserSessionRevocation, SelfReadService, AccountSessions (+12 more)

### Community 4 - "InactivityGovernanceIntegrationTests.java"
Cohesion: 0.07
Nodes (24): AuditRetentionScheduleConfig, Override, DormantAuthorityRevocationService, InactivityDeactivationService, PasswordChangeGraceService, DormancyPolicyConfig, DormancyScheduleConfig, Override (+16 more)

### Community 5 - ".advanceBy"
Cohesion: 0.13
Nodes (3): DormancyRun, DormantAuthorityRevocationServiceTests, InactivityDeactivationServiceTests

### Community 6 - "ScimGroupServiceTests"
Cohesion: 0.14
Nodes (5): NewScimGroup, AddMembers, RemoveMembers, ScimGroupReplacement, ScimGroupServiceTests

### Community 7 - "ScimUser"
Cohesion: 0.06
Nodes (7): DuplicateUserNameException, ScimLoginState, ScimUser, InMemoryScimQueryRepository, InMemoryScimUserRepository, Override, java.lang.reflect.RecordComponent

### Community 8 - ".require"
Cohesion: 0.09
Nodes (3): LoginAttemptServiceTests, LoginLockoutTests, org.springframework.security.core.AuthenticationException

### Community 9 - "Attribute"
Cohesion: 0.04
Nodes (44): And, Attribute, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE (+36 more)

### Community 10 - "ConnectorAdministrationServiceTests.java"
Cohesion: 0.09
Nodes (12): AuditRetentionService, AuditRetentionPolicyConfig, AuditEventRetention, AuditRetentionPolicy, LoggingOperationalAlerts, LogEvent, ScheduledJobMetrics, ConnectorTokenPolicy (+4 more)

### Community 11 - "ScimSecurityChainOrderTests.java"
Cohesion: 0.14
Nodes (10): ScimSecurityConfig, BackendApplicationTests, MockHttpServletRequest, ScimSecurityChainOrderTests, javax.sql.DataSource, org.assertj.core.api.SoftAssertions, org.springframework.core.annotation.Order, org.springframework.security.web.FilterChainProxy (+2 more)

### Community 12 - "ScimUserServiceTests"
Cohesion: 0.12
Nodes (6): RemovePassword, SetPassword, Revocation, ScimUserServiceTests, Override, Tombstone

### Community 14 - "org.springframework.data.jpa.repository.Query"
Cohesion: 0.11
Nodes (10): UserCounterJpaRepository, ScimGroupJpaRepository, ScimGroupMemberJpaRepository, ScimGroupMemberRow, ScimResourceJpaRepository, ScimUserJpaRepository, org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.jpa.repository.Lock (+2 more)

### Community 16 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.07
Nodes (9): SpaRoutes, LockoutPolicyTests, SpaRoutesScimNamespaceTests, ReservedServerPaths, SpaRoutesTests, SpaShell, org.junit.jupiter.api.Nested, org.junit.jupiter.params.ParameterizedTest (+1 more)

### Community 17 - "org.junit.jupiter.api.BeforeEach"
Cohesion: 0.22
Nodes (27): RequestIdFilter, ConnectorAdministrationService, ConnectorTokenScope, READ_ONLY, READ_WRITE, ContainerTestConfiguration, UserCounterServiceTests, InMemorySessionRegistryConfiguration (+19 more)

### Community 18 - "ScimQuery"
Cohesion: 0.10
Nodes (11): Hit, Result, ScimQuery, ScimSort, Override, ScimQueryPersistenceAdapter, ScimSearchServiceTests, ScimSortTests (+3 more)

### Community 20 - "AuthenticatedConnector"
Cohesion: 0.08
Nodes (12): ScimGroupListing, ScimGroupResource, ScimListedResource, ScimSearchListing, ScimGroupController, ScimGroupRenderer, ScimSearchController, ScimSearchRenderer (+4 more)

### Community 21 - "AuditTrailServiceTests.java"
Cohesion: 0.07
Nodes (28): AuditAdministrativeRefusal, CREDENTIALLESS_TARGET, LAST_ENABLED_ADMINISTRATOR, PROTECTED_RESOURCE, SELF_DISABLE, SELF_TARGET, AuditGroupAttribute, DISPLAY_NAME (+20 more)

### Community 22 - "UserCounter"
Cohesion: 0.15
Nodes (4): UserCounter, Override, UserCounterPersistenceAdapter, UserCounterTests

### Community 23 - "AuditAppendOnlyIntegrationTests"
Cohesion: 0.05
Nodes (18): AuditRetentionServiceTests, CountingRetention, Override, AuditAppendOnlyIntegrationTests, CapturedLog, Override, EcsLogCapture, Override (+10 more)

### Community 24 - "ScimFilterPath"
Cohesion: 0.08
Nodes (41): of(), parent(), ScimFilterPath, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE (+33 more)

### Community 25 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.16
Nodes (3): AuditTrailService, Override, org.springframework.transaction.annotation.Transactional

### Community 26 - "AuthController.java"
Cohesion: 0.10
Nodes (8): CurrentPasswordRejectedException, ForbiddenIdentityChangeException, UnknownIdentityException, UnknownConnectorException, InvalidConnectorTokenLifetimeException, org.springframework.web.bind.annotation.DeleteMapping, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.ResponseStatus

### Community 27 - ".given"
Cohesion: 0.09
Nodes (6): Override, Group, SelfRecord, LoginIdentityServiceTests, SelfReadServiceTests, org.springframework.security.core.userdetails.UserDetails

### Community 30 - "ScimUserPatchOperationTests"
Cohesion: 0.16
Nodes (7): AddEmails, EmailUpdate, RemoveEmailPart, RemoveEmails, RemoveText, UpdateEmails, ScimUserPatchOperationTests

### Community 31 - "RecordingAuditTrail"
Cohesion: 0.14
Nodes (3): Override, Recorded, RecordingAuditTrail

### Community 32 - "OperationalTelemetryIntegrationTests"
Cohesion: 0.10
Nodes (14): Builder, ManagementPortIntegrationTests, Builder, SuppressWarnings, OperationalTelemetryIntegrationTests, Session, CookieManager, java.net.CookieManager (+6 more)

### Community 33 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.09
Nodes (14): Override, MetricTag, Override, Override, ScimBearerAuthenticationFilter, ScimBearerChallenge, Override, ScimWriteScopeRule (+6 more)

### Community 35 - "ScimDeletionIntegrationTests"
Cohesion: 0.08
Nodes (6): TransactionTemplate, InactivityGovernanceIntegrationTests, ThrowingRunnable, Override, ScimDeletionIntegrationTests, FunctionalInterface

### Community 36 - "ScimPageRequest"
Cohesion: 0.12
Nodes (5): ScimQueryRequest, InvalidScimQueryException, token(), ScimPageRequest, ScimQueryRequestTests

### Community 37 - "ConnectorTokenSecretTests"
Cohesion: 0.10
Nodes (8): SecureRandom, ConnectorTokenDigest, Override, Minted, Presented, ConnectorTokenSecretTests, java.security.MessageDigest, java.security.SecureRandom

### Community 38 - ".toDomain"
Cohesion: 0.09
Nodes (3): ScimLoginStateValue, ScimUserEmailValue, ScimUserEntity

### Community 39 - ".status"
Cohesion: 0.10
Nodes (5): MockHttpSession, ProbeController, SecurityConfigTests, AdminAccountEndpointTests, MockHttpSession

### Community 40 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.05
Nodes (36): 1. Get VPC Info, 2. Deploy, 3. Get Private Key (if CloudFormation created it), 4. Deploy Application, 5. Test, Quick Start, Alerts, Architecture (+28 more)

### Community 41 - "ScimUserProfile"
Cohesion: 0.09
Nodes (10): NewScimUser, ScimUserListing, Override, ScimUserReplacement, ScimUserResource, ScimGroupReference, ScimUserProfile, ScimVersionPrecondition (+2 more)

### Community 42 - "ScimUserPatchOperation"
Cohesion: 0.10
Nodes (23): ScimEmailPart, PRIMARY, TYPE, VALUE, ScimName, MergeName, NamePart, FAMILY_NAME (+15 more)

### Community 43 - "ScimResourceType"
Cohesion: 0.10
Nodes (8): ScimResourceType, GROUP, USER, Comparison, ScimQuerySql, Override, ScimQuerySqlTests, Override

### Community 46 - "AuditOperation"
Cohesion: 0.06
Nodes (31): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+23 more)

### Community 47 - "IdentitySummary"
Cohesion: 0.19
Nodes (7): IdentitySummary, AdminAccountController, AdminAccountControllerTests, Override, RecordingService, java.security.Principal, org.springframework.web.bind.annotation.PostMapping

### Community 48 - "ScimConnectorToken"
Cohesion: 0.12
Nodes (4): ScimConnectorToken, ScimConnectorTokenRepository, InMemoryScimConnectorTokenRepository, Override

### Community 49 - "org.springframework.boot.test.context.TestConfiguration"
Cohesion: 0.23
Nodes (6): SessionRegistryConfiguration, SessionRegistryConfiguration, SessionRegistryConfiguration, DormancyTestClockConfiguration, org.springframework.boot.test.context.TestConfiguration, org.springframework.context.annotation.Primary

### Community 51 - "compilerOptions"
Cohesion: 0.07
Nodes (29): compilerOptions, allowImportingTsExtensions, baseUrl, isolatedModules, jsx, lib, module, moduleDetection (+21 more)

### Community 52 - "tools.jackson.databind.JsonNode"
Cohesion: 0.14
Nodes (8): RemoveAllMembers, ReplaceMembers, ScimGroupPatchOperation, SetDisplayName, ScimGroupRequestReader, ScimUserRequestReader, java.util.regex.Pattern, tools.jackson.databind.JsonNode

### Community 54 - "ScimFilterParser"
Cohesion: 0.23
Nodes (4): InvalidScimFilterException, Comparison, ResolvedPath, ScimFilterParser

### Community 55 - "ScimUserEdit"
Cohesion: 0.14
Nodes (4): Override, ScimPasswordChange, ScimUserEdit, Override

### Community 56 - "AuthControllerTests"
Cohesion: 0.14
Nodes (10): LoginOutcome, AuthController, ChangePasswordRequest, Override, LoginRequest, UserResponse, AuthControllerTests, MockHttpServletResponse (+2 more)

### Community 57 - "AttributeRef"
Cohesion: 0.12
Nodes (22): Attribute, ScimAuditFilterShapes, And, AttributeRef, Comparison, Not, Operator, CO (+14 more)

### Community 58 - "Attribute"
Cohesion: 0.13
Nodes (13): Kind, CLEAR, SET, UNCHANGED, Attribute, Kind, BOOLEAN, COMPLEX (+5 more)

### Community 60 - "devDependencies"
Cohesion: 0.07
Nodes (27): eslint, @eslint/js, eslint-plugin-react-hooks, fallow, devDependencies, dependency-cruiser, eslint, @eslint/js (+19 more)

### Community 61 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 63 - "ScimEmail"
Cohesion: 0.18
Nodes (4): ScimEmail, Condition, ScimEmailFilter, ScimEmailTests

### Community 64 - "AuditEventRecordingIntegrationTests"
Cohesion: 0.09
Nodes (4): AuditEventRecordingIntegrationTests, MockHttpSession, ResultActions, ScimLoginStateTests

### Community 66 - "App.tsx"
Cohesion: 0.13
Nodes (17): App(), AuthRole, AuthProvider(), AuthStatus, useAuth(), GuestRoute(), ProtectedRoute(), SessionRoute() (+9 more)

### Community 67 - "ScimSeedService"
Cohesion: 0.23
Nodes (4): ScimSeedService, SeededIdentity, ScimSeedConfig, org.springframework.boot.ApplicationRunner

### Community 68 - "jakarta.persistence.Entity"
Cohesion: 0.13
Nodes (9): UserCounterEntity, ScimGroupMemberEntity, ScimPasswordHistoryEntity, ScimPasswordHistoryJpaRepository, Override, ScimPasswordHistoryPersistenceAdapter, jakarta.persistence.Entity, jakarta.persistence.Table (+1 more)

### Community 69 - "ScimRequestObservationConventionTests"
Cohesion: 0.17
Nodes (7): Override, ScimRequestObservationConvention, ServerRequestObservationContext, ScimRequestObservationConventionTests, io.micrometer.common.KeyValues, org.springframework.http.server.observation.DefaultServerRequestObservationConvention, org.springframework.http.server.observation.ServerRequestObservationContext

### Community 70 - "ScimConnector"
Cohesion: 0.12
Nodes (7): ConnectorAuthenticationService, ConnectorTokenSecret, ScimConnector, ScimConnectorRepository, InMemoryScimConnectorRepository, Override, org.springframework.mock.web.MockHttpServletResponse

### Community 71 - "http.ts"
Cohesion: 0.16
Nodes (16): decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, SessionRequest, ApiDecoder (+8 more)

### Community 72 - "org.springframework.context.annotation.Bean"
Cohesion: 0.10
Nodes (14): LoginService, LoginLockoutConfig, PasswordEncoder, SecurityConfig, LockoutPolicy, PasswordNormalization, PasswordPolicy, SecurityConfigPasswordEncoderTests (+6 more)

### Community 73 - "LogContextTests"
Cohesion: 0.16
Nodes (4): Override, LogContext, Scope, LogContextTests

### Community 74 - "ScimConnectorTokenEntity"
Cohesion: 0.15
Nodes (4): ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 75 - "ScimExternalIdEntity"
Cohesion: 0.13
Nodes (8): Override, Key, ScimExternalIdEntity, ScimExternalIdJpaRepository, Override, ScimExternalIdPersistenceAdapter, jakarta.persistence.IdClass, ScimExternalIdEntity.Key

### Community 77 - "AuditUserAttribute"
Cohesion: 0.13
Nodes (11): AuditUserAttribute, ACTIVE, DISPLAY_NAME, EMAILS, GROUPS, LOCALE, NAME, PASSWORD (+3 more)

### Community 78 - ".handle"
Cohesion: 0.11
Nodes (11): ScimErrorException, ScimExceptionHandler, Op, ADD, REMOVE, REPLACE, Path, ScimUserPatchReader (+3 more)

### Community 79 - "ScimUserServiceTests.java"
Cohesion: 0.08
Nodes (11): PasswordHistoryPolicy, PasswordReusedException, PreconditionFailedException, PreconditionRequiredException, ProtectedResourceException, Reason, MUTABILITY, NO_TARGET (+3 more)

### Community 80 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.09
Nodes (22): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Error contract (+14 more)

### Community 81 - "compilerOptions"
Cohesion: 0.09
Nodes (21): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+13 more)

### Community 82 - "AdminConnectorController"
Cohesion: 0.30
Nodes (5): IssuedConnectorToken, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest, RotateTokenRequest

### Community 84 - "PasswordChangeLifecycleIntegrationTests"
Cohesion: 0.27
Nodes (3): Cookie, PasswordChangeLifecycleIntegrationTests, jakarta.servlet.http.Cookie

### Community 85 - "ScimUserPersistenceAdapter"
Cohesion: 0.18
Nodes (3): Override, ScimUserPersistenceAdapter, org.springframework.data.domain.Sort

### Community 86 - "scripts"
Cohesion: 0.10
Nodes (20): scripts, analyze, build, dev, format, format:check, lint, preview (+12 more)

### Community 88 - "ScimUserPatchReaderTests"
Cohesion: 0.15
Nodes (4): RemoveActive, SetActive, SetText, ScimUserPatchReaderTests

### Community 89 - "Testing Guide"
Cohesion: 0.11
Nodes (18): Architecture tests, Assert exactly, not loosely, Calling the API from a spec, Coverage excludes — why the list is explicit, E2E, Fallow, Flakiness — the rules that keep these tests green, Forcing a refused request (+10 more)

### Community 90 - "IndexedSessions"
Cohesion: 0.22
Nodes (6): Override, AccountSessionsAdapterTests, IndexedSessions, Override, MapSession, org.springframework.session.MapSession

### Community 91 - "SpaFrontendTests"
Cohesion: 0.20
Nodes (6): ModelAndView, Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.web.servlet.ModelAndView

### Community 92 - "AuditEvent"
Cohesion: 0.15
Nodes (10): AuditEvent, AuditEventRepository, AuditOutcome, FAILURE, SUCCESS, AuditEventJpaRepository, AuditEventPersistenceAdapter, Override (+2 more)

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
Cohesion: 0.24
Nodes (5): HttpAuditRequestContext, Override, HttpAuditRequestContextTests, MockHttpServletRequest, org.springframework.mock.web.MockHttpServletRequest

### Community 102 - "MetricTagTests"
Cohesion: 0.27
Nodes (3): MockHttpServletRequest, ServerRequestObservationContext, MetricTagTests

### Community 104 - "ScimConnectorEntity"
Cohesion: 0.21
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
Cohesion: 0.28
Nodes (5): SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession

### Community 110 - "ScimEndToEndIntegrationTests"
Cohesion: 0.27
Nodes (3): org.junit.jupiter.api.AfterEach, ScimEndToEndIntegrationTests, org.springframework.test.web.servlet.request.RequestPostProcessor

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
Cohesion: 0.16
Nodes (6): AbsoluteSessionLifetimeFilter, AbsoluteSessionLifetimePolicy, ScimReleaseGate, ScimReleaseGateFilter, AbsoluteSessionLifetimePolicyTests, org.springframework.web.filter.OncePerRequestFilter

### Community 117 - "ScimAttributeProjection"
Cohesion: 0.21
Nodes (6): SuppressWarnings, Kind, GROUP, USER, ScimAttributeProjection, Search

### Community 119 - "dependencies"
Cohesion: 0.15
Nodes (13): class-variance-authority, clsx, dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom (+5 more)

### Community 120 - "deploy.sh"
Cohesion: 0.42
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 122 - "org.springframework.web.bind.annotation.GetMapping"
Cohesion: 0.22
Nodes (6): UnknownSessionIdentityException, SelfController, ScimDiscoveryController, org.springframework.web.bind.annotation.GetMapping, org.springframework.web.bind.annotation.RequestMapping, org.springframework.web.bind.annotation.RestController

### Community 124 - "AGENTS.md — frontend"
Cohesion: 0.17
Nodes (11): AGENTS.md — frontend, Architecture, Backend contract, Baseline gate, Commands, Component library, Conditional gates, Fallow (+3 more)

### Community 125 - "Rule"
Cohesion: 0.24
Nodes (7): PasswordPolicyViolationException, PasswordRuleViolation, Rule, CONTAINS_USER_NAME, REUSED, TOO_LONG, TOO_SHORT

### Community 126 - "org.junit.jupiter.api.AfterEach"
Cohesion: 0.11
Nodes (9): AccountSessionsAdapter, AfterCommitAdapter, Override, RedisSessionRevocationIntegrationTests, AfterCommitAdapterTests, org.junit.jupiter.api.AfterEach, org.springframework.session.FindByIndexNameSessionRepository, org.springframework.session.Session (+1 more)

### Community 130 - "org.springframework.stereotype.Repository"
Cohesion: 0.18
Nodes (6): AuditEventRetentionAdapter, Override, Override, ScheduledJobLockAdapter, ScimTombstonePersistenceAdapter, org.springframework.stereotype.Repository

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

### Community 138 - "UserCounterService"
Cohesion: 0.11
Nodes (7): UserCounterService, CountResponse, UserCounterController, UserCounterRepository, Override, RecordingCounterService, UserCounterControllerTests

### Community 140 - "Delivery plan"
Cohesion: 0.20
Nodes (10): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 0a — Permanent lockout, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 4 — Groups and Admin authority, Slice 5 — Complete query protocol (+2 more)

### Community 141 - "front-end"
Cohesion: 0.20
Nodes (9): Backend contract, Component library, front-end, Project structure, Scripts, Setup, Styling, Technology stack (+1 more)

### Community 142 - "AGENTS.md — backend"
Cohesion: 0.22
Nodes (8): AGENTS.md — backend, API contract, Architecture constraints, Baseline gate, Conditional gate: mutation testing, Reading a gate's result, Security-sensitive changes, Verification

### Community 144 - "AuditPasswordChangeRefusal"
Cohesion: 0.22
Nodes (8): AuditPasswordChangeRefusal, ACCOUNT_DISABLED, ACCOUNT_LOCKED, BAD_CURRENT_PASSWORD, CONTAINS_USER_NAME, REUSED, TOO_LONG, TOO_SHORT

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

### Community 150 - "ContainerTestConfiguration.java"
Cohesion: 0.28
Nodes (6): GenericContainer, org.springframework.boot.testcontainers.service.connection.ServiceConnection, org.testcontainers.containers.GenericContainer, org.testcontainers.containers.PostgreSQLContainer, org.testcontainers.utility.DockerImageName, PostgreSQLContainer

### Community 151 - "Domain and authority model"
Cohesion: 0.25
Nodes (8): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, Protected recovery resources, SCIM User replaces Account

### Community 152 - "monorepo-base"
Cohesion: 0.25
Nodes (7): Deploying to AWS, Frontend/backend integration, Layout, monorepo-base, Toolchain pins, Working on the backend, Working on the frontend

### Community 153 - "ScimGroupMemberId"
Cohesion: 0.24
Nodes (3): Override, ScimGroupMemberId, jakarta.persistence.Embeddable

### Community 154 - "Cause"
Cohesion: 0.25
Nodes (6): Cause, ADMIN_MEMBERSHIP_REMOVED, DEACTIVATED, DELETED, PASSWORD_CHANGED, USER_NAME_CHANGED

### Community 155 - "MockFilterChain"
Cohesion: 0.24
Nodes (3): AbsoluteSessionLifetimeFilterTests, MockFilterChain, org.springframework.mock.web.MockFilterChain

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
- **65 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ScimUser` connect `ScimUser` to `ScimGroup`, `IdentityAdministrationServiceTests`, `AuditTrail`, `InactivityGovernanceIntegrationTests.java`, `.advanceBy`, `ScimGroupServiceTests`, `.require`, `ConnectorAdministrationServiceTests.java`, `ScimUserServiceTests`, `org.junit.jupiter.api.BeforeEach`, `AuthenticatedConnector`, `AuditAppendOnlyIntegrationTests`, `.given`, `.toDomain`, `ScimUserProfile`, `.of`, `.created`, `AuditEventRecordingIntegrationTests`, `ScimSeedService`, `org.springframework.context.annotation.Bean`, `ScimUserServiceTests.java`, `.sessionsOf`, `ScimUserPersistenceAdapter`, `.deactivateOverdueUsers`, `.get`, `org.junit.jupiter.api.AfterEach`?**
  _High betweenness centrality (0.041) - this node is a cross-community bridge._
- **Why does `AuditTrail` connect `AuditTrail` to `ScimGroup`, `IdentityAdministrationServiceTests`, `InactivityGovernanceIntegrationTests.java`, `.advanceBy`, `ConnectorAdministrationServiceTests.java`, `org.junit.jupiter.api.BeforeEach`, `AuditTrailServiceTests`, `AuditTrailServiceTests.java`, `AuditAppendOnlyIntegrationTests`, `org.springframework.transaction.annotation.Transactional`, `AuthController.java`, `RecordingAuditTrail`, `.of`, `ScimConnectorToken`, `AuthControllerTests`, `.issueToken`, `ScimSeedService`, `AuditUserAttribute`, `.deactivateOverdueUsers`?**
  _High betweenness centrality (0.026) - this node is a cross-community bridge._
- **Why does `AuditOperation` connect `AuditOperation` to `AuditEventRecordingIntegrationTests`, `ScimGroup`, `jakarta.persistence.Entity`, `ScimGroupServiceTests`, `org.springframework.context.annotation.Bean`, `ConnectorAdministrationServiceTests.java`, `.of`, `ScimUserServiceTests.java`, `org.junit.jupiter.api.BeforeEach`, `ScimConnectorLifecycleIntegrationTests`, `AuditTrailServiceTests`, `AuditTrailServiceTests.java`, `AuditAppendOnlyIntegrationTests`, `org.springframework.transaction.annotation.Transactional`, `ScimUserProvisioningIntegrationTests`, `AuditEvent`, `RecordingAuditTrail`?**
  _High betweenness centrality (0.021) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _598 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.junit.jupiter.api.Test` be split into smaller, more focused modules?**
  _Cohesion score 0.03819849874895746 - nodes in this community are weakly interconnected._
- **Should `ScimGroup` be split into smaller, more focused modules?**
  _Cohesion score 0.06666666666666667 - nodes in this community are weakly interconnected._
- **Should `IdentityAdministrationServiceTests` be split into smaller, more focused modules?**
  _Cohesion score 0.07946069994262765 - nodes in this community are weakly interconnected._