# Graph Report - monorepo-base-issue-17  (2026-09-29)

## Corpus Check
- 398 files · ~254,652 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: (none) 10, .example 1, .properties 1)

## Summary
- 4364 nodes · 14486 edges · 184 communities (124 shown, 60 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 1586 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `96dc398c`
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
- .require
- SCIM 2.0 account-management specification plan
- AfterCommitAdapterTests.java
- org.springframework.web.bind.annotation.GetMapping
- ConnectorAdministrationService
- AuditAppendOnlyIntegrationTests
- ScimExceptionHandler.java
- Backend API Contract (OpenAPI 3.1)
- AdminAccountEndpointTests.java
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
- AuditTrailServiceTests
- Baseline Full Extensive Test Levels
- org.springframework.web.bind.annotation.PostMapping
- ScimUserEdit
- ScimGroupController.java
- tools.jackson.databind.JsonNode
- SCIM 2.0 account-management research
- assertthat
- ScimUser
- org.junit.jupiter.params.provider.ValueSource
- AuthenticatedConnector
- RFC requirements and implications
- bcryptpasswordencoder
- org.springframework.transaction.annotation.Transactional
- RequestIdFilterTests
- 3. ECS-structured logging with redaction enforced structurally
- ScimUserPatchOperationTests
- AuditTrailServiceTests.java
- SecurityConfig.java
- Credential and cryptographic policy
- .created
- Spring Session In Redis
- ConnectorTokenScope
- ScimUserEntity
- SpaFrontendTests
- AuditOperation
- AdminAccountEndpointTests
- org.junit.jupiter.api.Test
- AuditTrail
- .fromSearchRequest
- RecordingAuditTrail
- AuditEventRecordingIntegrationTests
- AuditUserAttribute
- Tokens
- IndexedSessions
- ScimConnector
- ScimResourceEntity
- ScimDiscoveryIntegrationTests
- ScimQueryProtocolIntegrationTests
- org.springframework.http.ResponseEntity
- components.json
- ScimPatchRefusedException
- .of
- Domain and authority model
- ScimConnectorTokenTests
- .handle
- .of
- AuthControllerTests
- ScimExternalIdEntity
- EcsLogFormatTests
- UserCounter
- ScimEmail
- org.springframework.jdbc.core.JdbcTemplate
- ScimGroup
- ScimGroupResource
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
- ScimUserRequestReader
- ScimFilterParser
- ScimUserPatchOperation
- .ofIfMatch
- org.junit.jupiter.api.AfterEach
- org.junit.jupiter.params.provider.Arguments
- HttpAuditRequestContextTests.java
- transactiondefinition
- transactionstatus
- .hashCode
- .current
- ScimUserPatchReader
- drivermanager
- inmemoryuserdetailsmanager
- Condition
- .findAllByUserIdOrderBySetAtDescIdDesc
- FakeSaltedEncoder
- .removeOnlyWhatThisClassCreated
- ScimConnectorToken
- .requiresWriteScope
- org.junit.jupiter.params.provider.CsvSource
- AuditEventEntity
- RecordingTransactionManager
- .of

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

## Communities (184 total, 60 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 2 - "Attribute"
Cohesion: 0.03
Nodes (54): And, Attribute, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE (+46 more)

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

### Community 17 - "ScimUserPatchReaderTests.java"
Cohesion: 0.12
Nodes (27): addemails, MergeName, NamePart, FAMILY_NAME, FORMATTED, GIVEN_NAME, HONORIFIC_PREFIX, HONORIFIC_SUFFIX (+19 more)

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
Cohesion: 0.18
Nodes (6): SpaRoutes, SpaRoutesScimNamespaceTests, ReservedServerPaths, SpaRoutesTests, SpaShell, org.junit.jupiter.params.ParameterizedTest

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

### Community 31 - "org.springframework.context.annotation.Bean"
Cohesion: 0.11
Nodes (13): LoginLockoutConfig, SecurityConfig, SessionRegistryConfiguration, SessionRegistryConfiguration, SecurityConfigPasswordEncoderTests, SessionRegistryConfiguration, InMemoryAccountSessions, Override (+5 more)

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
Cohesion: 0.23
Nodes (4): ScimConnectorEntity, ScimConnectorJpaRepository, Override, ScimConnectorPersistenceAdapter

### Community 56 - "AuditRetentionPolicy"
Cohesion: 0.05
Nodes (31): AuditRetentionService, AuditRetentionPolicyConfig, AuditRetentionScheduleConfig, Override, AuditEventRetention, AuditRetentionPolicy, ScimSeedConfig, AuditRetentionServiceTests (+23 more)

### Community 58 - "ScimAttributeProjectionTests"
Cohesion: 0.09
Nodes (7): SuppressWarnings, Kind, GROUP, USER, ScimAttributeProjection, SuppressWarnings, ScimAttributeProjectionTests

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "ScimQuerySql.java"
Cohesion: 0.13
Nodes (28): and, And, AttributeRef, Comparison, Not, Operator, CO, EQ (+20 more)

### Community 63 - "ScimUserServiceTests"
Cohesion: 0.11
Nodes (5): NewScimUser, SetPassword, Revocation, ScimUserServiceTests, Override

### Community 64 - "ConnectorTokenSecretTests"
Cohesion: 0.11
Nodes (7): ConnectorTokenDigest, Override, Presented, ConnectorTokenSecretTests, java.security.MessageDigest, nosuchalgorithmexception, standardcharsets

### Community 65 - "auth-context-value.ts"
Cohesion: 0.09
Nodes (17): App(), AuthUser, AuthProvider(), AuthContext, AuthContextState, AuthContextValue, apiFetchMock, request() (+9 more)

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 68 - "RedisSessionRevocationIntegrationTests.java"
Cohesion: 0.12
Nodes (15): AccountSessionsAdapter, RedisSessionRevocationIntegrationTests, BackendApplicationTests, classmode, javax.sql.DataSource, org.springframework.session.FindByIndexNameSessionRepository, org.springframework.session.Session, org.springframework.stereotype.Component (+7 more)

### Community 70 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.06
Nodes (34): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Attribute projection, Audit and retention, Backend gates, Connector identity and token lifecycle (+26 more)

### Community 71 - "AfterCommitAdapterTests.java"
Cohesion: 0.14
Nodes (12): AfterCommitAdapter, Override, AfterCommitAdapterTests, 2. Revoke a disabled account's sessions after the commit, Alternatives considered, Consequences, Context, Decision (+4 more)

### Community 72 - "org.springframework.web.bind.annotation.GetMapping"
Cohesion: 0.14
Nodes (5): ScimDiscovery, ScimDiscoveryController, ProbeController, ScimDiscoveryTests, org.springframework.web.bind.annotation.GetMapping

### Community 73 - "ConnectorAdministrationService"
Cohesion: 0.12
Nodes (5): ConnectorAdministrationService, ConnectorSummary, ConnectorTokenSummary, IssuedConnectorToken, ConnectorAdministrationServiceTests

### Community 75 - "ScimExceptionHandler.java"
Cohesion: 0.15
Nodes (7): PasswordHistoryPolicy, PreconditionFailedException, PreconditionRequiredException, ScimPasswordHistoryRepository, ScimPasswordHistoryJpaRepository, ScimPasswordHistoryPersistenceAdapter, InMemoryScimPasswordHistoryRepository

### Community 76 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.11
Nodes (22): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, deleteSession operation, getCurrentUser operation, getHealth operation, getSession operation, login operation, logout operation (+14 more)

### Community 77 - "AdminAccountEndpointTests.java"
Cohesion: 0.10
Nodes (22): assertthatcode, chronounit, classpathresource, containsstring, content, cookie, csrftoken, filter (+14 more)

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
Cohesion: 0.14
Nodes (9): Attribute, Kind, BOOLEAN, COMPLEX, DATE_TIME, REFERENCE, STRING, ScimQueryVocabulary (+1 more)

### Community 82 - "ScimConditionalWriteIntegrationTests"
Cohesion: 0.09
Nodes (12): CountingPasswordEncoder, Override, CountingPasswordEncoder, Override, RefusalTimingEquivalenceTests, ScimConditionalWriteIntegrationTests, 1. Count login attempts on the login path, Alternatives considered (+4 more)

### Community 83 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.25
Nodes (9): ALBListener, ALBTargetGroup, TargetGroupAttachment, ALB To EC2 To RDS And Redis Topology, AWS CloudFormation Deployment Guide, Stack Parameters Reference, Existing VPC Prerequisite, Infra Troubleshooting Runbook (+1 more)

### Community 84 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 87 - "uuid"
Cohesion: 0.06
Nodes (29): arraylist, ScimGroupReference, ScimName, ScimUserProfile, ScimTombstonePersistenceAdapter, base64, biginteger, collectors (+21 more)

### Community 88 - "jakarta.persistence.Entity"
Cohesion: 0.12
Nodes (20): ScimGroupMemberEntity, ScimPasswordHistoryEntity, cascadetype, collectiontable, column, elementcollection, embedded, embeddedid (+12 more)

### Community 89 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.09
Nodes (21): AbsoluteSessionLifetimeFilter, Override, AbsoluteSessionLifetimePolicy, Override, Override, ScimBearerAuthenticationFilter, ScimBearerChallenge, ScimReleaseGate (+13 more)

### Community 91 - "Baseline Full Extensive Test Levels"
Cohesion: 0.25
Nodes (8): ArchUnit Baseline Gate, Always ./mvnw Never Bare mvn, Trace Before You Delete, Frontend Local Semgrep Ruleset, Baseline Full Extensive Test Levels, The mutate Flag Replaces the Array, It Does Not Narrow It, Image Tag Plus Digest Pinning, Toolchain Pin Table

### Community 92 - "org.springframework.web.bind.annotation.PostMapping"
Cohesion: 0.13
Nodes (12): UserCounterService, CountResponse, UserCounterController, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest, RotateTokenRequest, Override (+4 more)

### Community 93 - "ScimUserEdit"
Cohesion: 0.11
Nodes (9): Override, Kind, CLEAR, SET, UNCHANGED, ScimPasswordChange, ScimUserEdit, Override (+1 more)

### Community 94 - "ScimGroupController.java"
Cohesion: 0.07
Nodes (29): authenticationprincipal, UnknownIdentityException, UnsafeIdentityChangeException, AdminAccountController, UnknownConnectorException, ScimSearchController, InvalidConnectorTokenLifetimeException, cachecontrol (+21 more)

### Community 95 - "tools.jackson.databind.JsonNode"
Cohesion: 0.27
Nodes (6): RemoveAllMembers, ReplaceMembers, ScimGroupPatchOperation, SetDisplayName, ScimGroupRequestReader, tools.jackson.databind.JsonNode

### Community 96 - "SCIM 2.0 account-management research"
Cohesion: 0.22
Nodes (7): Executive finding, Existing application seams, Primary sources, Recommended implementation order, Resolved RFC decisions, SCIM 2.0 account-management research, Testing strategy

### Community 97 - "assertthat"
Cohesion: 0.06
Nodes (35): arrays, assertthat, assertthatnoexception, assertthatthrownby, atomicinteger, attributeref, authentication, authenticationmanager (+27 more)

### Community 98 - "ScimUser"
Cohesion: 0.07
Nodes (12): ScimSeedService, SeededIdentity, DuplicateUserNameException, ScimUser, ScimUserRepository, Override, ScimUserPersistenceAdapter, CountingPasswordEncoder (+4 more)

### Community 99 - "org.junit.jupiter.params.provider.ValueSource"
Cohesion: 0.13
Nodes (48): arraynode, authenticationexception, autowired, RequestIdFilter, ContainerTestConfiguration, UserCounterServiceTests, InMemorySessionRegistryConfiguration, ScimReleaseGateDefaultIntegrationTests (+40 more)

### Community 100 - "AuthenticatedConnector"
Cohesion: 0.12
Nodes (10): ScimGroupService, ScimUserListing, ScimUserResource, AuthenticatedConnector, Cause, DEACTIVATED, DELETED, PASSWORD_CHANGED (+2 more)

### Community 101 - "RFC requirements and implications"
Cohesion: 0.09
Nodes (24): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+16 more)

### Community 103 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.18
Nodes (4): AuditTrailService, Override, AuditEvent, org.springframework.transaction.annotation.Transactional

### Community 105 - "3. ECS-structured logging with redaction enforced structurally"
Cohesion: 0.40
Nodes (4): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Status

### Community 107 - "AuditTrailServiceTests.java"
Cohesion: 0.07
Nodes (27): AuditAdministrativeRefusal, LAST_ENABLED_ADMINISTRATOR, PROTECTED_RESOURCE, SELF_DISABLE, AuditEventRepository, AuditGroupAttribute, DISPLAY_NAME, MEMBERS (+19 more)

### Community 108 - "SecurityConfig.java"
Cohesion: 0.07
Nodes (32): anonymousauthenticationfilter, argon2passwordencoder, authenticationentrypoint, authorizationfilter, ScimSecurityConfig, ScimSecurityChainOrderTests, basicauthenticationfilter, changesessionidauthenticationstrategy (+24 more)

### Community 109 - "Credential and cryptographic policy"
Cohesion: 0.25
Nodes (8): Credential and cryptographic policy, Hashing and primitives, Key rotation and storage, Lockout policy, Password policy, Response headers, Session lifetime, Uniform authentication timing

### Community 111 - "Spring Session In Redis"
Cohesion: 0.33
Nodes (7): Backend Architecture Boundaries, Redis Compose Service, Spring Session In Redis, Flag ADR Conflicts Explicitly, docs/adr Decision Records, RedisCluster ElastiCache, RedisSubnetGroup

### Community 112 - "ConnectorTokenScope"
Cohesion: 0.12
Nodes (7): ConnectorTokenScope, READ_ONLY, READ_WRITE, ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 113 - "ScimUserEntity"
Cohesion: 0.11
Nodes (4): ScimLoginStateValue, ScimUserEmailValue, ScimUserEntity, jakarta.persistence.Embeddable

### Community 114 - "SpaFrontendTests"
Cohesion: 0.18
Nodes (6): Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.web.servlet.ModelAndView, requestdispatcher

### Community 115 - "AuditOperation"
Cohesion: 0.06
Nodes (32): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+24 more)

### Community 117 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (16): applicationconversionservice, AuditRetentionStartupTests, AbsoluteSessionLifetimeFilterTests, SecurityConfigTests, AbsoluteSessionLifetimePolicyTests, Connectors, ConnectorTokenPolicyTests, Lifetime (+8 more)

### Community 118 - "AuditTrail"
Cohesion: 0.06
Nodes (26): AuditTrail, AfterCommit, LoginAttemptService, LoginIdentityService, LoginService, ScimUserSessionRevocation, AccountSessions, ScimSearchService (+18 more)

### Community 119 - ".fromSearchRequest"
Cohesion: 0.15
Nodes (3): ScimQueryRequest, InvalidScimQueryException, ScimQueryRequestTests

### Community 120 - "RecordingAuditTrail"
Cohesion: 0.16
Nodes (3): Override, Recorded, RecordingAuditTrail

### Community 122 - "AuditUserAttribute"
Cohesion: 0.14
Nodes (10): AuditUserAttribute, ACTIVE, DISPLAY_NAME, EMAILS, LOCALE, NAME, PASSWORD, PREFERRED_LANGUAGE (+2 more)

### Community 124 - "IndexedSessions"
Cohesion: 0.28
Nodes (5): Override, AccountSessionsAdapterTests, IndexedSessions, Override, org.springframework.session.MapSession

### Community 125 - "ScimConnector"
Cohesion: 0.19
Nodes (4): ScimConnector, ConnectorAuthenticationServiceTests, InMemoryScimConnectorRepository, Override

### Community 128 - "ScimQueryProtocolIntegrationTests"
Cohesion: 0.14
Nodes (4): ScimQueryProtocolIntegrationTests, org.junit.jupiter.api.BeforeAll, org.junit.jupiter.api.TestInstance, org.junit.jupiter.params.provider.MethodSource

### Community 129 - "org.springframework.http.ResponseEntity"
Cohesion: 0.15
Nodes (5): Search, ScimSearchRenderer, ScimUserController, ScimUserRenderer, org.springframework.http.ResponseEntity

### Community 130 - "components.json"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

### Community 131 - "ScimPatchRefusedException"
Cohesion: 0.36
Nodes (4): Reason, MUTABILITY, NO_TARGET, ScimPatchRefusedException

### Community 133 - "Domain and authority model"
Cohesion: 0.25
Nodes (8): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, Protected recovery resources, SCIM User replaces Account

### Community 135 - ".handle"
Cohesion: 0.14
Nodes (11): ScimErrorException, ScimExceptionHandler, InvalidPreconditionException, Accounts and identity provisioning, Current account model, SCIM target model, Error contract, Log formatting (+3 more)

### Community 136 - ".of"
Cohesion: 0.12
Nodes (5): NormalizedDisplayName, PasswordNormalization, NormalizedDisplayNameTests, PasswordNormalizationTests, normalizer

### Community 137 - "AuthControllerTests"
Cohesion: 0.17
Nodes (7): AuthController, LoginRequest, UserResponse, AuthControllerTests, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy, org.springframework.security.web.context.SecurityContextRepository, org.springframework.session.web.http.CookieSerializer

### Community 138 - "ScimExternalIdEntity"
Cohesion: 0.13
Nodes (8): Override, Key, ScimExternalIdEntity, ScimExternalIdJpaRepository, Override, ScimExternalIdPersistenceAdapter, jakarta.persistence.IdClass, ScimExternalIdEntity.Key

### Community 139 - "EcsLogFormatTests"
Cohesion: 0.15
Nodes (8): LockoutHasNoDurationTests, EcsLogFormatTests, ResultActions, Decision, files, java.lang.reflect.RecordComponent, org.springframework.core.env.Environment, path

### Community 140 - "UserCounter"
Cohesion: 0.09
Nodes (9): getCount operation, incrementCount operation, resetCount operation, UserCounter, UserCounterRepository, UserCounterEntity, Override, UserCounterPersistenceAdapter (+1 more)

### Community 142 - "org.springframework.jdbc.core.JdbcTemplate"
Cohesion: 0.21
Nodes (6): AuditEventRetentionAdapter, Override, ScimConditionalWrites, org.springframework.jdbc.core.JdbcTemplate, org.springframework.test.web.servlet.request.RequestPostProcessor, timestamp

### Community 143 - "ScimGroup"
Cohesion: 0.08
Nodes (12): DuplicateDisplayNameException, ProtectedResourceException, ReservedResourceName, ADMIN_GROUP, BOOTSTRAP_ADMIN, ScimGroup, ScimGroupMember, UnknownGroupMemberException (+4 more)

### Community 144 - "ScimGroupResource"
Cohesion: 0.19
Nodes (3): ScimGroupResource, ScimGroupController, ScimGroupRenderer

### Community 145 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 146 - "Frontend Architecture Doc"
Cohesion: 0.11
Nodes (20): Colors Come From index.css Tokens, Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, Vite Full-Reloads on Any Watched HTML Write, One-Way Import Direction Through the Layers, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline (+12 more)

### Community 147 - ".of"
Cohesion: 0.18
Nodes (4): Attribute, ScimAuditFilterShapes, ScimAuditFilterShapesTests, org.junit.jupiter.params.provider.EnumSource

### Community 149 - "IdentitySummary"
Cohesion: 0.25
Nodes (4): IdentitySummary, AdminAccountControllerTests, Override, RecordingService

### Community 154 - "ScimQueryVocabulary.java"
Cohesion: 0.04
Nodes (77): active, ScimFilterPath, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE (+69 more)

### Community 155 - "ScimUserPatchReaderTests"
Cohesion: 0.15
Nodes (4): RemoveActive, SetActive, SetText, ScimUserPatchReaderTests

### Community 156 - "org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder"
Cohesion: 0.35
Nodes (3): ScimEndToEndIntegrationTests, org.springframework.mock.web.MockHttpSession, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

### Community 157 - "Kiro: graphify enforcement"
Cohesion: 0.40
Nodes (4): graphify-runner, Kiro: graphify enforcement, The hooks, When the refresh fails

### Community 158 - "org.springframework.data.jpa.repository.Query"
Cohesion: 0.12
Nodes (14): UserCounterJpaRepository, ScimGroupMemberId, ScimGroupJpaRepository, ScimGroupMemberJpaRepository, ScimGroupMemberRow, ScimResourceJpaRepository, ScimUserJpaRepository, collection (+6 more)

### Community 159 - "ScimResourceType"
Cohesion: 0.06
Nodes (21): ScimGroupListing, ScimListedResource, ScimSearchListing, ScimPageRequest, Hit, Result, ScimQuery, ScimResourceType (+13 more)

### Community 161 - "ScimFilterParser"
Cohesion: 0.15
Nodes (6): InvalidScimFilterException, ResolvedPath, ScimFilterParser, Search, filtering, sorting and projection, Filtering, java.util.regex.Pattern

### Community 162 - "ScimUserPatchOperation"
Cohesion: 0.36
Nodes (6): ScimEmailFilter, EmailUpdate, RemoveEmailPart, RemoveEmails, ScimUserPatchOperation, UpdateEmails

### Community 163 - ".ofIfMatch"
Cohesion: 0.17
Nodes (4): Override, ScimUserReplacement, ScimUserReplacementTests, ScimVersionPreconditionTests

### Community 166 - "HttpAuditRequestContextTests.java"
Cohesion: 0.60
Nodes (3): handlermapping, requestcontextholder, servletrequestattributes

### Community 170 - ".current"
Cohesion: 0.31
Nodes (4): HttpAuditRequestContext, Override, HttpAuditRequestContextTests, org.springframework.mock.web.MockHttpServletRequest

### Community 171 - "ScimUserPatchReader"
Cohesion: 0.21
Nodes (6): Op, ADD, REMOVE, REPLACE, Path, ScimUserPatchReader

### Community 174 - "Condition"
Cohesion: 0.25
Nodes (5): Condition, ScimEmailPart, PRIMARY, TYPE, VALUE

### Community 179 - "ScimConnectorToken"
Cohesion: 0.09
Nodes (16): ConnectorAuthenticationService, ConnectorTokenSecret, Minted, ScimConnectorRepository, ScimConnectorToken, ScimConnectorTokenRepository, Override, MutableClock (+8 more)

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
- **449 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+444 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 820 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **60 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Backend API Contract (OpenAPI 3.1)` connect `Backend API Contract (OpenAPI 3.1)` to `UserCounter`?**
  _High betweenness centrality (0.117) - this node is a cross-community bridge._
- **Why does `login operation` connect `Backend API Contract (OpenAPI 3.1)` to `auth.helpers.ts`, `http.ts`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Why does `vitest` connect `http.ts` to `sources.ts`, `auth-context-value.ts`, `accounts.tsx`, `package.json`, `App.tsx`?**
  _High betweenness centrality (0.041) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _449 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Attribute` be split into smaller, more focused modules?**
  _Cohesion score 0.03442879499217527 - nodes in this community are weakly interconnected._