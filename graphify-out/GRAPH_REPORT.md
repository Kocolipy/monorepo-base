# Graph Report - monorepo-base-issue-15  (2026-09-27)

## Corpus Check
- 366 files · ~228,383 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: (none) 10, .example 1, .properties 1)

## Summary
- 3795 nodes · 12399 edges · 179 communities (124 shown, 55 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 1455 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `57dae62f`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- AuthControllerTests.java
- AuditTrail
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
- route-guards.tsx
- infra/ Is Deployment Material Not An App
- .given
- tsconfig.test.json
- SecurityConfig.java
- Domain Documentation Guide
- GitHub Issue Tracker Guide
- IdentityAdministrationServiceTests
- PIT Scoped To Touched Tests
- Graphify Runner Agent
- AuthenticatedConnector
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
- AuditRetentionServiceTests.java
- Backend API Contract (OpenAPI 3.1)
- .ofUser
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh
- AuditRetentionPolicy
- ScimUserServiceTests
- ConnectorTokenSecretTests
- auth-context-value.ts
- ArchitectureTest.java
- accountseed
- ReservedResourceName
- LoginAttemptServiceTests
- SCIM 2.0 account-management specification plan
- AfterCommitAdapterTests.java
- .scimType
- ConnectorAdministrationService
- AuditAppendOnlyIntegrationTests
- dependencies
- IdentitySummary
- ScimUserAttributesTests
- LogContextTests
- ScimBearerAuthenticationFilterTests
- MutableClock
- ScimConditionalWriteIntegrationTests
- assertthat
- BackendApplication.java
- ScimConnectorEntity
- verify.sh
- list
- jakarta.persistence.Entity
- jakarta.servlet.http.HttpServletRequest
- AuditTrailServiceTests
- ScimUserSessionRevocationTests.java
- RecordingCounterService
- ScimUserProfile
- ScimGroupController.java
- ScimSecurityChainOrderTests.java
- RFC requirements and implications
- AuditAdministrativeRefusal
- ScimUser
- SCIM 2.0 account-management research
- Credential and cryptographic policy
- Delivery plan
- bcryptpasswordencoder
- org.springframework.transaction.annotation.Transactional
- Query contract
- Domain and authority model
- ScimUserPatchOperationTests
- AuditEvent
- Definition of Done
- Write semantics
- ScimLoginState
- AdminAccountEndpointTests.java
- ScimConnectorTokenEntity
- ScimUserEntity
- SpaFrontendTests
- AuditOperation
- .of
- org.junit.jupiter.api.Test
- InMemoryScimUserRepository
- EcsLogCapture
- RecordingAuditTrail
- 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal
- AuditUserAttribute
- CapturedLog
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
- ScimConnectorToken
- .handle
- .of
- AuthControllerTests
- ScimExternalIdEntity
- EcsLogFormatTests
- UserCounter
- ScimEmail
- org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
- InMemoryScimGroupRepository
- AuditEventRetention
- RequestIdFilterTests
- Frontend Architecture Doc
- .overlapEnd
- ScimSchemas
- AuditRefusalReason
- filterchainproxy
- mockfilterchain
- mockhttpservletresponse
- recordcomponent
- AWS CloudFormation Deployment Guide
- ScimUserPatchReaderTests
- Baseline Full Extensive Test Levels
- IndexedSessions
- org.springframework.data.jpa.repository.JpaRepository
- ScimPageRequest
- InMemoryScimExternalIdRepository
- Spring Session In Redis
- UserCounterServiceTests
- .the_string_form_says_whether_a_password_was_sent_and_never_what_it_was
- AuditTrailServiceTests.java
- UnknownConnectorException
- CountingPasswordEncoder
- transactiondefinition
- transactionstatus
- CountingPasswordEncoder
- HttpAuditRequestContextTests.java
- ScimUserPatchReader
- drivermanager
- inmemoryuserdetailsmanager
- ScimEmailFilter
- FakeSaltedEncoder
- ScimExceptionHandler.java
- .summarize
- CountingPasswordEncoder

## God Nodes (most connected - your core abstractions)
1. `ScimUser` - 109 edges
2. `ScimGroupProvisioningIntegrationTests` - 77 edges
3. `ScimGroup` - 72 edges
4. `IdentityAdministrationServiceTests` - 65 edges
5. `ScimConditionalWriteIntegrationTests` - 64 edges
6. `AuditTrail` - 60 edges
7. `AuditOperation` - 58 edges
8. `RecordingAuditTrail` - 53 edges
9. `ReservedResourceName` - 50 edges
10. `ScimUserRepository` - 50 edges

## Surprising Connections (you probably didn't know these)
- `Consequences` --references--> `AuditTrailServiceTests`  [INFERRED]
  docs/adr/0004-audit-append-failure-semantics.md → backend/src/test/java/com/example/backend/audit/application/AuditTrailServiceTests.java
- `getCurrentUser operation` --shares_data_with--> `getCurrentUser()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `Frontend Local Semgrep Ruleset` --semantically_similar_to--> `Backend Semgrep Baseline Gate`  [INFERRED] [semantically similar]
  frontend/AGENTS.md → backend/AGENTS.md
- `Decision` --references--> `AfterCommit`  [INFERRED]
  docs/adr/0002-revoke-sessions-after-commit.md → backend/src/main/java/com/example/backend/auth/application/AfterCommit.java
- `Existing application seams` --references--> `SecurityConfig`  [INFERRED]
  docs/research/scim-v2-account-management-research.md → backend/src/main/java/com/example/backend/auth/config/SecurityConfig.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Public Request Path Through The Stack** — infra_infrastructure_alblistener, infra_infrastructure_albtargetgroup, infra_infrastructure_ec2instance [EXTRACTED 1.00]
- **Mutation Testing as the Load-Bearing-Test Doctrine on Both Sides** — frontend_docs_testing_guide_stryker_mutate_trap, frontend_docs_testing_guide_assert_exactly [INFERRED 0.85]
- **Local Compose Versus Cloud Datastores** — backend_compose_postgres_service, backend_compose_redis_service, infra_infrastructure_dbinstance, infra_infrastructure_rediscluster [INFERRED 0.85]
- **Published Credential Exposure Surface** — agents_published_credentials_warning, backend_readme_dev_default_credentials, infra_infrastructure_app_credential_parameters, backend_semgrep_rules_service_security_be_hardcoded_credential_literal [INFERRED 0.85]

## Communities (179 total, 55 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 1 - "AuthControllerTests.java"
Cohesion: 0.07
Nodes (31): assertthatcode, assertthatnoexception, authenticationmanager, chronounit, classpathresource, containsstring, content, cookie (+23 more)

### Community 2 - "AuditTrail"
Cohesion: 0.10
Nodes (21): AuditTrail, AfterCommit, LoginAttemptService, LoginIdentityService, ScimUserSessionRevocation, AccountSessions, UserCounterService, UserCounterRepository (+13 more)

### Community 3 - "accounts.tsx"
Cohesion: 0.13
Nodes (29): components/ui Is a Package Placeholder, useAuth(), useAuthState(), useSessionRequest(), Button(), ButtonProps, buttonVariants, Card() (+21 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.17
Nodes (13): Every Playwright Spec Needs a testMatch, Shared Playwright storageState for Auth, ADMIN_CREDENTIALS, captureSessionCookie(), expireSession(), login(), loginAs(), postAdminAction() (+5 more)

### Community 5 - "http.ts"
Cohesion: 0.15
Nodes (18): decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, SessionRequest, SessionResult (+10 more)

### Community 6 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 8 - ".require"
Cohesion: 0.12
Nodes (10): LoginOutcome, LoginService, LoginLockoutTests, 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision (+2 more)

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

### Community 17 - "ScimUserServiceTests.java"
Cohesion: 0.09
Nodes (38): addemails, NamePart, FAMILY_NAME, FORMATTED, GIVEN_NAME, HONORIFIC_PREFIX, HONORIFIC_SUFFIX, MIDDLE_NAME (+30 more)

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
Nodes (14): ScimWriteScopeRule, SpaRoutes, LockoutPolicyTests, ScimPageRequestTests, ScimVersionPreconditionTests, ScimWriteScopeRuleTests, SpaRoutesScimNamespaceTests, ReservedServerPaths (+6 more)

### Community 26 - "DBInstance RDS PostgreSQL"
Cohesion: 0.29
Nodes (8): docs/openapi.yaml API Contract, Postgres Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, DBInstance RDS PostgreSQL, DBSubnetGroup

### Community 27 - "route-guards.tsx"
Cohesion: 0.13
Nodes (17): Accounts and identity provisioning, CONTEXT, Current account model, Request paths, Sessions, AuthRole, AuthStatus, GuestRoute() (+9 more)

### Community 28 - "infra/ Is Deployment Material Not An App"
Cohesion: 0.29
Nodes (7): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, CONTEXT.md Domain Glossary, gh CLI Conventions, GitHub Issues As Issue Tracker, PRs As Request Surface Flag

### Community 29 - ".given"
Cohesion: 0.14
Nodes (3): Override, LoginIdentityServiceTests, org.springframework.security.core.userdetails.UserDetails

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "SecurityConfig.java"
Cohesion: 0.05
Nodes (42): argon2passwordencoder, authenticationentrypoint, authorizationfilter, AuditRetentionPolicyConfig, LoginLockoutConfig, SecurityConfig, ScimSecurityConfig, ScimSeedConfig (+34 more)

### Community 32 - "Domain Documentation Guide"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 33 - "GitHub Issue Tracker Guide"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 36 - "Graphify Runner Agent"
Cohesion: 0.40
Nodes (5): Graphify Runner Agent, Recorded Interpreter Guard, Graph Shrink Refusal, Graphify Refresh Before Commit, CLAUDE.md Graphify Override

### Community 37 - "AuthenticatedConnector"
Cohesion: 0.07
Nodes (16): NewScimGroup, AddMembers, RemoveMembers, ScimGroupReplacement, ScimGroupResource, ScimGroupService, SuppressWarnings, Kind (+8 more)

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

### Community 50 - "uuid"
Cohesion: 0.12
Nodes (7): ConnectorAuthenticationService, ScimConnectorRepository, ScimConnectorTokenRepository, instant, optional, org.springframework.stereotype.Repository, uuid

### Community 56 - "AuditRetentionServiceTests.java"
Cohesion: 0.13
Nodes (17): AuditRetentionService, AuditRetentionScheduleConfig, Override, Override, LoggingOperationalAlerts, LogEvent, badcredentialsexception, crontask (+9 more)

### Community 57 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.10
Nodes (22): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, deleteSession operation, getCurrentUser operation, getHealth operation, getSession operation, login operation, logout operation (+14 more)

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "AuditRetentionPolicy"
Cohesion: 0.22
Nodes (4): applicationconversionservice, AuditRetentionPolicy, AuditRetentionPolicyTests, propertysourcesplaceholderconfigurer

### Community 63 - "ScimUserServiceTests"
Cohesion: 0.09
Nodes (8): NewScimUser, ScimUserReplacement, ScimUserResource, SetActive, Revocation, ScimUserServiceTests, InMemoryScimPasswordHistoryRepository, Override

### Community 64 - "ConnectorTokenSecretTests"
Cohesion: 0.09
Nodes (12): ConnectorTokenDigest, Override, ConnectorTokenSecret, Minted, Presented, ConnectorTokenSecretTests, base64, hashset (+4 more)

### Community 65 - "auth-context-value.ts"
Cohesion: 0.08
Nodes (21): Frontend SPA Entry HTML, App(), AuthUser, AuthProvider(), AuthContext, AuthContextState, AuthContextValue, apiFetchMock (+13 more)

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.08
Nodes (26): archcondition, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, conditionevents, configuration (+18 more)

### Community 68 - "ReservedResourceName"
Cohesion: 0.13
Nodes (8): ProtectedResourceException, ReservedResourceName, ADMIN_GROUP, BOOTSTRAP_ADMIN, ScimResourceType, GROUP, USER, dataintegrityviolationexception

### Community 70 - "SCIM 2.0 account-management specification plan"
Cohesion: 0.09
Nodes (22): Accepted policy deviations, Actors, Admin API and Accounts page, Application architecture, Audit and retention, Connector identity and token lifecycle, Deviations from the Standalone User Access Control standard, Goals (+14 more)

### Community 71 - "AfterCommitAdapterTests.java"
Cohesion: 0.13
Nodes (12): AfterCommitAdapter, Override, AfterCommitAdapterTests, 2. Revoke a disabled account's sessions after the commit, Alternatives considered, Consequences, Context, Decision (+4 more)

### Community 72 - ".scimType"
Cohesion: 0.18
Nodes (9): 3. ECS-structured logging with redaction enforced structurally, Consequences, Context, Decision, Status, Error contract, Log formatting, Operational telemetry (+1 more)

### Community 74 - "AuditAppendOnlyIntegrationTests"
Cohesion: 0.05
Nodes (6): AuditAppendOnlyIntegrationTests, AuditEventRecordingIntegrationTests, ResultActions, ProbeController, SecurityConfigTests, AdminAccountEndpointTests

### Community 76 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 77 - "IdentitySummary"
Cohesion: 0.10
Nodes (16): IdentitySummary, UnknownIdentityException, UnsafeIdentityChangeException, AdminAccountController, IssuedConnectorToken, AdminConnectorController, CreateConnectorRequest, IssueTokenRequest (+8 more)

### Community 78 - "ScimUserAttributesTests"
Cohesion: 0.05
Nodes (7): ScimDiscovery, ScimGroupAttributes, Attribute, ScimUserAttributes, ScimDiscoveryTests, SuppressWarnings, ScimUserAttributesTests

### Community 79 - "LogContextTests"
Cohesion: 0.19
Nodes (4): Override, LogContext, Scope, LogContextTests

### Community 81 - "MutableClock"
Cohesion: 0.17
Nodes (6): AuditRetentionServiceTests, CountingRetention, Override, Override, Override, MutableClock

### Community 83 - "assertthat"
Cohesion: 0.15
Nodes (43): assertthat, autowired, RequestIdFilter, ConnectorTokenScope, READ_ONLY, READ_WRITE, ContainerTestConfiguration, InMemorySessionRegistryConfiguration (+35 more)

### Community 84 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 85 - "ScimConnectorEntity"
Cohesion: 0.22
Nodes (4): ScimConnectorEntity, ScimConnectorJpaRepository, Override, ScimConnectorPersistenceAdapter

### Community 87 - "list"
Cohesion: 0.11
Nodes (13): arraylist, collectors, comparator, datetimeformatter, linkedhashmap, linkedhashset, list, locale (+5 more)

### Community 88 - "jakarta.persistence.Entity"
Cohesion: 0.06
Nodes (30): UserCounterEntity, ScimGroupMemberEntity, Override, ScimGroupMemberId, ScimPasswordHistoryEntity, ScimUserEmailValue, ScimPasswordHistoryJpaRepository, Override (+22 more)

### Community 89 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.08
Nodes (22): AbsoluteSessionLifetimeFilter, Override, AbsoluteSessionLifetimePolicy, Override, Override, ScimBearerAuthenticationFilter, ScimBearerChallenge, ScimReleaseGate (+14 more)

### Community 91 - "ScimUserSessionRevocationTests.java"
Cohesion: 0.13
Nodes (8): Override, Cause, DEACTIVATED, PASSWORD_CHANGED, USER_NAME_CHANGED, ScimUserSessionRevocationTests, cause, enumset

### Community 92 - "RecordingCounterService"
Cohesion: 0.27
Nodes (3): Override, RecordingCounterService, UserCounterControllerTests

### Community 93 - "ScimUserProfile"
Cohesion: 0.11
Nodes (9): Override, Kind, CLEAR, SET, UNCHANGED, ScimPasswordChange, ScimUserEdit, Override (+1 more)

### Community 94 - "ScimGroupController.java"
Cohesion: 0.11
Nodes (26): authenticationprincipal, UserCounterController, ScimDiscoveryController, InvalidConnectorTokenLifetimeException, cachecontrol, cookievalue, httpstatus, notblank (+18 more)

### Community 95 - "ScimSecurityChainOrderTests.java"
Cohesion: 0.11
Nodes (18): anonymousauthenticationfilter, BackendApplicationTests, ScimSecurityChainOrderTests, basicauthenticationfilter, classmode, csrffilter, disableencodeurlfilter, exceptiontranslationfilter (+10 more)

### Community 96 - "RFC requirements and implications"
Cohesion: 0.17
Nodes (12): Authentication and filter-chain separation, Base URI, media type and discovery, Connector-scoped externalId, CRUD, replacement and PATCH, Deletion, tombstones and audit, ETags and multi-writer concurrency, Groups and authorization, RFC requirements and implications (+4 more)

### Community 97 - "AuditAdministrativeRefusal"
Cohesion: 0.29
Nodes (4): AuditAdministrativeRefusal, LAST_ENABLED_ADMINISTRATOR, PROTECTED_RESOURCE, SELF_DISABLE

### Community 98 - "ScimUser"
Cohesion: 0.11
Nodes (6): DuplicateUserNameException, ScimUser, ScimUserJpaRepository, Override, ScimUserPersistenceAdapter, Override

### Community 99 - "SCIM 2.0 account-management research"
Cohesion: 0.22
Nodes (7): Executive finding, Existing application seams, Primary sources, Recommended implementation order, Resolved RFC decisions, SCIM 2.0 account-management research, Testing strategy

### Community 100 - "Credential and cryptographic policy"
Cohesion: 0.25
Nodes (8): Credential and cryptographic policy, Hashing and primitives, Key rotation and storage, Lockout policy, Password policy, Response headers, Session lifetime, Uniform authentication timing

### Community 101 - "Delivery plan"
Cohesion: 0.20
Nodes (10): Delivery plan, Slice 0 — Persistence and stable-identity prefactor, Slice 0a — Permanent lockout, Slice 1 — Connector security and public discovery, Slice 2 — User create/read/search foundation, Slice 3 — User conditional PUT/PATCH/DELETE, Slice 4 — Groups and Admin authority, Slice 5 — Complete query protocol (+2 more)

### Community 103 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.20
Nodes (3): AuditTrailService, Override, org.springframework.transaction.annotation.Transactional

### Community 104 - "Query contract"
Cohesion: 0.40
Nodes (5): Attribute projection, Filtering, POST search, Query contract, Sorting

### Community 105 - "Domain and authority model"
Cohesion: 0.29
Nodes (7): Authority, Authorization matrix, Domain and authority model, Dormant authority revocation, Forced and self-service password change, Inactivity deactivation, SCIM User replaces Account

### Community 106 - "ScimUserPatchOperationTests"
Cohesion: 0.15
Nodes (6): AddEmails, EmailUpdate, RemoveEmailPart, UpdateEmails, ScimUserPatchOperationTests, org.junit.jupiter.params.provider.EnumSource

### Community 107 - "AuditEvent"
Cohesion: 0.17
Nodes (9): AuditEvent, AuditOutcome, FAILURE, SUCCESS, AuditEventJpaRepository, AuditEventPersistenceAdapter, Override, AuditEventEntity (+1 more)

### Community 108 - "Definition of Done"
Cohesion: 0.40
Nodes (5): Backend gates, Contract and behavior, Definition of Done, Documentation and graph, Frontend gates

### Community 109 - "Write semantics"
Cohesion: 0.40
Nodes (5): DELETE and tombstones, PATCH, POST, PUT, Write semantics

### Community 110 - "ScimLoginState"
Cohesion: 0.08
Nodes (4): ScimLoginState, ReservedResourceNameTests, ScimLoginStateTests, ScimUserTests

### Community 111 - "AdminAccountEndpointTests.java"
Cohesion: 0.14
Nodes (12): AccountSessionsAdapter, NormalizedUserName, RedisSessionRevocationIntegrationTests, jsonpath, matchers, org.springframework.session.FindByIndexNameSessionRepository, org.springframework.session.Session, org.springframework.stereotype.Component (+4 more)

### Community 112 - "ScimConnectorTokenEntity"
Cohesion: 0.15
Nodes (4): ScimConnectorTokenEntity, ScimConnectorTokenJpaRepository, Override, ScimConnectorTokenPersistenceAdapter

### Community 113 - "ScimUserEntity"
Cohesion: 0.12
Nodes (3): ScimLoginStateValue, ScimResourceEntity, ScimUserEntity

### Community 114 - "SpaFrontendTests"
Cohesion: 0.20
Nodes (6): Override, SpaErrorViewResolver, SpaFrontendTests, org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver, org.springframework.web.servlet.ModelAndView, requestdispatcher

### Community 115 - "AuditOperation"
Cohesion: 0.09
Nodes (23): AuditOperation, ACCOUNT_DISABLE, ACCOUNT_ENABLE, CONNECTOR_CREATE, CONNECTOR_DELETE, CONNECTOR_TOKEN_ISSUE, CONNECTOR_TOKEN_REVOKE, CONNECTOR_TOKEN_ROTATE (+15 more)

### Community 117 - "org.junit.jupiter.api.Test"
Cohesion: 0.05
Nodes (12): AuditRetentionStartupTests, RefusalTimingEquivalenceTests, AbsoluteSessionLifetimeFilterTests, AbsoluteSessionLifetimePolicyTests, Connectors, Tokens, PasswordNormalizationTests, CompositeKeyContractTests (+4 more)

### Community 118 - "InMemoryScimUserRepository"
Cohesion: 0.12
Nodes (19): arrays, assertthatthrownby, atomicinteger, authentication, authenticationexception, ConnectorTokenPolicy, ScimGroupMember, Override (+11 more)

### Community 119 - "EcsLogCapture"
Cohesion: 0.18
Nodes (6): EcsLogCapture, Override, bytearrayoutputstream, ch.qos.logback.classic.LoggerContext, ch.qos.logback.core.OutputStreamAppender, structuredlogencoder

### Community 120 - "RecordingAuditTrail"
Cohesion: 0.18
Nodes (3): Override, Recorded, RecordingAuditTrail

### Community 121 - "4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal"
Cohesion: 0.25
Nodes (6): 4. Audit append failure semantics: fail-closed on a write, fail-open on a refusal, Alternatives considered, Consequences, Context, Decision, Status

### Community 122 - "AuditUserAttribute"
Cohesion: 0.15
Nodes (10): AuditUserAttribute, ACTIVE, DISPLAY_NAME, EMAILS, LOCALE, NAME, PASSWORD, PREFERRED_LANGUAGE (+2 more)

### Community 123 - "CapturedLog"
Cohesion: 0.24
Nodes (7): CapturedLog, ch.qos.logback.classic.Level, ch.qos.logback.classic.Logger, ch.qos.logback.classic.spi.ILoggingEvent, ch.qos.logback.core.read.ListAppender, function, keyvaluepair

### Community 124 - "ScimOffsetPage"
Cohesion: 0.18
Nodes (5): Override, ScimOffsetPage, ScimOffsetPageTests, org.springframework.data.domain.Pageable, org.springframework.data.domain.Sort

### Community 125 - "ScimConnector"
Cohesion: 0.19
Nodes (4): ScimConnector, ConnectorAuthenticationServiceTests, InMemoryScimConnectorRepository, Override

### Community 126 - "ScimGroup"
Cohesion: 0.11
Nodes (8): ScimGroup, ScimGroupReference, ScimGroupMemberJpaRepository, ScimGroupMemberRow, Override, ScimGroupPersistenceAdapter, ScimResourceJpaRepository, org.springframework.data.jpa.repository.Query

### Community 127 - "ScimDiscoveryIntegrationTests"
Cohesion: 0.19
Nodes (3): ScimConditionalWrites, ScimDiscoveryIntegrationTests, org.springframework.test.web.servlet.request.RequestPostProcessor

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

### Community 134 - "ScimConnectorToken"
Cohesion: 0.19
Nodes (4): ScimConnectorToken, ScimConnectorTokenTests, InMemoryScimConnectorTokenRepository, Override

### Community 135 - ".handle"
Cohesion: 0.13
Nodes (7): ScimErrorException, ScimExceptionHandler, InvalidPreconditionException, PreconditionFailedException, PreconditionRequiredException, org.springframework.http.HttpStatus, org.springframework.web.bind.annotation.RestControllerAdvice

### Community 137 - "AuthControllerTests"
Cohesion: 0.17
Nodes (7): AuthController, LoginRequest, UserResponse, AuthControllerTests, org.springframework.security.core.Authentication, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy, org.springframework.session.web.http.CookieSerializer

### Community 138 - "ScimExternalIdEntity"
Cohesion: 0.13
Nodes (7): Override, Key, ScimExternalIdEntity, ScimExternalIdJpaRepository, Override, ScimExternalIdPersistenceAdapter, ScimExternalIdEntity.Key

### Community 140 - "UserCounter"
Cohesion: 0.10
Nodes (8): getCount operation, incrementCount operation, resetCount operation, CountResponse, UserCounter, Override, UserCounterPersistenceAdapter, UserCounterTests

### Community 143 - "InMemoryScimGroupRepository"
Cohesion: 0.11
Nodes (3): ScimSeedServiceTests, InMemoryScimGroupRepository, Override

### Community 144 - "AuditEventRetention"
Cohesion: 0.25
Nodes (3): AuditEventRetention, AuditEventRetentionAdapter, Override

### Community 146 - "Frontend Architecture Doc"
Cohesion: 0.15
Nodes (16): Colors Come From index.css Tokens, Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, Vite Full-Reloads on Any Watched HTML Write, One-Way Import Direction Through the Layers, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline (+8 more)

### Community 147 - ".overlapEnd"
Cohesion: 0.16
Nodes (3): ConnectorTokenPolicyTests, Lifetime, RotationOverlap

### Community 149 - "AuditRefusalReason"
Cohesion: 0.33
Nodes (6): AuditRefusalReason, ACCOUNT_DISABLED, ACCOUNT_LOCKED, BAD_CREDENTIALS, OTHER, UNKNOWN_ACCOUNT

### Community 154 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.25
Nodes (9): ALBListener, ALBTargetGroup, TargetGroupAttachment, ALB To EC2 To RDS And Redis Topology, AWS CloudFormation Deployment Guide, Stack Parameters Reference, Existing VPC Prerequisite, Infra Troubleshooting Runbook (+1 more)

### Community 155 - "ScimUserPatchReaderTests"
Cohesion: 0.12
Nodes (7): ScimName, MergeName, ScimUserPatchReaderTests, graphify-runner, Kiro: graphify enforcement, The hooks, When the refresh fails

### Community 156 - "Baseline Full Extensive Test Levels"
Cohesion: 0.25
Nodes (8): ArchUnit Baseline Gate, Always ./mvnw Never Bare mvn, Trace Before You Delete, Frontend Local Semgrep Ruleset, Baseline Full Extensive Test Levels, The mutate Flag Replaces the Array, It Does Not Narrow It, Image Tag Plus Digest Pinning, Toolchain Pin Table

### Community 157 - "IndexedSessions"
Cohesion: 0.28
Nodes (5): Override, AccountSessionsAdapterTests, IndexedSessions, Override, org.springframework.session.MapSession

### Community 158 - "org.springframework.data.jpa.repository.JpaRepository"
Cohesion: 0.17
Nodes (9): UserCounterJpaRepository, ScimGroupEntity, ScimGroupJpaRepository, collection, lockmodetype, org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.jpa.repository.Lock, org.springframework.data.jpa.repository.Modifying (+1 more)

### Community 159 - "ScimPageRequest"
Cohesion: 0.24
Nodes (3): ScimGroupListing, ScimUserListing, ScimPageRequest

### Community 160 - "InMemoryScimExternalIdRepository"
Cohesion: 0.47
Nodes (3): Alias, InMemoryScimExternalIdRepository, Override

### Community 161 - "Spring Session In Redis"
Cohesion: 0.33
Nodes (7): Backend Architecture Boundaries, Redis Compose Service, Spring Session In Redis, Flag ADR Conflicts Explicitly, docs/adr Decision Records, RedisCluster ElastiCache, RedisSubnetGroup

### Community 164 - "AuditTrailServiceTests.java"
Cohesion: 0.11
Nodes (18): AuditEventRepository, AuditGroupAttribute, DISPLAY_NAME, MEMBERS, AuditRequest, AuditRequestContext, AuditScimRefusal, INVALID_VALUE (+10 more)

### Community 170 - "HttpAuditRequestContextTests.java"
Cohesion: 0.22
Nodes (7): HttpAuditRequestContext, Override, HttpAuditRequestContextTests, handlermapping, org.springframework.mock.web.MockHttpServletRequest, requestcontextholder, servletrequestattributes

### Community 171 - "ScimUserPatchReader"
Cohesion: 0.19
Nodes (9): Op, ADD, REMOVE, REPLACE, Path, ScimUserPatchReader, SCIM target model, Supported User profile (+1 more)

### Community 174 - "ScimEmailFilter"
Cohesion: 0.20
Nodes (6): Condition, ScimEmailFilter, ScimEmailPart, PRIMARY, TYPE, VALUE

### Community 178 - "ScimExceptionHandler.java"
Cohesion: 0.16
Nodes (4): DuplicateDisplayNameException, PasswordHistoryPolicy, PasswordReusedException, UnknownGroupMemberException

## Ambiguous Edges - Review These
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **396 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `verify.sh script` (+391 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 700 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **55 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `count()` connect `RFC requirements and implications` to `auth-context-value.ts`, `ScimUserPatchReader`?**
  _High betweenness centrality (0.095) - this node is a cross-community bridge._
- **Why does `SCIM target model` connect `ScimUserPatchReader` to `RFC requirements and implications`, `route-guards.tsx`, `.handle`?**
  _High betweenness centrality (0.084) - this node is a cross-community bridge._
- **Why does `Backend API Contract (OpenAPI 3.1)` connect `Backend API Contract (OpenAPI 3.1)` to `UserCounter`?**
  _High betweenness centrality (0.068) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _396 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AuthControllerTests.java` be split into smaller, more focused modules?**
  _Cohesion score 0.06666666666666667 - nodes in this community are weakly interconnected._