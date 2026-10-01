# Graph Report - monorepo-base-scim-password-policy  (2026-10-01)

## Corpus Check
- 491 files · ~328,416 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 5517 nodes · 19002 edges · 229 communities (168 shown, 61 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2161 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e10faa9b`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.junit.jupiter.api.Test
- ScimGroup
- IdentityAdministrationServiceTests
- ScimUserResource
- connectors.tsx
- .advanceBy
- ScimGroupServiceTests
- .toDomain
- .changePassword
- Attribute
- ScimUserRepository
- org.springframework.context.annotation.Bean
- ScimUserServiceTests
- ScimGroupProvisioningIntegrationTests
- org.springframework.data.jpa.repository.Query
- ScimAttributeProjectionTests
- org.junit.jupiter.params.ParameterizedTest
- org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
- ScimQuery
- AuditTrail
- AuthenticatedConnector
- AuthControllerTests.java
- UserCounter
- AuditAppendOnlyIntegrationTests
- ScimQueryVocabulary.java
- org.springframework.transaction.annotation.Transactional
- AuthController.java
- .given
- .post
- ScimFilterParserTests
- ScimUserPatchOperationTests
- RecordingAuditTrail
- ManagementPortIntegrationTests
- jakarta.servlet.http.HttpServletRequest
- OperationalTelemetryIntegrationTests
- ScimDeletionIntegrationTests
- ScimConformanceFixtureTests
- ConnectorTokenSecretTests
- jakarta.persistence.Entity
- PasswordChangeLifecycleIntegrationTests
- AWS CloudFormation Deployment Guide
- SecurityConfig.java
- ScimUserServiceTests.java
- ScimResourceType
- .seed
- .created
- AuditOperation
- IdentitySummary
- AfterCommitAdapterTests.java
- AuditEventQuery
- ScimConnectorLifecycleIntegrationTests
- compilerOptions
- tools.jackson.databind.JsonNode
- .readCreate
- ScimFilterParser
- ScimUserEdit
- AuthControllerTests
- ScimQuerySql.java
- Attribute
- ScimUserProvisioningIntegrationTests
- devDependencies
- Workflow
- .issueToken
- ScimEmail
- ApiContractFixtureTests
- ScimBearerAuthenticationFilterTests
- App.tsx
- .write
- AuditTrailServiceTests.java
- ScimRequestObservationConventionTests
- ScimConnectorToken
- api.ts
- CapturedLog
- LogContextTests
- ScimConnectorTokenEntity
- ScimExternalIdEntity
- ScimDiscoveryIntegrationTests
- AuditUserAttribute
- .handle
- ScimUser
- SCIM 2.0 account-management specification plan
- compilerOptions
- .require
- .status
- InactivityGovernanceIntegrationTests
- .of
- scripts
- ScimUserAttributesTests
- ScimUserPatchReaderTests
- ScimFixtures
- IndexedSessions
- ScimUserSessionRevocation
- ScimLoginStateTests
- Tokens
- components.json
- auth.helpers.ts
- include
- SpaFrontendTests
- ScimUserPersistenceAdapter
- showcase.tsx
- lib.sh
- org.springframework.mock.web.MockHttpServletRequest
- ScimConnector
- AuditListingEndToEndIntegrationTests
- org.junit.jupiter.params.provider.EnumSource
- ScimGroupRequestReaderTests
- stryker.config.json
- Backend
- SessionController
- EcsLogCapture
- ScimEndToEndIntegrationTests
- accounts.tsx
- Architecture
- auth-context-value.ts
- sources.ts
- AGENTS.md
- .violation
- ConnectorAuthenticationServiceTests
- ScimGroupPersistenceAdapter
- ScheduledJobMetricsTests
- deploy.sh
- ScimConnectorEntity
- ScimResourceEntity
- ScheduledJobMetrics
- AGENTS.md — frontend
- .ofIfMatch
- ScimSeedIntegrationTests
- InMemoryScimExternalIdRepository
- PasswordPolicyViolationException
- ScimAttributeProjection
- ScimSeedLockAdapter.java
- RFC requirements and implications
- package.json
- .fromSearchRequest
- SelfReadIntegrationTests
- mvnw
- EcsLogFormatTests
- org.springframework.web.bind.annotation.PostMapping
- ScimGroupController.java
- Delivery plan
- front-end
- AGENTS.md — backend
- SCIM 2.0 account-management research
- Credential and cryptographic policy
- ManagementSessionConfiguration.java
- BoundedInputStream
- 2. Revoke a disabled account's sessions after the commit
- monorepo-base
- Testing Guide
- assertthat
- AuditRetentionPolicy
- ScimPasswordHistoryEntity
- CONTEXT
- dependencies
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- 5. Serialize scheduled jobs on per-job lock rows
- SelfControllerTests
- ArchitectureTest.java
- .fields
- Domain and authority model
- Domain Docs
- Issue tracker: GitHub
- Query contract
- Definition of Done
- Write semantics
- 1. Count login attempts on the login path
- cleanup.sh
- get-vpc-info.sh
- Kiro: graphify enforcement
- dev-stop.sh
- ScimRequestBodyLimitFilterTests
- Graphify Runner
- 3. ECS-structured logging with redaction enforced structurally
- ScimSchemas
- mutate
- .the_string_form_says_whether_a_password_was_sent_and_never_what_it_was
- dev.sh
- integration-test.sh
- semgrep.sh
- CLAUDE.md
- OperationalTelemetryIntegrationTests.java
- RequestIdFilterTests
- auth-context.test.tsx
- RedisSessionRevocationIntegrationTests
- ignorePatterns
- org.junit.jupiter.params.provider.CsvSource
- AuditEventEntity
- RefusalTimingEquivalenceTests
- org.junit.jupiter.params.provider.Arguments
- CountingPasswordEncoder
- API contract check
- BackendApplication.java
- reporters
- prettier.config.mjs
- graphify-guard.sh
- graphify-refresh.sh
- bootstrap.sh
- package.sh
- thresholds
- Supported schemas
- clearTextReporter
- _comment_mutate
- ref_node_url
- com.example:backend
- eslint-plugin-react-refresh
- uuid
- globals
- @playwright/test
- @tailwindcss/vite
- @testing-library/jest-dom
- @testing-library/react
- .of
- @testing-library/user-event
- @types/node
- @types/react
- typescript
- typescript-eslint
- accounts.test.tsx
- vite-plugin-compression2
- vitest
- @vitest/coverage-v8
- @vitest/ui

## God Nodes (most connected - your core abstractions)
1. `ScimUser` - 151 edges
2. `ScimGroup` - 83 edges
3. `AuditTrail` - 79 edges
4. `ScimGroupProvisioningIntegrationTests` - 78 edges
5. `AuditOperation` - 76 edges
6. `ScimUserRepository` - 75 edges
7. `ScimFilterPath` - 74 edges
8. `ScimResourceType` - 69 edges
9. `RecordingAuditTrail` - 69 edges
10. `AuthenticatedConnector` - 68 edges

## Surprising Connections (you probably didn't know these)
- `AdminAuditController` --references--> `AuditEventListingService`  [EXTRACTED]
  backend/src/main/java/com/example/backend/audit/controller/AdminAuditController.java → backend/src/main/java/com/example/backend/audit/application/AuditEventListingService.java
- `AuditRetentionService` --references--> `AuditEventRetention`  [EXTRACTED]
  backend/src/main/java/com/example/backend/audit/application/AuditRetentionService.java → backend/src/main/java/com/example/backend/audit/domain/AuditEventRetention.java
- `AuditRetentionService` --references--> `AuditRetentionPolicy`  [EXTRACTED]
  backend/src/main/java/com/example/backend/audit/application/AuditRetentionService.java → backend/src/main/java/com/example/backend/audit/domain/AuditRetentionPolicy.java
- `AuditAppendOnlyIntegrationTests` --references--> `AuditRetentionService`  [EXTRACTED]
  backend/src/test/java/com/example/backend/audit/AuditAppendOnlyIntegrationTests.java → backend/src/main/java/com/example/backend/audit/application/AuditRetentionService.java
- `AuditTrailService` --references--> `AuditEventRepository`  [EXTRACTED]
  backend/src/main/java/com/example/backend/audit/application/AuditTrailService.java → backend/src/main/java/com/example/backend/audit/domain/AuditEventRepository.java

## Import Cycles
- None detected.

## Communities (229 total, 61 thin omitted)

### Community 0 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (14): AuditRetentionStartupTests, AuditEventQueryTests, DormancyPolicyStartupTests, LoginLockoutConfigTests, Connectors, ScimDispatcherErrorFilterTests, ConnectorTokenPolicyTests, Lifetime (+6 more)

### Community 1 - "ScimGroup"
Cohesion: 0.07
Nodes (6): DuplicateDisplayNameException, ScimGroup, UnknownGroupMemberException, ScimGroupTests, InMemoryScimGroupRepository, Override

### Community 2 - "IdentityAdministrationServiceTests"
Cohesion: 0.11
Nodes (4): IdentityAdministrationService, DirectGroup, DirectGroup, IdentityAdministrationServiceTests

### Community 3 - "ScimUserResource"
Cohesion: 0.18
Nodes (3): ScimUserResource, ScimUserController, ScimUserRenderer

### Community 4 - "connectors.tsx"
Cohesion: 0.12
Nodes (22): Connector, connectorPath(), CONNECTORS_PATH, ConnectorToken, decodeJson(), DirectGroup, formatDate(), IssuedToken (+14 more)

### Community 5 - ".advanceBy"
Cohesion: 0.10
Nodes (8): ScheduledJob, DORMANT_AUTHORITY_REVOCATION, INACTIVITY_DEACTIVATION, Override, DormantAuthorityRevocationServiceTests, InactivityDeactivationServiceTests, InMemoryScheduledJobLock, Override

### Community 6 - "ScimGroupServiceTests"
Cohesion: 0.13
Nodes (5): NewScimGroup, AddMembers, ScimGroupReplacement, ScimVersionPrecondition, ScimGroupServiceTests

### Community 7 - ".toDomain"
Cohesion: 0.09
Nodes (5): ScimLoginStateValue, ScimUserEmailValue, ScimUserEntity, jakarta.persistence.Embeddable, org.hibernate.annotations.DynamicUpdate

### Community 8 - ".changePassword"
Cohesion: 0.09
Nodes (11): AuditPasswordChangeRefusal, ACCOUNT_DISABLED, ACCOUNT_LOCKED, BAD_CURRENT_PASSWORD, CONTAINS_USER_NAME, REUSED, TOO_LONG, TOO_SHORT (+3 more)

### Community 9 - "Attribute"
Cohesion: 0.03
Nodes (54): And, Attribute, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE (+46 more)

### Community 10 - "ScimUserRepository"
Cohesion: 0.04
Nodes (52): AuditRetentionService, AuditRetentionScheduleConfig, Override, LoggingOperationalAlerts, DormantAuthorityRevocationService, InactivityDeactivationService, LoginIdentityService, LoginService (+44 more)

### Community 11 - "org.springframework.context.annotation.Bean"
Cohesion: 0.07
Nodes (19): AuditRetentionPolicyConfig, LoginLockoutConfig, PasswordEncoder, SecurityConfig, SecureRandom, ScimSecurityConfig, SessionRegistryConfiguration, CountingPasswordEncoder (+11 more)

### Community 12 - "ScimUserServiceTests"
Cohesion: 0.10
Nodes (8): NewScimUser, ScimUserReplacement, SetPassword, ScimUserProfile, FakeSaltedEncoder, Override, Revocation, ScimUserServiceTests

### Community 14 - "org.springframework.data.jpa.repository.Query"
Cohesion: 0.15
Nodes (11): ScimExternalIdJpaRepository, ScimGroupJpaRepository, ScimResourceJpaRepository, ScimUserJpaRepository, collection, lockmodetype, org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.jpa.repository.Lock (+3 more)

### Community 16 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.10
Nodes (8): SpaRoutes, SpaRoutesScimNamespaceTests, ReservedServerPaths, SpaRoutesTests, SpaShell, org.junit.jupiter.api.Nested, org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource

### Community 17 - "org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder"
Cohesion: 0.10
Nodes (74): arraynode, autowired, RequestIdFilter, ConnectorAdministrationService, ConnectorTokenScope, READ_ONLY, READ_WRITE, NormalizedUserName (+66 more)

### Community 18 - "ScimQuery"
Cohesion: 0.08
Nodes (11): Hit, Result, ScimQuery, NamedParameterJdbcTemplate, Override, Result, ScimQueryPersistenceAdapter, ScimSearchServiceTests (+3 more)

### Community 19 - "AuditTrail"
Cohesion: 0.08
Nodes (4): AuditFilterShape, AuditTrail, AuditTrailServiceTests, RecordingRepository

### Community 20 - "AuthenticatedConnector"
Cohesion: 0.11
Nodes (8): ScimGroupListing, ScimGroupResource, ScimGroupController, ScimGroupRenderer, ScimSearchRenderer, AuthenticatedConnector, datetimeformatter, org.springframework.http.ResponseEntity

### Community 21 - "AuthControllerTests.java"
Cohesion: 0.09
Nodes (12): assertthatnoexception, authenticationmanager, AccountSessionsAdapter, content, defaultcookieserializer, grantedauthority, jsonpath, mockmvc (+4 more)

### Community 22 - "UserCounter"
Cohesion: 0.10
Nodes (7): UserCounter, UserCounterRepository, UserCounterEntity, UserCounterJpaRepository, Override, UserCounterPersistenceAdapter, UserCounterTests

### Community 23 - "AuditAppendOnlyIntegrationTests"
Cohesion: 0.09
Nodes (4): AuditAppendOnlyIntegrationTests, AuditEventRecordingIntegrationTests, MockHttpSession, ResultActions

### Community 24 - "ScimQueryVocabulary.java"
Cohesion: 0.03
Nodes (80): active, of(), parent(), ScimFilterPath, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY (+72 more)

### Community 25 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.15
Nodes (5): AuditTrailService, Override, AuditEvent, Override, org.springframework.transaction.annotation.Transactional

### Community 26 - "AuthController.java"
Cohesion: 0.05
Nodes (31): AdminAuditController, InvalidAuditQueryException, CurrentPasswordRejectedException, ForbiddenIdentityChangeException, UnknownIdentityException, UnknownSessionIdentityException, UnsafeIdentityChangeException, AdminAccountController (+23 more)

### Community 27 - ".given"
Cohesion: 0.09
Nodes (6): Override, Group, SelfRecord, LoginIdentityServiceTests, SelfReadServiceTests, org.springframework.security.core.userdetails.UserDetails

### Community 30 - "ScimUserPatchOperationTests"
Cohesion: 0.11
Nodes (8): Condition, ScimEmailFilter, AddEmails, EmailUpdate, RemoveEmailPart, RemoveEmails, UpdateEmails, ScimUserPatchOperationTests

### Community 31 - "RecordingAuditTrail"
Cohesion: 0.13
Nodes (8): AuditScimRefusal, INVALID_VALUE, MUTABILITY, NO_TARGET, UNIQUENESS, Override, Recorded, RecordingAuditTrail

### Community 32 - "ManagementPortIntegrationTests"
Cohesion: 0.19
Nodes (8): Builder, ManagementPortIntegrationTests, Builder, Session, CookieManager, java.net.http.HttpClient, org.junit.jupiter.api.AfterAll, org.junit.jupiter.api.BeforeAll

### Community 33 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.06
Nodes (23): AbsoluteSessionLifetimeFilter, Override, AbsoluteSessionLifetimePolicy, Override, Override, ScimBearerAuthenticationFilter, ScimBearerChallenge, ErrorDocumentResponse (+15 more)

### Community 34 - "OperationalTelemetryIntegrationTests"
Cohesion: 0.18
Nodes (3): SuppressWarnings, OperationalTelemetryIntegrationTests, org.junit.jupiter.api.TestInstance

### Community 36 - "ScimConformanceFixtureTests"
Cohesion: 0.18
Nodes (6): FunctionalInterface, MethodOrderer.OrderAnnotation, ScimConformanceFixtureTests, Step, org.junit.jupiter.api.TestMethodOrder, org.springframework.http.HttpMethod

### Community 37 - "ConnectorTokenSecretTests"
Cohesion: 0.11
Nodes (7): ConnectorTokenDigest, Override, Minted, Presented, ConnectorTokenSecretTests, java.security.MessageDigest, nosuchalgorithmexception

### Community 38 - "jakarta.persistence.Entity"
Cohesion: 0.16
Nodes (17): cascadetype, collectiontable, column, elementcollection, embedded, embeddedid, enumerated, enumtype (+9 more)

### Community 39 - "PasswordChangeLifecycleIntegrationTests"
Cohesion: 0.29
Nodes (3): Cookie, PasswordChangeLifecycleIntegrationTests, jakarta.servlet.http.Cookie

### Community 40 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.05
Nodes (36): 1. Get VPC Info, 2. Deploy, 3. Get Private Key (if CloudFormation created it), 4. Deploy Application, 5. Test, Quick Start, Alerts, Architecture (+28 more)

### Community 41 - "SecurityConfig.java"
Cohesion: 0.06
Nodes (35): anonymousauthenticationfilter, argon2passwordencoder, authenticationentrypoint, authorizationfilter, PasswordNormalization, BackendApplicationTests, MockHttpServletRequest, ScimSecurityChainOrderTests (+27 more)

### Community 42 - "ScimUserServiceTests.java"
Cohesion: 0.10
Nodes (37): addemails, ScimEmailPart, PRIMARY, TYPE, VALUE, NamePart, FAMILY_NAME, FORMATTED (+29 more)

### Community 43 - "ScimResourceType"
Cohesion: 0.16
Nodes (6): ScimResourceType, GROUP, USER, Comparison, ScimQuerySql, Override

### Community 44 - ".seed"
Cohesion: 0.09
Nodes (10): ScimSeedService, SeededIdentity, ScimSeedConfig, ScimSeedLock, CountingPasswordEncoder, Override, RecordingSeedLock, ScimSeedServiceTests (+2 more)

### Community 46 - "AuditOperation"
Cohesion: 0.06
Nodes (30): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+22 more)

### Community 47 - "IdentitySummary"
Cohesion: 0.11
Nodes (11): GroupSummary, IdentitySummary, AdminAccountControllerTests, Override, RecordingService, deletemapping, java.lang.reflect.Method, modifier (+3 more)

### Community 48 - "AfterCommitAdapterTests.java"
Cohesion: 0.21
Nodes (6): AfterCommitAdapter, Override, AfterCommitAdapterTests, transactionsynchronization, transactionsynchronizationmanager, transactionsynchronizationutils

### Community 49 - "AuditEventQuery"
Cohesion: 0.07
Nodes (20): AuditEventListingService, AuditEventPage, AuditEventQuery, AuditEventReader, AuditOutcome, FAILURE, SUCCESS, AuditEventReadAdapter (+12 more)

### Community 51 - "compilerOptions"
Cohesion: 0.07
Nodes (29): compilerOptions, allowImportingTsExtensions, baseUrl, isolatedModules, jsx, lib, module, moduleDetection (+21 more)

### Community 52 - "tools.jackson.databind.JsonNode"
Cohesion: 0.11
Nodes (14): RemoveAllMembers, RemoveMembers, ReplaceMembers, ScimGroupPatchOperation, SetDisplayName, ScimGroupRequestReader, Op, ADD (+6 more)

### Community 54 - "ScimFilterParser"
Cohesion: 0.23
Nodes (4): InvalidScimFilterException, Comparison, ResolvedPath, ScimFilterParser

### Community 55 - "ScimUserEdit"
Cohesion: 0.11
Nodes (6): ScimName, Override, ScimPasswordChange, ScimUserEdit, Override, MergeName

### Community 56 - "AuthControllerTests"
Cohesion: 0.11
Nodes (11): LoginOutcome, AuthController, ChangePasswordRequest, Override, LoginRequest, UserResponse, AuthControllerTests, org.springframework.security.core.Authentication (+3 more)

### Community 57 - "ScimQuerySql.java"
Cohesion: 0.13
Nodes (29): and, And, AttributeRef, Comparison, Not, Operator, CO, EQ (+21 more)

### Community 58 - "Attribute"
Cohesion: 0.16
Nodes (9): Attribute, Kind, BOOLEAN, COMPLEX, DATE_TIME, REFERENCE, STRING, ScimQueryVocabulary (+1 more)

### Community 59 - "ScimUserProvisioningIntegrationTests"
Cohesion: 0.17
Nodes (4): ScimUserProvisioningIntegrationTests, org.junit.jupiter.api.extension.ExtendWith, org.springframework.boot.test.system.CapturedOutput, org.springframework.boot.test.system.OutputCaptureExtension

### Community 60 - "devDependencies"
Cohesion: 0.07
Nodes (27): eslint, @eslint/js, eslint-plugin-react-hooks, fallow, devDependencies, dependency-cruiser, eslint, @eslint/js (+19 more)

### Community 61 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 64 - "ApiContractFixtureTests"
Cohesion: 0.05
Nodes (17): ApiContractFixtureTests, Fixture, Cookie, FunctionalInterface, MethodOrderer.OrderAnnotation, Override, Step, ContractRecorder (+9 more)

### Community 65 - "ScimBearerAuthenticationFilterTests"
Cohesion: 0.15
Nodes (5): AbsoluteSessionLifetimeFilterTests, MockHttpServletRequest, MockHttpServletResponse, ScimBearerAuthenticationFilterTests, MockFilterChain

### Community 66 - "App.tsx"
Cohesion: 0.10
Nodes (26): App(), CONFINED, AuthRole, AuthStatus, useAuth(), GuestRoute(), ProtectedRoute(), SessionRoute() (+18 more)

### Community 67 - ".write"
Cohesion: 0.13
Nodes (9): Cause, ADMIN_MEMBERSHIP_REMOVED, DEACTIVATED, DELETED, PASSWORD_CHANGED, USER_NAME_CHANGED, InMemoryScimTombstoneRepository, Override (+1 more)

### Community 68 - "AuditTrailServiceTests.java"
Cohesion: 0.07
Nodes (24): AuditAdministrativeRefusal, CREDENTIALLESS_TARGET, LAST_ENABLED_ADMINISTRATOR, PROTECTED_RESOURCE, SELF_DISABLE, SELF_TARGET, AuditEventRepository, AuditGroupAttribute (+16 more)

### Community 69 - "ScimRequestObservationConventionTests"
Cohesion: 0.17
Nodes (7): Override, ScimRequestObservationConvention, ServerRequestObservationContext, ScimRequestObservationConventionTests, io.micrometer.common.KeyValues, org.springframework.http.server.observation.DefaultServerRequestObservationConvention, org.springframework.http.server.observation.ServerRequestObservationContext

### Community 70 - "ScimConnectorToken"
Cohesion: 0.12
Nodes (12): ConnectorAuthenticationService, ConnectorTokenPolicy, ConnectorTokenSecret, ScimConnectorRepository, ScimConnectorToken, ScimConnectorTokenRepository, InMemoryScimConnectorTokenRepository, Override (+4 more)

### Community 71 - "api.ts"
Cohesion: 0.12
Nodes (22): changePassword(), classifyRejection(), decodeRuleMessage(), decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock (+14 more)

### Community 72 - "CapturedLog"
Cohesion: 0.21
Nodes (6): CapturedLog, Override, ch.qos.logback.classic.Logger, ch.qos.logback.core.read.ListAppender, function, keyvaluepair

### Community 73 - "LogContextTests"
Cohesion: 0.19
Nodes (4): Override, LogContext, Scope, LogContextTests

### Community 74 - "ScimConnectorTokenEntity"
Cohesion: 0.15
Nodes (4): ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 75 - "ScimExternalIdEntity"
Cohesion: 0.12
Nodes (7): Override, Key, ScimExternalIdEntity, Override, ScimExternalIdPersistenceAdapter, jakarta.persistence.IdClass, ScimExternalIdEntity.Key

### Community 77 - "AuditUserAttribute"
Cohesion: 0.13
Nodes (11): AuditUserAttribute, ACTIVE, DISPLAY_NAME, EMAILS, GROUPS, LOCALE, NAME, PASSWORD (+3 more)

### Community 78 - ".handle"
Cohesion: 0.06
Nodes (22): ScimErrorException, ScimExceptionHandler, InvalidPreconditionException, Rule, CONTAINS_USER_NAME, REUSED, TOO_LONG, TOO_SHORT (+14 more)

### Community 79 - "ScimUser"
Cohesion: 0.05
Nodes (35): arrays, atomicinteger, authentication, authenticationexception, AfterCommit, AccountSessions, DuplicateUserNameException, LockoutPolicy (+27 more)

### Community 80 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.09
Nodes (22): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Error contract (+14 more)

### Community 81 - "compilerOptions"
Cohesion: 0.09
Nodes (21): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+13 more)

### Community 82 - ".require"
Cohesion: 0.08
Nodes (4): LoginAttemptService, LoginAttemptServiceTests, LoginLockoutTests, org.springframework.security.core.AuthenticationException

### Community 83 - ".status"
Cohesion: 0.10
Nodes (4): MockHttpSession, SecurityConfigTests, AdminAccountEndpointTests, MockHttpSession

### Community 84 - "InactivityGovernanceIntegrationTests"
Cohesion: 0.13
Nodes (5): TransactionTemplate, InactivityGovernanceIntegrationTests, FunctionalInterface, ThrowingRunnable, Override

### Community 86 - "scripts"
Cohesion: 0.10
Nodes (20): scripts, analyze, build, dev, format, format:check, lint, preview (+12 more)

### Community 87 - "ScimUserAttributesTests"
Cohesion: 0.05
Nodes (10): alwaysReturned(), projectableNames(), schemaAttributes(), ScimDiscovery, ScimGroupAttributes, Attribute, ScimUserAttributes, ScimDiscoveryTests (+2 more)

### Community 89 - "ScimFixtures"
Cohesion: 0.17
Nodes (9): Fixture, Override, Kind, GROUP, USER, Resource, Fixture, Probe (+1 more)

### Community 90 - "IndexedSessions"
Cohesion: 0.24
Nodes (6): Override, AccountSessionsAdapterTests, IndexedSessions, Override, MapSession, org.springframework.session.MapSession

### Community 91 - "ScimUserSessionRevocation"
Cohesion: 0.29
Nodes (3): Override, ScimUserSessionRevocation, ScimUserSessionRevocationTests

### Community 94 - "components.json"
Cohesion: 0.11
Nodes (17): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+9 more)

### Community 95 - "auth.helpers.ts"
Cohesion: 0.08
Nodes (30): anonymousApi(), cleanUp(), deprovision(), groupsTable(), openAccounts(), RUN, scimApi(), settle() (+22 more)

### Community 96 - "include"
Cohesion: 0.11
Nodes (16): compilerOptions, types, exclude, extends, include, node, src/**/*.test.ts, src/**/*.test.tsx (+8 more)

### Community 97 - "SpaFrontendTests"
Cohesion: 0.18
Nodes (7): ModelAndView, Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.web.servlet.ModelAndView, requestdispatcher

### Community 99 - "showcase.tsx"
Cohesion: 0.28
Nodes (13): Button(), ButtonProps, buttonVariants, Card(), CardContent(), CardDescription(), CardFooter(), CardHeader() (+5 more)

### Community 100 - "lib.sh"
Cohesion: 0.22
Nodes (12): die(), load_backend_env(), log(), require_cmd(), require_docker(), require_maven(), require_node(), require_port_free() (+4 more)

### Community 101 - "org.springframework.mock.web.MockHttpServletRequest"
Cohesion: 0.09
Nodes (16): HttpAuditRequestContext, Override, MetricTag, HttpAuditRequestContextTests, MockHttpServletRequest, MockHttpServletRequest, ServerRequestObservationContext, MetricTagTests (+8 more)

### Community 102 - "ScimConnector"
Cohesion: 0.33
Nodes (3): ScimConnector, InMemoryScimConnectorRepository, Override

### Community 103 - "AuditListingEndToEndIntegrationTests"
Cohesion: 0.11
Nodes (7): AuditListingEndToEndIntegrationTests, ClockedSession, Override, AuditListingIntegrationTests, Seeded, jakarta.servlet.ServletContext, org.springframework.mock.web.MockHttpSession

### Community 104 - "org.junit.jupiter.params.provider.EnumSource"
Cohesion: 0.16
Nodes (7): Attribute, ScimAuditFilterShapes, Kind, CLEAR, SET, UNCHANGED, org.junit.jupiter.params.provider.EnumSource

### Community 106 - "stryker.config.json"
Cohesion: 0.12
Nodes (15): cleanTempDir, concurrency, coverageAnalysis, htmlReporter, fileName, jsonReporter, fileName, packageManager (+7 more)

### Community 107 - "Backend"
Cohesion: 0.13
Nodes (14): Audit trail database roles, Audit trail retention, Backend, Build and test, Bundle a frontend, Configuration, Inactivity governance, Log in (+6 more)

### Community 108 - "SessionController"
Cohesion: 0.33
Nodes (5): SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession

### Community 109 - "EcsLogCapture"
Cohesion: 0.21
Nodes (7): EcsLogCapture, Override, bytearrayoutputstream, ch.qos.logback.classic.LoggerContext, ch.qos.logback.core.OutputStreamAppender, OutputStreamAppender, structuredlogencoder

### Community 110 - "ScimEndToEndIntegrationTests"
Cohesion: 0.27
Nodes (3): org.junit.jupiter.api.AfterEach, ScimEndToEndIntegrationTests, org.springframework.test.web.servlet.request.RequestPostProcessor

### Community 111 - "accounts.tsx"
Cohesion: 0.13
Nodes (18): useSessionRequest(), actionFailure(), formatInstant(), GROUPS_PATH, namesSameUser(), UserAction, userActionPath(), USERS_PATH (+10 more)

### Community 112 - "Architecture"
Cohesion: 0.14
Nodes (13): Architecture, Build, Dev-server reloads, Routing, Styling and the token pipeline, The `@/` alias, The layers, What is deliberately absent (+5 more)

### Community 113 - "auth-context-value.ts"
Cohesion: 0.10
Nodes (17): AuthUser, PasswordChangeOutcome, AuthContext, AuthContextState, AuthContextValue, apiFetchMock, request(), state (+9 more)

### Community 114 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 115 - "AGENTS.md"
Cohesion: 0.15
Nodes (11): Agent, Build and validation, Documentation, Environment, Frontend/backend integration, graphify, Ignore rules, Layout (+3 more)

### Community 118 - "ScimGroupPersistenceAdapter"
Cohesion: 0.10
Nodes (7): ScimGroupMemberEntity, Override, ScimGroupMemberId, ScimGroupMemberJpaRepository, ScimGroupMemberRow, Override, ScimGroupPersistenceAdapter

### Community 120 - "deploy.sh"
Cohesion: 0.42
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 121 - "ScimConnectorEntity"
Cohesion: 0.23
Nodes (4): ScimConnectorEntity, ScimConnectorJpaRepository, Override, ScimConnectorPersistenceAdapter

### Community 123 - "ScheduledJobMetrics"
Cohesion: 0.31
Nodes (5): atomiclong, ScheduledJobMetrics, io.micrometer.core.instrument.Counter, io.micrometer.core.instrument.MeterRegistry, timegauge

### Community 124 - "AGENTS.md — frontend"
Cohesion: 0.17
Nodes (11): AGENTS.md — frontend, Architecture, Backend contract, Baseline gate, Commands, Component library, Conditional gates, Fallow (+3 more)

### Community 127 - "InMemoryScimExternalIdRepository"
Cohesion: 0.43
Nodes (3): Alias, InMemoryScimExternalIdRepository, Override

### Community 129 - "ScimAttributeProjection"
Cohesion: 0.22
Nodes (6): SuppressWarnings, Kind, GROUP, USER, ScimAttributeProjection, Search

### Community 131 - "RFC requirements and implications"
Cohesion: 0.18
Nodes (11): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+3 more)

### Community 132 - "package.json"
Cohesion: 0.18
Nodes (10): engines, node, npm, name, overrides, qs, packageManager, private (+2 more)

### Community 133 - ".fromSearchRequest"
Cohesion: 0.18
Nodes (3): ScimQueryRequest, InvalidScimQueryException, ScimQueryRequestTests

### Community 134 - "SelfReadIntegrationTests"
Cohesion: 0.16
Nodes (6): SessionRegistryConfiguration, DormancyTestClockConfiguration, Cookie, SelfReadIntegrationTests, chronounit, org.springframework.context.annotation.Primary

### Community 135 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 137 - "EcsLogFormatTests"
Cohesion: 0.28
Nodes (3): EcsLogFormatTests, MockHttpSession, ResultActions

### Community 138 - "org.springframework.web.bind.annotation.PostMapping"
Cohesion: 0.09
Nodes (14): CountResponse, UserCounterController, ConnectorSummary, ConnectorTokenSummary, IssuedConnectorToken, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest (+6 more)

### Community 139 - "ScimGroupController.java"
Cohesion: 0.29
Nodes (6): authenticationprincipal, org.springframework.web.bind.annotation.PatchMapping, org.springframework.web.bind.annotation.PutMapping, requestheader, requestparam, servleturicomponentsbuilder

### Community 140 - "Delivery plan"
Cohesion: 0.20
Nodes (10): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 0a — Permanent lockout, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 4 — Groups and Admin authority, Slice 5 — Complete query protocol (+2 more)

### Community 141 - "front-end"
Cohesion: 0.20
Nodes (9): Backend contract, Component library, front-end, Project structure, Scripts, Setup, Styling, Technology stack (+1 more)

### Community 142 - "AGENTS.md — backend"
Cohesion: 0.22
Nodes (8): AGENTS.md — backend, API contract, Architecture constraints, Baseline gate, Conditional gate: mutation testing, Reading a gate's result, Security-sensitive changes, Verification

### Community 145 - "SCIM 2.0 account-management research"
Cohesion: 0.22
Nodes (7): Executive finding, Existing application seams, Primary sources, Recommended implementation order, Resolved RFC decisions, SCIM 2.0 account-management research, Testing strategy

### Community 146 - "Credential and cryptographic policy"
Cohesion: 0.22
Nodes (9): Credential and cryptographic policy, Hashing and primitives, Key rotation and storage, Lockout policy, Log formatting, Password policy, Response headers, Session lifetime (+1 more)

### Community 148 - "ManagementSessionConfiguration.java"
Cohesion: 0.29
Nodes (8): abstracthttpsessionapplicationinitializer, ManagementSessionConfiguration, DelegatingFilterProxyRegistrationBean, managementporttype, org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort, org.springframework.boot.autoconfigure.condition.ConditionalOnClass, org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean, org.springframework.session.web.http.SessionRepositoryFilter

### Community 149 - "BoundedInputStream"
Cohesion: 0.26
Nodes (6): BoundedInputStream, BoundedRequest, Override, ScimRequestBodyLimitFilter, jakarta.servlet.http.HttpServletRequestWrapper, jakarta.servlet.ServletInputStream

### Community 150 - "2. Revoke a disabled account's sessions after the commit"
Cohesion: 0.29
Nodes (6): 2. Revoke a disabled account's sessions after the commit, Alternatives considered, Consequences, Context, Decision, Status

### Community 152 - "monorepo-base"
Cohesion: 0.25
Nodes (7): Deploying to AWS, Frontend/backend integration, Layout, monorepo-base, Toolchain pins, Working on the backend, Working on the frontend

### Community 153 - "Testing Guide"
Cohesion: 0.11
Nodes (18): Architecture tests, Assert exactly, not loosely, Calling the API from a spec, Coverage excludes — why the list is explicit, E2E, Fallow, Flakiness — the rules that keep these tests green, Forcing a refused request (+10 more)

### Community 154 - "assertthat"
Cohesion: 0.06
Nodes (24): assertthat, assertthatcode, assertthatthrownby, attributeref, DormancyRun, token(), countdownlatch, dispatchertype (+16 more)

### Community 155 - "AuditRetentionPolicy"
Cohesion: 0.15
Nodes (7): applicationconversionservice, AuditEventRetention, AuditRetentionPolicy, AuditRetentionPolicyTests, org.springframework.boot.test.context.runner.ApplicationContextRunner, propertysourcesplaceholderconfigurer, systemenvironmentpropertysource

### Community 157 - "ScimPasswordHistoryEntity"
Cohesion: 0.31
Nodes (4): ScimPasswordHistoryEntity, ScimPasswordHistoryJpaRepository, Override, ScimPasswordHistoryPersistenceAdapter

### Community 158 - "CONTEXT"
Cohesion: 0.29
Nodes (6): Accounts and identity provisioning, CONTEXT, Current account model, Request paths, SCIM target model, Sessions

### Community 159 - "dependencies"
Cohesion: 0.15
Nodes (13): class-variance-authority, clsx, dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom (+5 more)

### Community 160 - "4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal"
Cohesion: 0.29
Nodes (6): 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal, Alternatives considered, Consequences, Context, Decision, Status

### Community 162 - "5. Serialize scheduled jobs on per-job lock rows"
Cohesion: 0.29
Nodes (6): 5. Serialize scheduled jobs on per-job lock rows, Alternatives considered, Consequences, Context, Decision, Status

### Community 165 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 166 - ".fields"
Cohesion: 0.27
Nodes (4): AuditRetentionServiceTests, CountingRetention, Override, ch.qos.logback.classic.spi.ILoggingEvent

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
Cohesion: 0.33
Nodes (6): Attribute projection, Filtering, Pagination, POST search, Query contract, Sorting

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

### Community 180 - "Graphify Runner"
Cohesion: 0.50
Nodes (3): Graphify Runner, Reporting, Steps

### Community 181 - "3. ECS-structured logging with redaction enforced structurally"
Cohesion: 0.33
Nodes (5): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status

### Community 184 - "mutate"
Cohesion: 0.18
Nodes (11): !src/**/*.test.ts, !src/**/*.test.tsx, !src/**/*.testHelpers.ts, !src/**/*.testHelpers.tsx, !test/**, mutate, !src/components/ui/**, !src/**/*.d.ts (+3 more)

### Community 191 - "OperationalTelemetryIntegrationTests.java"
Cohesion: 0.06
Nodes (32): atomicreference, verify.sh script, ScimRequestBodyTooLargeException, ScimRequestLimits, ScimWriteScopeRule, LockoutHasNoDurationTests, cookiepolicy, files (+24 more)

### Community 193 - "auth-context.test.tsx"
Cohesion: 0.24
Nodes (8): AuthProvider(), api, CONFINED, CREDENTIALS, mounted(), USER, wrapper(), useAuthState()

### Community 196 - "ignorePatterns"
Cohesion: 0.22
Nodes (9): ignorePatterns, .agents, artifacts, .claude, coverage, dist, graphify-out, playwright-report (+1 more)

### Community 197 - "org.junit.jupiter.params.provider.CsvSource"
Cohesion: 0.11
Nodes (4): ScimAuditFilterShapesTests, ScimPageRequestTests, ScimWriteScopeRuleTests, org.junit.jupiter.params.provider.CsvSource

### Community 198 - "AuditEventEntity"
Cohesion: 0.39
Nodes (4): AuditEventJpaRepository, AuditEventPersistenceAdapter, Override, AuditEventEntity

### Community 202 - "API contract check"
Cohesion: 0.40
Nodes (4): API contract check, Running it, What is checked, When it fails

### Community 203 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 205 - "reporters"
Cohesion: 0.40
Nodes (5): reporters, clear-text, html, json, progress

### Community 212 - "thresholds"
Cohesion: 0.50
Nodes (4): thresholds, break, high, low

### Community 213 - "Supported schemas"
Cohesion: 0.67
Nodes (3): Group, Supported schemas, User

### Community 216 - "clearTextReporter"
Cohesion: 0.67
Nodes (3): clearTextReporter, allowColor, maxTestsToLog

### Community 217 - "_comment_mutate"
Cohesion: 0.67
Nodes (3): _comment_mutate, src/components/ui/** is vendored placeholder code, due to be deleted when the in-house shadcn package is published. Its mutants are edits to Tailwind class strings, and killing them means pinning assertions to markup that is about to be replaced., src/main.tsx is the composition root: its only statement is a createRoot call against the real document, so every mutant is either uncoverable or a restatement of what the smoke E2E already proves.

### Community 221 - "uuid"
Cohesion: 0.05
Nodes (30): arraylist, AuditEventRetentionAdapter, Override, ScheduledJobLockAdapter, ScimListedResource, ScimSearchListing, ScimUserListing, PasswordHistoryPolicy (+22 more)

### Community 233 - "accounts.test.tsx"
Cohesion: 0.15
Nodes (5): GroupRow, UserRow, apiFetchMock, auth, Result

## Knowledge Gaps
- **617 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+612 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **61 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ScimUser` connect `ScimUser` to `ScimGroup`, `IdentityAdministrationServiceTests`, `ScimUserResource`, `.advanceBy`, `ScimGroupServiceTests`, `.toDomain`, `.changePassword`, `ScimUserRepository`, `ScimUserServiceTests`, `org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder`, `AuthControllerTests.java`, `AuditAppendOnlyIntegrationTests`, `assertthat`, `.given`, `SelfControllerTests`, `ScimUserServiceTests.java`, `.seed`, `.created`, `RedisSessionRevocationIntegrationTests`, `ScimConnectorToken`, `.require`, `.status`, `InactivityGovernanceIntegrationTests`, `uuid`, `ScimUserPersistenceAdapter`?**
  _High betweenness centrality (0.023) - this node is a cross-community bridge._
- **Why does `AuditOperation` connect `AuditOperation` to `AuditTrailServiceTests.java`, `jakarta.persistence.Entity`, `AuditEventEntity`, `ScimConnectorToken`, `ScimGroupServiceTests`, `ScimUserRepository`, `ScimUserServiceTests.java`, `ScimUserServiceTests`, `ScimUser`, `AuditEventQuery`, `org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder`, `ScimConnectorLifecycleIntegrationTests`, `AuditAppendOnlyIntegrationTests`, `org.springframework.transaction.annotation.Transactional`, `AuthController.java`, `ScimUserProvisioningIntegrationTests`, `uuid`, `RecordingAuditTrail`?**
  _High betweenness centrality (0.021) - this node is a cross-community bridge._
- **Why does `ScimResourceType` connect `ScimResourceType` to `ScimUserResource`, `.write`, `.fromSearchRequest`, `org.junit.jupiter.params.provider.CsvSource`, `assertthat`, `ScimUserRepository`, `ScimGroupController.java`, `ScimUser`, `ScimFilterParserTests`, `ScimQuery`, `AuthenticatedConnector`, `ScimFilterParser`, `ScimQuerySql.java`, `Attribute`, `uuid`?**
  _High betweenness centrality (0.020) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _617 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.junit.jupiter.api.Test` be split into smaller, more focused modules?**
  _Cohesion score 0.04145658263305322 - nodes in this community are weakly interconnected._
- **Should `ScimGroup` be split into smaller, more focused modules?**
  _Cohesion score 0.07341269841269842 - nodes in this community are weakly interconnected._
- **Should `IdentityAdministrationServiceTests` be split into smaller, more focused modules?**
  _Cohesion score 0.10707803992740472 - nodes in this community are weakly interconnected._