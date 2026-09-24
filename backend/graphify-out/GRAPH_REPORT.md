# Graph Report - back-end  (2026-09-22)

## Corpus Check
- 43 files · ~22,627 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 390 nodes · 690 edges · 33 communities (23 shown, 10 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 75 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `72625f54`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SecurityConfig.java
- UserCounter
- Redis 8.2 Alpine Service
- SpaFrontendTests.java
- RecordingCounterService
- AuthController.java
- What You Must Do When Invoked
- graphify reference: extra exports and benchmark
- Issue tracker: GitHub
- BackendApplication.java
- graphify reference: query, path, explain
- Actuator Health Endpoint
- com.example:backend
- Domain Docs
- AGENTS.md
- graphify reference: add a URL and watch a folder
- graphify reference: commit hook and native CLAUDE.md integration
- graphify reference: incremental update and cluster-only
- graphify reference: GitHub clone and cross-repo merge
- graphify reference: transcribe video and audio
- extraction-spec.md
- JSESSIONID Cookie Authentication
- httpstatus
- AWS CloudFormation Deployment Guide
- transactional
- ArchitectureTest.java
- deploy.sh
- UserCounterService
- cleanup.sh
- get-vpc-info.sh
- semgrep.sh
- responsestatusexception
- org.junit.jupiter.api.Test

## God Nodes (most connected - your core abstractions)
1. `UserCounter` - 17 edges
2. `AuthController` - 14 edges
3. `UserCounterService` - 13 edges
4. `UserCounterEntity` - 13 edges
5. `SpaFrontendTests` - 13 edges
6. `AWS CloudFormation Deployment Guide` - 13 edges
7. `AuthControllerTests` - 12 edges
8. `What You Must Do When Invoked` - 12 edges
9. `SecurityConfig` - 10 edges
10. `SessionController` - 10 edges

## Surprising Connections (you probably didn't know these)
- `Actuator Health Endpoint` --conceptually_related_to--> `Management Health Configuration`  [INFERRED]
  README.md → src/main/resources/application.yaml
- `Redis 8.2 Alpine Service` --shares_data_with--> `Redis Connection Configuration`  [INFERRED]
  compose.yaml → src/main/resources/application.yaml
- `Spring Session` --conceptually_related_to--> `Redis 8.2 Alpine Service`  [INFERRED]
  README.md → compose.yaml
- `Application Credentials` --conceptually_related_to--> `JSESSIONID Cookie Authentication`  [INFERRED]
  src/main/resources/application.yaml → README.md
- `AuthControllerTests` --references--> `AuthController`  [EXTRACTED]
  src/test/java/com/example/backend/auth/AuthControllerTests.java → src/main/java/com/example/backend/auth/AuthController.java

## Import Cycles
- None detected.

## Communities (33 total, 10 thin omitted)

### Community 0 - "SecurityConfig.java"
Cohesion: 0.09
Nodes (26): assertthatthrownby, authentication, authenticationentrypoint, authenticationmanager, badcredentialsexception, bcryptpasswordencoder, daoauthenticationprovider, httpmethod (+18 more)

### Community 1 - "UserCounter"
Cohesion: 0.09
Nodes (16): id, jakarta.persistence.Entity, jakarta.persistence.Table, lockmodetype, optional, org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.jpa.repository.Lock, org.springframework.data.jpa.repository.Query (+8 more)

### Community 2 - "Redis 8.2 Alpine Service"
Cohesion: 0.22
Nodes (10): Redis Health Check, Redis Persistent Volume, Redis 8.2 Alpine Service, Backend Service, Redis Session Persistence, Spring Session, Backend Application Configuration, Redis Connection Configuration (+2 more)

### Community 3 - "SpaFrontendTests.java"
Cohesion: 0.08
Nodes (25): assertthat, autowired, forwardedurl, get, jakarta.servlet.Filter, map, mockhttpsession, mockmvcbuilders (+17 more)

### Community 4 - "RecordingCounterService"
Cohesion: 0.31
Nodes (3): Override, RecordingCounterService, UserCounterControllerTests

### Community 5 - "AuthController.java"
Cohesion: 0.12
Nodes (24): instant, jakarta.servlet.http.HttpSession, java.security.Principal, notblank, org.springframework.security.authentication.AuthenticationManager, org.springframework.security.web.context.SecurityContextRepository, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.GetMapping (+16 more)

### Community 6 - "What You Must Do When Invoked"
Cohesion: 0.08
Nodes (24): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+16 more)

### Community 7 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 8 - "Issue tracker: GitHub"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: GitHub, Pull requests as a triage surface, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 9 - "BackendApplication.java"
Cohesion: 0.50
Nodes (3): org.springframework.boot.autoconfigure.SpringBootApplication, springapplication, BackendApplication

### Community 10 - "graphify reference: query, path, explain"
Cohesion: 0.33
Nodes (5): For /graphify explain, For /graphify path, graphify reference: query, path, explain, Step 0 — Constrained query expansion (REQUIRED before traversal), Step 1 — Traversal

### Community 13 - "Domain Docs"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 14 - "AGENTS.md"
Cohesion: 0.17
Nodes (11): Agent skills, API contract, Architecture constraints, Baseline gates, Domain docs, graphify, Issue tracker, Mutation testing (+3 more)

### Community 15 - "graphify reference: add a URL and watch a folder"
Cohesion: 0.50
Nodes (3): For /graphify add, For --watch, graphify reference: add a URL and watch a folder

### Community 16 - "graphify reference: commit hook and native CLAUDE.md integration"
Cohesion: 0.50
Nodes (3): For git commit hook, For native CLAUDE.md integration, graphify reference: commit hook and native CLAUDE.md integration

### Community 17 - "graphify reference: incremental update and cluster-only"
Cohesion: 0.50
Nodes (3): For --cluster-only, For --update (incremental re-extraction), graphify reference: incremental update and cluster-only

### Community 23 - "AWS CloudFormation Deployment Guide"
Cohesion: 0.06
Nodes (32): 1. Get VPC Info, 2. Deploy, 3. Get Private Key (if CloudFormation created it), 4. Deploy Application, 5. Test, Quick Start, Architecture, AWS CloudFormation Deployment Guide (+24 more)

### Community 25 - "ArchitectureTest.java"
Cohesion: 0.13
Nodes (15): classes, com.tngtech.archunit.junit.AnalyzeClasses, com.tngtech.archunit.lang.ArchRule, controller, entitymanager, generalcodingrules, importoption, noclasses (+7 more)

### Community 26 - "deploy.sh"
Cohesion: 0.41
Nodes (12): check_prerequisites(), create_parameters_file(), deploy_jar(), deploy_stack(), display_outputs(), get_inputs(), main(), print_error() (+4 more)

### Community 27 - "UserCounterService"
Cohesion: 0.20
Nodes (5): org.springframework.stereotype.Service, org.springframework.transaction.annotation.Transactional, UserCounterService, UserCounterRepository, UserCounterServiceTests

### Community 28 - "cleanup.sh"
Cohesion: 0.70
Nodes (4): print_error(), print_info(), print_warn(), cleanup.sh script

### Community 29 - "get-vpc-info.sh"
Cohesion: 0.70
Nodes (4): print_header(), print_info(), print_warn(), get-vpc-info.sh script

### Community 32 - "org.junit.jupiter.api.Test"
Cohesion: 0.28
Nodes (7): jakarta.servlet.http.HttpServletRequest, MockHttpServletRequest, org.junit.jupiter.api.Test, org.springframework.web.bind.annotation.DeleteMapping, org.springframework.web.bind.annotation.ResponseStatus, LoginRequest, AuthControllerTests

## Knowledge Gaps
- **94 isolated node(s):** `com.example:backend`, `semgrep.sh script`, `Usage`, `What graphify is for`, `Step 0 - GitHub repos and multi-path merge (only if a URL or several paths)` (+89 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **10 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `UserCounterService` connect `UserCounterService` to `SpaFrontendTests.java`, `RecordingCounterService`, `AuthController.java`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Why does `UserCounter` connect `UserCounter` to `UserCounterService`?**
  _High betweenness centrality (0.016) - this node is a cross-community bridge._
- **What connects `com.example:backend`, `semgrep.sh script`, `Usage` to the rest of the system?**
  _94 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SecurityConfig.java` be split into smaller, more focused modules?**
  _Cohesion score 0.08558558558558559 - nodes in this community are weakly interconnected._
- **Should `UserCounter` be split into smaller, more focused modules?**
  _Cohesion score 0.09291521486643438 - nodes in this community are weakly interconnected._
- **Should `SpaFrontendTests.java` be split into smaller, more focused modules?**
  _Cohesion score 0.07897793263646923 - nodes in this community are weakly interconnected._
- **Should `AuthController.java` be split into smaller, more focused modules?**
  _Cohesion score 0.12462462462462462 - nodes in this community are weakly interconnected._