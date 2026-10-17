# Localhost for Android

Localhost is a local development server platform and runtime environment designed for Android devices. It enables developers to host web applications, test backend code, manage isolated environments, inspect network traffic, and access services locally or over the internet via secure tunnels.

---

## Core Features

- Multi-Runtime Support: Native execution for Python (Flask, FastAPI, venv, pip), Node.js (Express, npm), PHP with Composer, Java OpenJDK 17 (.jar and source code execution), and static HTML, CSS, JS web servers.
- Project Controls: Reorganized control center featuring Server Stop, Restart, and Start controls at the top, Cloudflare Tunnel with custom port configuration, LAN Port Forwarding with port prompt, VPN Visits access control, and Delete Project at the bottom in red.
- Project Creation and Import: Template scaffolding, ZIP archive import, Storage Access Framework device file import, direct file sharing from external apps, and public GitHub repository URL cloning (`https://github.com/user/repo`).
- Configurations Management: Dedicated Configurations tab replacing packages view, featuring entrypoint priority sequencing (app.py default first, followed by main.py, fully configurable), live project root .env file reader and editor with auto-injection, older Python version installer, requirements freezer (`pip freeze > requirements.txt`), and fresh isolated .venv generation.
- Real-Time Syntax Validation: In-editor syntax validation engine with status badge and inline diagnostics for JSON (trailing comma detection and bracket checks), Python (unmatched brackets, unclosed quotes, missing colons), PHP (opening tags, braces), JavaScript, TypeScript, HTML, XML, and .env files.
- Scoped File Explorer: Breadcrumb navigation, scroll position preservation between editor transitions, unsaved changes confirmation dialog (Save and Exit, Discard, Cancel), context menu actions (Rename, Details, Share, Download, Cut, Copy, Delete), and rendered Markdown preview.
- Terminal and Logs: Reordered tabs with Terminal followed by Log. Persistent command history across sessions, quick accessory keyboard keys (`Tab`, `Ctrl+C`, `clear`, `ls -la`, `pwd`, `pip list`), detailed HTTP request logging (client IP, method, path, color-coded status codes, and duration), and one-tap log export via Android Share Sheet.
- Real-Time Server Analytics: Linux `/proc` CPU utilization sampler, memory usage metrics, disk storage quota tracking (250MB threshold), and request breakdown by HTTP method and status code ranges.
- Local Nginx HTTPS Proxy: Embedded TLS reverse proxy with automatic self-signed certificate generation to access local servers securely over HTTPS (`https://localhost:8443` and `https://<LAN_IP>:8443`).
- Cloudflare Named Tunnels: Cloudflared named tunnel integration with encrypted persistent token storage, token history dropdown selection, and instant activation.
- System and Performance Settings: CPU Wake Lock toggle to prevent processor sleep during active server sessions, direct button launcher for Android system battery optimization exemption settings, and interactive theme accent color customizer with live color wheel.
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
./gradlew assembleFullDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/full/debug/app-full-debug.apk`
or in the project root as `Localhost.apk`.

---

## Usage Guide

### 1. Creating and Managing Projects

- From Template: Tap `+` on the Projects screen, enter a project name, select a runtime (Python, Node.js, PHP, Java, Static), specify the entry file and port, and tap Create.
- From GitHub: Tap the download icon in the top bar, enter any public repository URL (`https://github.com/owner/repo`), and Localhost clones and configures the workspace automatically.
- From ZIP or Device File: Use the import action to load files or archives directly into the active project.
- Exiting Editor: If modifications are detected when exiting the editor, a confirmation prompt appears asking to Save and Exit, Discard, or Cancel.

### 2. Configurations and Environments

- Entrypoint Priority: Specify execution order for entrypoint scripts (for Python, defaults to app.py first, then main.py).
- Environment Variables: Edit the .env file directly from the Configurations tab. Values are automatically loaded and injected into process runtime environments.
- Python Virtual Environments: Click Regenerate Fresh .venv to build an isolated virtual environment in the project root directory. Use Freeze Requirements to write current dependencies to requirements.txt.
- PHP Composer: Manage dependencies directly with composer.phar support via the in-app terminal or package manager.

### 3. Tunnels and Control Panel

- Controls Ordering: Use the Control tab to access lifecycle operations:
  1. Stop, Restart, and Start buttons at the top.
  2. Cloudflare Quick Tunnel with custom port prompt.
  3. LAN Port Forwarding with port input.
  4. VPN Visits toggle.
  5. Delete Project button at the bottom.

### 4. Terminal and Logs

- Access the combined Terminal and Log tab from the project details screen.
- Persistent Terminal: Command inputs and terminal output persist across navigation changes.
- Log Filtering: Filter logs by STDOUT, STDERR, or SYSTEM streams, or search log text without vertical clipping.
- HTTP Request Tracking: Incoming requests are recorded with timestamp, client IP, method, status code, and latency in milliseconds.
- Export Logs: Use the Share button to export complete log outputs to text files or external applications.

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
│   │   └── TunnelModels
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
    │   ├── ProjectsViewModel
    │   └── database/
    │       ├── DatabaseScreen
    │       └── DatabaseViewModel
    ├── logs/
    │   ├── LogViewerScreen
    │   └── LogsViewModel
    ├── files/
    │   ├── FileExplorerScreen
    │   ├── SyntaxValidator
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

## Changes in Version 1.0.5

- Interactive Database CLI Terminal: Added a full-featured interactive MariaDB CLI terminal tab supporting standard SQL commands (`SHOW DATABASES;`, `SHOW TABLES;`, `DESCRIBE <table>;`, `SELECT ...;`, `USE <db>;`, `STATUS;`, `clear`, `help`). Features clean ASCII grid-table formatting for query results (`+----+----+`) with timing and rows affected, command history navigation with `↑` and `↓` buttons, quick accessory keys, and click-to-copy output.
- Harmonized Database Controls & Dashboard UI: Redesigned the Database Controls & Settings dashboard using 16.dp rounded surface cards matching the Project Details Control tab. Features dynamic process status chip badges (`ONLINE (: 3306)` / `STOPPED`), large process control buttons, structured connection settings with toggleable password visibility, live telemetry metric tiles (Databases, Users, Port), and a dedicated Danger Zone card.
- Database Explorer & Visualizer Auto-Refresh & 1-Tap Recovery: Automatic reload of database lists, table inventories, schemas, and live rows on tab switch with persistent Refresh buttons. Added 1-tap "Start Service" notification banners across Explorer, Visualize, and Terminal tabs whenever MariaDB is stopped.
- MariaDB Service Health Check & Stale Socket Fix: Replaced dead `.sock` file checks with reliable TCP `127.0.0.1:3306` socket health checks dispatched to background IO threads. Automatically purges stale `mariadb.sock` files before daemon startup and upon shutdown, preventing false "online" UI states and deadlock loops.
- Resilient Query Execution & Dual Socket/TCP Fallback: Updated the internal Python query runner (`db_query.py`) to attempt Unix domain socket connections first and gracefully fall back to TCP `127.0.0.1:3306`.
- Starter Database Auto-Seeding (`cats_db`): Automatically seeds a relational starter database with sample tables (`owners`, `cats`) and foreign keys on first daemon boot, allowing instant ER diagram inspection and foreign key relation line visualization in the Visualize tab.
- Symlink-Aware Project Deletion: Fixed an issue where projects with broken symlinks inside Python virtual environments (`.venv/bin/`) failed recursive file deletion and caused deleted projects to auto-resurrect as Static HTML projects. Upgraded file tree deletion to use `LinkOption.NOFOLLOW_LINKS` and updated filesystem synchronization to ignore hidden residue.

## Changes in Version 1.0.4

- Shared File Project Creation with Stack & Port Configuration: Seamless project creation when sharing ZIP archives or snippets from external applications (WhatsApp, file managers, browsers). Users can customize Project Name, select the desired Runtime Stack (Python, Node.js, PHP, Static, Java), and specify a custom Port number, with automatic `.venv` configuration for Python projects.
- Database Studio Branding & Streamlining: Renamed the navigation item and screen header to "Database", eliminated redundant status indicator dot before the MariaDB Server banner, and cleaned up interface headers.
- Explorer & Visualize Dropdown Synchronization: Fixed non-responsive and empty table/database dropdowns with auto-refresh on tab switch, dynamic database selection, scoped `SHOW TABLES FROM \`db\`` and `DESCRIBE \`db\`.\`tbl\`` queries, and resilient socket binding.
- Auto-Refreshing Database Schemas: Switching databases instantly clears previous table selections and re-queries schemas and table contents without requiring manual navigation reloads.

---

## Changes in Version 1.0.3

- Complete Segregation of Port Forwarding & Cloudflare Named Tunnels: Completely isolated states, controls, and inputs between quick HTTPS port forwarding and Cloudflare Zero Trust named token tunnels. Public port forwarding never touches, populates, or resets the Cloudflare token input field or its connection status.
- Independent Concurrent Tunnels & Auto-Reconnect: Both Port Forwarding and Cloudflare Named Tunnels can run concurrently without mutual cancellation. Added automatic reconnection resilience to restore dropped sessions if temporary network changes occur.
- Carrier Network HTTP/2 Transport: Configured standard HTTP/2 transport across all Cloudflare tunnels to prevent aggressive UDP/QUIC packet filtering and drops on mobile networks.
- Full MariaDB Native Integration: Complete MariaDB client and manager supporting table creation, interactive data viewer, full-fidelity SQL query console, and SQL/CSV import & export.
- Runtime Package Manager Streamlining: Cleaned up installed package cards in Settings by removing redundant status checkmarks and keeping simple one-tap uninstall buttons.
- Process Lifecycle & Dynamic Library Fixes: Added MariaDB native shared library directory (runtimes/mariadb/lib) to LD_LIBRARY_PATH and eliminated process detachment crashes.
- Dynamic Supervisor Service Notification: Foreground service notification dynamically tracks active Web Servers, Port Forwarding, Cloudflare Named Tunnels, and Nginx Proxy states independently.

---

## Changes in Version 1.0.2

- VS Code Style Worldwide Port Forwarding: Allows specifying any local port to tunnel directly to a public worldwide HTTPS URL via Cloudflare Quick Tunnel, accessible from anywhere on the internet, featuring instant link copying, browser launch, and QR code sharing.
- Database Services Default to Stopped: Initial database service state for both MySQL and PostgreSQL defaults to stopped instead of automatically running.
- Offline Connection Testing Guard: Test Connection action is disabled and rejected when the database service is stopped, preventing false test attempts against inactive services.
- Full Builtin SQL Function Support: Expanded recursive SQL expression evaluator supporting standard built-in functions for MySQL and PostgreSQL, including REPEAT, REPLACE, CHAR, CHR, CONCAT, UPPER, LOWER, LENGTH, CHAR_LENGTH, TRIM, LTRIM, RTRIM, REVERSE, LEFT, RIGHT, LPAD, RPAD, SUBSTRING, MD5, HEX, UNHEX, ROUND, FLOOR, CEIL, ABS, MOD, POW, SQRT, PI, RAND, COALESCE, IFNULL, NULLIF, NOW, CURRENT_TIMESTAMP, CURDATE, CURRENT_DATE, CURTIME, CURRENT_TIME, VERSION, DATABASE, and USER.
- Empty Default Query Input: Query Console input field starts empty without hardcoded pre-filled queries.
- Instant User Management State Updates: Immediately reflects newly created users, password updates, and privilege changes in the UI without requiring tab switching.
- Dedicated Database Activity Logs: Added Logs tab in Database Studio displaying real-time timestamped event logs for service status changes, connection tests, query executions, and user modifications, with copy and clear actions.
- Text Editor Valid Badge Removal: Removed the redundant valid tick badge from the editor header, displaying badges only when syntax errors or warnings are detected.
- Text Editor Scroll and Focus Stability: Eliminated scroll-to-top behavior when the virtual keyboard opens, and added automatic viewport repositioning to keep the cursor in view when typing after scrolling.
- System Navigation Bar Insets Compliance: Fixed layout occlusion by ensuring all non-bottom-bar destinations and full-screen views respect Android 3-button system navigation bars (Back, Home, Recents).
- High Frequency Theme Color Picker Updates: Throttled color wheel and shade slider updates to 0.05 seconds (50ms) for real-time responsiveness.
- Sample Project Scaffold: Created Python database test project with .env configuration, supporting both MySQL and PostgreSQL modes with 5 sample cat records and dynamic record creation.

---

## Changes in Version 1.0.1

- Dual Independent Database Services: Decoupled MySQL (port 3306) and PostgreSQL (port 5432) into distinct, independently controlled services with individual connection status, port binding, and service switcher tabs.
- Real-Time Connection Verification: Connect action performs socket connection verification to the target host and port before establishing an active session, displaying descriptive feedback on connection failure.
- Non-Persistent Connection Testing: Test Connection action verifies reachability without modifying or persisting active connection state.
- Default Root User and Empty Password: Default user configured to root with empty password credentials for both MySQL and PostgreSQL engines.
- Database User Management: Added user administration actions including Change Password and Delete User directly from the Users and Permissions management tab.
- Interactive Database Schema Visualizer: Added Visualize tab rendering interactive schema diagrams with database root nodes, table entity cards displaying column names, data types, and primary keys, and foreign key relationships connected with distinct orange connector lines in a non-overlapping staggered layout.
- Export Database: Renamed Export SQL tab to Export Database, generating full database exports (.sql) with table schemas and optional record data, along with direct database SQL import.
- Database Studio Compact Header: Collapsible credentials bar saving vertical screen real estate for Explorer, Visualize, and Query Console tabs, with quick Test and Connect/Disconnect controls.
- Port Forwarding Active Links: Live HTTPS reverse proxy and direct LAN HTTP URL display with Copy, Open in Browser, and Stop Forwarding actions directly inside the project Control tab.
- Database Query Console and Resilience: Added quick query chips, detailed query execution error cards, and case-insensitive table querying with pre-populated PostgreSQL tables.
- Default Root User Privileges: Enforced ALL PRIVILEGES by default for the root user across both MySQL and PostgreSQL engines.
- PHP Runtime Execution: Non-blocking CLI runner preventing Android seccomp sandbox crashes on imported PHP projects.
- SQL Dump Import: Added SQL import action allowing selection and execution of .sql dump files directly into existing databases.
- Automatic Python Virtual Environment Scaffolding: Automatically scaffolds an isolated .venv environment with pyvenv.cfg, activation scripts, and site-packages on project creation, ZIP archive extraction, GitHub repository clone, and external share imports.
- Project Archive Export Filtering: Automatically excludes .venv directories from exported project ZIP archives and detects .env files, displaying a user prompt to exclude sensitive environment variables (recommended) or include them.
- File Editor First Keystroke Scroll Fix: Resolved first typing scroll-to-top bug by preventing editor text buffer state resets during active user input.
- Light Mode Input Field Contrast: Fixed white text rendering in light mode input fields by using adaptive text colors across text fields.
- Storage Quota Slider Lower Bound: Enforced minimum storage quota slider value of at least current disk usage plus 5 MB.
- Unsaved Changes Navigation Confirmation: Added confirmation prompts when navigating away from modified storage quota, entrypoints, and settings panels.
- Hex Picker and Shade Slider Decoupling: Refactored color picker wheel canvas to prevent pixelation under dark shades and decoupled shade slider adjustments from hue calculations.
- Contextual Foreground Service Notification: Replaced static standby notification with dynamic listing of running servers, Cloudflare tunnels, and port forwards; automatically removes notification when all services are stopped.
- Process Log Filter Field Padding: Added horizontal and vertical padding around the log filter input field matching terminal accessory bar spacing.
- Circular Project Logo: Top-left navigation branding and splash screen app logos styled as a circular icon.
- Two-Step Project Creation Workflow: Project creation split into two steps: runtime stack selection first, followed by source selection (Empty, ZIP archive, or Git clone) with strict project name validation enforcing uniqueness across existing projects and alphanumeric or underscore characters.
- External Share Sheet Integration: Receive files shared directly from WhatsApp or any Android app with an immediate prompt to import into an existing project or create a new project.
- File Explorer Clipboard and Folder Sharing: Full Copy, Cut, and Paste support with Paste appearing only when items are on the clipboard, directory sharing with automatic on-the-fly ZIP compression, and connected portrait mode Details and Rename dialogs.
- Interactive Line Graph Analytics: Replaced bar charts with smooth, touch-interactive line graphs with grid lines, gradient fills, and touch inspection points displaying exact timestamps and metric values.
- Reordered Control Tab: Reordered controls to place Server Process Control (Stop, Restart, Start) first, followed by LAN Port Forwarding, Cloudflare Tunnel (token input only), VPN Visits access control with customizable warning message, and Delete Project.
- Light Mode and Theme Refinements: Proper secondary palette utilization for elevated surfaces in light mode, improved placeholder text contrast, throttled color wheel updates (100ms) to eliminate lag, darkness and shade slider, and bidirectional manual hex code synchronization.
- System Telemetry RAM Metric: Physical RAM progress indicator aligned with the active theme accent instead of hardcoded success green.
- PHP Composer Download Button: Download card button in Settings standardized to match Python and Node.js download card styling.
- App About Section: Streamlined changelog highlights with clean bullet points and a link to the project repository.
- Window Insets and Layout: Resolved status bar padding in the file viewer and code editor so system status bars and camera cutouts never overlap UI controls; ensured navigation bar padding on terminal and accessory bars.
- Process Logs and Terminal: Expanded text height in log filters to prevent vertical font glyph truncation; integrated formatted timestamps [HH:mm:ss] across live logs; routed visited request logs to STDERR.
- Persistent Terminal Sessions: Terminal histories in both the main CLI and project detail terminal now persist across tab transitions and navigation changes without clearing on return.
- Real-Time Syntax Validation: Added syntax validator for JSON, Python, PHP, JavaScript, and HTML with status indicators in the editor header.
- Dedicated Language Configurations: Separated configuration options for each runtime (Python, PHP, Node.js, Java, and Static HTML) with editable main entrypoint file names (e.g. app.py, main.py, index.php, server.js).
- Python Tooling and Virtual Environments: Automatic .env file loading and saving with immediate edit and delete state reactivity; fresh .venv regeneration; Freeze Requirements modal prompting for custom output file name and path; older Python version selector.
- PHP Composer Integration: Added dark Material 3 styled Composer management panel to run composer install, update, and require package commands.
- Cloudflare Tunnel Dashboard: Display active live public URLs for named and quick tunnels directly on the project dashboard with copy and open in browser actions.
- Theme Customizer and Hex Code Inputs: Added support for 5 distinct palette roles (Primary, Secondary, Success, Fail, Warn) with direct hex code text input, interactive color wheel, quick presets, and Light, Dark, and System Default theme modes.
- Storage Quota: Added configurable slider supporting project storage quotas from 100MB up to 1GB.
- Back Gesture Fix: Improved back navigation across bottom navigation tabs and project detail tabs to ensure back gestures navigate 1 step back in chronological order.
- VPN Detection and Configurable Error Page: Added configurable VPN error message option in Project Settings under VPN control, defaulting to "VPN Detected, Please turn it off. If you think it is a mistake, contact the Developer." Flagged VPN visitors are served a responsive dark theme error page displaying the custom message, unique error code (e.g. ERR_VPN_403_XXXXXX), visitor IPv4 or IPv6 address, and server target. The exact error code and visitor IP are also recorded to the project terminal log.
- Responsive Demo Project Landing Pages: Redesigned empty starter templates for Static HTML5, Python Flask, Node.js Express, PHP, and Java to share the responsive card design, automatically adjusting to screen dimensions and presenting basic stack information (runtime version, port, host, status, and health ping endpoints).
- Database Integration (MySQL and PostgreSQL): Added dedicated Databases tab to bottom navigation providing full MySQL and PostgreSQL client integration. Includes host, port, root user, and password connection configuration with test button; Database Explorer with table schema inspector and interactive table data viewer; Query Console with multi-line SQL editor, quick template snippets, execution timing, and query results viewer; and User and Permissions Management supporting user creation, privilege grants and revokes, and user deletion.

---

## License

Apache License 2.0
