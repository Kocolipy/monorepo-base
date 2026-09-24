# Graph Report - monorepo-base  (2026-09-24)

## Corpus Check
- 95 files · ~44,307 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: (none) 9, .example 1, .properties 1)

## Summary
- 812 nodes · 1375 edges · 53 communities (39 shown, 14 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 123 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `46585fef`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- sources.ts
- showcase.tsx
- org.junit.jupiter.api.Test
- UserCounter
- Backend Semgrep Baseline Gate
- ArchitectureTest.java
- EC2Instance
- SessionController.java
- AuthController.java
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
- Kiro: graphify enforcement
- mvnw
- AWS CloudFormation Deployment Guide
- Frontend Technology Stack
- DBInstance RDS PostgreSQL
- Spring Session In Redis
- infra/ Is Deployment Material Not An App
- tsconfig.test.json
- Domain Documentation Guide
- GitHub Issue Tracker Guide
- PIT Scoped To Touched Tests
- Graphify Runner Agent
- BackendApplication.java
- cleanup.sh
- get-vpc-info.sh
- App And DB Credential Parameters
- Graphify Runner
- Frontend Project Structure
- prettier.config.mjs
- dev.sh
- integration-test.sh
- semgrep.sh
- CLAUDE.md
- bootstrap.sh
- package.sh
- Test Static index.html Stub
- com.example:backend
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
9. `apiFetch()` - 14 edges
10. `UserCounterService` - 13 edges

## Surprising Connections (you probably didn't know these)
- `05 — The auth status state machine is read by its callers — Worth exploring` --references--> `useAuth()`  [INFERRED]
  architecture-review.md → frontend/src/auth/auth-context-value.ts
- `02 — Every caller of `apiFetch` re-derives status meaning — Strong` --references--> `apiFetch()`  [INFERRED]
  architecture-review.md → frontend/src/lib/http.ts
- `Top recommendation` --references--> `apiFetch()`  [INFERRED]
  architecture-review.md → frontend/src/lib/http.ts
- `Frontend Local Semgrep Ruleset` --semantically_similar_to--> `Backend Semgrep Baseline Gate`  [INFERRED] [semantically similar]
  frontend/AGENTS.md → backend/AGENTS.md
- `04 — The Counter slice is seven modules over a two-field row — Worth exploring` --references--> `UserCounterRepository`  [INFERRED]
  architecture-review.md → backend/src/main/java/com/example/backend/counter/domain/UserCounterRepository.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Public Request Path Through The Stack** — infra_infrastructure_alblistener, infra_infrastructure_albtargetgroup, infra_infrastructure_ec2instance [EXTRACTED 1.00]
- **Mutation Testing as the Load-Bearing-Test Doctrine on Both Sides** — frontend_docs_testing_guide_stryker_mutate_trap, frontend_docs_testing_guide_assert_exactly [INFERRED 0.85]
- **Local Compose Versus Cloud Datastores** — backend_compose_postgres_service, backend_compose_redis_service, infra_infrastructure_dbinstance, infra_infrastructure_rediscluster [INFERRED 0.85]
- **Published Credential Exposure Surface** — agents_published_credentials_warning, backend_readme_dev_default_credentials, infra_infrastructure_app_credential_parameters, backend_semgrep_rules_service_security_be_hardcoded_credential_literal [INFERRED 0.85]

## Communities (53 total, 14 thin omitted)

### Community 0 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 1 - "showcase.tsx"
Cohesion: 0.07
Nodes (40): Frontend Architecture Doc, Deliberately Absent Concerns and Where They Go, One-Way Import Direction Through the Layers, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline, components/ui Is a Package Placeholder, Vitest Deliberately Omits the Tailwind Vite Plugin (+32 more)

### Community 2 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (42): 01 — "Is this an SPA route?" is answered twice, differently — Strong, assertthat, assertthatcode, autowired, Override, SpaErrorViewResolver, SecurityConfigTests, BackendApplicationTests (+34 more)

### Community 3 - "UserCounter"
Cohesion: 0.06
Nodes (26): 04 — The Counter slice is seven modules over a two-field row — Worth exploring, getCount operation, UserCounterService, UserCounter, UserCounterRepository, UserCounterEntity, UserCounterJpaRepository, Override (+18 more)

### Community 4 - "Backend Semgrep Baseline Gate"
Cohesion: 0.07
Nodes (29): Long-Gate Sentinel And Log Pattern, ArchUnit Baseline Gate, Always ./mvnw Never Bare mvn, Security-Sensitive Change Policy, Backend Semgrep Baseline Gate, be-authorize-any-request-permit-all, be-cors-wildcard-origin, be-csrf-disabled (+21 more)

### Community 5 - "ArchitectureTest.java"
Cohesion: 0.12
Nodes (16): ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, controller, entitymanager, generalcodingrules, importoption (+8 more)

### Community 6 - "EC2Instance"
Cohesion: 0.25
Nodes (8): Backend Serves SPA And Forwards Routes, ALBListener, ALBTargetGroup, EC2Instance, EC2InstanceProfile, EC2Role, TargetGroupAttachment, Infra Troubleshooting Runbook

### Community 7 - "SessionController.java"
Cohesion: 0.07
Nodes (25): deleteSession operation, incrementCount operation, CountResponse, UserCounterController, SessionController, SessionResponse, UpdateSessionRequest, ProbeController (+17 more)

### Community 8 - "AuthController.java"
Cohesion: 0.06
Nodes (46): assertthatnoexception, assertthatthrownby, authentication, authenticationentrypoint, authenticationmanager, AuthController, LoginRequest, UserResponse (+38 more)

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
Cohesion: 0.05
Nodes (41): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge, engines (+33 more)

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
Cohesion: 0.07
Nodes (37): 02 — Every caller of `apiFetch` re-derives status meaning — Strong, 03 — The runtime configuration surface has no module at all — Strong, 05 — The auth status state machine is read by its callers — Worth exploring, 06 — A template whose identity has no seam — Speculative, Architecture review — deepening opportunities, Candidates, Top recommendation, Backend API Contract (OpenAPI 3.1) (+29 more)

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
Cohesion: 0.28
Nodes (9): EC2KeyPair, Infra Quickstart Flow, ALB To EC2 To RDS And Redis Topology, Ship JAR From backend/target, AWS CloudFormation Deployment Guide, Existing VPC Prerequisite, SSH Key Management Via SSM, Deploying To AWS From infra/ (+1 more)

### Community 25 - "Frontend Technology Stack"
Cohesion: 0.29
Nodes (7): No Parent-Relative Paths From An App, SPA Build Contract, with-frontend Maven Profile, Claim: No Router Data Layer Or Auth, No .env Required In Frontend, Frontend Technology Stack, npm ci Not npm install

### Community 26 - "DBInstance RDS PostgreSQL"
Cohesion: 0.29
Nodes (8): docs/openapi.yaml API Contract, Postgres Compose Service, Auth API Endpoints, Count API Endpoints, Per-User Counts In PostgreSQL, Session API Endpoints, DBInstance RDS PostgreSQL, DBSubnetGroup

### Community 27 - "Spring Session In Redis"
Cohesion: 0.33
Nodes (7): Backend Architecture Boundaries, Redis Compose Service, Spring Session In Redis, RedisCluster ElastiCache, RedisSubnetGroup, Image Tag Plus Digest Pinning, Toolchain Pin Table

### Community 28 - "infra/ Is Deployment Material Not An App"
Cohesion: 0.22
Nodes (9): infra/ Is Deployment Material Not An App, infra-up Targets Are Local Docker Deps, Monorepo Layout Contract, Flag ADR Conflicts Explicitly, docs/adr Decision Records, CONTEXT.md Domain Glossary, gh CLI Conventions, GitHub Issues As Issue Tracker (+1 more)

### Community 30 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 32 - "Domain Documentation Guide"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 33 - "GitHub Issue Tracker Guide"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

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
Cohesion: 0.50
Nodes (5): Published Default Credentials Warning, Development Default Credentials, be-hardcoded-credential-literal, App And DB Credential Parameters, Stack Parameters Reference

### Community 41 - "Graphify Runner"
Cohesion: 0.50
Nodes (3): Graphify Runner, Reporting, Steps

### Community 42 - "Frontend Project Structure"
Cohesion: 0.67
Nodes (3): shadcn Placeholder Primitives, src/ Dependency Direction Rules, Frontend Project Structure

### Community 59 - "dev-stop.sh"
Cohesion: 0.60
Nodes (3): pid_in_repo(), dev-stop.sh script, terminate()

## Ambiguous Edges - Review These
- `Frontend Technology Stack` → `Claim: No Router Data Layer Or Auth`  [AMBIGUOUS]
  frontend/AGENTS.md · relation: conceptually_related_to
- `login operation` → `Shared Playwright storageState for Auth`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **221 isolated node(s):** `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend`, `semgrep.sh script`, `$schema` (+216 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 329 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **14 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Frontend Technology Stack` and `Claim: No Router Data Layer Or Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `login operation` and `Shared Playwright storageState for Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `login operation` connect `apiFetch` to `AuthController.java`, `Backend Semgrep Baseline Gate`?**
  _High betweenness centrality (0.173) - this node is a cross-community bridge._
- **Why does `Shared Playwright storageState for Auth` connect `Backend Semgrep Baseline Gate` to `apiFetch`?**
  _High betweenness centrality (0.145) - this node is a cross-community bridge._
- **What connects `graphify-guard.sh script`, `graphify-refresh.sh script`, `com.example:backend` to the rest of the system?**
  _221 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `showcase.tsx` be split into smaller, more focused modules?**
  _Cohesion score 0.07481005260081823 - nodes in this community are weakly interconnected._
- **Should `org.junit.jupiter.api.Test` be split into smaller, more focused modules?**
  _Cohesion score 0.05555555555555555 - nodes in this community are weakly interconnected._