# Graph Report - monorepo-base  (2026-09-24)

## Corpus Check
- 34 files · ~33,956 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 819 nodes · 1371 edges · 56 communities (44 shown, 12 thin omitted)
- Extraction: 90% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 129 edges (avg confidence: 0.84)
- Token cost: 148,000 input · 11,000 output

## Community Hubs (Navigation)
- Auth And Security Config
- SPA Pages And Auth Context
- SPA Serving And Error Views
- User Counter Application Layer
- Frontend Architecture And Testing Docs
- User Counter Persistence
- Monorepo Agent Rules
- Session API And OpenAPI Contract
- Login Logout Controller Tests
- Frontend Dev Dependencies
- Stryker Mutation Config
- App TypeScript Config
- CSRF Frontend Contract
- Frontend Package Manifest
- npm Script Definitions
- ArchUnit Architecture Rules
- Node TypeScript Config
- Architecture Test Helpers
- Shared Shell Script Library
- Root Agent Instructions
- Infra Deploy Script
- EC2 Instance And SSH Access
- Maven Wrapper Script
- CloudFormation Deployment Guide
- Backend Semgrep Security Gate
- Frontend Backend SPA Contract
- PostgreSQL Data And API Surface
- Toolchain Pins And Baseline Gates
- Redis Sessions And ADRs
- Runtime Frontend Dependencies
- Test TypeScript Config
- Vite And Vitest Build Config
- Domain Documentation Guide
- GitHub Issue Tracker Guide
- ESLint Flat Config
- Mutation Tool Adapters
- Graphify Runner Agent
- Spring Boot Entry Point
- Infra Cleanup Script
- VPC Info Script
- Credential Exposure Surface
- Graphify Runner Doc
- Frontend Source Layering
- Node Engine Pins
- Prettier Config
- Local Dev Orchestration Script
- Integration Test Script
- Semgrep Wrapper Script
- CLAUDE.md Graphify Override
- Vitest Test Setup
- Bootstrap Script
- Package Release Script
- Static index.html Stub
- Backend Maven Coordinate

## God Nodes (most connected - your core abstractions)
1. `compilerOptions` - 19 edges
2. `scripts` - 19 edges
3. `SecurityConfigTests` - 18 edges
4. `AuthControllerTests` - 17 edges
5. `UserCounter` - 17 edges
6. `AuthController` - 17 edges
7. `SpaFrontendTests` - 15 edges
8. `compilerOptions` - 15 edges
9. `UserCounterService` - 13 edges
10. `SecurityConfig` - 12 edges

## Surprising Connections (you probably didn't know these)
- `Frontend Local Semgrep Ruleset` --semantically_similar_to--> `Backend Semgrep Baseline Gate`  [INFERRED] [semantically similar]
  frontend/AGENTS.md → backend/AGENTS.md
- `getCurrentUser operation` --shares_data_with--> `getCurrentUser()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `login operation` --shares_data_with--> `login()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `logout operation` --shares_data_with--> `logout()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `403 Is a Missing Token, Not a Logout` --references--> `request()`  [INFERRED]
  backend/FRONTEND.md → frontend/src/auth/api.ts

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Mutation Testing as the Load-Bearing-Test Doctrine on Both Sides** — frontend_docs_testing_guide_stryker_mutate_trap, frontend_docs_testing_guide_assert_exactly [INFERRED 0.85]
- **CSRF Double-Submit Contract Across Backend, SPA and API Doc** — backend_src_main_java_com_example_backend_auth_config_securityconfig_securityconfig_csrftokenrepository, backend_src_main_java_com_example_backend_auth_authcontroller_authcontroller_issuecsrftoken, backend_frontend_csrf_token_echo, backend_docs_openapi_csrftoken_parameter [INFERRED 0.95]
- **Public Request Path Through The Stack** — infra_infrastructure_alblistener, infra_infrastructure_albtargetgroup, infra_infrastructure_ec2instance [EXTRACTED 1.00]
- **Local Compose Versus Cloud Datastores** — backend_compose_postgres_service, backend_compose_redis_service, infra_infrastructure_dbinstance, infra_infrastructure_rediscluster [INFERRED 0.85]
- **Published Credential Exposure Surface** — agents_published_credentials_warning, backend_readme_dev_default_credentials, infra_infrastructure_app_credential_parameters, backend_semgrep_rules_service_security_be_hardcoded_credential_literal [INFERRED 0.85]

## Communities (56 total, 12 thin omitted)

### Community 0 - "Auth And Security Config"
Cohesion: 0.06
Nodes (46): assertthatnoexception, assertthatthrownby, authentication, authenticationentrypoint, authenticationmanager, AuthController, SecurityConfig, badcredentialsexception (+38 more)

### Community 1 - "SPA Pages And Auth Context"
Cohesion: 0.07
Nodes (42): 15-Minute Session Inactivity Window, One-Way Import Direction Through the Layers, components/ui Is a Package Placeholder, Frontend SPA Entry HTML, App(), AuthUser, AuthProvider(), AuthContext (+34 more)

### Community 2 - "SPA Serving And Error Views"
Cohesion: 0.06
Nodes (39): assertthat, assertthatcode, Override, SpaErrorViewResolver, BackendApplicationTests, SpaFrontendTests, chronounit, classmode (+31 more)

### Community 3 - "User Counter Application Layer"
Cohesion: 0.09
Nodes (20): getCount operation, getCurrentUser operation, resetCount operation, UserResponse, UserCounterService, CountResponse, UserCounterController, UserCounterRepository (+12 more)

### Community 4 - "Frontend Architecture And Testing Docs"
Cohesion: 0.05
Nodes (39): login operation, Colors Come From index.css Tokens, aliases, components, hooks, lib, ui, utils (+31 more)

### Community 5 - "User Counter Persistence"
Cohesion: 0.09
Nodes (16): UserCounter, UserCounterEntity, UserCounterJpaRepository, Override, UserCounterPersistenceAdapter, UserCounterTests, id, jakarta.persistence.Entity (+8 more)

### Community 6 - "Monorepo Agent Rules"
Cohesion: 0.05
Nodes (33): Mutation Report Template, Mutation Tool Adapters, Mutation Testing Skill, Mutation Finding Severity, infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, Fix Recommendation Patterns (+25 more)

### Community 7 - "Session API And OpenAPI Contract"
Cohesion: 0.10
Nodes (22): Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, deleteSession operation, getHealth operation, getSession operation, incrementCount operation, logout operation, JSESSIONID Session Cookie Security Scheme (+14 more)

### Community 8 - "Login Logout Controller Tests"
Cohesion: 0.17
Nodes (7): CSRF Token Rotates on Login and Logout, LoginRequest, AuthControllerTests, SecurityConfigTests, jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse, org.junit.jupiter.api.Test

### Community 9 - "Frontend Dev Dependencies"
Cohesion: 0.07
Nodes (29): devDependencies, dependency-cruiser, eslint, @eslint/js, eslint-plugin-react-hooks, eslint-plugin-react-refresh, fallow, globals (+21 more)

### Community 10 - "Stryker Mutation Config"
Cohesion: 0.07
Nodes (26): cleanTempDir, clearTextReporter, allowColor, maxTestsToLog, _comment_mutate, concurrency, coverageAnalysis, htmlReporter (+18 more)

### Community 11 - "App TypeScript Config"
Cohesion: 0.09
Nodes (21): compilerOptions, allowImportingTsExtensions, baseUrl, isolatedModules, jsx, lib, module, moduleDetection (+13 more)

### Community 12 - "CSRF Frontend Contract"
Cohesion: 0.14
Nodes (19): Front-End Changes Required (FRONTEND.md), 403 Is a Missing Token, Not a Logout, Content-Security-Policy Response Header, Echo XSRF-TOKEN Cookie in X-XSRF-TOKEN Header, Login Is Public but Not CSRF-Exempt, Logout Returns an Expired Session Cookie, Read the CSRF Cookie at Request Time, Frontend Local Semgrep Ruleset (+11 more)

### Community 13 - "Frontend Package Manifest"
Cohesion: 0.10
Nodes (20): name, packageManager, private, type, version, class-variance-authority, dependency-cruiser, eslint (+12 more)

### Community 14 - "npm Script Definitions"
Cohesion: 0.11
Nodes (19): scripts, analyze, build, dev, format, format:check, lint, preview (+11 more)

### Community 15 - "ArchUnit Architecture Rules"
Cohesion: 0.12
Nodes (17): autowired, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, controller, entitymanager, generalcodingrules (+9 more)

### Community 16 - "Node TypeScript Config"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 17 - "Architecture Test Helpers"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 18 - "Shared Shell Script Library"
Cohesion: 0.26
Nodes (12): die(), load_backend_env(), log(), pinned_version(), require_cmd(), require_docker(), require_maven(), require_node() (+4 more)

### Community 19 - "Root Agent Instructions"
Cohesion: 0.15
Nodes (11): Agent, Agent documentation, Build and validation, Environment, Frontend/backend integration, graphify, Layout, Line endings (+3 more)

### Community 20 - "Infra Deploy Script"
Cohesion: 0.41
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 21 - "EC2 Instance And SSH Access"
Cohesion: 0.20
Nodes (11): Ignore rules, Repo-Wide LF Line Endings, EC2Instance, EC2InstanceProfile, EC2KeyPair, EC2Role, Infra Quickstart Flow, Ship JAR From backend/target (+3 more)

### Community 22 - "Maven Wrapper Script"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 23 - "CloudFormation Deployment Guide"
Cohesion: 0.25
Nodes (9): ALBListener, ALBTargetGroup, TargetGroupAttachment, ALB To EC2 To RDS And Redis Topology, AWS CloudFormation Deployment Guide, Stack Parameters Reference, Existing VPC Prerequisite, Infra Troubleshooting Runbook (+1 more)

### Community 24 - "Backend Semgrep Security Gate"
Cohesion: 0.25
Nodes (8): Long-Gate Sentinel And Log Pattern, Security-Sensitive Change Policy, Backend Semgrep Baseline Gate, be-authorize-any-request-permit-all, be-cors-wildcard-origin, be-csrf-disabled, be-jpql-string-concatenation, be-session-fixation-disabled

### Community 25 - "Frontend Backend SPA Contract"
Cohesion: 0.25
Nodes (8): No Parent-Relative Paths From An App, SPA Build Contract, with-frontend Maven Profile, Backend Serves SPA And Forwards Routes, Claim: No Router Data Layer Or Auth, No .env Required In Frontend, Frontend Technology Stack, npm ci Not npm install

### Community 26 - "PostgreSQL Data And API Surface"
Cohesion: 0.29
Nodes (8): docs/openapi.yaml API Contract, Postgres Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, DBInstance RDS PostgreSQL, DBSubnetGroup

### Community 27 - "Toolchain Pins And Baseline Gates"
Cohesion: 0.25
Nodes (8): ArchUnit Baseline Gate, Always ./mvnw Never Bare mvn, Trace Before You Delete, Frontend Local Semgrep Ruleset, Baseline Full Extensive Test Levels, The mutate Flag Replaces the Array, It Does Not Narrow It, Image Tag Plus Digest Pinning, Toolchain Pin Table

### Community 28 - "Redis Sessions And ADRs"
Cohesion: 0.33
Nodes (7): Backend Architecture Boundaries, Redis Compose Service, Spring Session In Redis, Flag ADR Conflicts Explicitly, docs/adr Decision Records, RedisCluster ElastiCache, RedisSubnetGroup

### Community 29 - "Runtime Frontend Dependencies"
Cohesion: 0.29
Nodes (7): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge

### Community 30 - "Test TypeScript Config"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 31 - "Vite And Vitest Build Config"
Cohesion: 0.33
Nodes (5): ref_node_url, @tailwindcss/vite, vite, vite-plugin-compression2, @vitejs/plugin-react-swc

### Community 32 - "Domain Documentation Guide"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 33 - "GitHub Issue Tracker Guide"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 34 - "ESLint Flat Config"
Cohesion: 0.33
Nodes (5): @eslint/js, eslint-plugin-react-hooks, eslint-plugin-react-refresh, globals, typescript-eslint

### Community 35 - "Mutation Tool Adapters"
Cohesion: 0.40
Nodes (5): PIT JVM Adapter, StrykerJS Adapter, Changed-File Scoping Rule, Mutation Tool Ecosystem Detection, PIT Scoped To Touched Tests

### Community 36 - "Graphify Runner Agent"
Cohesion: 0.40
Nodes (5): Graphify Runner Agent, Recorded Interpreter Guard, Graph Shrink Refusal, Graphify Refresh Before Commit, CLAUDE.md Graphify Override

### Community 37 - "Spring Boot Entry Point"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 38 - "Infra Cleanup Script"
Cohesion: 0.70
Nodes (4): print_error(), print_info(), print_warn(), cleanup.sh script

### Community 39 - "VPC Info Script"
Cohesion: 0.70
Nodes (4): print_header(), print_info(), print_warn(), get-vpc-info.sh script

### Community 40 - "Credential Exposure Surface"
Cohesion: 0.67
Nodes (4): Published Default Credentials Warning, Development Default Credentials, be-hardcoded-credential-literal, App And DB Credential Parameters

### Community 41 - "Graphify Runner Doc"
Cohesion: 0.50
Nodes (3): Graphify Runner, Reporting, Steps

### Community 42 - "Frontend Source Layering"
Cohesion: 0.67
Nodes (3): shadcn Placeholder Primitives, src/ Dependency Direction Rules, Frontend Project Structure

### Community 43 - "Node Engine Pins"
Cohesion: 0.67
Nodes (3): engines, node, npm

## Ambiguous Edges - Review These
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to
- `Claim: No Router Data Layer Or Auth` → `Frontend Technology Stack`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to

## Knowledge Gaps
- **229 isolated node(s):** `SourceFile`, `ButtonProps`, `LoginLocationState`, `CountResponse`, `components` (+224 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 332 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Claim: No Router Data Layer Or Auth` and `Frontend Technology Stack`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Frontend Testing Guide` connect `Frontend Architecture And Testing Docs` to `Toolchain Pins And Baseline Gates`?**
  _High betweenness centrality (0.191) - this node is a cross-community bridge._
- **Why does `The mutate Flag Replaces the Array, It Does Not Narrow It` connect `Toolchain Pins And Baseline Gates` to `Frontend Architecture And Testing Docs`?**
  _High betweenness centrality (0.177) - this node is a cross-community bridge._
- **What connects `SourceFile`, `ButtonProps`, `LoginLocationState` to the rest of the system?**
  _229 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Auth And Security Config` be split into smaller, more focused modules?**
  _Cohesion score 0.060285563194077206 - nodes in this community are weakly interconnected._
- **Should `SPA Pages And Auth Context` be split into smaller, more focused modules?**
  _Cohesion score 0.07297726070861978 - nodes in this community are weakly interconnected._