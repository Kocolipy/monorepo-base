# Graph Report - monorepo-base  (2026-09-25)

## Corpus Check
- 149 files · ~75,457 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: (none) 9, .example 1, .properties 1)

## Summary
- 1324 nodes · 2925 edges · 87 communities (70 shown, 17 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 446 edges (avg confidence: 0.83)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `632edd38`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- vitest
- UserCounterRepository
- AdminAccountEndpointTests.java
- showcase.tsx
- auth.helpers.ts
- api.ts
- Workflow
- Backend Runtime Configuration
- AccountTests
- devDependencies
- stryker.config.json
- compilerOptions
- Frontend Local Semgrep Ruleset
- package.json
- scripts
- AccountSummary
- compilerOptions
- IndexedSessions
- lib.sh
- AGENTS.md
- deploy.sh
- Kiro: graphify enforcement
- mvnw
- AWS CloudFormation Deployment Guide
- RFC requirements and implications
- EC2Instance
- DBInstance RDS PostgreSQL
- route-guards.tsx
- infra/ Is Deployment Material Not An App
- auth-context-value.ts
- tsconfig.test.json
- App.tsx
- Domain Documentation Guide
- GitHub Issue Tracker Guide
- Baseline Full Extensive Test Levels
- PIT Scoped To Touched Tests
- Graphify Runner Agent
- AccountServiceTests
- cleanup.sh
- get-vpc-info.sh
- Backend Semgrep Baseline Gate
- Graphify Runner
- Frontend Project Structure
- Spring Session In Redis
- prettier.config.mjs
- dev.sh
- integration-test.sh
- semgrep.sh
- CLAUDE.md
- AccountEntity
- bootstrap.sh
- package.sh
- Test Static index.html Stub
- com.example:backend
- AccountAdministrationServiceTests
- AuthController.java
- .require
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh
- AccountRole
- UserCounterEntity
- accounts.tsx
- AccountPersistenceAdapter
- ArchitectureTest.java
- Account
- SecurityConfig
- UserCounter
- AccountService
- SecurityConfig.java
- components.json
- eslint.config.js
- org.junit.jupiter.api.Test
- org.junit.jupiter.api.BeforeEach
- dependencies
- AccountRepository
- vite.config.ts
- engines
- overrides
- @testing-library/jest-dom
- AuthControllerTests.java
- LoginAttemptServiceTests
- Open candidates
- .findByUsername
- Frontend Architecture Doc

## God Nodes (most connected - your core abstractions)
1. `Account` - 47 edges
2. `AccountAdministrationServiceTests` - 44 edges
3. `AccountRole` - 26 edges
4. `AdminAccountEndpointTests` - 25 edges
5. `AccountSummary` - 24 edges
6. `SecurityConfigTests` - 23 edges
7. `AccountTests` - 22 edges
8. `AccountRepository` - 21 edges
9. `AuthControllerTests` - 21 edges
10. `AccountAdministrationService` - 20 edges

## Surprising Connections (you probably didn't know these)
- `Top recommendation` --references--> `AccountSummary`  [INFERRED]
  architecture-review.md → backend/src/main/java/com/example/backend/auth/application/AccountSummary.java
- `Consequences` --references--> `LoginLockoutTests`  [INFERRED]
  docs/adr/0001-count-login-attempts-on-the-login-path.md → backend/src/test/java/com/example/backend/auth/application/LoginLockoutTests.java
- `Sessions` --references--> `resolveSessionRoute()`  [INFERRED]
  CONTEXT.md → frontend/src/auth/session-route.ts
- `Frontend Local Semgrep Ruleset` --semantically_similar_to--> `Backend Semgrep Baseline Gate`  [INFERRED] [semantically similar]
  frontend/AGENTS.md → backend/AGENTS.md
- `Existing application seams` --references--> `SecurityConfig`  [INFERRED]
  docs/research/scim-v2-account-management-research.md → backend/src/main/java/com/example/backend/auth/config/SecurityConfig.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Public Request Path Through The Stack** — infra_infrastructure_alblistener, infra_infrastructure_albtargetgroup, infra_infrastructure_ec2instance [EXTRACTED 1.00]
- **Mutation Testing as the Load-Bearing-Test Doctrine on Both Sides** — frontend_docs_testing_guide_stryker_mutate_trap, frontend_docs_testing_guide_assert_exactly [INFERRED 0.85]
- **Local Compose Versus Cloud Datastores** — backend_compose_postgres_service, backend_compose_redis_service, infra_infrastructure_dbinstance, infra_infrastructure_rediscluster [INFERRED 0.85]
- **Published Credential Exposure Surface** — agents_published_credentials_warning, backend_readme_dev_default_credentials, infra_infrastructure_app_credential_parameters, backend_semgrep_rules_service_security_be_hardcoded_credential_literal [INFERRED 0.85]

## Communities (87 total, 17 thin omitted)

### Community 0 - "vitest"
Cohesion: 0.12
Nodes (14): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+6 more)

### Community 1 - "UserCounterRepository"
Cohesion: 0.19
Nodes (7): UserCounterRepository, comparator, hashmap, list, objects, optional, org.springframework.stereotype.Repository

### Community 2 - "AdminAccountEndpointTests.java"
Cohesion: 0.05
Nodes (43): ~~11 — Two administrative namespaces, one of them dead~~ ✅ `c74b917` + `fa2da47`, autowired, Override, SpaErrorViewResolver, ProbeController, SecurityConfigTests, SessionRegistryConfiguration, BackendApplicationTests (+35 more)

### Community 3 - "showcase.tsx"
Cohesion: 0.23
Nodes (16): components/ui Is a Package Placeholder, Button(), ButtonProps, buttonVariants, Card(), CardContent(), CardDescription(), CardFooter() (+8 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.11
Nodes (22): Colors Come From index.css Tokens, Vite Full-Reloads on Any Watched HTML Write, Frontend Testing Guide, Arch Suite Reads Sources Through node:fs, toHaveTextContent Is a Substring Match, Coverage Excludes Are Listed, Not Globbed, E2E Flakiness Rules, Plant the Violation to Prove a Rule Fails (+14 more)

### Community 5 - "api.ts"
Cohesion: 0.13
Nodes (24): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, getCurrentUser operation, getSession operation, login operation, logout operation, updateSession operation, decodeUser() (+16 more)

### Community 6 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 7 - "Backend Runtime Configuration"
Cohesion: 0.22
Nodes (9): getHealth operation, JSESSIONID Session Cookie Security Scheme, Backend Runtime Configuration, Actuator Health and Info Exposure, JSESSIONID Cookie Attributes, PostgreSQL Datasource, Redis Session Namespace backend:session, Backend Test Configuration (+1 more)

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
Cohesion: 0.10
Nodes (20): name, packageManager, private, type, version, class-variance-authority, dependency-cruiser, eslint (+12 more)

### Community 14 - "scripts"
Cohesion: 0.11
Nodes (19): scripts, analyze, build, dev, format, format:check, lint, preview (+11 more)

### Community 15 - "AccountSummary"
Cohesion: 0.06
Nodes (27): ~~12 — The account row is still copied for the wire~~ ✅ (uncommitted), getCount operation, incrementCount operation, resetCount operation, AccountSummary, UnknownAccountException, UnsafeAccountChangeException, AdminAccountController (+19 more)

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "IndexedSessions"
Cohesion: 0.07
Nodes (26): arraylist, AfterCommit, AccountSessionsAdapter, Override, AfterCommitAdapter, Override, AccountSessionsAdapterTests, IndexedSessions (+18 more)

### Community 18 - "lib.sh"
Cohesion: 0.24
Nodes (14): die(), load_backend_env(), log(), pinned_version(), port_holder(), require_cmd(), require_docker(), require_maven() (+6 more)

### Community 19 - "AGENTS.md"
Cohesion: 0.12
Nodes (14): Agent, Agent documentation, Build and validation, Environment, Frontend/backend integration, graphify, Ignore rules, Layout (+6 more)

### Community 20 - "deploy.sh"
Cohesion: 0.42
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 21 - "Kiro: graphify enforcement"
Cohesion: 0.40
Nodes (4): graphify-runner, Kiro: graphify enforcement, The hooks, When the refresh fails

### Community 22 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 23 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.25
Nodes (9): ALBListener, ALBTargetGroup, TargetGroupAttachment, ALB To EC2 To RDS And Redis Topology, AWS CloudFormation Deployment Guide, Stack Parameters Reference, Existing VPC Prerequisite, Infra Troubleshooting Runbook (+1 more)

### Community 24 - "RFC requirements and implications"
Cohesion: 0.06
Nodes (32): SpaRoutes, ReservedServerPaths, SpaRoutesTests, SpaShell, Accounts and identity provisioning, CONTEXT, Current account model, Request paths (+24 more)

### Community 25 - "EC2Instance"
Cohesion: 0.14
Nodes (16): No Parent-Relative Paths From An App, SPA Build Contract, with-frontend Maven Profile, Backend Serves SPA And Forwards Routes, Claim: No Router Data Layer Or Auth, No .env Required In Frontend, Frontend Technology Stack, EC2Instance (+8 more)

### Community 26 - "DBInstance RDS PostgreSQL"
Cohesion: 0.29
Nodes (8): docs/openapi.yaml API Contract, Postgres Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, DBInstance RDS PostgreSQL, DBSubnetGroup

### Community 27 - "route-guards.tsx"
Cohesion: 0.25
Nodes (10): AuthRole, AuthStatus, SessionRoute(), DEFAULT_DESTINATION, LOGIN_PATH, resolveSessionRoute(), SessionRequirement, SessionRoute (+2 more)

### Community 28 - "infra/ Is Deployment Material Not An App"
Cohesion: 0.29
Nodes (7): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, CONTEXT.md Domain Glossary, gh CLI Conventions, GitHub Issues As Issue Tracker, PRs As Request Surface Flag

### Community 29 - "auth-context-value.ts"
Cohesion: 0.11
Nodes (14): AuthUser, AuthContext, AuthContextState, AuthContextValue, apiFetchMock, request(), state, wrapper() (+6 more)

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "App.tsx"
Cohesion: 0.18
Nodes (9): Frontend SPA Entry HTML, App(), AuthProvider(), GuestRoute(), ProtectedRoute(), frontend_src_index, container, Login() (+1 more)

### Community 32 - "Domain Documentation Guide"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 33 - "GitHub Issue Tracker Guide"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 34 - "Baseline Full Extensive Test Levels"
Cohesion: 0.25
Nodes (8): ArchUnit Baseline Gate, Always ./mvnw Never Bare mvn, Trace Before You Delete, Frontend Local Semgrep Ruleset, Baseline Full Extensive Test Levels, The mutate Flag Replaces the Array, It Does Not Narrow It, Image Tag Plus Digest Pinning, Toolchain Pin Table

### Community 36 - "Graphify Runner Agent"
Cohesion: 0.40
Nodes (5): Graphify Runner Agent, Recorded Interpreter Guard, Graph Shrink Refusal, Graphify Refresh Before Commit, CLAUDE.md Graphify Override

### Community 37 - "AccountServiceTests"
Cohesion: 0.13
Nodes (8): Override, AccountServiceTests, 1. Count login attempts on the login path, Alternatives considered, Consequences, Context, Decision, Status

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

### Community 43 - "Spring Session In Redis"
Cohesion: 0.33
Nodes (7): Backend Architecture Boundaries, Redis Compose Service, Spring Session In Redis, Flag ADR Conflicts Explicitly, docs/adr Decision Records, RedisCluster ElastiCache, RedisSubnetGroup

### Community 50 - "AccountEntity"
Cohesion: 0.20
Nodes (7): AccountEntity, column, enumerated, enumtype, id, jakarta.persistence.Entity, jakarta.persistence.Table

### Community 56 - "AccountAdministrationServiceTests"
Cohesion: 0.15
Nodes (9): ~~14 — Account status is enforced only at authentication~~ ✅ `d431d67`, 16 — Session revocation is a non-transactional write inside a transaction — Worth exploring (new), AccountAdministrationServiceTests, InMemoryAccountSessions, Override, Override, PendingCommit, Consequences (+1 more)

### Community 57 - "AuthController.java"
Cohesion: 0.09
Nodes (26): deleteSession operation, LoginService, AuthController, LoginRequest, UserResponse, SessionController, SessionResponse, UpdateSessionRequest (+18 more)

### Community 58 - ".require"
Cohesion: 0.19
Nodes (4): LoginLockoutTests, Override, MutableClock, org.springframework.security.core.AuthenticationException

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "AccountRole"
Cohesion: 0.19
Nodes (11): assertthat, assertthatthrownby, authentication, AccountRole, ADMIN, USER, badcredentialsexception, duration (+3 more)

### Community 63 - "UserCounterEntity"
Cohesion: 0.22
Nodes (5): UserCounterEntity, UserCounterJpaRepository, Override, UserCounterPersistenceAdapter, org.springframework.data.jpa.repository.Lock

### Community 64 - "accounts.tsx"
Cohesion: 0.24
Nodes (13): useAuth(), useAuthState(), useSessionRequest(), AccountAction, Accounts(), actionFailure(), decodeAccount(), decodeAccounts() (+5 more)

### Community 65 - "AccountPersistenceAdapter"
Cohesion: 0.19
Nodes (8): AccountJpaRepository, AccountPersistenceAdapter, Override, lockmodetype, org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.jpa.repository.Modifying, org.springframework.data.jpa.repository.Query, param

### Community 66 - "ArchitectureTest.java"
Cohesion: 0.10
Nodes (21): ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, component, configuration, controller, entity (+13 more)

### Community 67 - "Account"
Cohesion: 0.15
Nodes (4): AccountAdministrationService, Account, InMemoryAccountRepository, Override

### Community 68 - "SecurityConfig"
Cohesion: 0.32
Nodes (3): LoginLockoutConfig, SecurityConfig, org.springframework.context.annotation.Bean

### Community 69 - "UserCounter"
Cohesion: 0.16
Nodes (4): UserCounter, UserCounterServiceTests, UserCounterTests, org.springframework.transaction.annotation.Transactional

### Community 70 - "AccountService"
Cohesion: 0.27
Nodes (9): accountseed, 08 — Seeded accounts are identified by position, not by role — Strong (narrowed), AccountSeed, AccountService, AccountSeedConfig, org.springframework.boot.ApplicationRunner, org.springframework.context.annotation.Configuration, org.springframework.security.crypto.password.PasswordEncoder (+1 more)

### Community 71 - "SecurityConfig.java"
Cohesion: 0.17
Nodes (11): authenticationentrypoint, bcryptpasswordencoder, changesessionidauthenticationstrategy, cookiecsrftokenrepository, daoauthenticationprovider, httpmethod, httpsessionsecuritycontextrepository, org.springframework.security.config.annotation.web.builders.HttpSecurity (+3 more)

### Community 72 - "components.json"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

### Community 73 - "eslint.config.js"
Cohesion: 0.33
Nodes (5): @eslint/js, eslint-plugin-react-hooks, eslint-plugin-react-refresh, globals, typescript-eslint

### Community 74 - "org.junit.jupiter.api.Test"
Cohesion: 0.30
Nodes (4): ~~15 — The indexed-session fact is authored five times, one of them wrongly~~ ✅ (uncommitted), AdminAccountEndpointTests, org.junit.jupiter.api.Test, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

### Community 75 - "org.junit.jupiter.api.BeforeEach"
Cohesion: 0.24
Nodes (3): Override, PrefixPasswordEncoder, org.junit.jupiter.api.BeforeEach

### Community 76 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 77 - "AccountRepository"
Cohesion: 0.20
Nodes (9): LoginAttemptService, AccountRepository, AccountSessions, LockoutPolicy, clock, org.springframework.security.core.userdetails.UserDetails, org.springframework.security.core.userdetails.UserDetailsService, org.springframework.stereotype.Service (+1 more)

### Community 78 - "vite.config.ts"
Cohesion: 0.33
Nodes (5): ref_node_url, @tailwindcss/vite, vite, vite-plugin-compression2, @vitejs/plugin-react-swc

### Community 79 - "engines"
Cohesion: 0.67
Nodes (3): engines, node, npm

### Community 82 - "AuthControllerTests.java"
Cohesion: 0.12
Nodes (13): assertthatcode, assertthatnoexception, authenticationmanager, chronounit, content, defaultcookieserializer, inmemoryuserdetailsmanager, mediatype (+5 more)

### Community 83 - "LoginAttemptServiceTests"
Cohesion: 0.22
Nodes (3): ~~09 — The login path must remember to count its own attempts~~ ✅ `d72e3c5`, What shipped, and how well, LoginAttemptServiceTests

### Community 84 - "Open candidates"
Cohesion: 0.15
Nodes (11): 03 — The runtime configuration surface has no module at all — Strong (new load-bearing keys), 04 — The Counter slice is seven modules over a two-field row — Worth exploring (contrast sharpened again), 06 — A template whose identity has no seam — Speculative (unchanged), 10 — The role vocabulary is authored on both sides of the contract — Speculative (unchanged), 13 — Schema evolution has no module — Strong (scope halved, core untouched), Architecture review — deepening opportunities, Open candidates, Top recommendation (+3 more)

### Community 85 - ".findByUsername"
Cohesion: 0.22
Nodes (4): authenticationexception, AccountSeedConfigTests, org.springframework.security.authentication.AuthenticationManager, usernamepasswordauthenticationtoken

### Community 86 - "Frontend Architecture Doc"
Cohesion: 0.33
Nodes (7): Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, One-Way Import Direction Through the Layers, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline, Vitest Deliberately Omits the Tailwind Vite Plugin

## Ambiguous Edges - Review These
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **269 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `USER` (+264 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 418 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **17 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `~~12 — The account row is still copied for the wire~~ ✅ (uncommitted)` connect `AccountSummary` to `AccountAdministrationServiceTests`, `ArchitectureTest.java`, `LoginAttemptServiceTests`?**
  _High betweenness centrality (0.213) - this node is a cross-community bridge._
- **Why does `AdminAccount` connect `AccountSummary` to `accounts.tsx`, `route-guards.tsx`, `auth-context-value.ts`?**
  _High betweenness centrality (0.210) - this node is a cross-community bridge._
- **Why does `Frontend Testing Guide` connect `auth.helpers.ts` to `Baseline Full Extensive Test Levels`?**
  _High betweenness centrality (0.103) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `AccountAdministrationServiceTests` (e.g. with `~~12 — The account row is still copied for the wire~~ ✅ (uncommitted)` and `~~14 — Account status is enforced only at authentication~~ ✅ `d431d67``) actually correct?**
  _`AccountAdministrationServiceTests` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _269 weakly-connected nodes found - possible documentation gaps or missing edges._