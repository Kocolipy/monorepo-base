# Graph Report - monorepo-base-admin-user-list  (2026-09-24)

## Corpus Check
- 134 files · ~55,367 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: (none) 10, .example 1, .properties 1)

## Summary
- 1181 nodes · 2580 edges · 83 communities (64 shown, 19 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 334 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `cc5cbeaf`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- UserCounterService
- SecurityConfigTests.java
- showcase.tsx
- auth.helpers.ts
- api.ts
- Workflow
- Backend API Contract (OpenAPI 3.1)
- SecurityConfig.java
- devDependencies
- stryker.config.json
- compilerOptions
- Frontend Local Semgrep Ruleset
- package.json
- scripts
- ArchitectureTest.java
- compilerOptions
- components.json
- lib.sh
- AGENTS.md
- deploy.sh
- Kiro: graphify enforcement
- mvnw
- AWS CloudFormation Deployment Guide
- .servesSpaShell
- EC2Instance
- DBInstance RDS PostgreSQL
- App.tsx
- infra/ Is Deployment Material Not An App
- showcase.test.tsx
- tsconfig.test.json
- auth-context-value.ts
- Domain Documentation Guide
- GitHub Issue Tracker Guide
- Baseline Full Extensive Test Levels
- PIT Scoped To Touched Tests
- Graphify Runner Agent
- BackendApplication.java
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
- org.junit.jupiter.api.Test
- bootstrap.sh
- package.sh
- Test Static index.html Stub
- com.example:backend
- Account
- AuthController.java
- AccountServiceTests
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh
- LoginLockoutTests.java
- AccountRole
- AuthControllerTests.java
- AccountPersistenceAdapter
- SessionController
- UserCounter
- UserCounterEntity
- InMemoryAccountRepository.java
- SecurityConfig
- Frontend Architecture Doc
- MutableClock
- AccountEntity
- UserCounterPersistenceAdapter
- org.junit.jupiter.api.BeforeEach
- dependencies
- eslint.config.js
- vite.config.ts
- engines
- overrides
- @testing-library/jest-dom
- authentication

## God Nodes (most connected - your core abstractions)
1. `Account` - 47 edges
2. `AccountRole` - 27 edges
3. `AccountAdministrationServiceTests` - 27 edges
4. `SecurityConfigTests` - 24 edges
5. `AccountTests` - 23 edges
6. `AuthControllerTests` - 22 edges
7. `AuthController` - 21 edges
8. `AccountRepository` - 21 edges
9. `InMemoryAccountRepository` - 20 edges
10. `AccountSummary` - 19 edges

## Surprising Connections (you probably didn't know these)
- `getCurrentUser operation` --shares_data_with--> `getCurrentUser()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `Sessions` --references--> `resolveSessionRoute()`  [INFERRED]
  CONTEXT.md → frontend/src/auth/session-route.ts
- `Frontend Local Semgrep Ruleset` --semantically_similar_to--> `Backend Semgrep Baseline Gate`  [INFERRED] [semantically similar]
  frontend/AGENTS.md → backend/AGENTS.md
- `login operation` --shares_data_with--> `login()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `logout operation` --shares_data_with--> `logout()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Public Request Path Through The Stack** — infra_infrastructure_alblistener, infra_infrastructure_albtargetgroup, infra_infrastructure_ec2instance [EXTRACTED 1.00]
- **Mutation Testing as the Load-Bearing-Test Doctrine on Both Sides** — frontend_docs_testing_guide_stryker_mutate_trap, frontend_docs_testing_guide_assert_exactly [INFERRED 0.85]
- **Local Compose Versus Cloud Datastores** — backend_compose_postgres_service, backend_compose_redis_service, infra_infrastructure_dbinstance, infra_infrastructure_rediscluster [INFERRED 0.85]
- **Published Credential Exposure Surface** — agents_published_credentials_warning, backend_readme_dev_default_credentials, infra_infrastructure_app_credential_parameters, backend_semgrep_rules_service_security_be_hardcoded_credential_literal [INFERRED 0.85]

## Communities (83 total, 19 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 1 - "UserCounterService"
Cohesion: 0.09
Nodes (17): getCount operation, incrementCount operation, resetCount operation, UserCounterService, CountResponse, UserCounterController, UserCounterRepository, ProbeController (+9 more)

### Community 2 - "SecurityConfigTests.java"
Cohesion: 0.06
Nodes (40): autowired, Override, SpaErrorViewResolver, AdminUserEndpointTests, BackendApplicationTests, SpaFrontendTests, classmode, containsstring (+32 more)

### Community 3 - "showcase.tsx"
Cohesion: 0.18
Nodes (20): components/ui Is a Package Placeholder, useAuth(), Button(), ButtonProps, buttonVariants, Card(), CardContent(), CardDescription() (+12 more)

### Community 4 - "auth.helpers.ts"
Cohesion: 0.11
Nodes (21): Colors Come From index.css Tokens, Vite Full-Reloads on Any Watched HTML Write, Frontend Testing Guide, Arch Suite Reads Sources Through node:fs, toHaveTextContent Is a Substring Match, Coverage Excludes Are Listed, Not Globbed, E2E Flakiness Rules, Plant the Violation to Prove a Rule Fails (+13 more)

### Community 5 - "api.ts"
Cohesion: 0.14
Nodes (18): logout operation, decodeUser(), getCurrentUser(), login(), logout(), apiFetchMock, TEST_LOGIN, SessionRequest (+10 more)

### Community 6 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 7 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.13
Nodes (15): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, getCurrentUser operation, getHealth operation, getSession operation, login operation, JSESSIONID Session Cookie Security Scheme, updateSession operation (+7 more)

### Community 8 - "SecurityConfig.java"
Cohesion: 0.15
Nodes (17): authenticationentrypoint, AuthController, bcryptpasswordencoder, changesessionidauthenticationstrategy, cookiecsrftokenrepository, daoauthenticationprovider, httpmethod, httpsessionsecuritycontextrepository (+9 more)

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

### Community 15 - "ArchitectureTest.java"
Cohesion: 0.10
Nodes (20): ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, configuration, controller, entity, entitymanager (+12 more)

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "components.json"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

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

### Community 24 - ".servesSpaShell"
Cohesion: 0.14
Nodes (11): SpaRoutes, ReservedServerPaths, SpaRoutesTests, SpaShell, Accounts and roles, CONTEXT, Request paths, Sessions (+3 more)

### Community 25 - "EC2Instance"
Cohesion: 0.14
Nodes (16): No Parent-Relative Paths From An App, SPA Build Contract, with-frontend Maven Profile, Backend Serves SPA And Forwards Routes, Claim: No Router Data Layer Or Auth, No .env Required In Frontend, Frontend Technology Stack, EC2Instance (+8 more)

### Community 26 - "DBInstance RDS PostgreSQL"
Cohesion: 0.29
Nodes (8): docs/openapi.yaml API Contract, Postgres Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, DBInstance RDS PostgreSQL, DBSubnetGroup

### Community 27 - "App.tsx"
Cohesion: 0.19
Nodes (13): AuthRole, AuthProvider(), AuthStatus, GuestRoute(), ProtectedRoute(), SessionRoute(), DEFAULT_DESTINATION, LOGIN_PATH (+5 more)

### Community 28 - "infra/ Is Deployment Material Not An App"
Cohesion: 0.29
Nodes (7): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, CONTEXT.md Domain Glossary, gh CLI Conventions, GitHub Issues As Issue Tracker, PRs As Request Surface Flag

### Community 29 - "showcase.test.tsx"
Cohesion: 0.14
Nodes (7): App(), apiFetchMock, auth, ref_node_url, @testing-library/react, @testing-library/user-event, vitest

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "auth-context-value.ts"
Cohesion: 0.25
Nodes (11): AuthUser, AuthContext, AuthContextState, AuthContextValue, useAuthState(), apiFetchMock, request(), state (+3 more)

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

### Community 37 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

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

### Community 50 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (11): LoginRequest, UserResponse, LoginAttemptServiceTests, AccountSeedConfigTests, SecurityConfigTests, AuthControllerTests, LoginLockoutTests, AccountTests (+3 more)

### Community 56 - "Account"
Cohesion: 0.08
Nodes (11): AccountAdministrationService, AccountSummary, AdminUserController, AdminUserResponse, Account, AccountAdministrationServiceTests, AdminUserControllerTests, Override (+3 more)

### Community 57 - "AuthController.java"
Cohesion: 0.13
Nodes (14): UnknownAccountException, UnsafeAccountChangeException, cookievalue, httpstatus, jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse, notblank, org.springframework.web.bind.annotation.DeleteMapping (+6 more)

### Community 58 - "AccountServiceTests"
Cohesion: 0.16
Nodes (8): accountseed, AccountSeed, AccountService, Override, AccountSeedConfig, AccountServiceTests, org.springframework.boot.ApplicationRunner, org.springframework.context.annotation.Configuration

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

### Community 62 - "LoginLockoutTests.java"
Cohesion: 0.26
Nodes (9): assertthat, assertthatthrownby, LoginAttemptService, LockoutPolicy, InMemoryAccountRepository, duration, instant, lockedexception (+1 more)

### Community 63 - "AccountRole"
Cohesion: 0.16
Nodes (11): AccountRepository, AccountRole, ADMIN, USER, org.springframework.security.core.userdetails.UserDetails, org.springframework.security.core.userdetails.UserDetailsService, org.springframework.security.crypto.password.PasswordEncoder, org.springframework.stereotype.Service (+3 more)

### Community 64 - "AuthControllerTests.java"
Cohesion: 0.12
Nodes (13): assertthatcode, assertthatnoexception, authenticationmanager, badcredentialsexception, chronounit, defaultcookieserializer, inmemoryuserdetailsmanager, mediatype (+5 more)

### Community 65 - "AccountPersistenceAdapter"
Cohesion: 0.23
Nodes (6): AccountJpaRepository, AccountPersistenceAdapter, Override, org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.jpa.repository.Modifying, org.springframework.data.jpa.repository.Query

### Community 66 - "SessionController"
Cohesion: 0.27
Nodes (7): deleteSession operation, SessionController, SessionResponse, UpdateSessionRequest, SessionControllerTests, jakarta.servlet.http.HttpSession, org.springframework.web.bind.annotation.PutMapping

### Community 68 - "UserCounterEntity"
Cohesion: 0.21
Nodes (7): UserCounterEntity, column, enumerated, enumtype, id, jakarta.persistence.Entity, jakarta.persistence.Table

### Community 69 - "InMemoryAccountRepository.java"
Cohesion: 0.19
Nodes (7): comparator, hashmap, list, map, objects, optional, org.springframework.stereotype.Repository

### Community 70 - "SecurityConfig"
Cohesion: 0.39
Nodes (3): LoginLockoutConfig, SecurityConfig, org.springframework.context.annotation.Bean

### Community 71 - "Frontend Architecture Doc"
Cohesion: 0.18
Nodes (11): Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, One-Way Import Direction Through the Layers, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline, Vitest Deliberately Omits the Tailwind Vite Plugin, Frontend SPA Entry HTML (+3 more)

### Community 72 - "MutableClock"
Cohesion: 0.20
Nodes (6): Override, MutableClock, clock, value, zoneid, zoneoffset

### Community 74 - "UserCounterPersistenceAdapter"
Cohesion: 0.25
Nodes (6): UserCounterJpaRepository, Override, UserCounterPersistenceAdapter, lockmodetype, org.springframework.data.jpa.repository.Lock, param

### Community 75 - "org.junit.jupiter.api.BeforeEach"
Cohesion: 0.24
Nodes (3): Override, PrefixPasswordEncoder, org.junit.jupiter.api.BeforeEach

### Community 76 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 77 - "eslint.config.js"
Cohesion: 0.33
Nodes (5): @eslint/js, eslint-plugin-react-hooks, eslint-plugin-react-refresh, globals, typescript-eslint

### Community 78 - "vite.config.ts"
Cohesion: 0.40
Nodes (4): @tailwindcss/vite, vite, vite-plugin-compression2, @vitejs/plugin-react-swc

### Community 79 - "engines"
Cohesion: 0.67
Nodes (3): engines, node, npm

## Ambiguous Edges - Review These
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **245 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `USER` (+240 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 376 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **19 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Backend API Contract (OpenAPI 3.1)` connect `Backend API Contract (OpenAPI 3.1)` to `UserCounterService`, `SessionController`, `api.ts`?**
  _High betweenness centrality (0.267) - this node is a cross-community bridge._
- **Why does `login operation` connect `Backend API Contract (OpenAPI 3.1)` to `auth.helpers.ts`, `api.ts`?**
  _High betweenness centrality (0.172) - this node is a cross-community bridge._
- **Why does `Shared Playwright storageState for Auth` connect `auth.helpers.ts` to `Backend API Contract (OpenAPI 3.1)`?**
  _High betweenness centrality (0.129) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _245 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `UserCounterService` be split into smaller, more focused modules?**
  _Cohesion score 0.09082125603864734 - nodes in this community are weakly interconnected._