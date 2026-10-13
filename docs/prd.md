Localhost - Product Requirements Document
Product: Localhost, a Mobile Server Hosting Platform for Android Build system: Gradle (Kotlin DSL) Status: Draft v1.0
1. Overview
Localhost turns an Android smartphone into a lightweight personal cloud / VPS-style hosting environment. Users can host websites, APIs, backend services and apps written in PHP, Python (Flask), Node.js, Java and JavaScript directly from their phone, manage them from a native app, a web dashboard or a built-in CLI, and expose them to the internet through Cloudflare Tunnel without port forwarding, static IPs or router changes.
2. Goals and Non-Goals
Goals
• Run multiple isolated server projects simultaneously on one device.
• Make setup trivial: create a project, pick a runtime, press Start.
• Provide secure public access (HTTPS, custom domains) via Cloudflare Tunnel.
• Stay reliable for long-running workloads (foreground service, crash recovery).
• Minimize CPU, RAM, battery and storage usage.
• Offer three management surfaces with feature parity: native UI, web dashboard, CLI.
Non-Goals (v1)
• Docker or full container orchestration.
• Hosting databases other than SQLite and the optional Supabase integration.
• Multi-device clustering or load balancing.
• iOS support.
• Running as a production-grade replacement for a data-center VPS.
3. Target Users
Persona
Need
Student / hobbyist developer
Free, always-available place to host projects and demos
Indie developer
Quick staging server and webhook receiver
Tinkerer / maker
Run small APIs, bots and IoT backends on a spare phone
Educator
Portable server environment for teaching web development
4. Key Use Cases
1. Deploy a Flask API on port 5000 and reach it at api.example.com through a tunnel.
2. Host a static or PHP website on the local network for testing on other devices.
3. Run a Node.js bot or webhook handler 24/7 with automatic restart.
4. Use the CLI over the dashboard to start, stop and tail logs from a laptop.
5. Back a hosted app with Supabase auth and database.
5. Functional Requirements
Priority key: P0 = must have for v1, P1 = should have, P2 = later.
5.1 Multi-Language Runtime Support
ID
Requirement
Priority
RT-1
Support PHP (built-in server or php-fpm with a lightweight web server)
P0
RT-2
Support Python with Flask (and generic WSGI/ASGI apps)
P0
RT-3
Support Node.js (npm scripts, Express, etc.)
P0
RT-4
Support static / JavaScript front-end hosting
P0
RT-5
Support Java (JAR and embedded servers such as Jetty/Spring Boot)
P1
RT-6
Runtime manager: install, update and remove runtime versions on demand to keep the base APK small
P0
RT-7
Per-project runtime version selection
P1
RT-8
Package installation (pip, npm, composer) from within a project
P1
5.2 Project and Server Management
ID
Requirement
Priority
PM-1
Create, edit, duplicate, delete projects from templates (Flask, Express, PHP site, static site, Java)
P0
PM-2
Per-project config: name, runtime, port, working directory, startup command, environment variables
P0
PM-3
Port conflict detection and suggestion
P0
PM-4
Run multiple projects concurrently
P0
PM-5
Start, stop, restart controls
P0
PM-6
Auto-start on app launch and optionally on device boot
P1
PM-7
Auto-restart on crash with configurable backoff and max retries
P0
PM-8
Import projects from a ZIP, Git repository or device storage
P1
PM-9
Export/backup a project as ZIP
P1
PM-10
Secrets-aware environment variables (masked in UI, stored encrypted)
P0
PM-11
Per-project resource limits (RAM cap, CPU priority, max log size)
P1
5.3 File Management
ID
Requirement
Priority
FM-1
Built-in file browser scoped to each project directory
P0
FM-2
Create, rename, move, delete, upload and download files
P0
FM-3
Syntax-highlighted code editor for common file types
P1
FM-4
Integration with Android Storage Access Framework for importing files
P0
FM-5
Optional Git support (clone, pull, push)
P2
5.4 Logs and Monitoring
ID
Requirement
Priority
LM-1
Real-time stdout/stderr log streaming per project
P0
LM-2
Log search, filter by level, copy and export
P1
LM-3
Log rotation with size and age limits
P0
LM-4
Per-project status: running, stopped, crashed, restarting, with uptime
P0
LM-5
Device and per-project metrics: CPU, RAM, storage, network throughput
P0
LM-6
Metric history charts (last hour / day)
P1
LM-7
Alerts via notification on crash, high usage or tunnel disconnect
P1
5.5 Cloudflare Tunnel Integration
ID
Requirement
Priority
CF-1
Bundle or download the cloudflared binary for the device ABI
P0
CF-2
Authenticate with Cloudflare (API token or tunnel token) and store credentials securely
P0
CF-3
Create and manage tunnels from the app
P0
CF-4
Map a project's local port to a public hostname (subdomain or custom domain)
P0
CF-5
Automatic DNS record creation for mapped hostnames
P1
CF-6
HTTPS terminated at Cloudflare; show certificate and domain status
P0
CF-7
Tunnel health monitoring and automatic reconnect
P0
CF-8
Quick tunnels (temporary trycloudflare.com URLs) for instant sharing
P1
CF-9
Per-project enable/disable of public exposure
P0
5.6 Networking
ID
Requirement
Priority
NW-1
Local-network hosting with the LAN URL and QR code displayed per project
P0
NW-2
Choose bind address per project (localhost-only or all interfaces)
P0
NW-3
Detect network changes (Wi-Fi to mobile data) and keep services and tunnel healthy
P0
NW-4
Optional Wi-Fi-only mode to protect mobile data
P1
5.7 Background Execution and Reliability
ID
Requirement
Priority
BG-1
Foreground service with persistent notification (start/stop all, status summary)
P0
BG-2
Partial wake lock and Wi-Fi lock only while servers are running, user-toggleable
P0
BG-3
Guided onboarding for battery-optimization exemption and OEM-specific background restrictions
P0
BG-4
Crash detection and recovery for individual projects and for the supervisor
P0
BG-5
Restore running servers after app update, device reboot or process death
P1
BG-6
Thermal and battery-aware throttling (pause or warn at low battery or high temperature)
P1
5.8 Web-Based Management Dashboard
ID
Requirement
Priority
WD-1
Embedded lightweight HTTP server serving the dashboard (default port configurable, e.g. 8080)
P0
WD-2
Feature parity with the native UI: projects, controls, logs, files, metrics, tunnels
P0
WD-3
Authentication (username and password, session tokens; optional TOTP)
P0
WD-4
Dashboard accessible over LAN by default and publicly only through an explicit tunnel mapping
P0
WD-5
WebSocket streaming for logs and metrics
P0
WD-6
Rate limiting and brute-force lockout
P0
5.9 Built-in CLI
ID
Requirement
Priority
CLI-1
In-app terminal providing a localhost command set
P0
CLI-2
Commands: list, create, start, stop, restart, logs [-f], status, env, tunnel, exec, stats
P0
CLI-3
Access to each runtime's shell tools (python, node, php, pip, npm) inside the project environment
P0
CLI-4
Remote CLI client over the dashboard API (usable from a laptop via an API token)
P1
CLI-5
Scriptable output (--json) and shell completion
P1
5.10 Supabase Integration (Optional)
ID
Requirement
Priority
SB-1
Connect a Supabase project via URL and keys, stored encrypted
P1
SB-2
Inject Supabase credentials into project environment variables with one toggle
P1
SB-3
Starter templates using Supabase auth, database and storage
P2
SB-4
Connection test and status indicator
P1
6. Non-Functional Requirements
Performance and Efficiency
• Idle supervisor overhead: under 60 MB RAM and under 1% average CPU with no projects running.
• Cold start of the supervisor service in under 2 seconds.
• Base APK under 40 MB (runtimes downloaded on demand); runtime packs are separately versioned.
• Log and metrics pipelines are batched and bounded to avoid battery drain.
• Dashboard assets are served pre-compressed and cached.
Reliability
• Target 99% supervisor uptime on a charging, exempted device over a 7-day window.
• No data loss of project files on crash; configuration writes are atomic.
Security
• All credentials (Cloudflare, Supabase, dashboard) stored in Android Keystore-backed encrypted storage.
• Projects run in isolated working directories and cannot read other projects' files or the app's private data.
• Dashboard and CLI API require authentication; default bind is LAN-restricted and public exposure is opt-in.
• Clear warning before exposing any service publicly; optional Cloudflare Access integration.
• Supply-chain safeguards: signature and checksum verification for downloaded runtimes and cloudflared.
Compatibility
• minSdk 26 (Android 8.0), targetSdk 35 (updated annually).
• ABIs: arm64-v8a (primary), armeabi-v7a (best effort), x86_64 (emulator).
Usability
• Material 3 design, dark mode, tablet and landscape support.
• First project running in under 3 minutes from install.
• Accessibility: TalkBack support, scalable text, sufficient contrast.
7. Technical Architecture
7.1 Stack
• Language: Kotlin (Coroutines and Flow); native C/C++ via NDK only where needed.
• UI: Jetpack Compose with Material 3.
• DI: Hilt.
• Persistence: Room (projects, logs index, metrics), DataStore (settings), EncryptedFile/Keystore (secrets).
• Dashboard server: Ktor (CIO engine) with WebSockets, serving a bundled SPA front end.
• Background work: Foreground Service plus WorkManager for deferred tasks.
• Process management: ProcessBuilder supervisor with per-project process groups.
7.2 Gradle Project Structure
The project uses a multi-module Gradle build with Kotlin DSL, a version catalog (gradle/libs.versions.toml) and convention plugins in build-logic/.
localhost/
 settings.gradle.kts
 build.gradle.kts
 gradle/libs.versions.toml
 build-logic/convention/          # shared android-app / android-library / compose plugins
 app/                             # Application, navigation, manifest, Hilt entry
 core/
    common/                      # utilities, result types, logging
    data/                        # Room, DataStore, repositories
    model/                       # domain models
    security/                    # Keystore, secrets, auth
    designsystem/                # Compose theme and components
 runtime/
    manager/                     # download, verify, install runtime packs
    process/                     # supervisor, restart policy, resource limits
    templates/                   # project templates
 tunnel/                          # cloudflared lifecycle, Cloudflare API client
 server-dashboard/                # Ktor server and bundled web UI assets
 cli/                             # command parser, in-app terminal, remote client
 feature/
    projects/ logs/ files/ monitor/ tunnel/ settings/ supabase/
 7.3 Build Configuration
• Build types: debug, release (R8 minification and resource shrinking enabled).
• Product flavors: full (GitHub/F-Droid distribution) and play (Play-policy compliant variant).
• ABI splits and App Bundle for per-device APK size reduction.
• Gradle tasks: a buildWebUi task runs the front-end build and copies output into server-dashboard assets; runtime pack manifests are generated at build time.
• CI: GitHub Actions running ./gradlew lint test assembleRelease, with dependency and license checks.
• Static analysis: ktlint, detekt, Android Lint.
7.4 Core Components
1. Supervisor Service: foreground service that owns all child processes, restart policies, resource sampling and the tunnel.
2. Runtime Manager: downloads runtime packs, verifies checksums, unpacks into app-private storage and exposes executable paths.
3. Process Supervisor: launches each project with its environment, working directory and limits; captures stdout/stderr into ring-buffered log files.
4. Tunnel Manager: runs cloudflared, tracks ingress rules per project and handles reconnects.
5. Management API: one internal API consumed by the Compose UI, the web dashboard and the CLI to guarantee parity.
6. Metrics Collector: samples /proc and Android APIs at adaptive intervals.
8. Platform Constraints and Risks
Android is not a traditional server OS. These need early technical spikes:
Risk
Impact
Mitigation
Android 10+ blocks executing binaries written to app data (W^X)
Runtimes may not launch
Ship executables as native libraries in jniLibs (run from nativeLibraryDir), or use a proot-based userland as fallback; validate in a spike before committing
Android 12+ phantom process killer (default limit around 32 child processes)
Servers killed in the background
Keep process count low, use a single supervisor, document the adb and developer-option workaround, minimize fork-heavy runtimes
Foreground-service type rules and time limits on newer Android versions
Service can be stopped by the OS
Choose the correct foreground service type, handle timeout callbacks and restart gracefully
OEM battery management (aggressive background killing)
Unreliable uptime
Onboarding flow with per-OEM guidance, battery-exemption request, health checks
No stock JVM on Android
Java runtime not trivial
Evaluate an aarch64 Bionic-compatible JDK pack versus running JARs through a dex-based embedded server; scope Java to P1
Google Play policy on downloading and executing code
Store rejection
Ship a full flavor via GitHub and F-Droid first; design the play flavor to bundle runtimes and restrict features
Public exposure of a phone-hosted service
Security incidents
Opt-in exposure, warnings, mandatory auth on the dashboard, Cloudflare Access support
Battery and thermal impact
Poor experience, device wear
Adaptive sampling, wake lock only when needed, thermal and battery thresholds
Cloudflare ToS and API changes
Feature breakage
Isolate Cloudflare logic in the tunnel module, version the API client, support plain tunnel tokens
9. User Experience
Primary Screens
1. Home / Overview: all projects with status chips, global start/stop, device stats summary.
2. Project Detail: controls, port, URLs (local and public), config, env vars, tabs for Logs, Files and Metrics.
3. Create Project: template picker and guided config.
4. Tunnels: tunnel status, hostname mappings, domain setup wizard.
5. Terminal (CLI): full-screen terminal with command history and runtime shells.
6. Monitor: CPU, RAM, storage and network graphs.
7. Settings: dashboard credentials, runtimes, battery and background settings, Supabase, backup, about.
Onboarding Flow
1. Welcome and permission explanation (notifications, storage as needed).
2. Battery-optimization and background-restriction setup.
3. Choose and install a first runtime.
4. Create a sample project and start it.
5. Optional: connect Cloudflare and publish the sample.
10. Success Metrics
Metric
Target
Time to first running project
under 3 minutes (median)
7-day server uptime on exempted devices
99%
Crash-free sessions
99.5%
Idle battery drain attributable to app
under 2% per hour with one light project
Tunnel connection success rate
above 98%
Users with at least one public tunnel after 14 days
above 30%
11. Release Plan
Milestone 0 - Feasibility Spikes (2-3 weeks)
Execute a binary from nativeLibraryDir, run Python and Node, run cloudflared, measure phantom-process and battery behavior on at least three OEM devices.
Milestone 1 - MVP (8-10 weeks)
Project CRUD, supervisor service, PHP, Python/Flask, Node.js and static hosting, start/stop/restart, live logs, env vars, file manager, foreground service, local-network hosting.
Milestone 2 - Public Access and Control (6-8 weeks)
Cloudflare Tunnel integration, custom domains, web dashboard with authentication, in-app CLI, auto-restart and crash recovery, resource monitoring.
Milestone 3 - Polish and Expansion (6-8 weeks)
Java runtime, Supabase integration, templates, remote CLI client, alerts, backup/export, boot auto-start, Play-flavor evaluation.
Milestone 4 - Hardening and Launch
Security review, accessibility audit, performance tuning, documentation, beta program, public release.
12. Out-of-Scope and Future Ideas
• Docker-compatible container support
• Scheduled jobs and cron manager
• Built-in reverse proxy with path-based routing and local TLS
• Git-push-to-deploy webhooks
• SQLite/MariaDB/Redis managed add-ons
• Plugin system for additional runtimes (Go, Rust, Ruby)
• Multi-device fleet management
13. Open Questions
1. Primary distribution channel: GitHub/F-Droid first, or Play Store compliance from day one?
2. Should Java target an embedded server on ART first, or a full JDK pack?
3. Is root or Shizuku support in scope for relaxing process and battery limits?
4. Will Cloudflare authentication use API tokens, OAuth, or user-supplied tunnel tokens only?
5. What is the policy for multi-user access to the dashboard (single admin versus roles)?
14. Acceptance Criteria Summary (v1)
• A user can install the app, create a Flask, Node.js, PHP or static project, and start it with live logs.
• At least three projects can run concurrently without the supervisor being killed on a reference device with battery exemption.
• A crashed project restarts automatically according to its policy.
• A project can be exposed on a Cloudflare hostname over HTTPS and reached from the public internet.
• The web dashboard and CLI can perform start, stop, restart, log tailing and env editing with authentication.
• The project builds from a clean checkout with ./gradlew assembleRelease.