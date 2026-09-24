# Graph Report - monorepo-base  (2026-09-24)

## Corpus Check
- 97 files · ~38,578 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: (none) 9, .example 1, .properties 1)

## Summary
- 831 nodes · 1386 edges · 62 communities (48 shown, 14 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 115 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `974d594a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AuthController.java
- showcase.tsx
- org.junit.jupiter.api.Test
- UserCounter
- Frontend Architecture Doc
- ArchitectureTest.java
- Workflow
- Backend API Contract (OpenAPI 3.1)
- .login
- devDependencies
- stryker.config.json
- compilerOptions
- Frontend Local Semgrep Ruleset
- package.json
- scripts
- components.json
- compilerOptions
- apiFetch
- lib.sh
- AGENTS.md
- deploy.sh
- SecurityConfig.java
- mvnw
- EC2Instance
- Backend Semgrep Baseline Gate
- Frontend Technology Stack
- DBInstance RDS PostgreSQL
- Spring Session In Redis
- Monorepo Layout Contract
- dependencies
- tsconfig.test.json
- vite.config.ts
- Domain Documentation Guide
- GitHub Issue Tracker Guide
- eslint.config.js
- PIT JVM Adapter
- Graphify Runner Agent
- BackendApplication.java
- cleanup.sh
- get-vpc-info.sh
- App And DB Credential Parameters
- Graphify Runner
- Frontend Project Structure
- engines
- prettier.config.mjs
- dev.sh
- integration-test.sh
- semgrep.sh
- CLAUDE.md
- @testing-library/jest-dom
- bootstrap.sh
- package.sh
- Test Static index.html Stub
- com.example:backend
- AuthControllerTests.java
- AuthController
- SecurityConfig
- dev-stop.sh
- graphify-guard.sh
- graphify-refresh.sh

## God Nodes (most connected - your core abstractions)
1. `scripts` - 19 edges
2. `compilerOptions` - 19 edges
3. `SecurityConfigTests` - 18 edges
4. `AuthController` - 17 edges
5. `UserCounter` - 17 edges
6. `AuthControllerTests` - 17 edges
7. `SpaFrontendTests` - 15 edges
8. `compilerOptions` - 15 edges
9. `UserCounterService` - 13 edges
10. `LoginRequest` - 12 edges

## Surprising Connections (you probably didn't know these)
- `getCurrentUser operation` --shares_data_with--> `getCurrentUser()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `login operation` --shares_data_with--> `login()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `logout operation` --shares_data_with--> `logout()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `Frontend Local Semgrep Ruleset` --semantically_similar_to--> `Backend Semgrep Baseline Gate`  [INFERRED] [semantically similar]
  frontend/AGENTS.md → backend/AGENTS.md
- `Baseline Full Extensive Test Levels` --semantically_similar_to--> `ArchUnit Baseline Gate`  [INFERRED] [semantically similar]
  frontend/AGENTS.md → backend/AGENTS.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Public Request Path Through The Stack** — infra_infrastructure_alblistener, infra_infrastructure_albtargetgroup, infra_infrastructure_ec2instance [EXTRACTED 1.00]
- **Mutation Testing as the Load-Bearing-Test Doctrine on Both Sides** — frontend_docs_testing_guide_stryker_mutate_trap, frontend_docs_testing_guide_assert_exactly [INFERRED 0.85]
- **Local Compose Versus Cloud Datastores** — backend_compose_postgres_service, backend_compose_redis_service, infra_infrastructure_dbinstance, infra_infrastructure_rediscluster [INFERRED 0.85]
- **Published Credential Exposure Surface** — agents_published_credentials_warning, backend_readme_dev_default_credentials, infra_infrastructure_app_credential_parameters, backend_semgrep_rules_service_security_be_hardcoded_credential_literal [INFERRED 0.85]

## Communities (62 total, 14 thin omitted)

### Community 0 - "AuthController.java"
Cohesion: 0.14
Nodes (16): deleteSession operation, cookievalue, httpstatus, notblank, org.springframework.security.core.AuthenticationException, org.springframework.web.bind.annotation.DeleteMapping, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.PostMapping (+8 more)

### Community 1 - "showcase.tsx"
Cohesion: 0.09
Nodes (33): One-Way Import Direction Through the Layers, components/ui Is a Package Placeholder, Coverage Excludes Are Listed, Not Globbed, Frontend SPA Entry HTML, App(), AuthUser, AuthProvider(), AuthContext (+25 more)

### Community 2 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (42): assertthat, assertthatcode, autowired, Override, SpaErrorViewResolver, SecurityConfigTests, BackendApplicationTests, SpaFrontendTests (+34 more)

### Community 3 - "UserCounter"
Cohesion: 0.06
Nodes (24): getCount operation, incrementCount operation, resetCount operation, UserCounterService, CountResponse, UserCounterController, UserCounter, UserCounterRepository (+16 more)

### Community 4 - "Frontend Architecture Doc"
Cohesion: 0.10
Nodes (21): login operation, Colors Come From index.css Tokens, Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, Vite Full-Reloads on Any Watched HTML Write, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline (+13 more)

### Community 5 - "ArchitectureTest.java"
Cohesion: 0.07
Nodes (29): UserCounterEntity, UserCounterJpaRepository, UserCounterPersistenceAdapter, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, controller (+21 more)

### Community 6 - "Workflow"
Cohesion: 0.07
Nodes (26): Mutation Report Template, Mutation Tool Adapters, Mutation Testing Skill, Mutation Finding Severity, Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python (+18 more)

### Community 7 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.14
Nodes (16): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, getHealth operation, getSession operation, logout operation, JSESSIONID Session Cookie Security Scheme, updateSession operation, SessionController (+8 more)

### Community 8 - ".login"
Cohesion: 0.26
Nodes (4): LoginRequest, AuthControllerTests, jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse

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
Cohesion: 0.09
Nodes (22): name, packageManager, private, type, version, class-variance-authority, clsx, dependency-cruiser (+14 more)

### Community 14 - "scripts"
Cohesion: 0.11
Nodes (19): scripts, analyze, build, dev, format, format:check, lint, preview (+11 more)

### Community 15 - "components.json"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

### Community 16 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "apiFetch"
Cohesion: 0.09
Nodes (29): getCurrentUser(), login(), logout(), apiFetch(), ApiRequestInit, csrfToken(), isUnsafe(), SAFE_METHODS (+21 more)

### Community 18 - "lib.sh"
Cohesion: 0.24
Nodes (14): die(), load_backend_env(), log(), pinned_version(), port_holder(), require_cmd(), require_docker(), require_maven() (+6 more)

### Community 19 - "AGENTS.md"
Cohesion: 0.12
Nodes (14): Agent, Agent documentation, Build and validation, Environment, Frontend/backend integration, graphify, Ignore rules, Layout (+6 more)

### Community 20 - "deploy.sh"
Cohesion: 0.42
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 21 - "SecurityConfig.java"
Cohesion: 0.14
Nodes (13): authenticationentrypoint, bcryptpasswordencoder, changesessionidauthenticationstrategy, cookiecsrftokenrepository, daoauthenticationprovider, httpmethod, inmemoryuserdetailsmanager, org.springframework.security.config.annotation.web.builders.HttpSecurity (+5 more)

### Community 22 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 23 - "EC2Instance"
Cohesion: 0.17
Nodes (16): ALBListener, ALBTargetGroup, EC2Instance, EC2InstanceProfile, EC2KeyPair, EC2Role, TargetGroupAttachment, Infra Quickstart Flow (+8 more)

### Community 24 - "Backend Semgrep Baseline Gate"
Cohesion: 0.17
Nodes (12): Long-Gate Sentinel And Log Pattern, Security-Sensitive Change Policy, Backend Semgrep Baseline Gate, be-authorize-any-request-permit-all, be-cors-wildcard-origin, be-csrf-disabled, be-jpql-string-concatenation, be-session-fixation-disabled (+4 more)

### Community 25 - "Frontend Technology Stack"
Cohesion: 0.22
Nodes (9): No Parent-Relative Paths From An App, SPA Build Contract, with-frontend Maven Profile, Backend Serves SPA And Forwards Routes, Claim: No Router Data Layer Or Auth, No .env Required In Frontend, Frontend Technology Stack, npm ci Not npm install (+1 more)

### Community 26 - "DBInstance RDS PostgreSQL"
Cohesion: 0.29
Nodes (8): docs/openapi.yaml API Contract, Postgres Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, DBInstance RDS PostgreSQL, DBSubnetGroup

### Community 27 - "Spring Session In Redis"
Cohesion: 0.29
Nodes (8): ArchUnit Baseline Gate, Always ./mvnw Never Bare mvn, Redis Compose Service, Spring Session In Redis, RedisCluster ElastiCache, RedisSubnetGroup, Image Tag Plus Digest Pinning, Toolchain Pin Table

### Community 28 - "Monorepo Layout Contract"
Cohesion: 0.20
Nodes (10): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, Backend Architecture Boundaries, Flag ADR Conflicts Explicitly, docs/adr Decision Records, CONTEXT.md Domain Glossary, gh CLI Conventions (+2 more)

### Community 29 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "vite.config.ts"
Cohesion: 0.33
Nodes (5): ref_node_url, @tailwindcss/vite, vite, vite-plugin-compression2, @vitejs/plugin-react-swc

### Community 32 - "Domain Documentation Guide"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 33 - "GitHub Issue Tracker Guide"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 34 - "eslint.config.js"
Cohesion: 0.33
Nodes (5): @eslint/js, eslint-plugin-react-hooks, eslint-plugin-react-refresh, globals, typescript-eslint

### Community 35 - "PIT JVM Adapter"
Cohesion: 0.40
Nodes (5): PIT JVM Adapter, StrykerJS Adapter, Changed-File Scoping Rule, Mutation Tool Ecosystem Detection, PIT Scoped To Touched Tests

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

### Community 40 - "App And DB Credential Parameters"
Cohesion: 0.67
Nodes (4): Published Default Credentials Warning, Development Default Credentials, be-hardcoded-credential-literal, App And DB Credential Parameters

### Community 41 - "Graphify Runner"
Cohesion: 0.50
Nodes (3): Graphify Runner, Reporting, Steps

### Community 42 - "Frontend Project Structure"
Cohesion: 0.67
Nodes (3): shadcn Placeholder Primitives, src/ Dependency Direction Rules, Frontend Project Structure

### Community 43 - "engines"
Cohesion: 0.67
Nodes (3): engines, node, npm

### Community 56 - "AuthControllerTests.java"
Cohesion: 0.17
Nodes (10): assertthatnoexception, assertthatthrownby, authentication, authenticationmanager, badcredentialsexception, defaultcookieserializer, httpsessionsecuritycontextrepository, mockhttpservletresponse (+2 more)

### Community 57 - "AuthController"
Cohesion: 0.27
Nodes (8): getCurrentUser operation, AuthController, UserResponse, org.springframework.security.authentication.AuthenticationManager, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy, org.springframework.security.web.context.SecurityContextRepository, org.springframework.security.web.csrf.CsrfTokenRepository, org.springframework.session.web.http.CookieSerializer

### Community 58 - "SecurityConfig"
Cohesion: 0.44
Nodes (4): SecurityConfig, org.springframework.context.annotation.Bean, org.springframework.security.core.userdetails.UserDetailsService, org.springframework.security.crypto.password.PasswordEncoder

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

## Ambiguous Edges - Review These
- `login operation` → `Shared Playwright storageState for Auth`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to

## Knowledge Gaps
- **236 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `$schema` (+231 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 342 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **14 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `login operation` and `Shared Playwright storageState for Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `login operation` connect `Frontend Architecture Doc` to `.login`, `apiFetch`, `Backend API Contract (OpenAPI 3.1)`?**
  _High betweenness centrality (0.218) - this node is a cross-community bridge._
- **Why does `Frontend Testing Guide` connect `Frontend Architecture Doc` to `Backend Semgrep Baseline Gate`, `showcase.tsx`?**
  _High betweenness centrality (0.207) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _236 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AuthController.java` be split into smaller, more focused modules?**
  _Cohesion score 0.14210526315789473 - nodes in this community are weakly interconnected._
- **Should `showcase.tsx` be split into smaller, more focused modules?**
  _Cohesion score 0.09019607843137255 - nodes in this community are weakly interconnected._