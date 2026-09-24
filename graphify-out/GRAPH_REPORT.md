# Graph Report - monorepo-base  (2026-09-24)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 776 nodes · 1369 edges · 40 communities (30 shown, 10 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 148 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `25033a6a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SecurityConfig.java
- SecurityConfigTests.java
- showcase.tsx
- UserCounter
- CloudFormation Infrastructure Template
- AuthController.java
- package.json
- devDependencies
- stryker.config.json
- compilerOptions
- components.json
- scripts
- ArchitectureTest.java
- compilerOptions
- org.junit.jupiter.api.Test
- sources.ts
- deploy.sh
- Frontend AGENTS.md
- Backend API Contract (OpenAPI 3.1)
- Workflow
- AGENTS.md
- tsconfig.test.json
- lib.sh
- mvnw
- cleanup.sh
- get-vpc-info.sh
- BackendApplication.java
- prettier.config.mjs
- semgrep.sh
- com.example:backend
- Domain Docs
- Issue tracker: GitHub
- Graphify Runner
- dev.sh
- integration-test.sh
- CLAUDE.md
- bootstrap.sh
- package.sh

## God Nodes (most connected - your core abstractions)
1. `scripts` - 19 edges
2. `compilerOptions` - 19 edges
3. `SecurityConfigTests` - 18 edges
4. `AuthControllerTests` - 17 edges
5. `UserCounter` - 17 edges
6. `AuthController` - 17 edges
7. `SpaFrontendTests` - 15 edges
8. `compilerOptions` - 15 edges
9. `UserCounterService` - 13 edges
10. `Frontend AGENTS.md` - 13 edges

## Surprising Connections (you probably didn't know these)
- `403 Is a Missing Token, Not a Logout` --references--> `request()`  [INFERRED]
  backend/FRONTEND.md → frontend/src/auth/api.ts
- `Baseline / Full / Extensive Test Levels` --semantically_similar_to--> `Two Baseline Gates Finish a Change`  [INFERRED] [semantically similar]
  frontend/AGENTS.md → backend/README.md
- `getCurrentUser operation` --shares_data_with--> `getCurrentUser()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `login operation` --shares_data_with--> `login()`  [INFERRED]
  backend/docs/openapi.yaml → frontend/src/auth/api.ts
- `Echo XSRF-TOKEN Cookie in X-XSRF-TOKEN Header` --references--> `login()`  [INFERRED]
  backend/FRONTEND.md → frontend/src/auth/api.ts

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Mutation Testing as the Load-Bearing-Test Doctrine on Both Sides** — backend_readme_pit_mutation_testing, backend_readme_default_mutators_provisional, frontend_docs_testing_guide_stryker_mutate_trap, frontend_docs_testing_guide_assert_exactly, backend_agents_load_bearing_test [INFERRED 0.85]
- **CSRF Double-Submit Contract Across Backend, SPA and API Doc** — backend_src_main_java_com_example_backend_auth_config_securityconfig_securityconfig_csrftokenrepository, backend_src_main_java_com_example_backend_auth_authcontroller_authcontroller_issuecsrftoken, backend_frontend_csrf_token_echo, backend_docs_openapi_csrftoken_parameter [INFERRED 0.95]
- **Session Persistence From Local Compose to ElastiCache** — backend_src_main_resources_application_session_namespace, backend_compose_redis, backend_cloudformation_infrastructure_rediscluster, backend_readme_redis_session_persistence [INFERRED 0.95]
- **SPA Authentication Flow the Docs Do Not Mention** — frontend_src_auth_api_login, frontend_src_auth_auth_context_authprovider, frontend_src_auth_protected_route_protectedroute, frontend_src_pages_login_login, backend_docs_openapi_login [INFERRED 0.95]

## Communities (40 total, 10 thin omitted)

### Community 0 - "SecurityConfig.java"
Cohesion: 0.08
Nodes (29): assertthatnoexception, assertthatthrownby, authentication, authenticationentrypoint, authenticationmanager, SecurityConfig, badcredentialsexception, bcryptpasswordencoder (+21 more)

### Community 1 - "SecurityConfigTests.java"
Cohesion: 0.06
Nodes (39): assertthat, assertthatcode, Override, SpaErrorViewResolver, BackendApplicationTests, SpaFrontendTests, chronounit, classmode (+31 more)

### Community 2 - "showcase.tsx"
Cohesion: 0.07
Nodes (42): 15-Minute Session Inactivity Window, Trace Before You Delete, One-Way Import Direction Through the Layers, components/ui Is a Package Placeholder, App(), AuthUser, AuthProvider(), AuthContext (+34 more)

### Community 3 - "UserCounter"
Cohesion: 0.07
Nodes (22): resetCount operation, UserCounterService, UserCounter, UserCounterRepository, UserCounterEntity, UserCounterJpaRepository, Override, UserCounterPersistenceAdapter (+14 more)

### Community 4 - "CloudFormation Infrastructure Template"
Cohesion: 0.07
Nodes (40): Backend AGENTS.md, Update openapi.yaml With Every Controller Change, Read a Gate Result From a Log Sentinel, Onion Layering: Adapters to Application to Domain, Session Changes Need a Real Redis Check, Security-Sensitive Change Policy, CloudFormation Infrastructure Template, ALB Security Group (+32 more)

### Community 5 - "AuthController.java"
Cohesion: 0.08
Nodes (35): deleteSession operation, AuthController, UserResponse, CountResponse, UserCounterController, SessionController, SessionResponse, UpdateSessionRequest (+27 more)

### Community 6 - "package.json"
Cohesion: 0.06
Nodes (34): engines, node, npm, name, packageManager, private, type, version (+26 more)

### Community 7 - "devDependencies"
Cohesion: 0.07
Nodes (29): devDependencies, dependency-cruiser, eslint, @eslint/js, eslint-plugin-react-hooks, eslint-plugin-react-refresh, fallow, globals (+21 more)

### Community 8 - "stryker.config.json"
Cohesion: 0.07
Nodes (26): cleanTempDir, clearTextReporter, allowColor, maxTestsToLog, _comment_mutate, concurrency, coverageAnalysis, htmlReporter (+18 more)

### Community 9 - "compilerOptions"
Cohesion: 0.09
Nodes (21): compilerOptions, allowImportingTsExtensions, baseUrl, isolatedModules, jsx, lib, module, moduleDetection (+13 more)

### Community 10 - "components.json"
Cohesion: 0.11
Nodes (18): aliases, components, hooks, lib, ui, utils, iconLibrary, rsc (+10 more)

### Community 11 - "scripts"
Cohesion: 0.11
Nodes (19): scripts, analyze, build, dev, format, format:check, lint, preview (+11 more)

### Community 12 - "ArchitectureTest.java"
Cohesion: 0.12
Nodes (17): autowired, ArchitectureTest, classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, controller, entitymanager, generalcodingrules (+9 more)

### Community 13 - "compilerOptions"
Cohesion: 0.12
Nodes (16): compilerOptions, allowImportingTsExtensions, isolatedModules, lib, module, moduleDetection, moduleResolution, noEmit (+8 more)

### Community 14 - "org.junit.jupiter.api.Test"
Cohesion: 0.15
Nodes (8): CSRF Token Rotates on Login and Logout, LoginRequest, AuthControllerTests, ProbeController, SecurityConfigTests, jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse, org.junit.jupiter.api.Test

### Community 15 - "sources.ts"
Cohesion: 0.17
Nodes (13): blankComments(), files, sources, configSource, routes, testFiles, readSource(), readSources() (+5 more)

### Community 16 - "deploy.sh"
Cohesion: 0.41
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 17 - "Frontend AGENTS.md"
Cohesion: 0.06
Nodes (42): A Test Must Be Load-Bearing, Front-End Changes Required (FRONTEND.md), Content-Security-Policy Response Header, Logout Returns an Expired Session Cookie, A 100% PIT Score Is Provisional, PIT Mutation Testing, Serve SPA from backend frontend/dist, Test Static index.html Stub (+34 more)

### Community 18 - "Backend API Contract (OpenAPI 3.1)"
Cohesion: 0.10
Nodes (28): Deploy Guide Curls Routes the API Does Not Expose, Backend API Contract (OpenAPI 3.1), X-XSRF-TOKEN Header Parameter, getCount operation, getCurrentUser operation, getSession operation, incrementCount operation, login operation (+20 more)

### Community 19 - "Workflow"
Cohesion: 0.08
Nodes (22): Fix Recommendation Patterns, Report Template, Trend Comparison (`--history`), Cosmic Ray / Python, Custom, mutmut / Python, PIT / JVM, Stryker.NET / .NET (+14 more)

### Community 20 - "AGENTS.md"
Cohesion: 0.14
Nodes (12): Agent, Agent documentation, Build and validation, Environment, Frontend/backend integration, graphify, Ignore rules, Layout (+4 more)

### Community 21 - "tsconfig.test.json"
Cohesion: 0.29
Nodes (6): compilerOptions, types, exclude, extends, include, ./tsconfig.json

### Community 22 - "lib.sh"
Cohesion: 0.31
Nodes (10): die(), load_backend_env(), log(), require_cmd(), require_docker(), require_maven(), require_node(), lib.sh script (+2 more)

### Community 23 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 24 - "cleanup.sh"
Cohesion: 0.70
Nodes (4): print_error(), print_info(), print_warn(), cleanup.sh script

### Community 25 - "get-vpc-info.sh"
Cohesion: 0.70
Nodes (4): print_header(), print_info(), print_warn(), get-vpc-info.sh script

### Community 26 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): BackendApplication, org.springframework.boot.autoconfigure.SpringBootApplication, springapplication

### Community 32 - "Domain Docs"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 33 - "Issue tracker: GitHub"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 34 - "Graphify Runner"
Cohesion: 0.50
Nodes (3): Graphify Runner, Reporting, Steps

## Ambiguous Edges - Review These
- `Shared Playwright storageState for Auth` → `login operation`  [AMBIGUOUS]
  frontend/docs/TESTING_GUIDE.md · relation: conceptually_related_to
- `Echo XSRF-TOKEN Cookie in X-XSRF-TOKEN Header` → `No .env Required Yet`  [AMBIGUOUS]
  backend/FRONTEND.md · relation: conceptually_related_to

## Knowledge Gaps
- **217 isolated node(s):** `SourceFile`, `ButtonProps`, `LoginLocationState`, `CountResponse`, `components` (+212 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 304 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **10 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Shared Playwright storageState for Auth` and `login operation`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Echo XSRF-TOKEN Cookie in X-XSRF-TOKEN Header` and `No .env Required Yet`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `vitest` connect `showcase.tsx` to `Backend API Contract (OpenAPI 3.1)`, `package.json`, `sources.ts`?**
  _High betweenness centrality (0.111) - this node is a cross-community bridge._
- **Why does `Echo XSRF-TOKEN Cookie in X-XSRF-TOKEN Header` connect `Backend API Contract (OpenAPI 3.1)` to `SecurityConfig.java`, `Frontend AGENTS.md`?**
  _High betweenness centrality (0.087) - this node is a cross-community bridge._
- **Why does `login()` connect `Backend API Contract (OpenAPI 3.1)` to `showcase.tsx`?**
  _High betweenness centrality (0.079) - this node is a cross-community bridge._
- **What connects `SourceFile`, `ButtonProps`, `LoginLocationState` to the rest of the system?**
  _217 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SecurityConfig.java` be split into smaller, more focused modules?**
  _Cohesion score 0.07948717948717948 - nodes in this community are weakly interconnected._