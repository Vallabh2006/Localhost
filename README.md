# Localhost

Localhost is an all-in-one local development environment and server hosting platform for Android. It enables developers to scaffold, run, edit, manage, and tunnel web applications directly on Android devices with background persistence and real-time telemetry.

---

## Core Features

- Multi-Runtime Support: Native process execution for Python (Flask/FastAPI/pip), Node.js (Express/npm), PHP with Composer dependency management, Java OpenJDK 17 (.jar and source execution), and static HTML/CSS/JS web servers.
- Project Controls: Instant Start, Stop, and Restart buttons on both dashboard process cards and project detail views.
- Project Creation and Import: Template scaffolding, ZIP archive import and SAF export with dependency exclusion, and direct GitHub repository URL cloning (`https://github.com/user/repo`).
- Scoped File Explorer: Breadcrumb address navigation with recent path suggestions, long-press context menu (Rename, Details, Download, Cut, Copy, Delete), new file and folder creation, centered GIF previews, and rendered Markdown viewer.
- Real-Time Code Editor: Syntax color highlighting for Python, JavaScript, TypeScript, PHP, Java, HTML, CSS, JSON, and Shell scripts, paired with contextual token and variable auto-complete.
- Local Nginx HTTPS Proxy: Embedded TLS reverse proxy with automatic self-signed certificate generation to access local servers securely over HTTPS (`https://localhost:8443` and `https://<LAN_IP>:8443`).
- Cloudflare Named Tunnels: Cloudflared named tunnel integration with encrypted persistent token storage, token history dropdown selection, and instant activation.
- In-App CLI Terminal: Interactive terminal shell featuring virtual accessory keyboard keys (`Tab`, `Ctrl+C`, `|`, `-`, `/`, `~`, `↑`, `↓`, `Clear`), forgiving flag and command parsing (`-ls a`, `curl`, `chmod`, `composer`, `pip`, `python`, `node`), command auto-suggestions, and styled prompt format.
- Real-Time Server Telemetry: Accurate Linux `/proc` CPU utilization sampler with multi-core detection, physical RAM and JVM heap analysis, storage monitoring, battery and uptime tracking, listening socket health with collision checks, and live Rx/Tx network throughput.
- Adaptive Supervisor Service: Single foreground notification active strictly when a server process is running or a tunnel is connected, automatically dismissed when all services are idle.
- Modern Material 3 UI: Compact 48dp top bars, custom accent color themes (Cyan, Emerald, Purple, Amber, Crimson), and scalable responsive layouts for phones and tablets.
- Security and Isolation: AES-256-GCM Keystore encryption for tunnel tokens and dashboard credentials, sandboxed process execution directories, and scoped filesystem operations.

---

## Getting Started

### Prerequisites

- Android Studio Koala / Ladybug or newer
- JDK 17 or JDK 21
- Android SDK Platform 35
- Android device or emulator running Android 8.0+ (minSdk 26)

### Building the Project

```bash
git clone https://github.com/localhost/localhost-android.git
cd localhost-android
./gradlew assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/full/debug/app-full-debug.apk`

---

## Usage Guide

### 1. Creating and Importing Projects

- From Template: Tap `+` on the Projects screen, enter a project name, select a runtime (Python, Node.js, PHP, Java, Static), specify the entry file and port, and tap Create.
- From GitHub: Tap the download icon in the top bar, enter any public repository URL (`https://github.com/owner/repo`), and Localhost clones and configures the workspace automatically.
- From ZIP Archive: Tap the folder import icon to load an archive from Android storage. Localhost extracts the files, autodetects the project runtime, and sets up startup commands.

### 2. Dependency Management

- Python: Use the Packages tab to install packages individually or install via `requirements.txt`.
- PHP: Download and run `composer.phar` directly through the PHP package manager or via the in-app terminal (`composer install`, `composer require <pkg>`).
- Node.js: Run `npm install` directly within project directories using the CLI terminal.

### 3. Tunnels and HTTPS Access

- Local HTTPS Proxy: Open the Tunnel screen, select your target project, and tap Start HTTPS Proxy. Access your project securely at `https://localhost:8443` or from other devices on your local network.
- Cloudflare Named Tunnel: Enter your Cloudflare Tunnel token. Localhost securely saves the token in your encrypted history list for quick one-tap reuse.

### 4. In-App CLI Terminal

#### Common Commands

- `pwd`: Print working directory.
- `cd <path>`: Change directory.
- `ls [-la] [path]`: List directory contents with forgiving flag support (e.g. `-ls a`).
- `tree [path]`: View directory hierarchy tree.
- `cat <file>`: Print file contents.
- `mkdir <dir>`: Create directory.
- `touch <file>`: Create file.
- `cp <src> <dst>`: Copy file or folder.
- `mv <src> <dst>`: Move or rename file or folder.
- `rm [-r] <path>`: Delete file or directory.
- `curl <url>`: Send HTTP GET request and display response.
- `chmod <perms> <file>`: Update file permissions.
- `composer <args>`: Execute PHP Composer.
- `python <script.py>` / `pip <args>`: Execute Python and pip.
- `node <script.js>` / `npm <args>`: Execute Node.js and npm.
- `php <script.php>`: Execute PHP scripts.
- `list`: List all configured projects.
- `start <name>` / `stop <name>` / `restart <name>`: Manage server lifecycles.
- `logs <name>`: Display recent stdout and stderr logs.
- `status`: Show platform and service status.
- `clear`: Clear console output.

---

## Architecture and Modules

```
Localhost/
├── app/
│   ├── Application
│   ├── Supervisor Foreground Service
│   ├── BootReceiver
│   └── MainActivity
│
├── core/
│   ├── common/
│   │   ├── Network Utilities
│   │   ├── QR Code Generator
│   │   └── Archive Extraction
│   ├── data/
│   │   ├── Room Database
│   │   ├── DataStore Preferences
│   │   └── Repositories
│   ├── model/
│   │   ├── Project
│   │   ├── RuntimeType
│   │   ├── LogEntry
│   │   ├── ResourceMetrics
│   │   ├── TunnelModels
│   │   └── RuntimePack
│   ├── security/
│   │   ├── Keystore-backed SecretStore
│   │   └── Password Hashing
│   └── designsystem/
│       ├── Material 3 Theme
│       ├── Components
│       └── Color Tokens
│
├── runtime/
│   ├── manager/
│   │   ├── Runtime Pack Discovery
│   │   ├── Runtime Downloads
│   │   ├── ComposerManager
│   │   └── Execution Environment
│   ├── process/
│   │   ├── Process Supervisor
│   │   ├── ManagedProcess Lifecycle
│   │   └── ProcessMetricsSampler
│   └── templates/
│       ├── Flask
│       ├── Express
│       ├── PHP
│       ├── Java
│       └── Static
│
├── tunnel/
│   ├── CloudflareTunnelManager
│   ├── NginxManager
│   └── SelfSignedCertHelper
│
├── server-dashboard/
│   ├── Ktor CIO Server
│   ├── REST API
│   ├── WebSockets
│   └── SPA Static Asset Server
│
├── cli/
│   ├── LocalhostCli
│   └── TerminalScreen
│
└── feature/
    ├── projects/
    │   ├── ProjectListScreen
    │   ├── ProjectDetailScreen
    │   └── ProjectsViewModel
    ├── logs/
    │   ├── LogViewerScreen
    │   └── LogsViewModel
    ├── files/
    │   ├── FileExplorerScreen
    │   ├── SyntaxHighlighter
    │   └── FilesViewModel
    ├── monitor/
    │   ├── MonitorScreen
    │   └── MonitorViewModel
    ├── tunnel/
    │   ├── TunnelScreen
    │   └── TunnelViewModel
    └── settings/
        ├── SettingsScreen
        └── SettingsViewModel
```

---

## License

Apache License 2.0
