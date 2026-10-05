
# Localhost

Localhost transforms an Android device into a personal cloud and server hosting platform. Host web applications, REST APIs, microservices, and static sites written in Python (Flask), Node.js (Express), PHP, Static HTML/JS/CSS, and Java directly from your phone. Manage projects through the native Material 3 Android UI, an embedded Web Dashboard, or a built-in CLI terminal, and expose services to the public internet securely using Cloudflare Tunnel.

---

## Key Features

- Multi-Language Runtimes: Run Python (Flask), Node.js (Express), PHP, Static websites, and Java directly on Android.
- Pip Dependencies from Requirements: Validate and install dependencies from `requirements.txt` (or custom `.txt` files) with automatic file detection and error handling for invalid requirements files.
- Live Server QR Code and Copyable Links: When servers are online, generate dynamic QR codes for instant mobile/desktop LAN scanning, alongside 1-tap clipboard copying for local LAN (`http://<IP>:<PORT>`) and loopback (`http://127.0.0.1:<PORT>`) URLs.
- ZIP and Archive Import: Import pre-built projects from `.zip` or `.tar.gz` archives with automatic runtime detection (PHP, Node.js, Python, Java, Static) and port conflict resolution.
- Scoped File Explorer and In-App Editor: Browse project directory trees, edit source code, preview assets, and extract archives in place.
- Isolated Process Supervisor: Android Foreground Service with process tracking, crash loop protection, socket cleanup, and automatic engine fallback.
- Cloudflare Tunnel Integration: Expose local servers over HTTPS using quick trycloudflare.com tunnels or custom domain named tunnels without router configuration or port forwarding.
- Web Management Dashboard: Embedded Ktor CIO server delivering a real-time SPA dashboard accessible over Wi-Fi/LAN or public tunnels with file editing, log streaming, and package management.
- In-App CLI Terminal: Interactive terminal shell with top input positioning, selectable/copyable output, bash navigation (`cd`, `ls`, `tree`, `cp`, `mv`, `rm`, `mkdir`, `touch`), runtime execution (`python`, `pip`, `node`, `npm`, `php`), and JSON output support.
- Environment Variable Management: Manage key-value configurations, mask secrets, and bulk-import `.env` files with automatic injection into running processes.
- Real-Time Logs and Monitoring: Selectable/copyable live stdout/stderr streams, log search, and live CPU/RAM/storage metrics.
- Background Reliability: Partial wake locks, Wi-Fi locks, battery optimization exemption handling, and boot auto-start receiver.
- Supabase Integration: Seamless connection and automatic injection of Supabase credentials into project environments.

---

## Getting Started

### Prerequisites

- Android Studio Koala / Ladybug or newer
- JDK 17 or JDK 21
- Android SDK Platform 35
- Android device or emulator running Android 8.0+ (minSdk 26)

## Usage

### 1. Creating or Importing a Project

-  **From Template**: Open Localhost, tap `+`, enter a project name, choose a runtime (Python Flask, Node.js Express, PHP, Java, Static), and tap Create.
-  **From ZIP Archive**: Tap `Choose ZIP Archive` on the Create Project screen or the folder upload icon on the Projects list. Localhost extracts your archive, detects the runtime, and configures the start command.

### 2. Installing Dependencies

- Open the project detail view and navigate to the **Packages** tab.
-  **From Requirements File (.txt)**: Enter or select a `.txt` file (e.g., `requirements.txt`). Localhost validates file existence and structure before running `pip install -r <file>`. If the file is missing or invalid, an informative error is displayed.
-  **Single Package**: Enter a package name (e.g., `requests`, `fastapi`, `dotenv`) or select from popular libraries to install immediately.

### 3. QR Code and Local Network Access

- When a server is online (`RUNNING`), the **Overview** tab displays the generated **QR Code** and copyable links for both the LAN IP (`http://<LAN_IP>:<PORT>`) and Localhost (`http://127.0.0.1:<PORT>`).
- Scan the QR code from any smartphone, tablet, or laptop on the same Wi-Fi network to open the site directly.

### 4. In-App CLI Commands

#### Navigation and File Operations

-  `pwd`: Print current working directory.
-  `cd <path>`: Change directory (e.g. `cd my-flask-app`, `cd ..`).
-  `ls [path]`: List files and subdirectories with sizes.
-  `tree [path]`: Display directory tree structure hierarchy.
-  `cat <file>`: Print file contents.
-  `mkdir <dir>`: Create new folder.
-  `touch <file>`: Create new file.
-  `cp <src> <dst>`: Copy file or folder.
-  `mv <src> <dst>`: Move or rename file or folder.
-  `rm [-r] <path>`: Delete file or directory.
-  `clear`: Clear terminal history.

#### Runtime Execution

-  `python <script.py> [args]`: Run Python scripts directly.
-  `python3 -c "<code>"`: Run one-line Python code.
-  `pip <install|list|uninstall> [pkg]`: Run pip package manager.
-  `node <script.js>` / `npm <args>`: Run Node.js and npm.
-  `php <script.php>`: Run PHP scripts.
-  `exec <binary>`: Execute binaries in current directory.

#### Project and Server Management

-  `list [--json]`: List all projects.
-  `start <name>`: Start a server.
-  `stop <name>`: Stop a server.
-  `restart <name>`: Restart a server.
-  `logs <name>`: View recent logs.
-  `create <name> <runtime> <port>`: Scaffold a new project.
-  `env [list]`: View project environment variables.
-  `env set <project> <KEY> <VALUE>`: Set environment variable.
-  `tunnel <start|stop|status>`: Manage Cloudflare tunnel.
-  `status`: Show platform status.

---

## Security

- Sensitive credentials (Cloudflare tokens, Supabase keys, dashboard passwords) are encrypted with AES-256-GCM backed by Android Keystore.
- Process execution is isolated to project working directories with sanitized environment variables.
- The web management dashboard requires authentication and binds locally by default.



## Architecture and Module Structure

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
│   │
│   ├── data/
│   │   ├── Room Database
│   │   ├── DataStore Preferences
│   │   └── Repositories
│   │
│   ├── model/
│   │   ├── Project
│   │   ├── RuntimeType
│   │   ├── LogEntry
│   │   ├── Metrics
│   │   ├── Tunnel
│   │   └── RuntimePack
│   │
│   ├── security/
│   │   ├── Keystore-backed SecretStore
│   │   └── Password Hashing
│   │
│   ├── designsystem/
│   │   ├── Material 3 Theme
│   │   ├── Components
│   │   └── Color Tokens
│   │
│   └── runtime/
│       ├── manager/
│       │   ├── Runtime Pack Discovery
│       │   ├── Runtime Downloads
│       │   ├── Pause/Resume
│       │   ├── Archive Extraction
│       │   └── Execution Environment
│       │
│       ├── process/
│       │   ├── Process Supervisor
│       │   ├── ManagedProcess Lifecycle
│       │   └── /proc Metrics Sampler
│       │
│       ├── templates/
│       │   ├── Flask
│       │   ├── Express
│       │   ├── PHP
│       │   ├── Static
│       │   └── Java
│       │
│       ├── tunnel/
│       │   ├── Cloudflared Binary Manager
│       │   ├── Quick Tunnel
│       │   └── Named Tunnel
│       │
│       ├── server-dashboard/
│       │   ├── Ktor CIO Server
│       │   ├── REST API
│       │   ├── WebSockets
│       │   └── SPA Static Asset Server
│       │
│       └── cli/
│           ├── CLI Command Parser
│           └── Terminal Shell Engine
│
└── feature/
    ├── projects/
    │   ├── Project List
    │   ├── Creation Wizard
    │   ├── Project Details
    │   ├── Environment Variable Manager
    │   └── ZIP Import
    │
    ├── logs/
    │   ├── Live Log Streaming
    │   ├── Text Selection
    │   ├── Copy to Clipboard
    │   └── Log Search
    │
    ├── files/
    │   ├── File Explorer
    │   ├── Code Editor
    │   └── Archive Extractor
    │
    ├── monitor/
    │   ├── Hardware Resource Gauges
    │   ├── Network Diagnostics
    │   └── Process Control
    │
    ├── tunnel/
    │   └── Cloudflare Tunnel Management UI
    │
    ├── settings/
    │   ├── Runtime Manager
    │   ├── Pause/Resume
    │   ├── Battery Optimization
    │   └── Preferences
    │
    └── supabase/
        └── Supabase Connection & Key Manager
```
---

## License

Apache License 2.0