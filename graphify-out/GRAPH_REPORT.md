# Graph Report - monorepo-base  (2026-09-30)

## Corpus Check
- 401 files · ~255,053 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 4432 nodes · 14509 edges · 237 communities (163 shown, 74 thin omitted)
- Extraction: 89% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 1522 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1148e6c6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- .sessionsOf
- Attribute
- showcase.tsx
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
- ScimGroupTests
- DBInstance RDS PostgreSQL
- session-route.ts
- infra/ Is Deployment Material Not An App
- .given
- include
- SecurityConfig.java
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
- ScimConnector
- bootstrap.sh
- package.sh
- Test Static index.html Stub
- com.example:backend
- AuditRetentionScheduleConfig.java
- ScimResourceType
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
- ProbeController
- ConnectorAdministrationService
- AuditAppendOnlyIntegrationTests
- .seed
- Backend API Contract (OpenAPI 3.1)
- AuditAppendOnlyIntegrationTests.java
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
- RecordingCounterService
- ScimUserEdit
- AuthController.java
- tools.jackson.databind.JsonNode
- SCIM 2.0 account-management research
- ReservedResourceName
- ScimUser
- assertthat
- ScimGroupService
- RFC requirements and implications
- bcryptpasswordencoder
- org.springframework.transaction.annotation.Transactional
- RequestIdFilterTests
- 3. ECS-structured logging with redaction enforced structurally
- ScimUserPatchOperationTests
- AuditTrailServiceTests.java
- ScimSecurityChainOrderTests.java
- Credential and cryptographic policy
- .created
- Spring Session In Redis
- ScimConnectorTokenEntity
- .toDomain
- SpaFrontendTests
- AuditOperation
- .authenticatedSession
- org.junit.jupiter.api.Test
- AuditTrail
- .fromSearchRequest
- RecordingAuditTrail
- AuditEventRecordingIntegrationTests
- AuditUserAttribute
- ScimGroupResource
- IndexedSessions
- ConnectorAuthenticationServiceTests
- ScimResourceEntity
- ScimDiscoveryIntegrationTests
- ScimQueryProtocolIntegrationTests
- ScimUserResource
- components.json
- ScimPatchRefusedException
- ScimGroupController.java
- Domain and authority model
- ScimConnectorTokenTests
- .handle
- .of
- AuthControllerTests
- ScimExternalIdPersistenceAdapter
- EcsLogFormatTests
- UserCounter
- ScimEmail
- .increment
- ScimGroup
- AuthenticatedConnector
- dependencies
- Frontend Architecture Doc
- ScimAttributeProjection
- ScimSchemas
- IdentitySummary
- filterchainproxy
- MockFilterChain
- accounts.tsx
- recordcomponent
- ScimQueryVocabulary.java
- ScimUserPatchReaderTests
- org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
- Kiro: graphify enforcement
- org.springframework.data.jpa.repository.Query
- ScimQuery
- LoginLockoutTests.java
- ScimFilterParser
- .write
- ScimLoginState
- ScimUserPersistenceAdapter
- org.junit.jupiter.params.provider.Arguments
- ScimGroupPersistenceAdapter
- transactiondefinition
- transactionstatus
- ScimGroupMemberJpaRepository
- .current
- ScimUserPatchReader
- drivermanager
- inmemoryuserdetailsmanager
- MutableClock
- ScimPasswordHistoryPersistenceAdapter
- EcsLogCapture
- .removeOnlyWhatThisClassCreated
- AuditRetentionServiceTests.java
- ScimConnectorToken
- SessionController
- CapturedLog
- App.tsx
- AuditRetentionPolicy
- mutate
- .requiresWriteScope
- Operator
- Delivery plan
- org.junit.jupiter.params.provider.CsvSource
- ignorePatterns
- Frontend Technology Stack
- RequestIdFilterTests.java
- RecordingTransactionManager
- .of
- RefusalTimingEquivalenceTests
- CONTEXT
- 1. Count login attempts on the login path
- 2. Revoke a disabled account's sessions after the commit
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- UserCounterEntity
- .normalize
- CountingPasswordEncoder
- CountingPasswordEncoder
- Query contract
- Definition of Done
- Write semantics
- reporters
- thresholds
- .detachLog
- Supported schemas
- clearTextReporter
- _comment_mutate
- ref_node_url
- .claude/CLAUDE.md
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
- .graphify_probe.sh

## God Nodes (most connected - your core abstractions)
1. `ScimUser` - 114 edges
2. `ScimGroupProvisioningIntegrationTests` - 77 edges
3. `ScimFilterPath` - 74 edges
4. `ScimGroup` - 74 edges
5. `ScimResourceType` - 69 edges
6. `AuditTrail` - 66 edges
7. `AuthenticatedConnector` - 65 edges
8. `IdentityAdministrationServiceTests` - 65 edges
9. `ScimConditionalWriteIntegrationTests` - 64 edges
10. `ScimQueryProtocolIntegrationTests` - 63 edges

## Surprising Connections (you probably didn't know these)
- `getCurrentUser operation` --shares_data_with--> `getCurrentUser()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `Frontend Local Semgrep Ruleset` --semantically_similar_to--> `Backend Semgrep Baseline Gate`  [INFERRED] [semantically similar]
  frontend/AGENTS.md → backend/AGENTS.md
- `login operation` --shares_data_with--> `login()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `logout operation` --shares_data_with--> `logout()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `lib/ Is a Leaf` --rationale_for--> `cn()`  [INFERRED]
  frontend/docs/ARCHITECTURE.md → frontend/src/lib/utils.ts

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Public Request Path Through The Stack** — infra_infrastructure_alblistener, infra_infrastructure_albtargetgroup, infra_infrastructure_ec2instance [EXTRACTED 1.00]
- **Mutation Testing as the Load-Bearing-Test Doctrine on Both Sides** — frontend_docs_testing_guide_stryker_mutate_trap, frontend_docs_testing_guide_assert_exactly [INFERRED 0.85]
- **Local Compose Versus Cloud Datastores** — backend_compose_postgres_service, backend_compose_redis_service, infra_infrastructure_dbinstance, infra_infrastructure_rediscluster [INFERRED 0.85]
- **Published Credential Exposure Surface** — agents_published_credentials_warning, backend_readme_dev_default_credentials, infra_infrastructure_app_credential_parameters, backend_semgrep_rules_service_security_be_hardcoded_credential_literal [INFERRED 0.85]

## Communities (237 total, 74 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 1 - ".sessionsOf"
Cohesion: 0.17
Nodes (3): Override, ScimUserSessionRevocationTests, cause

### Community 2 - "Attribute"
Cohesion: 0.04
Nodes (44): And, Attribute, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY, EMAILS_TYPE, EMAILS_VALUE (+36 more)

### Community 3 - "showcase.tsx"
Cohesion: 0.27
Nodes (13): One-Way Import Direction Through the Layers, components/ui Is a Package Placeholder, Button(), ButtonProps, buttonVariants, Card(), CardContent(), CardDescription() (+5 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.17
Nodes (12): Every Playwright Spec Needs a testMatch, Shared Playwright storageState for Auth, ADMIN_CREDENTIALS, captureSessionCookie(), expireSession(), login(), loginAs(), postAdminAction() (+4 more)

### Community 5 - "http.ts"
Cohesion: 0.14
Nodes (18): logout operation, decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, SessionRequest (+10 more)

### Community 6 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 9 - "devDependencies"
Cohesion: 0.07
Nodes (27): eslint, @eslint/js, eslint-plugin-react-hooks, fallow, devDependencies, dependency-cruiser, eslint, @eslint/js (+19 more)

### Community 10 - "stryker.config.json"
Cohesion: 0.12
Nodes (15): cleanTempDir, concurrency, coverageAnalysis, htmlReporter, fileName, jsonReporter, fileName, packageManager (+7 more)

### Community 11 - "compilerOptions"
Cohesion: 0.07
Nodes (29): compilerOptions, allowImportingTsExtensions, baseUrl, isolatedModules, jsx, lib, module, moduleDetection (+21 more)

### Community 12 - "Frontend Local Semgrep Ruleset"
Cohesion: 0.25
Nodes (8): Frontend Local Semgrep Ruleset, fe-dangerously-set-inner-html, fe-document-write, fe-eval-or-dynamic-function, fe-hardcoded-credential, fe-inner-html-assignment, fe-target-blank-without-noopener, Local Ruleset Keeps the Scan Offline and Deterministic

### Community 13 - "package.json"
Cohesion: 0.18
Nodes (10): engines, node, npm, name, overrides, qs, packageManager, private (+2 more)

### Community 14 - "scripts"
Cohesion: 0.10
Nodes (20): scripts, analyze, build, dev, format, format:check, lint, preview (+12 more)

### Community 16 - "compilerOptions"
Cohesion: 0.09
Nodes (21): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+13 more)

### Community 17 - "ScimUserServiceTests.java"
Cohesion: 0.11
Nodes (36): addemails, ScimEmailPart, PRIMARY, TYPE, VALUE, NamePart, FAMILY_NAME, FORMATTED (+28 more)

### Community 18 - "lib.sh"
Cohesion: 0.22
Nodes (12): die(), load_backend_env(), log(), require_cmd(), require_docker(), require_maven(), require_node(), require_port_free() (+4 more)

### Community 19 - "AGENTS.md"
Cohesion: 0.15
Nodes (11): Agent, Agent documentation, Build and validation, Environment, Frontend/backend integration, graphify, Layout, Line endings (+3 more)

### Community 20 - "deploy.sh"
Cohesion: 0.42
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 21 - "ScimGroupProvisioningIntegrationTests"
Cohesion: 0.12
Nodes (4): Alias, InMemoryScimExternalIdRepository, Override, ScimGroupProvisioningIntegrationTests

### Community 22 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 23 - "EC2Instance"
Cohesion: 0.20
Nodes (11): Ignore rules, Repo-Wide LF Line Endings, EC2Instance, EC2InstanceProfile, EC2KeyPair, EC2Role, Infra Quickstart Flow, Ship JAR From backend/target (+3 more)

### Community 24 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.09
Nodes (9): attributeref, SpaRoutes, LockoutPolicyTests, SpaRoutesScimNamespaceTests, ReservedServerPaths, SpaRoutesTests, SpaShell, org.junit.jupiter.params.ParameterizedTest (+1 more)

### Community 26 - "DBInstance RDS PostgreSQL"
Cohesion: 0.29
Nodes (8): docs/openapi.yaml API Contract, Postgres Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, DBInstance RDS PostgreSQL, DBSubnetGroup

### Community 27 - "session-route.ts"
Cohesion: 0.25
Nodes (10): AuthRole, AuthStatus, SessionRoute(), DEFAULT_DESTINATION, LOGIN_PATH, resolveSessionRoute(), SessionRequirement, SessionRoute (+2 more)

### Community 28 - "infra/ Is Deployment Material Not An App"
Cohesion: 0.29
Nodes (7): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, CONTEXT.md Domain Glossary, gh CLI Conventions, GitHub Issues As Issue Tracker, PRs As Request Surface Flag

### Community 29 - ".given"
Cohesion: 0.12
Nodes (3): Override, LoginIdentityServiceTests, org.springframework.security.core.userdetails.UserDetails

### Community 30 - "include"
Cohesion: 0.11
Nodes (16): compilerOptions, types, exclude, extends, include, node, src/**/*.test.ts, src/**/*.test.tsx (+8 more)

### Community 31 - "SecurityConfig.java"
Cohesion: 0.05
Nodes (39): argon2passwordencoder, authenticationentrypoint, authorizationfilter, AuditRetentionPolicyConfig, LoginLockoutConfig, PasswordEncoder, SecurityConfig, ScimSecurityConfig (+31 more)

### Community 32 - "Domain Documentation Guide"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 33 - "GitHub Issue Tracker Guide"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 34 - "IdentityAdministrationServiceTests"
Cohesion: 0.09
Nodes (4): IdentityAdministrationService, IdentityAdministrationServiceTests, Override, PendingCommit

### Community 36 - "Graphify Runner Agent"
Cohesion: 0.40
Nodes (5): Graphify Runner Agent, Recorded Interpreter Guard, Graph Shrink Refusal, Graphify Refresh Before Commit, CLAUDE.md Graphify Override

### Community 37 - "ScimGroupServiceTests"
Cohesion: 0.13
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

### Community 50 - "ScimConnector"
Cohesion: 0.15
Nodes (7): ScimConnector, ScimConnectorEntity, ScimConnectorJpaRepository, Override, ScimConnectorPersistenceAdapter, InMemoryScimConnectorRepository, Override

### Community 56 - "AuditRetentionScheduleConfig.java"
Cohesion: 0.19
Nodes (10): AuditRetentionService, AuditRetentionScheduleConfig, Override, AuditEventRetention, crontask, crontrigger, org.slf4j.Logger, org.springframework.scheduling.annotation.EnableScheduling (+2 more)

### Community 57 - "ScimResourceType"
Cohesion: 0.11
Nodes (7): ScimResourceType, GROUP, USER, Comparison, ScimQuerySql, Override, ScimQuerySqlTests

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "ScimQuerySql.java"
Cohesion: 0.11
Nodes (31): and, Attribute, ScimAuditFilterShapes, And, AttributeRef, Comparison, Not, Operator (+23 more)

### Community 63 - "ScimUserServiceTests"
Cohesion: 0.08
Nodes (11): SetActive, FakeSaltedEncoder, Override, Revocation, ScimUserServiceTests, ScimVersionPreconditionTests, InMemoryScimPasswordHistoryRepository, Override (+3 more)

### Community 64 - "ConnectorTokenSecretTests"
Cohesion: 0.11
Nodes (8): ConnectorTokenDigest, Override, Minted, Presented, ConnectorTokenSecretTests, java.security.MessageDigest, nosuchalgorithmexception, standardcharsets

### Community 65 - "auth-context-value.ts"
Cohesion: 0.14
Nodes (10): AuthUser, AuthContext, AuthContextState, AuthContextValue, apiFetchMock, request(), state, wrapper() (+2 more)

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 68 - "RedisSessionRevocationIntegrationTests.java"
Cohesion: 0.19
Nodes (9): AccountSessionsAdapter, RedisSessionRevocationIntegrationTests, org.springframework.session.FindByIndexNameSessionRepository, org.springframework.session.Session, org.springframework.test.annotation.DirtiesContext, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, org.springframework.transaction.PlatformTransactionManager (+1 more)

### Community 69 - ".require"
Cohesion: 0.09
Nodes (3): LoginAttemptServiceTests, LoginLockoutTests, org.springframework.security.core.AuthenticationException

### Community 70 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.09
Nodes (22): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Error contract (+14 more)

### Community 71 - "AfterCommitAdapterTests.java"
Cohesion: 0.21
Nodes (6): AfterCommitAdapter, Override, AfterCommitAdapterTests, transactionsynchronization, transactionsynchronizationmanager, transactionsynchronizationutils

### Community 73 - "ConnectorAdministrationService"
Cohesion: 0.11
Nodes (5): ConnectorAdministrationService, ConnectorSummary, ConnectorTokenSummary, IssuedConnectorToken, ConnectorAdministrationServiceTests

### Community 75 - ".seed"
Cohesion: 0.13
Nodes (8): ScimSeedService, SeededIdentity, ScimSeedConfig, CountingPasswordEncoder, Override, ScimSeedServiceTests, org.springframework.boot.ApplicationRunner, seededidentity

### Community 76 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.13
Nodes (15): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, getCurrentUser operation, getHealth operation, getSession operation, login operation, JSESSIONID Session Cookie Security Scheme, updateSession operation (+7 more)

### Community 77 - "AuditAppendOnlyIntegrationTests.java"
Cohesion: 0.08
Nodes (39): assertthatcode, assertthatnoexception, authenticationmanager, NormalizedUserName, chronounit, classpathresource, containsstring, content (+31 more)

### Community 78 - "ScimUserAttributesTests"
Cohesion: 0.05
Nodes (10): alwaysReturned(), projectableNames(), schemaAttributes(), ScimDiscovery, ScimGroupAttributes, Attribute, ScimUserAttributes, ScimDiscoveryTests (+2 more)

### Community 79 - "LogContextTests"
Cohesion: 0.19
Nodes (4): Override, LogContext, Scope, LogContextTests

### Community 80 - "ScimBearerAuthenticationFilterTests"
Cohesion: 0.20
Nodes (3): SecureRandom, MockHttpServletRequest, ScimBearerAuthenticationFilterTests

### Community 81 - "Attribute"
Cohesion: 0.13
Nodes (13): Kind, CLEAR, SET, UNCHANGED, Attribute, Kind, BOOLEAN, COMPLEX (+5 more)

### Community 83 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.25
Nodes (9): ALBListener, ALBTargetGroup, TargetGroupAttachment, ALB To EC2 To RDS And Redis Topology, AWS CloudFormation Deployment Guide, Stack Parameters Reference, Existing VPC Prerequisite, Infra Troubleshooting Runbook (+1 more)

### Community 84 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 87 - "uuid"
Cohesion: 0.06
Nodes (22): assertthatthrownby, AuditEventRetentionAdapter, Override, PasswordHistoryPolicy, ScimQueryPersistenceAdapter, ScimTombstonePersistenceAdapter, ScimConditionalWrites, collectors (+14 more)

### Community 88 - "jakarta.persistence.Entity"
Cohesion: 0.08
Nodes (26): AuditEventJpaRepository, AuditEventPersistenceAdapter, Override, AuditEventEntity, ScimExternalIdEntity, ScimPasswordHistoryEntity, cascadetype, collectiontable (+18 more)

### Community 89 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.08
Nodes (20): AbsoluteSessionLifetimeFilter, Override, AbsoluteSessionLifetimePolicy, Override, Override, ScimBearerAuthenticationFilter, ScimBearerChallenge, ScimReleaseGate (+12 more)

### Community 90 - "AuditTrailServiceTests"
Cohesion: 0.08
Nodes (3): AuditFilterShape, AuditTrailServiceTests, RecordingRepository

### Community 91 - "Baseline Full Extensive Test Levels"
Cohesion: 0.25
Nodes (8): ArchUnit Baseline Gate, Always ./mvnw Never Bare mvn, Trace Before You Delete, Frontend Local Semgrep Ruleset, Baseline Full Extensive Test Levels, The mutate Flag Replaces the Array, It Does Not Narrow It, Image Tag Plus Digest Pinning, Toolchain Pin Table

### Community 92 - "RecordingCounterService"
Cohesion: 0.27
Nodes (3): Override, RecordingCounterService, UserCounterControllerTests

### Community 93 - "ScimUserEdit"
Cohesion: 0.13
Nodes (4): Override, ScimPasswordChange, ScimUserEdit, Override

### Community 94 - "AuthController.java"
Cohesion: 0.08
Nodes (27): LoginOutcome, UnknownIdentityException, UnsafeIdentityChangeException, AdminAccountController, UnknownConnectorException, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest (+19 more)

### Community 95 - "tools.jackson.databind.JsonNode"
Cohesion: 0.17
Nodes (7): RemoveAllMembers, ReplaceMembers, ScimGroupPatchOperation, SetDisplayName, ScimGroupRequestReader, ScimUserRequestReader, tools.jackson.databind.JsonNode

### Community 96 - "SCIM 2.0 account-management research"
Cohesion: 0.22
Nodes (7): Executive finding, Existing application seams, Primary sources, Recommended implementation order, Resolved RFC decisions, SCIM 2.0 account-management research, Testing strategy

### Community 97 - "ReservedResourceName"
Cohesion: 0.12
Nodes (13): arrays, ProtectedResourceException, ofStoredValue(), ReservedResourceName, ADMIN_GROUP, BOOTSTRAP_ADMIN, ScimGroupMember, UnknownGroupMemberException (+5 more)

### Community 98 - "ScimUser"
Cohesion: 0.18
Nodes (6): DuplicateUserNameException, ScimUser, ScimSearchServiceTests, InMemoryScimQueryRepository, InMemoryScimUserRepository, Override

### Community 99 - "assertthat"
Cohesion: 0.14
Nodes (48): arraynode, assertthat, autowired, RequestIdFilter, ConnectorTokenScope, READ_ONLY, READ_WRITE, AdminAccountEndpointTests (+40 more)

### Community 101 - "RFC requirements and implications"
Cohesion: 0.18
Nodes (11): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+3 more)

### Community 103 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.18
Nodes (4): AuditTrailService, Override, AuditEvent, org.springframework.transaction.annotation.Transactional

### Community 105 - "3. ECS-structured logging with redaction enforced structurally"
Cohesion: 0.33
Nodes (5): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status

### Community 106 - "ScimUserPatchOperationTests"
Cohesion: 0.11
Nodes (9): Condition, ScimEmailFilter, AddEmails, EmailUpdate, MergeName, RemoveEmailPart, RemoveEmails, UpdateEmails (+1 more)

### Community 107 - "AuditTrailServiceTests.java"
Cohesion: 0.07
Nodes (29): AuditAdministrativeRefusal, LAST_ENABLED_ADMINISTRATOR, PROTECTED_RESOURCE, SELF_DISABLE, AuditEventRepository, AuditGroupAttribute, DISPLAY_NAME, MEMBERS (+21 more)

### Community 108 - "ScimSecurityChainOrderTests.java"
Cohesion: 0.11
Nodes (18): anonymousauthenticationfilter, BackendApplicationTests, MockHttpServletRequest, ScimSecurityChainOrderTests, basicauthenticationfilter, classmode, csrffilter, disableencodeurlfilter (+10 more)

### Community 109 - "Credential and cryptographic policy"
Cohesion: 0.22
Nodes (9): Credential and cryptographic policy, Hashing and primitives, Key rotation and storage, Lockout policy, Log formatting, Password policy, Response headers, Session lifetime (+1 more)

### Community 111 - "Spring Session In Redis"
Cohesion: 0.33
Nodes (7): Backend Architecture Boundaries, Redis Compose Service, Spring Session In Redis, Flag ADR Conflicts Explicitly, docs/adr Decision Records, RedisCluster ElastiCache, RedisSubnetGroup

### Community 112 - "ScimConnectorTokenEntity"
Cohesion: 0.15
Nodes (4): ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 113 - ".toDomain"
Cohesion: 0.11
Nodes (4): ScimLoginStateValue, ScimUserEmailValue, ScimUserEntity, org.hibernate.annotations.DynamicUpdate

### Community 114 - "SpaFrontendTests"
Cohesion: 0.16
Nodes (8): ModelAndView, Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.stereotype.Component, org.springframework.web.servlet.ModelAndView, requestdispatcher

### Community 115 - "AuditOperation"
Cohesion: 0.08
Nodes (25): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+17 more)

### Community 117 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (13): AuditRetentionStartupTests, MockHttpSession, SecurityConfigTests, Connectors, Tokens, ConnectorTokenPolicyTests, Lifetime, RotationOverlap (+5 more)

### Community 118 - "AuditTrail"
Cohesion: 0.11
Nodes (20): AuditTrail, LoginIdentityService, ScimUserSessionRevocation, UserCounterService, ScimSearchService, ScimUserService, ScimExternalIdRepository, ScimGroupRepository (+12 more)

### Community 119 - ".fromSearchRequest"
Cohesion: 0.15
Nodes (4): ScimQueryRequest, InvalidScimQueryException, ScimQueryRequestTests, jsonnode

### Community 120 - "RecordingAuditTrail"
Cohesion: 0.16
Nodes (3): Override, Recorded, RecordingAuditTrail

### Community 121 - "AuditEventRecordingIntegrationTests"
Cohesion: 0.23
Nodes (3): AuditEventRecordingIntegrationTests, MockHttpSession, ResultActions

### Community 122 - "AuditUserAttribute"
Cohesion: 0.15
Nodes (10): AuditUserAttribute, ACTIVE, DISPLAY_NAME, EMAILS, LOCALE, NAME, PASSWORD, PREFERRED_LANGUAGE (+2 more)

### Community 123 - "ScimGroupResource"
Cohesion: 0.14
Nodes (7): ScimGroupListing, ScimGroupResource, ScimListedResource, Search, ScimGroupRenderer, ScimSearchRenderer, datetimeformatter

### Community 124 - "IndexedSessions"
Cohesion: 0.24
Nodes (6): Override, AccountSessionsAdapterTests, IndexedSessions, Override, MapSession, org.springframework.session.MapSession

### Community 126 - "ScimResourceEntity"
Cohesion: 0.23
Nodes (3): ScimGroupEntity, ScimResourceEntity, ScimGroupJpaRepository

### Community 128 - "ScimQueryProtocolIntegrationTests"
Cohesion: 0.14
Nodes (4): ScimQueryProtocolIntegrationTests, org.junit.jupiter.api.BeforeAll, org.junit.jupiter.api.TestInstance, org.junit.jupiter.params.provider.MethodSource

### Community 130 - "components.json"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

### Community 131 - "ScimPatchRefusedException"
Cohesion: 0.31
Nodes (4): Reason, MUTABILITY, NO_TARGET, ScimPatchRefusedException

### Community 132 - "ScimGroupController.java"
Cohesion: 0.16
Nodes (12): authenticationprincipal, ScimSearchListing, ScimUserListing, ScimSearchController, ScimPageRequest, org.springframework.web.bind.annotation.PatchMapping, org.springframework.web.bind.annotation.PutMapping, pathvariable (+4 more)

### Community 133 - "Domain and authority model"
Cohesion: 0.25
Nodes (8): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, Protected recovery resources, SCIM User replaces Account

### Community 135 - ".handle"
Cohesion: 0.13
Nodes (7): ScimErrorException, ScimExceptionHandler, InvalidPreconditionException, PreconditionFailedException, PreconditionRequiredException, org.springframework.http.HttpStatus, org.springframework.web.bind.annotation.RestControllerAdvice

### Community 137 - "AuthControllerTests"
Cohesion: 0.18
Nodes (7): AuthController, LoginRequest, UserResponse, AuthControllerTests, MockHttpServletResponse, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy, org.springframework.session.web.http.CookieSerializer

### Community 138 - "ScimExternalIdPersistenceAdapter"
Cohesion: 0.20
Nodes (5): Override, Key, ScimExternalIdJpaRepository, Override, ScimExternalIdPersistenceAdapter

### Community 139 - "EcsLogFormatTests"
Cohesion: 0.15
Nodes (8): LockoutHasNoDurationTests, EcsLogFormatTests, MockHttpSession, ResultActions, files, java.lang.reflect.RecordComponent, org.springframework.core.env.Environment, path

### Community 140 - "UserCounter"
Cohesion: 0.20
Nodes (4): UserCounter, Override, UserCounterPersistenceAdapter, UserCounterTests

### Community 141 - "ScimEmail"
Cohesion: 0.11
Nodes (14): arraylist, NewScimUser, Override, ScimUserReplacement, ScimEmail, ScimName, ScimUserProfile, ScimUserReplacementTests (+6 more)

### Community 142 - ".increment"
Cohesion: 0.16
Nodes (6): getCount operation, incrementCount operation, resetCount operation, CountResponse, UserCounterController, UserCounterRepository

### Community 143 - "ScimGroup"
Cohesion: 0.14
Nodes (4): DuplicateDisplayNameException, ScimGroup, InMemoryScimGroupRepository, Override

### Community 144 - "AuthenticatedConnector"
Cohesion: 0.16
Nodes (6): ScimDiscoveryController, ScimGroupController, ScimUserController, AuthenticatedConnector, org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.GetMapping

### Community 145 - "dependencies"
Cohesion: 0.15
Nodes (13): class-variance-authority, clsx, dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom (+5 more)

### Community 146 - "Frontend Architecture Doc"
Cohesion: 0.18
Nodes (14): Colors Come From index.css Tokens, Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, Vite Full-Reloads on Any Watched HTML Write, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline, Vitest Deliberately Omits the Tailwind Vite Plugin (+6 more)

### Community 147 - "ScimAttributeProjection"
Cohesion: 0.20
Nodes (6): SuppressWarnings, Kind, GROUP, USER, ScimAttributeProjection, linkedhashset

### Community 149 - "IdentitySummary"
Cohesion: 0.29
Nodes (4): IdentitySummary, AdminAccountControllerTests, Override, RecordingService

### Community 152 - "accounts.tsx"
Cohesion: 0.14
Nodes (14): useAuth(), useAuthState(), useSessionRequest(), AccountAction, Accounts(), actionFailure(), AdminAccount, decodeAccount() (+6 more)

### Community 154 - "ScimQueryVocabulary.java"
Cohesion: 0.03
Nodes (80): active, of(), parent(), ScimFilterPath, ACTIVE, DISPLAY_NAME, EMAILS, EMAILS_PRIMARY (+72 more)

### Community 157 - "Kiro: graphify enforcement"
Cohesion: 0.40
Nodes (4): graphify-runner, Kiro: graphify enforcement, The hooks, When the refresh fails

### Community 158 - "org.springframework.data.jpa.repository.Query"
Cohesion: 0.20
Nodes (9): UserCounterJpaRepository, ScimResourceJpaRepository, ScimUserJpaRepository, lockmodetype, org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.jpa.repository.Lock, org.springframework.data.jpa.repository.Modifying, org.springframework.data.jpa.repository.Query (+1 more)

### Community 159 - "ScimQuery"
Cohesion: 0.12
Nodes (6): Hit, Result, ScimQuery, Override, ScimSortTests, Override

### Community 160 - "LoginLockoutTests.java"
Cohesion: 0.15
Nodes (11): atomicinteger, authentication, authenticationexception, AfterCommit, LoginAttemptService, LoginService, AccountSessions, LockoutPolicy (+3 more)

### Community 161 - "ScimFilterParser"
Cohesion: 0.23
Nodes (4): InvalidScimFilterException, Comparison, ResolvedPath, ScimFilterParser

### Community 162 - ".write"
Cohesion: 0.11
Nodes (6): PasswordReusedException, Cause, DEACTIVATED, DELETED, PASSWORD_CHANGED, USER_NAME_CHANGED

### Community 164 - "ScimUserPersistenceAdapter"
Cohesion: 0.22
Nodes (3): Override, ScimUserPersistenceAdapter, org.springframework.data.domain.Sort

### Community 169 - "ScimGroupMemberJpaRepository"
Cohesion: 0.13
Nodes (7): ScimGroupReference, ScimGroupMemberEntity, Override, ScimGroupMemberId, ScimGroupMemberJpaRepository, ScimGroupMemberRow, collection

### Community 170 - ".current"
Cohesion: 0.29
Nodes (5): HttpAuditRequestContext, Override, HttpAuditRequestContextTests, MockHttpServletRequest, org.springframework.mock.web.MockHttpServletRequest

### Community 171 - "ScimUserPatchReader"
Cohesion: 0.17
Nodes (7): Op, ADD, REMOVE, REPLACE, Path, ScimUserPatchReader, token()

### Community 174 - "MutableClock"
Cohesion: 0.22
Nodes (5): AuditRetentionServiceTests, CountingRetention, Override, Override, MutableClock

### Community 175 - "ScimPasswordHistoryPersistenceAdapter"
Cohesion: 0.48
Nodes (3): ScimPasswordHistoryJpaRepository, Override, ScimPasswordHistoryPersistenceAdapter

### Community 176 - "EcsLogCapture"
Cohesion: 0.20
Nodes (8): EcsLogCapture, Override, bytearrayoutputstream, ch.qos.logback.classic.Logger, ch.qos.logback.classic.LoggerContext, ch.qos.logback.core.OutputStreamAppender, OutputStreamAppender, structuredlogencoder

### Community 178 - "AuditRetentionServiceTests.java"
Cohesion: 0.19
Nodes (8): Override, LoggingOperationalAlerts, LogEvent, badcredentialsexception, disabledexception, lockedexception, loggerfactory, usernamepasswordauthenticationtoken

### Community 179 - "ScimConnectorToken"
Cohesion: 0.11
Nodes (14): ConnectorAuthenticationService, ConnectorTokenPolicy, ConnectorTokenSecret, ScimConnectorRepository, ScimConnectorToken, ScimConnectorTokenRepository, InMemoryScimConnectorTokenRepository, Override (+6 more)

### Community 180 - "SessionController"
Cohesion: 0.30
Nodes (6): deleteSession operation, SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession

### Community 181 - "CapturedLog"
Cohesion: 0.24
Nodes (6): CapturedLog, ch.qos.logback.classic.Level, ch.qos.logback.classic.spi.ILoggingEvent, ch.qos.logback.core.read.ListAppender, function, keyvaluepair

### Community 182 - "App.tsx"
Cohesion: 0.18
Nodes (9): Coverage Excludes Are Listed, Not Globbed, Frontend SPA Entry HTML, App(), AuthProvider(), GuestRoute(), ProtectedRoute(), frontend_src_index, container (+1 more)

### Community 183 - "AuditRetentionPolicy"
Cohesion: 0.24
Nodes (4): applicationconversionservice, AuditRetentionPolicy, AuditRetentionPolicyTests, propertysourcesplaceholderconfigurer

### Community 184 - "mutate"
Cohesion: 0.18
Nodes (11): !src/**/*.test.ts, !src/**/*.test.tsx, !src/**/*.testHelpers.ts, !src/**/*.testHelpers.tsx, !test/**, mutate, !src/components/ui/**, !src/**/*.d.ts (+3 more)

### Community 186 - "Operator"
Cohesion: 0.20
Nodes (10): Operator, CO, EQ, EW, GE, GT, LE, LT (+2 more)

### Community 187 - "Delivery plan"
Cohesion: 0.20
Nodes (10): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 0a — Permanent lockout, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 4 — Groups and Admin authority, Slice 5 — Complete query protocol (+2 more)

### Community 188 - "org.junit.jupiter.params.provider.CsvSource"
Cohesion: 0.16
Nodes (4): ScimAuditFilterShapesTests, ScimPageRequestTests, org.junit.jupiter.params.provider.CsvSource, org.junit.jupiter.params.provider.EnumSource

### Community 189 - "ignorePatterns"
Cohesion: 0.22
Nodes (9): ignorePatterns, .agents, artifacts, .claude, coverage, dist, graphify-out, playwright-report (+1 more)

### Community 190 - "Frontend Technology Stack"
Cohesion: 0.25
Nodes (8): No Parent-Relative Paths From An App, SPA Build Contract, with-frontend Maven Profile, Backend Serves SPA And Forwards Routes, Claim: No Router Data Layer Or Auth, No .env Required In Frontend, Frontend Technology Stack, npm ci Not npm install

### Community 192 - "RequestIdFilterTests.java"
Cohesion: 0.29
Nodes (4): dispatchertype, mdc, org.springframework.mock.web.MockFilterChain, webutils

### Community 193 - "RecordingTransactionManager"
Cohesion: 0.43
Nodes (4): Override, RecordingTransactionManager, org.springframework.transaction.TransactionDefinition, org.springframework.transaction.TransactionStatus

### Community 196 - "CONTEXT"
Cohesion: 0.29
Nodes (6): Accounts and identity provisioning, CONTEXT, Current account model, Request paths, SCIM target model, Sessions

### Community 197 - "1. Count login attempts on the login path"
Cohesion: 0.29
Nodes (6): 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision, Status

### Community 198 - "2. Revoke a disabled account's sessions after the commit"
Cohesion: 0.29
Nodes (6): 2. Revoke a disabled account's sessions after the commit, Alternatives considered, Consequences, Context, Decision, Status

### Community 199 - "4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal"
Cohesion: 0.29
Nodes (6): 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal, Alternatives considered, Consequences, Context, Decision, Status

### Community 204 - "Query contract"
Cohesion: 0.33
Nodes (6): Attribute projection, Filtering, Pagination, POST search, Query contract, Sorting

### Community 205 - "Definition of Done"
Cohesion: 0.40
Nodes (5): Backend gates, Contract and behavior, Definition of Done, Documentation and graph, Frontend gates

### Community 206 - "Write semantics"
Cohesion: 0.40
Nodes (5): DELETE and tombstones, PATCH, POST, PUT, Write semantics

### Community 207 - "reporters"
Cohesion: 0.40
Nodes (5): reporters, clear-text, html, json, progress

### Community 208 - "thresholds"
Cohesion: 0.50
Nodes (4): thresholds, break, high, low

### Community 210 - "Supported schemas"
Cohesion: 0.67
Nodes (3): Group, Supported schemas, User

### Community 211 - "clearTextReporter"
Cohesion: 0.67
Nodes (3): clearTextReporter, allowColor, maxTestsToLog

### Community 212 - "_comment_mutate"
Cohesion: 0.67
Nodes (3): _comment_mutate, src/components/ui/** is vendored placeholder code, due to be deleted when the in-house shadcn package is published. Its mutants are edits to Tailwind class strings, and killing them means pinning assertions to markup that is about to be replaced., src/main.tsx is the composition root: its only statement is a createRoot call against the real document, so every mutant is either uncoverable or a restatement of what the smoke E2E already proves.

## Ambiguous Edges - Review These
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **500 isolated node(s):** `.graphify_probe.sh script`, `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script` (+495 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **74 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Backend API Contract (OpenAPI 3.1)` connect `Backend API Contract (OpenAPI 3.1)` to `SessionController`, `http.ts`, `.increment`?**
  _High betweenness centrality (0.099) - this node is a cross-community bridge._
- **Why does `login operation` connect `Backend API Contract (OpenAPI 3.1)` to `auth.helpers.ts`, `http.ts`?**
  _High betweenness centrality (0.073) - this node is a cross-community bridge._
- **Why does `Shared Playwright storageState for Auth` connect `auth.helpers.ts` to `Frontend Architecture Doc`, `Backend API Contract (OpenAPI 3.1)`?**
  _High betweenness centrality (0.061) - this node is a cross-community bridge._
- **What connects `.graphify_probe.sh script`, `graphify-guard.sh script`, `graphify-refresh.sh script` to the rest of the system?**
  _500 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Attribute` be split into smaller, more focused modules?**
  _Cohesion score 0.04053109713487072 - nodes in this community are weakly interconnected._