# Graph Report - monorepo-base-seed-fix  (2026-10-01)

## Corpus Check
- 473 files · ~310,839 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 16 file(s) not represented in the graph (top: (none) 10, .example 1, .properties 1)

## Summary
- 5189 nodes · 18194 edges · 200 communities (146 shown, 54 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 2317 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a7988266`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.junit.jupiter.api.Test
- InMemoryScimGroupRepository
- IdentityAdministrationServiceTests
- ScimVersionPrecondition
- connectors.tsx
- .advanceBy
- .of
- ScimUser
- .require
- Attribute
- org.springframework.stereotype.Service
- org.springframework.context.annotation.Bean
- ScimUserServiceTests
- ScimGroupProvisioningIntegrationTests
- org.springframework.data.jpa.repository.Query
- ScimAttributeProjectionTests
- org.junit.jupiter.params.ParameterizedTest
- assertthat
- .of
- AuditTrail
- ScimGroupResource
- set
- UserCounter
- AuditAppendOnlyIntegrationTests
- ScimQueryVocabulary.java
- org.springframework.transaction.annotation.Transactional
- AuthController.java
- .given
- .get
- ScimFilterParserTests
- ScimUserPatchOperationTests
- RecordingAuditTrail
- OperationalTelemetryIntegrationTests.java
- jakarta.servlet.http.HttpServletRequest
- .created
- ScimDeletionIntegrationTests
- ScimGroupController.java
- ConnectorTokenSecretTests
- .toDomain
- PasswordChangeLifecycleIntegrationTests
- AWS CloudFormation Deployment Guide
- SecurityConfig.java
- ScimUserServiceTests.java
- ScimResourceType
- .seed
- .created
- AuditOperation
- AdminAccountControllerTests.java
- ScimSeedService
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
- org.junit.jupiter.api.BeforeEach
- ScimEmail
- AuditEventRecordingIntegrationTests
- ScimBearerAuthenticationFilterTests
- session-route.ts
- .delete
- AuditEventEntity
- ScimRequestObservationConvention
- ScimConnector
- apiFetch
- ScimUserRepository
- LogContextTests
- ConnectorTokenScope
- org.springframework.stereotype.Repository
- ScimDiscoveryIntegrationTests
- AuditUserAttribute
- .handle
- ReservedResourceName
- SCIM 2.0 account-management specification plan
- compilerOptions
- LoginLockoutTests
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
- RefusalTimingEquivalenceTests
- components.json
- auth.helpers.ts
- tsconfig.test.json
- ScheduledJobMetricsTests
- AuditFilterShape
- accounts.tsx
- lib.sh
- org.springframework.mock.web.MockHttpServletRequest
- MetricTagTests
- AuditListingEndToEndIntegrationTests
- ScimAuditFilterShapesTests.java
- ScimExceptionHandlerMetricTests.java
- stryker.config.json
- Backend
- SessionController
- .overlapEnd
- org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
- .ofIfMatch
- The layers
- auth-context-value.ts
- vitest
- AGENTS.md
- ScimConditionalWriteIntegrationTests
- ScimQuerySqlTests
- ScimGroup
- ScimBearerAuthenticationFilter.java
- deploy.sh
- .deactivateOverdueUsers
- ScheduledJob
- org.springframework.boot.test.context.TestConfiguration
- AGENTS.md — frontend
- .changePassword
- InactivityGovernanceIntegrationTests.java
- AuditEvent
- SelfReadIntegrationTests
- ScimAttributeProjection
- DormancyPolicyStartupTests.java
- RFC requirements and implications
- package.json
- .fromSearchRequest
- AuditEventQueryTests
- mvnw
- Operator
- .requiresWriteScope
- org.springframework.web.bind.annotation.PostMapping
- RequestIdFilterTests
- Delivery plan
- front-end
- AGENTS.md — backend
- AuthenticatedConnector
- ScimReleaseGate
- SCIM 2.0 account-management research
- Credential and cryptographic policy
- ScimPatchRefusedException
- ManagementSessionConfiguration.java
- ScimRequestObservationConventionTests
- AfterCommitAdapterTests.java
- Quick Start
- monorepo-base
- InMemoryScimExternalIdRepository
- uuid
- SelfControllerTests
- ScimQueryRequestTests
- ScimExceptionHandler.java
- CONTEXT
- Common Tasks
- AbsoluteSessionLifetimePolicyTests
- Key
- 5. Serialize scheduled jobs on per-job lock rows
- ContainerTestConfiguration
- CountingPasswordEncoder
- ArchitectureTest.java
- EcsLogFormatTests
- PasswordPolicy
- Domain Docs
- Issue tracker: GitHub
- Query contract
- AbsoluteSessionLifetimePolicy
- Definition of Done
- Write semantics
- ScimExceptionHandlerMetricTests
- cleanup.sh
- get-vpc-info.sh
- Kiro: graphify enforcement
- dev-stop.sh
- .acquire
- Graphify Runner
- ScimSchemas
- dev.sh
- integration-test.sh
- semgrep.sh
- verify.sh
- CLAUDE.md
- prettier.config.mjs
- graphify-guard.sh
- graphify-refresh.sh
- bootstrap.sh
- package.sh
- com.example:backend
- list
- LockoutHasNoDurationTests.java
- .of
- App.tsx
- org.junit.jupiter.params.provider.Arguments

## God Nodes (most connected - your core abstractions)
1. `ScimUser` - 147 edges
2. `AuditTrail` - 83 edges
3. `ScimGroup` - 81 edges
4. `ScimUserRepository` - 80 edges
5. `AuditOperation` - 78 edges
6. `ScimGroupProvisioningIntegrationTests` - 78 edges
7. `ScimFilterPath` - 77 edges
8. `ScimResourceType` - 72 edges
9. `RecordingAuditTrail` - 72 edges
10. `InMemoryScimUserRepository` - 68 edges

## Surprising Connections (you probably didn't know these)
- `Decision` --references--> `ScheduledJobLock`  [INFERRED]
  docs/adr/0005-serialize-scheduled-jobs-on-per-job-lock-rows.md → backend/src/main/java/com/example/backend/auth/domain/ScheduledJobLock.java
- `Consequences` --references--> `AuditTrailServiceTests`  [INFERRED]
  docs/adr/0004-audit-append-failure-semantics.md → backend/src/test/java/com/example/backend/audit/application/AuditTrailServiceTests.java
- `Alerts` --references--> `OperationalTelemetryIntegrationTests`  [INFERRED]
  infra/README.md → backend/src/test/java/com/example/backend/observability/OperationalTelemetryIntegrationTests.java
- `Sessions` --references--> `resolveSessionRoute()`  [INFERRED]
  CONTEXT.md → frontend/src/auth/session-route.ts
- `Decision` --references--> `AfterCommit`  [INFERRED]
  docs/adr/0002-revoke-sessions-after-commit.md → backend/src/main/java/com/example/backend/auth/application/AfterCommit.java

## Import Cycles
- None detected.

## Communities (200 total, 54 thin omitted)

### Community 0 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (13): Override, AbsoluteSessionLifetimeFilterTests, ConnectorAdministrationServiceTests, Connectors, Tokens, DormancyPolicyTests, PasswordNormalizationTests, PasswordPolicyTests (+5 more)

### Community 1 - "InMemoryScimGroupRepository"
Cohesion: 0.14
Nodes (4): InMemoryScimGroupRepository, Override, Override, Result

### Community 2 - "IdentityAdministrationServiceTests"
Cohesion: 0.10
Nodes (4): IdentityAdministrationService, DirectGroup, IdentitySummary, IdentityAdministrationServiceTests

### Community 3 - "ScimVersionPrecondition"
Cohesion: 0.14
Nodes (6): NewScimUser, Override, ScimUserReplacement, ScimUserResource, ScimVersionPrecondition, ScimUserReplacementTests

### Community 4 - "connectors.tsx"
Cohesion: 0.09
Nodes (30): Connector, connectorPath(), CONNECTORS_PATH, ConnectorToken, decodeJson(), DirectGroup, formatDate(), formatInstant() (+22 more)

### Community 5 - ".advanceBy"
Cohesion: 0.12
Nodes (3): DormancyRun, DormantAuthorityRevocationServiceTests, InactivityDeactivationServiceTests

### Community 6 - ".of"
Cohesion: 0.13
Nodes (5): NewScimGroup, AddMembers, RemoveMembers, ScimGroupReplacement, ScimGroupServiceTests

### Community 7 - "ScimUser"
Cohesion: 0.13
Nodes (5): DuplicateUserNameException, ScimLoginState, ScimUser, InMemoryScimUserRepository, Override

### Community 8 - ".require"
Cohesion: 0.08
Nodes (4): Override, LoginAttemptServiceTests, PasswordChangeServiceTests, ScimUserSessionRevocationTests

### Community 9 - "Attribute"
Cohesion: 0.05
Nodes (36): Attribute, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE, EXTERNAL_ID (+28 more)

### Community 10 - "org.springframework.stereotype.Service"
Cohesion: 0.08
Nodes (38): atomiclong, AuditRetentionService, AuditRetentionScheduleConfig, Override, LoggingOperationalAlerts, DormantAuthorityRevocationService, InactivityDeactivationService, PasswordChangeGraceService (+30 more)

### Community 11 - "org.springframework.context.annotation.Bean"
Cohesion: 0.27
Nodes (3): SecurityConfig, SecurityConfigPasswordEncoderTests, org.springframework.context.annotation.Bean

### Community 12 - "ScimUserServiceTests"
Cohesion: 0.15
Nodes (5): SetPassword, Revocation, ScimUserServiceTests, InMemoryScimPasswordHistoryRepository, Override

### Community 14 - "org.springframework.data.jpa.repository.Query"
Cohesion: 0.08
Nodes (15): UserCounterJpaRepository, ScimGroupJpaRepository, ScimGroupMemberJpaRepository, ScimGroupMemberRow, ScimResourceJpaRepository, ScimUserJpaRepository, Override, ScimUserPersistenceAdapter (+7 more)

### Community 16 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.06
Nodes (10): attributeref, SpaRoutes, LockoutPolicyTests, SpaRoutesScimNamespaceTests, ReservedServerPaths, SpaRoutesTests, SpaShell, base64 (+2 more)

### Community 17 - "assertthat"
Cohesion: 0.11
Nodes (71): arraynode, assertthat, assertthatcode, assertthatnoexception, authenticationmanager, autowired, RequestIdFilter, NormalizedUserName (+63 more)

### Community 18 - ".of"
Cohesion: 0.15
Nodes (4): Hit, Result, ScimSort, ScimSortTests

### Community 19 - "AuditTrail"
Cohesion: 0.08
Nodes (3): AuditTrail, AuditTrailServiceTests, RecordingRepository

### Community 20 - "ScimGroupResource"
Cohesion: 0.22
Nodes (3): ScimGroupResource, ScimGroupController, ScimGroupRenderer

### Community 21 - "set"
Cohesion: 0.07
Nodes (27): AuditAdministrativeRefusal, CREDENTIALLESS_TARGET, LAST_ENABLED_ADMINISTRATOR, PROTECTED_RESOURCE, SELF_DISABLE, SELF_TARGET, AuditGroupAttribute, DISPLAY_NAME (+19 more)

### Community 22 - "UserCounter"
Cohesion: 0.10
Nodes (5): UserCounterService, UserCounter, UserCounterRepository, Override, UserCounterTests

### Community 24 - "ScimQueryVocabulary.java"
Cohesion: 0.04
Nodes (76): active, ScimFilterPath, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE (+68 more)

### Community 25 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.17
Nodes (3): AuditTrailService, Override, org.springframework.transaction.annotation.Transactional

### Community 26 - "AuthController.java"
Cohesion: 0.06
Nodes (26): AdminAuditController, InvalidAuditQueryException, CurrentPasswordRejectedException, ForbiddenIdentityChangeException, UnknownIdentityException, UnknownSessionIdentityException, UnsafeIdentityChangeException, AdminAccountController (+18 more)

### Community 27 - ".given"
Cohesion: 0.09
Nodes (6): Override, Group, SelfRecord, LoginIdentityServiceTests, SelfReadServiceTests, org.springframework.security.core.userdetails.UserDetails

### Community 28 - ".get"
Cohesion: 0.15
Nodes (3): ScimQueryProtocolIntegrationTests, org.junit.jupiter.params.provider.CsvSource, org.junit.jupiter.params.provider.MethodSource

### Community 30 - "ScimUserPatchOperationTests"
Cohesion: 0.18
Nodes (4): EmailUpdate, RemoveEmailPart, UpdateEmails, ScimUserPatchOperationTests

### Community 31 - "RecordingAuditTrail"
Cohesion: 0.16
Nodes (3): Override, Recorded, RecordingAuditTrail

### Community 32 - "OperationalTelemetryIntegrationTests.java"
Cohesion: 0.07
Nodes (23): Builder, ManagementPortIntegrationTests, Builder, SuppressWarnings, OperationalTelemetryIntegrationTests, Session, cookiepolicy, httpcookie (+15 more)

### Community 33 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.13
Nodes (9): Override, Override, Override, ScimBearerAuthenticationFilter, ScimBearerChallenge, Override, jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse (+1 more)

### Community 36 - "ScimGroupController.java"
Cohesion: 0.15
Nodes (14): authenticationprincipal, ScimDiscoveryController, ScimSearchController, ProbeController, Build, org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.GetMapping, org.springframework.web.bind.annotation.PatchMapping (+6 more)

### Community 37 - "ConnectorTokenSecretTests"
Cohesion: 0.11
Nodes (8): ConnectorTokenDigest, Override, Minted, Presented, ConnectorTokenSecretTests, java.security.MessageDigest, nosuchalgorithmexception, standardcharsets

### Community 38 - ".toDomain"
Cohesion: 0.05
Nodes (26): ScimGroupEntity, ScimGroupMemberEntity, ScimGroupMemberId, ScimLoginStateValue, ScimResourceEntity, ScimUserEmailValue, ScimUserEntity, cascadetype (+18 more)

### Community 40 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.11
Nodes (18): Architecture, AWS CloudFormation Deployment Guide, Can't Access via ALB, Can't Retrieve SSH Key, CloudFormation Creates Key (Recommended), Database Connection Error, Deploy Application, Deployment (+10 more)

### Community 41 - "SecurityConfig.java"
Cohesion: 0.10
Nodes (21): argon2passwordencoder, authenticationentrypoint, authorizationfilter, PasswordNormalization, changesessionidauthenticationstrategy, cookiecsrftokenrepository, daoauthenticationprovider, delegatingpasswordencoder (+13 more)

### Community 42 - "ScimUserServiceTests.java"
Cohesion: 0.09
Nodes (40): addemails, ScimEmailPart, PRIMARY, TYPE, VALUE, ScimName, AddEmails, MergeName (+32 more)

### Community 43 - "ScimResourceType"
Cohesion: 0.11
Nodes (9): ScimResourceType, GROUP, USER, Override, Result, ScimQueryPersistenceAdapter, ScimQuerySql, Override (+1 more)

### Community 44 - ".seed"
Cohesion: 0.12
Nodes (4): CountingPasswordEncoder, Override, RecordingSeedLock, ScimSeedServiceTests

### Community 46 - "AuditOperation"
Cohesion: 0.05
Nodes (37): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+29 more)

### Community 47 - "AdminAccountControllerTests.java"
Cohesion: 0.12
Nodes (10): GroupSummary, AdminAccountControllerTests, Override, RecordingService, deletemapping, java.lang.reflect.Method, modifier, patchmapping (+2 more)

### Community 48 - "ScimSeedService"
Cohesion: 0.17
Nodes (8): ScimSeedService, SeededIdentity, ScimSeedConfig, ScimSeedLock, ScimSeedLockAdapter, org.springframework.boot.ApplicationRunner, propagation, seededidentity

### Community 49 - "AuditEventQuery"
Cohesion: 0.10
Nodes (15): AuditEventListingService, AuditEventPage, AuditEventQuery, AuditEventReader, AuditOutcome, FAILURE, SUCCESS, AuditEventReadAdapter (+7 more)

### Community 51 - "compilerOptions"
Cohesion: 0.09
Nodes (21): compilerOptions, allowImportingTsExtensions, baseUrl, isolatedModules, jsx, lib, module, moduleDetection (+13 more)

### Community 52 - "tools.jackson.databind.JsonNode"
Cohesion: 0.14
Nodes (7): RemoveAllMembers, ReplaceMembers, ScimGroupPatchOperation, SetDisplayName, ScimGroupRequestReader, ScimUserRequestReader, tools.jackson.databind.JsonNode

### Community 54 - "ScimFilterParser"
Cohesion: 0.19
Nodes (4): ResolvedPath, ScimFilterParser, Search, filtering, sorting and projection, Filtering

### Community 55 - "ScimUserProfile"
Cohesion: 0.12
Nodes (5): Override, ScimPasswordChange, ScimUserEdit, Override, ScimUserProfile

### Community 56 - "AuthControllerTests"
Cohesion: 0.12
Nodes (9): LoginOutcome, AuthController, ChangePasswordRequest, Override, LoginRequest, UserResponse, AuthControllerTests, org.springframework.security.core.Authentication (+1 more)

### Community 57 - "ScimQuerySql.java"
Cohesion: 0.11
Nodes (29): and, InvalidScimFilterException, And, AttributeRef, Comparison, Not, Operator, CO (+21 more)

### Community 58 - "Attribute"
Cohesion: 0.11
Nodes (14): Kind, CLEAR, SET, UNCHANGED, Attribute, Kind, BOOLEAN, COMPLEX (+6 more)

### Community 60 - "devDependencies"
Cohesion: 0.07
Nodes (29): devDependencies, dependency-cruiser, eslint, @eslint/js, eslint-plugin-react-hooks, eslint-plugin-react-refresh, fallow, globals (+21 more)

### Community 61 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 63 - "ScimEmail"
Cohesion: 0.17
Nodes (4): ScimEmail, Condition, ScimEmailFilter, ScimEmailTests

### Community 66 - "session-route.ts"
Cohesion: 0.14
Nodes (24): Architecture, Backend contract, Routing, What is deliberately absent, Why `auth/` is its own folder and not a page, AuthRole, AuthProvider(), AuthStatus (+16 more)

### Community 67 - ".delete"
Cohesion: 0.14
Nodes (9): Cause, ADMIN_MEMBERSHIP_REMOVED, DEACTIVATED, DELETED, PASSWORD_CHANGED, USER_NAME_CHANGED, InMemoryScimTombstoneRepository, Override (+1 more)

### Community 68 - "AuditEventEntity"
Cohesion: 0.16
Nodes (6): AuditEventRepository, AuditRequestContext, AuditEventJpaRepository, AuditEventPersistenceAdapter, Override, AuditEventEntity

### Community 69 - "ScimRequestObservationConvention"
Cohesion: 0.22
Nodes (6): Override, ScimRequestObservationConvention, io.micrometer.common.KeyValues, keyvalue, org.springframework.http.server.observation.DefaultServerRequestObservationConvention, org.springframework.http.server.observation.ServerRequestObservationContext

### Community 70 - "ScimConnector"
Cohesion: 0.08
Nodes (10): ConnectorAuthenticationService, ScimConnector, ScimConnectorRepository, ScimConnectorEntity, ScimConnectorJpaRepository, Override, ScimConnectorPersistenceAdapter, ConnectorAuthenticationServiceTests (+2 more)

### Community 71 - "apiFetch"
Cohesion: 0.12
Nodes (26): Why every request goes through `lib/http.ts`, Unit-testing a module that calls the API, changePassword(), classifyRejection(), decodeRuleMessage(), decodeUser(), getCurrentUser(), login() (+18 more)

### Community 72 - "ScimUserRepository"
Cohesion: 0.07
Nodes (21): LoginIdentityService, SelfReadService, ScimGroupService, ScimSearchService, ScimUserService, PasswordReusedException, ScimExternalIdRepository, ScimGroupRepository (+13 more)

### Community 73 - "LogContextTests"
Cohesion: 0.19
Nodes (4): Override, LogContext, Scope, LogContextTests

### Community 74 - "ConnectorTokenScope"
Cohesion: 0.08
Nodes (12): ConnectorTokenSummary, ConnectorTokenScope, READ_ONLY, READ_WRITE, ScimConnectorToken, ScimConnectorTokenRepository, ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository (+4 more)

### Community 75 - "org.springframework.stereotype.Repository"
Cohesion: 0.07
Nodes (14): AuditEventRetention, AuditEventRetentionAdapter, Override, ScheduledJobLockAdapter, UserCounterEntity, UserCounterPersistenceAdapter, ScimExternalIdEntity, ScimExternalIdJpaRepository (+6 more)

### Community 76 - "ScimDiscoveryIntegrationTests"
Cohesion: 0.17
Nodes (4): ScimConditionalWrites, org.junit.jupiter.api.AfterEach, ScimDiscoveryIntegrationTests, org.springframework.test.web.servlet.request.RequestPostProcessor

### Community 77 - "AuditUserAttribute"
Cohesion: 0.14
Nodes (11): AuditUserAttribute, ACTIVE, DISPLAY_NAME, EMAILS, GROUPS, LOCALE, NAME, PASSWORD (+3 more)

### Community 78 - ".handle"
Cohesion: 0.08
Nodes (16): ScimErrorException, ScimExceptionHandler, Op, ADD, REMOVE, REPLACE, Path, ScimUserPatchReader (+8 more)

### Community 79 - "ReservedResourceName"
Cohesion: 0.10
Nodes (23): arrays, atomicinteger, authentication, authenticationexception, LockoutPolicy, ProtectedResourceException, ReservedResourceName, ADMIN_GROUP (+15 more)

### Community 80 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.10
Nodes (21): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Error contract (+13 more)

### Community 81 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 82 - "LoginLockoutTests"
Cohesion: 0.12
Nodes (8): LoginLockoutTests, 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision, Status, org.springframework.security.core.AuthenticationException

### Community 84 - "InactivityGovernanceIntegrationTests"
Cohesion: 0.10
Nodes (12): InactivityGovernanceIntegrationTests, ThrowingRunnable, Override, Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change (+4 more)

### Community 86 - "scripts"
Cohesion: 0.10
Nodes (20): scripts, analyze, build, dev, format, format:check, lint, preview (+12 more)

### Community 87 - "ScimUserAttributesTests"
Cohesion: 0.05
Nodes (8): ScimDiscovery, ScimGroupAttributes, Attribute, ScimUserAttributes, ScimDiscoveryTests, SuppressWarnings, ScimUserAttributesTests, Attribute projection

### Community 89 - "Rule"
Cohesion: 0.19
Nodes (8): PasswordPolicyViolationException, PasswordRuleViolation, Rule, CONTAINS_USER_NAME, REUSED, TOO_LONG, TOO_SHORT, Semgrep

### Community 90 - "IndexedSessions"
Cohesion: 0.28
Nodes (5): Override, AccountSessionsAdapterTests, IndexedSessions, Override, org.springframework.session.MapSession

### Community 91 - "SpaFrontendTests"
Cohesion: 0.20
Nodes (6): Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.web.servlet.ModelAndView, requestdispatcher

### Community 93 - "RefusalTimingEquivalenceTests"
Cohesion: 0.23
Nodes (3): CountingPasswordEncoder, Override, RefusalTimingEquivalenceTests

### Community 94 - "components.json"
Cohesion: 0.11
Nodes (17): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+9 more)

### Community 95 - "auth.helpers.ts"
Cohesion: 0.05
Nodes (47): Architecture tests, Assert exactly, not loosely, Calling the API from a spec, Coverage excludes — why the list is explicit, E2E, Fallow, Flakiness — the rules that keep these tests green, Forcing a refused request (+39 more)

### Community 96 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 98 - "AuditFilterShape"
Cohesion: 0.18
Nodes (7): And, AuditFilterShape, Override, Not, Or, Presence, ValuePath

### Community 99 - "accounts.tsx"
Cohesion: 0.12
Nodes (29): Component library, Component library, Button(), ButtonProps, buttonVariants, Card(), CardContent(), CardDescription() (+21 more)

### Community 100 - "lib.sh"
Cohesion: 0.24
Nodes (14): die(), load_backend_env(), log(), pinned_version(), port_holder(), require_cmd(), require_docker(), require_maven() (+6 more)

### Community 101 - "org.springframework.mock.web.MockHttpServletRequest"
Cohesion: 0.28
Nodes (4): HttpAuditRequestContext, Override, HttpAuditRequestContextTests, org.springframework.mock.web.MockHttpServletRequest

### Community 103 - "AuditListingEndToEndIntegrationTests"
Cohesion: 0.11
Nodes (7): AuditListingEndToEndIntegrationTests, ClockedSession, Override, AuditListingIntegrationTests, Seeded, jakarta.servlet.ServletContext, org.springframework.mock.web.MockHttpSession

### Community 104 - "ScimAuditFilterShapesTests.java"
Cohesion: 0.16
Nodes (4): Attribute, ScimAuditFilterShapes, ScimAuditFilterShapesTests, org.junit.jupiter.params.provider.EnumSource

### Community 105 - "ScimExceptionHandlerMetricTests.java"
Cohesion: 0.25
Nodes (8): AuditRequest, MetricTag, handlermapping, mockhttpservletresponse, requestcontextholder, serverhttpobservationfilter, serverrequestobservationcontext, servletrequestattributes

### Community 106 - "stryker.config.json"
Cohesion: 0.07
Nodes (26): cleanTempDir, clearTextReporter, allowColor, maxTestsToLog, _comment_mutate, concurrency, coverageAnalysis, htmlReporter (+18 more)

### Community 107 - "Backend"
Cohesion: 0.14
Nodes (13): Audit trail database roles, Audit trail retention, Backend, Build and test, Bundle a frontend, Configuration, Inactivity governance, Log in (+5 more)

### Community 108 - "SessionController"
Cohesion: 0.33
Nodes (5): SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession

### Community 109 - ".overlapEnd"
Cohesion: 0.18
Nodes (3): ConnectorTokenPolicyTests, Lifetime, RotationOverlap

### Community 110 - "org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder"
Cohesion: 0.33
Nodes (3): org.junit.jupiter.api.AfterEach, ScimEndToEndIntegrationTests, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

### Community 111 - ".ofIfMatch"
Cohesion: 0.23
Nodes (3): FakeSaltedEncoder, Override, ScimVersionPreconditionTests

### Community 112 - "The layers"
Cohesion: 0.15
Nodes (11): BackendApplication, Architecture, Dev-server reloads, Styling and the token pipeline, The `@/` alias, The layers, Why `components/ui/` is fenced off, Why `lib/` is a leaf (+3 more)

### Community 113 - "auth-context-value.ts"
Cohesion: 0.07
Nodes (27): AuthUser, PasswordChangeOutcome, api, CONFINED, CREDENTIALS, mounted(), USER, wrapper() (+19 more)

### Community 114 - "vitest"
Cohesion: 0.12
Nodes (14): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+6 more)

### Community 115 - "AGENTS.md"
Cohesion: 0.15
Nodes (11): Agent, Build and validation, Documentation, Environment, Frontend/backend integration, graphify, Ignore rules, Layout (+3 more)

### Community 118 - "ScimGroup"
Cohesion: 0.13
Nodes (5): DuplicateDisplayNameException, ScimGroup, ScimGroupReference, Override, ScimGroupPersistenceAdapter

### Community 119 - "ScimBearerAuthenticationFilter.java"
Cohesion: 0.28
Nodes (7): ScimWriteScopeRule, ioexception, jakarta.servlet.FilterChain, ordered, org.springframework.web.filter.OncePerRequestFilter, preauthenticatedauthenticationtoken, servletexception

### Community 120 - "deploy.sh"
Cohesion: 0.42
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 122 - "ScheduledJob"
Cohesion: 0.21
Nodes (7): ScheduledJob, DORMANT_AUTHORITY_REVOCATION, INACTIVITY_DEACTIVATION, PASSWORD_CHANGE_GRACE_DEACTIVATION, Override, InMemoryScheduledJobLock, Override

### Community 123 - "org.springframework.boot.test.context.TestConfiguration"
Cohesion: 0.23
Nodes (6): SessionRegistryConfiguration, SessionRegistryConfiguration, SessionRegistryConfiguration, DormancyTestClockConfiguration, org.springframework.boot.test.context.TestConfiguration, org.springframework.context.annotation.Primary

### Community 124 - "AGENTS.md — frontend"
Cohesion: 0.22
Nodes (8): AGENTS.md — frontend, Baseline gate, Commands, Conditional gates, Fallow, Semgrep, Testing, TypeScript

### Community 125 - ".changePassword"
Cohesion: 0.09
Nodes (15): Architecture constraints, AuditPasswordChangeRefusal, ACCOUNT_DISABLED, ACCOUNT_LOCKED, BAD_CURRENT_PASSWORD, CONTAINS_USER_NAME, REUSED, TOO_LONG (+7 more)

### Community 126 - "InactivityGovernanceIntegrationTests.java"
Cohesion: 0.08
Nodes (15): AccountSessionsAdapter, RedisSessionRevocationIntegrationTests, ScimSeedIntegrationTests, countdownlatch, executors, executorservice, future, illegaltransactionstateexception (+7 more)

### Community 127 - "AuditEvent"
Cohesion: 0.36
Nodes (3): AuditEvent, AuditEventListingServiceTests, AuditEventPageTests

### Community 129 - "ScimAttributeProjection"
Cohesion: 0.21
Nodes (5): SuppressWarnings, Kind, GROUP, USER, ScimAttributeProjection

### Community 130 - "DormancyPolicyStartupTests.java"
Cohesion: 0.16
Nodes (6): applicationconversionservice, AuditRetentionStartupTests, DormancyPolicyStartupTests, org.springframework.boot.test.context.runner.ApplicationContextRunner, propertysourcesplaceholderconfigurer, systemenvironmentpropertysource

### Community 131 - "RFC requirements and implications"
Cohesion: 0.15
Nodes (15): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+7 more)

### Community 132 - "package.json"
Cohesion: 0.05
Nodes (43): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge, engines (+35 more)

### Community 135 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 136 - "Operator"
Cohesion: 0.15
Nodes (11): Comparison, Operator, CO, EQ, EW, GE, GT, LE (+3 more)

### Community 138 - "org.springframework.web.bind.annotation.PostMapping"
Cohesion: 0.11
Nodes (13): CountResponse, UserCounterController, ConnectorSummary, IssuedConnectorToken, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest, RotateTokenRequest (+5 more)

### Community 140 - "Delivery plan"
Cohesion: 0.22
Nodes (9): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 0a — Permanent lockout, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 5 — Complete query protocol, Slice 6 — Operational Accounts page and audit (+1 more)

### Community 141 - "front-end"
Cohesion: 0.22
Nodes (8): Backend contract, front-end, Project structure, Scripts, Setup, Styling, Technology stack, Testing

### Community 142 - "AGENTS.md — backend"
Cohesion: 0.25
Nodes (7): AGENTS.md — backend, API contract, Baseline gate, Conditional gate: mutation testing, Reading a gate's result, Security-sensitive changes, Verification

### Community 143 - "AuthenticatedConnector"
Cohesion: 0.11
Nodes (5): Search, ScimSearchRenderer, ScimUserController, ScimUserRenderer, AuthenticatedConnector

### Community 144 - "ScimReleaseGate"
Cohesion: 0.29
Nodes (4): SCIM release gate, ScimReleaseGate, ScimReleaseGateFilter, ScimSecurityConfig

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

### Community 150 - "AfterCommitAdapterTests.java"
Cohesion: 0.13
Nodes (12): AfterCommitAdapter, Override, AfterCommitAdapterTests, 2. Revoke a disabled account's sessions after the commit, Alternatives considered, Consequences, Context, Decision (+4 more)

### Community 151 - "Quick Start"
Cohesion: 0.25
Nodes (6): 1. Get VPC Info, 2. Deploy, 3. Get Private Key (if CloudFormation created it), 4. Deploy Application, 5. Test, Quick Start

### Community 152 - "monorepo-base"
Cohesion: 0.25
Nodes (7): Deploying to AWS, Frontend/backend integration, Layout, monorepo-base, Toolchain pins, Working on the backend, Working on the frontend

### Community 153 - "InMemoryScimExternalIdRepository"
Cohesion: 0.43
Nodes (3): Alias, InMemoryScimExternalIdRepository, Override

### Community 154 - "uuid"
Cohesion: 0.07
Nodes (13): assertthatthrownby, atomicreference, ConnectorTokenPolicy, ConnectorTokenSecret, duration, instant, java.security.SecureRandom, org.junit.jupiter.api.Nested (+5 more)

### Community 157 - "ScimExceptionHandler.java"
Cohesion: 0.16
Nodes (6): PasswordHistoryPolicy, UnknownGroupMemberException, ScimPasswordHistoryEntity, ScimPasswordHistoryJpaRepository, Override, ScimPasswordHistoryPersistenceAdapter

### Community 158 - "CONTEXT"
Cohesion: 0.50
Nodes (3): CONTEXT, Request paths, Sessions

### Community 159 - "Common Tasks"
Cohesion: 0.29
Nodes (7): Check ALB Target Health, Common Tasks, Delete Stack, Inspect the Stack, Restart Application, Update Application, View Logs

### Community 162 - "5. Serialize scheduled jobs on per-job lock rows"
Cohesion: 0.29
Nodes (6): 5. Serialize scheduled jobs on per-job lock rows, Alternatives considered, Consequences, Context, Decision, Status

### Community 163 - "ContainerTestConfiguration"
Cohesion: 0.08
Nodes (23): anonymousauthenticationfilter, AuditEventReadAdapterIntegrationTests, BackendApplicationTests, ContainerTestConfiguration, ScimSecurityChainOrderTests, basicauthenticationfilter, classmode, csrffilter (+15 more)

### Community 165 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 166 - "EcsLogFormatTests"
Cohesion: 0.05
Nodes (26): AuditRetentionPolicyConfig, AuditRetentionPolicy, AuditRetentionServiceTests, CountingRetention, Override, CapturedLog, Override, AuditRetentionPolicyTests (+18 more)

### Community 168 - "Domain Docs"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 169 - "Issue tracker: GitHub"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 170 - "Query contract"
Cohesion: 0.50
Nodes (4): Pagination, POST search, Query contract, Sorting

### Community 172 - "Definition of Done"
Cohesion: 0.40
Nodes (5): Backend gates, Contract and behavior, Definition of Done, Documentation and graph, Frontend gates

### Community 173 - "Write semantics"
Cohesion: 0.40
Nodes (5): DELETE and tombstones, PATCH, POST, PUT, Write semantics

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

### Community 221 - "list"
Cohesion: 0.07
Nodes (25): arraylist, ScimGroupListing, ScimListedResource, ScimSearchListing, ScimUserListing, ScimPageRequest, ScimQuery, biginteger (+17 more)

### Community 223 - "LockoutHasNoDurationTests.java"
Cohesion: 0.31
Nodes (5): LockoutHasNoDurationTests, files, java.lang.reflect.RecordComponent, org.springframework.core.env.Environment, path

### Community 233 - "App.tsx"
Cohesion: 0.10
Nodes (11): App(), CONFINED, frontend_src_index, container, Login(), decodeCount(), Showcase(), apiFetchMock (+3 more)

## Knowledge Gaps
- **538 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+533 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 946 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **54 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Why `auth/` is its own folder and not a page` connect `session-route.ts` to `The layers`, `InactivityGovernanceIntegrationTests`, `apiFetch`?**
  _High betweenness centrality (0.036) - this node is a cross-community bridge._
- **Why does `Architecture` connect `session-route.ts` to `accounts.tsx`, `AGENTS.md — frontend`, `apiFetch`, `InactivityGovernanceIntegrationTests`?**
  _High betweenness centrality (0.030) - this node is a cross-community bridge._
- **Why does `InactivityGovernanceIntegrationTests` connect `InactivityGovernanceIntegrationTests` to `ContainerTestConfiguration`, `ScimUserRepository`, `org.springframework.stereotype.Service`, `AuthenticatedConnector`, `ReservedResourceName`, `assertthat`, `InactivityGovernanceIntegrationTests.java`, `org.springframework.boot.test.context.TestConfiguration`, `.changePassword`, `org.junit.jupiter.api.BeforeEach`?**
  _High betweenness centrality (0.027) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _538 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.junit.jupiter.api.Test` be split into smaller, more focused modules?**
  _Cohesion score 0.04095172729352551 - nodes in this community are weakly interconnected._
- **Should `InMemoryScimGroupRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.13846153846153847 - nodes in this community are weakly interconnected._
- **Should `IdentityAdministrationServiceTests` be split into smaller, more focused modules?**
  _Cohesion score 0.103954802259887 - nodes in this community are weakly interconnected._