# Graph Report - monorepo-base  (2026-09-24)

## Corpus Check
- 119 files · ~50,914 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 12 file(s) not represented in the graph (top: (none) 9, .example 1, .jsonc 1)

## Summary
- 691 nodes · 1280 edges · 32 communities (27 shown, 5 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 151 edges (avg confidence: 0.85)
- Token cost: 148,000 input · 9,200 output

## Community Hubs (Navigation)
- Spring Security Auth & CSRF Rotation
- SPA Route Fallback & Backend Test Suite
- Frontend App Root, Auth Client & Layering
- Counter Domain & Onion Layering
- AWS CloudFormation Infrastructure
- Session & Counter HTTP Contract
- Frontend Runtime Dependencies
- Frontend Dev Tooling Dependencies
- Stryker Mutation Configuration
- TypeScript App Project Config
- shadcn Component Aliases
- npm Script Catalogue
- ArchUnit Architecture Rules
- TypeScript Node Project Config
- Agent Instruction Protocol
- Frontend Architecture Test Suite
- CloudFormation Deploy Script
- Frontend Layering & Token Rationale
- Browser Security Headers & Semgrep Rules
- Load-Bearing Test Doctrine
- Playwright E2E Auth Fixtures
- TypeScript Test Project Config
- SPA Static Serving Contract
- Backend Baseline Gates
- Stack Cleanup Script
- VPC Discovery Script
- Spring Boot Application Entry
- Prettier Configuration
- Semgrep Gate Script
- Maven Project Coordinates

## God Nodes (most connected - your core abstractions)
1. `scripts` - 19 edges
2. `compilerOptions` - 19 edges
3. `SecurityConfigTests` - 18 edges
4. `AuthController` - 17 edges
5. `UserCounter` - 17 edges
6. `AuthControllerTests` - 17 edges
7. `Frontend AGENTS.md` - 16 edges
8. `SpaFrontendTests` - 15 edges
9. `compilerOptions` - 15 edges
10. `UserCounterService` - 13 edges

## Surprising Connections (you probably didn't know these)
- `403 Is a Missing Token, Not a Logout` --references--> `request()`  [INFERRED]
  backend/FRONTEND.md → frontend/src/auth/api.ts
- `Baseline / Full / Extensive Test Levels` --semantically_similar_to--> `Two Baseline Gates Finish a Change`  [INFERRED] [semantically similar]
  frontend/AGENTS.md → backend/README.md
- `The mutate Flag Replaces the Array, It Does Not Narrow It` --semantically_similar_to--> `A 100% PIT Score Is Provisional`  [INFERRED] [semantically similar]
  frontend/docs/TESTING_GUIDE.md → backend/README.md
- `Docs Claim No Router, Data Layer or Auth While src/auth Exists` --references--> `react-router-dom`  [INFERRED]
  frontend/AGENTS.md → frontend/package.json
- `Echo XSRF-TOKEN Cookie in X-XSRF-TOKEN Header` --references--> `request()`  [INFERRED]
  backend/FRONTEND.md → frontend/src/auth/api.ts

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **CSRF Double-Submit Contract Across Backend, SPA and API Doc** — backend_src_main_java_com_example_backend_auth_config_securityconfig_securityconfig_csrftokenrepository, backend_src_main_java_com_example_backend_auth_authcontroller_authcontroller_issuecsrftoken, backend_frontend_csrf_token_echo, backend_docs_openapi_csrftoken_parameter [INFERRED 0.95]
- **Session Persistence From Local Compose to ElastiCache** — backend_src_main_resources_application_session_namespace, backend_compose_redis, backend_cloudformation_infrastructure_rediscluster, backend_readme_redis_session_persistence [INFERRED 0.95]
- **Mutation Testing as the Load-Bearing-Test Doctrine on Both Sides** — backend_readme_pit_mutation_testing, backend_readme_default_mutators_provisional, frontend_docs_testing_guide_stryker_mutate_trap, frontend_docs_testing_guide_assert_exactly, backend_agents_load_bearing_test [INFERRED 0.85]
- **SPA Authentication Flow the Docs Do Not Mention** — frontend_src_auth_api_login, frontend_src_auth_auth_context_authprovider, frontend_src_auth_protected_route_protectedroute, frontend_src_pages_login_login, backend_docs_openapi_login [INFERRED 0.95]

## Communities (32 total, 5 thin omitted)

### Community 0 - "Spring Security Auth & CSRF Rotation"
Cohesion: 0.06
Nodes (46): assertthatnoexception, assertthatthrownby, authentication, authenticationentrypoint, authenticationmanager, CSRF Token Rotates on Login and Logout, AuthController, LoginRequest (+38 more)

### Community 1 - "SPA Route Fallback & Backend Test Suite"
Cohesion: 0.06
Nodes (41): assertthat, assertthatcode, Override, SpaErrorViewResolver, ProbeController, SecurityConfigTests, BackendApplicationTests, SpaFrontendTests (+33 more)

### Community 2 - "Frontend App Root, Auth Client & Layering"
Cohesion: 0.07
Nodes (42): 15-Minute Session Inactivity Window, One-Way Import Direction Through the Layers, components/ui Is a Package Placeholder, Coverage Excludes Are Listed, Not Globbed, App(), AuthUser, AuthProvider(), AuthContext (+34 more)

### Community 3 - "Counter Domain & Onion Layering"
Cohesion: 0.07
Nodes (22): Onion Layering: Adapters to Application to Domain, UserCounterService, UserCounter, UserCounterRepository, UserCounterEntity, UserCounterJpaRepository, Override, UserCounterPersistenceAdapter (+14 more)

### Community 4 - "AWS CloudFormation Infrastructure"
Cohesion: 0.06
Nodes (50): Session Changes Need a Real Redis Check, CloudFormation Infrastructure Template, ALB Security Group, Application Load Balancer, RDS PostgreSQL Instance, EC2 Application Instance, Managed EC2 Key Pair, EC2 Security Group (+42 more)

### Community 5 - "Session & Counter HTTP Contract"
Cohesion: 0.08
Nodes (24): deleteSession operation, UserResponse, CountResponse, UserCounterController, SessionController, SessionResponse, UpdateSessionRequest, Override (+16 more)

### Community 6 - "Frontend Runtime Dependencies"
Cohesion: 0.06
Nodes (37): dependencies, class-variance-authority, clsx, react, react-dom, react-router-dom, tailwind-merge, name (+29 more)

### Community 7 - "Frontend Dev Tooling Dependencies"
Cohesion: 0.07
Nodes (29): devDependencies, dependency-cruiser, eslint, @eslint/js, eslint-plugin-react-hooks, eslint-plugin-react-refresh, fallow, globals (+21 more)

### Community 8 - "Stryker Mutation Configuration"
Cohesion: 0.07
Nodes (26): cleanTempDir, clearTextReporter, allowColor, maxTestsToLog, _comment_mutate, concurrency, coverageAnalysis, htmlReporter (+18 more)

### Community 9 - "TypeScript App Project Config"
Cohesion: 0.09
Nodes (21): compilerOptions, allowImportingTsExtensions, baseUrl, isolatedModules, jsx, lib, module, moduleDetection (+13 more)

### Community 10 - "shadcn Component Aliases"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

### Community 11 - "npm Script Catalogue"
Cohesion: 0.11
Nodes (19): scripts, analyze, build, dev, format, format:check, lint, preview (+11 more)

### Community 12 - "ArchUnit Architecture Rules"
Cohesion: 0.12
Nodes (17): autowired, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, controller, entitymanager, generalcodingrules (+9 more)

### Community 13 - "TypeScript Node Project Config"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 14 - "Agent Instruction Protocol"
Cohesion: 0.15
Nodes (16): Backend AGENTS.md, Update openapi.yaml With Every Controller Change, Backend Domain Docs Protocol, CONTEXT.md and docs/adr Reading Order, Backend Issue Tracker Protocol, GitHub Issues via the gh CLI, Frontend AGENTS.md, Trace Before You Delete (+8 more)

### Community 15 - "Frontend Architecture Test Suite"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 16 - "CloudFormation Deploy Script"
Cohesion: 0.41
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 17 - "Frontend Layering & Token Rationale"
Cohesion: 0.19
Nodes (12): Colours Come Only From index.css Tokens, Frontend Architecture Doc, Vite Full-Reloads on Any Watched HTML Write, lib/ Is a Leaf, No types/ hooks/ utils/ Catch-All Dirs, Tailwind v4 CSS-First Token Pipeline, Vitest Deliberately Omits the Tailwind Vite Plugin, Frontend Testing Guide (+4 more)

### Community 18 - "Browser Security Headers & Semgrep Rules"
Cohesion: 0.20
Nodes (12): Front-End Changes Required (FRONTEND.md), Content-Security-Policy Response Header, Logout Returns an Expired Session Cookie, Baseline / Full / Extensive Test Levels, The mutate Flag Replaces the Array, It Does Not Narrow It, Frontend README, Frontend Local Semgrep Ruleset, fe-dangerously-set-inner-html (+4 more)

### Community 19 - "Load-Bearing Test Doctrine"
Cohesion: 0.22
Nodes (9): A Test Must Be Load-Bearing, Security-Sensitive Change Policy, Backend README, A 100% PIT Score Is Provisional, Development Default Credentials, PIT Mutation Testing, toHaveTextContent Is a Substring Match, fe-hardcoded-credential (+1 more)

### Community 20 - "Playwright E2E Auth Fixtures"
Cohesion: 0.36
Nodes (4): Shared Playwright storageState for Auth, login(), TEST_CREDENTIALS, @playwright/test

### Community 21 - "TypeScript Test Project Config"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 22 - "SPA Static Serving Contract"
Cohesion: 0.33
Nodes (5): Serve SPA from backend frontend/dist, Test Static index.html Stub, Frontend SPA Entry HTML, Monorepo README, Backend Serves the Built SPA

### Community 23 - "Backend Baseline Gates"
Cohesion: 0.40
Nodes (5): Read a Gate Result From a Log Sentinel, ArchUnit Architecture Gate, Two Baseline Gates Finish a Change, Semgrep Scan Gate, Local Ruleset Keeps the Scan Offline and Deterministic

### Community 24 - "Stack Cleanup Script"
Cohesion: 0.70
Nodes (4): print_error(), print_info(), print_warn(), cleanup.sh script

### Community 25 - "VPC Discovery Script"
Cohesion: 0.70
Nodes (4): print_header(), print_info(), print_warn(), get-vpc-info.sh script

### Community 26 - "Spring Boot Application Entry"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

## Ambiguous Edges - Review These
- `Echo XSRF-TOKEN Cookie in X-XSRF-TOKEN Header` → `No .env Required Yet`  [AMBIGUOUS]
  backend/FRONTEND.md · relation: conceptually_related_to
- `login operation` → `Shared Playwright storageState for Auth`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **170 isolated node(s):** `com.example:backend`, `semgrep.sh script`, `$schema`, `style`, `rsc` (+165 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 245 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Echo XSRF-TOKEN Cookie in X-XSRF-TOKEN Header` and `No .env Required Yet`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `login operation` and `Shared Playwright storageState for Auth`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `vitest` connect `Frontend App Root, Auth Client & Layering` to `AWS CloudFormation Infrastructure`, `Frontend Runtime Dependencies`, `Frontend Architecture Test Suite`?**
  _High betweenness centrality (0.137) - this node is a cross-community bridge._
- **Why does `Echo XSRF-TOKEN Cookie in X-XSRF-TOKEN Header` connect `AWS CloudFormation Infrastructure` to `Spring Security Auth & CSRF Rotation`, `Browser Security Headers & Semgrep Rules`?**
  _High betweenness centrality (0.111) - this node is a cross-community bridge._
- **Why does `login()` connect `AWS CloudFormation Infrastructure` to `Frontend App Root, Auth Client & Layering`?**
  _High betweenness centrality (0.098) - this node is a cross-community bridge._
- **What connects `com.example:backend`, `semgrep.sh script`, `$schema` to the rest of the system?**
  _170 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Spring Security Auth & CSRF Rotation` be split into smaller, more focused modules?**
  _Cohesion score 0.060718252499074414 - nodes in this community are weakly interconnected._