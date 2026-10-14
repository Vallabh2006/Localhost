package com.localhost.runtime.templates

import com.localhost.core.model.Project
import com.localhost.core.model.RuntimeType
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ProjectTemplate(
    val id: String,
    val name: String,
    val runtime: RuntimeType,
    val description: String,
    val defaultStartupCommand: String,
    val defaultFiles: Map<String, String>
)

@Singleton
class TemplateProvider @Inject constructor() {

    fun getTemplates(): List<ProjectTemplate> {
        return listOf(
            ProjectTemplate(
                id = "static-site",
                name = "Modern Static Web App",
                runtime = RuntimeType.STATIC,
                description = "Ultra-fast static web app featuring sleek responsive design, live clock, and device status dashboard.",
                defaultStartupCommand = "builtin",
                defaultFiles = mapOf(
                    "index.html" to """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{PROJECT_NAME}} • Localhost</title>
    <link rel="stylesheet" href="style.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
</head>
<body>
    <div class="card">
        <div class="header">
            <div class="logo-circle">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="#5A75E0" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M2 17L12 22L22 17" stroke="#5A75E0" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M2 12L12 17L22 12" stroke="#5A75E0" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </div>
            <div>
                <h1>{{PROJECT_NAME}}</h1>
                <span class="badge">Running directly on Android</span>
            </div>
        </div>

        <p class="desc">Your static application is deployed and serving requests locally via Android native web server.</p>

        <div class="grid">
            <div class="box">
                <span class="box-label">Port</span>
                <span class="box-val">:{{PORT}}</span>
            </div>
            <div class="box">
                <span class="box-label">Runtime</span>
                <span class="box-val">Static HTML/JS</span>
            </div>
            <div class="box">
                <span class="box-label">Status</span>
                <span class="box-val status-online"> Online</span>
            </div>
            <div class="box">
                <span class="box-label">Local Time</span>
                <span class="box-val" id="time-val">--:--:--</span>
            </div>
        </div>

        <div class="footer">
            <span>Powered by <strong>Localhost</strong> Mobile VPS</span>
        </div>
    </div>
    <script src="script.js"></script>
</body>
</html>
""".trimIndent(),
                    "style.css" to """
:root {
    --bg: #0c0e14;
    --surface: #141720;
    --surface-elevated: #1b1f2c;
    --border: #252a3a;
    --primary: #5A75E0;
    --primary-light: #758de8;
    --primary-container: rgba(90, 117, 224, 0.12);
    --text: #f3f4f8;
    --text-muted: #646e85;
    --status-green: #10b981;
}

* { box-sizing: border-box; margin: 0; padding: 0; }
body {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    background-color: var(--bg);
    color: var(--text);
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    padding: 1.5rem;
}

.card {
    max-width: 520px;
    width: 100%;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 16px;
    padding: 2rem;
    box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4);
}

.header {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-bottom: 1.25rem;
}

.logo-circle {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    background: var(--primary-container);
    border: 1px solid rgba(90, 117, 224, 0.3);
    display: flex;
    align-items: center;
    justify-content: center;
}

h1 {
    font-size: 1.35rem;
    font-weight: 700;
    color: var(--text);
    letter-spacing: -0.02em;
}

.badge {
    display: inline-block;
    background: rgba(16, 185, 129, 0.12);
    color: var(--status-green);
    border: 1px solid rgba(16, 185, 129, 0.3);
    padding: 0.2rem 0.6rem;
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 600;
    margin-top: 0.25rem;
}

.desc {
    color: #9aa3b8;
    font-size: 0.9rem;
    line-height: 1.5;
    margin-bottom: 1.5rem;
}

.grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.85rem;
    margin-bottom: 1.5rem;
}

.box {
    background: var(--surface-elevated);
    border: 1px solid var(--border);
    border-radius: 10px;
    padding: 0.85rem 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
}

.box-label {
    font-size: 0.72rem;
    font-weight: 600;
    text-transform: uppercase;
    color: var(--text-muted);
    letter-spacing: 0.05em;
}

.box-val {
    font-family: 'JetBrains Mono', monospace;
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--primary);
}

.status-online {
    color: var(--status-green);
}

.footer {
    padding-top: 1rem;
    border-top: 1px solid var(--border);
    text-align: center;
    font-size: 0.8rem;
    color: var(--text-muted);
}
""".trimIndent(),
                    "script.js" to """
function updateClock() {
    const el = document.getElementById('time-val');
    if (el) el.innerText = new Date().toLocaleTimeString();
}
setInterval(updateClock, 1000);
updateClock();
""".trimIndent()
                )
            ),
            ProjectTemplate(
                id = "flask-basic",
                name = "Flask Web API",
                runtime = RuntimeType.PYTHON,
                description = "Minimal Python Flask REST API server with routing and JSON response.",
                defaultStartupCommand = "python3 app.py",
                defaultFiles = mapOf(
                    "app.py" to """
from flask import Flask, jsonify
import os

app = Flask(__name__)
port = int(os.environ.get("PORT", {{PORT}}))

HTML = '''<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{PROJECT_NAME}} - Python Flask</title>
    <style>
        :root {
            --bg: #0c0e14;
            --surface: #141720;
            --surface-elevated: #1b1f2c;
            --border: #252a3a;
            --primary: #3b82f6;
            --success: #10b981;
            --text: #f3f4f8;
            --text-muted: #646e85;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background: var(--bg);
            color: var(--text);
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 1.25rem;
        }
        .card {
            max-width: 520px;
            width: 100%;
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 16px;
            padding: 2rem;
            box-shadow: 0 16px 40px rgba(0,0,0,0.4);
        }
        @media (max-width: 480px) { .card { padding: 1.5rem 1.25rem; } }
        h1 { font-size: 1.35rem; font-weight: 700; margin-bottom: 0.35rem; }
        .badge {
            display: inline-block;
            background: rgba(16, 185, 129, 0.15);
            color: var(--success);
            border: 1px solid rgba(16, 185, 129, 0.3);
            padding: 0.2rem 0.6rem;
            border-radius: 9999px;
            font-size: 0.72rem;
            font-weight: 700;
            text-transform: uppercase;
            margin-bottom: 1.25rem;
        }
        .desc {
            background: var(--surface-elevated);
            border: 1px solid var(--border);
            border-left: 3px solid var(--primary);
            border-radius: 10px;
            padding: 1rem;
            font-size: 0.92rem;
            line-height: 1.5;
            margin-bottom: 1.25rem;
        }
        .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; margin-bottom: 1.25rem; }
        @media (max-width: 440px) { .grid { grid-template-columns: 1fr; } }
        .box { background: var(--surface-elevated); padding: 0.8rem 1rem; border-radius: 10px; border: 1px solid var(--border); }
        .label { color: var(--text-muted); font-size: 0.72rem; text-transform: uppercase; font-weight: 600; margin-bottom: 0.25rem; }
        .val { font-family: monospace; font-size: 0.9rem; font-weight: 600; }
        .btn { display: block; width: 100%; padding: 0.8rem; background: var(--primary); color: #fff; font-weight: 600; font-size: 0.9rem; border: none; border-radius: 10px; text-align: center; text-decoration: none; }
        .footer { margin-top: 1.25rem; text-align: center; font-size: 0.75rem; color: var(--text-muted); }
    </style>
</head>
<body>
    <div class="card">
        <h1>{{PROJECT_NAME}}</h1>
        <div class="badge">Python Flask - Online</div>
        <div class="desc">Your Python Flask service is running and serving requests directly on Android via Localhost.</div>
        <div class="grid">
            <div class="box"><div class="label">Runtime</div><div class="val">Python / Flask</div></div>
            <div class="box"><div class="label">Port</div><div class="val">:{{PORT}}</div></div>
            <div class="box"><div class="label">Host</div><div class="val">0.0.0.0</div></div>
            <div class="box"><div class="label">Status</div><div class="val" style="color:var(--success);">Online</div></div>
        </div>
        <a class="btn" href="/api/ping">Test API Endpoint (/api/ping)</a>
        <div class="footer">Localhost Mobile VPS - Android Protected Host</div>
    </div>
</body>
</html>'''

@app.route("/")
def home():
    return HTML

@app.route("/api/ping", methods=["GET"])
def ping():
    return jsonify({"status": "online", "runtime": "Python", "framework": "Flask", "port": port})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=port)
""".trimIndent(),
                    "requirements.txt" to "flask>=3.0.0\n"
                )
            ),
            ProjectTemplate(
                id = "express-basic",
                name = "Express.js REST API",
                runtime = RuntimeType.NODEJS,
                description = "Lightweight Node.js Express server with JSON endpoints.",
                defaultStartupCommand = "node server.js",
                defaultFiles = mapOf(
                    "server.js" to """
const express = require('express');
const app = express();
const port = process.env.PORT || {{PORT}};

const HTML = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{PROJECT_NAME}} - Node.js Express</title>
    <style>
        :root {
            --bg: #0c0e14;
            --surface: #141720;
            --surface-elevated: #1b1f2c;
            --border: #252a3a;
            --primary: #22c55e;
            --success: #10b981;
            --text: #f3f4f8;
            --text-muted: #646e85;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background: var(--bg);
            color: var(--text);
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 1.25rem;
        }
        .card {
            max-width: 520px;
            width: 100%;
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 16px;
            padding: 2rem;
            box-shadow: 0 16px 40px rgba(0,0,0,0.4);
        }
        @media (max-width: 480px) { .card { padding: 1.5rem 1.25rem; } }
        h1 { font-size: 1.35rem; font-weight: 700; margin-bottom: 0.35rem; }
        .badge {
            display: inline-block;
            background: rgba(16, 185, 129, 0.15);
            color: var(--success);
            border: 1px solid rgba(16, 185, 129, 0.3);
            padding: 0.2rem 0.6rem;
            border-radius: 9999px;
            font-size: 0.72rem;
            font-weight: 700;
            text-transform: uppercase;
            margin-bottom: 1.25rem;
        }
        .desc {
            background: var(--surface-elevated);
            border: 1px solid var(--border);
            border-left: 3px solid var(--primary);
            border-radius: 10px;
            padding: 1rem;
            font-size: 0.92rem;
            line-height: 1.5;
            margin-bottom: 1.25rem;
        }
        .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; margin-bottom: 1.25rem; }
        @media (max-width: 440px) { .grid { grid-template-columns: 1fr; } }
        .box { background: var(--surface-elevated); padding: 0.8rem 1rem; border-radius: 10px; border: 1px solid var(--border); }
        .label { color: var(--text-muted); font-size: 0.72rem; text-transform: uppercase; font-weight: 600; margin-bottom: 0.25rem; }
        .val { font-family: monospace; font-size: 0.9rem; font-weight: 600; }
        .btn { display: block; width: 100%; padding: 0.8rem; background: var(--primary); color: #fff; font-weight: 600; font-size: 0.9rem; border: none; border-radius: 10px; text-align: center; text-decoration: none; }
        .footer { margin-top: 1.25rem; text-align: center; font-size: 0.75rem; color: var(--text-muted); }
    </style>
</head>
<body>
    <div class="card">
        <h1>{{PROJECT_NAME}}</h1>
        <div class="badge">Node.js Express - Online</div>
        <div class="desc">Your Node.js Express server is running and serving traffic directly on Android via Localhost.</div>
        <div class="grid">
            <div class="box"><div class="label">Runtime</div><div class="val">Node.js / Express</div></div>
            <div class="box"><div class="label">Port</div><div class="val">:${'$'}{port}</div></div>
            <div class="box"><div class="label">Host</div><div class="val">0.0.0.0</div></div>
            <div class="box"><div class="label">Status</div><div class="val" style="color:var(--success);">Online</div></div>
        </div>
        <a class="btn" href="/api/health">Test Health Endpoint (/api/health)</a>
        <div class="footer">Localhost Mobile VPS - Android Protected Host</div>
    </div>
</body>
</html>`;

app.get('/', (req, res) => {
    res.send(HTML);
});

app.get('/api/health', (req, res) => {
    res.json({ status: 'online', uptime: process.uptime(), runtime: 'Node.js', framework: 'Express', port: port });
});

app.listen(port, '0.0.0.0', () => {
    console.log(`Server listening on 0.0.0.0:${'$'}{port}`);
});
""".trimIndent(),
                    "package.json" to """
{
  "name": "{{PROJECT_NAME}}",
  "version": "1.0.0",
  "main": "server.js",
  "scripts": {
    "start": "node server.js"
  },
  "dependencies": {
    "express": "^4.19.2"
  }
}
""".trimIndent()
                )
            ),
            ProjectTemplate(
                id = "php-basic",
                name = "PHP Demo App",
                runtime = RuntimeType.PHP,
                description = "PHP web application running with native PHP server engine (XAMPP-like SAPI).",
                defaultStartupCommand = "builtin",
                defaultFiles = mapOf(
                    "index.php" to """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{PROJECT_NAME}} • PHP Server</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #0c0e14;
            --surface: #141720;
            --surface-elevated: #1b1f2c;
            --border: #252a3a;
            --primary: #5A75E0;
            --text: #f3f4f8;
            --text-muted: #646e85;
            --status-green: #10b981;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
            background: var(--bg);
            color: var(--text);
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 1.5rem;
        }
        .card {
            max-width: 540px;
            width: 100%;
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 16px;
            padding: 2rem;
            box-shadow: 0 16px 40px rgba(0,0,0,0.4);
        }
        h1 { font-size: 1.4rem; color: var(--text); margin-bottom: 0.4rem; font-weight: 700; letter-spacing: -0.02em; }
        .badge {
            display: inline-block;
            background: rgba(16, 185, 129, 0.12);
            color: var(--status-green);
            border: 1px solid rgba(16, 185, 129, 0.3);
            padding: 0.2rem 0.6rem;
            border-radius: 9999px;
            font-size: 0.75rem;
            font-weight: 600;
            margin-bottom: 1.5rem;
        }
        .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.85rem; margin-bottom: 1.25rem; }
        .box {
            background: var(--surface-elevated);
            padding: 0.85rem 1rem;
            border-radius: 10px;
            border: 1px solid var(--border);
        }
        .label { color: var(--text-muted); font-size: 0.72rem; text-transform: uppercase; font-weight: 600; margin-bottom: 0.25rem; }
        .val { font-family: 'JetBrains Mono', monospace; color: var(--primary); font-weight: 600; font-size: 0.95rem; }
        .console {
            background: #0a0c10;
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 1rem;
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.85rem;
            color: #d1d5db;
            line-height: 1.6;
        }
    </style>
</head>
<body>
    <div class="card">
        <h1>{{PROJECT_NAME}}</h1>
        <div class="badge">PHP Server Running on Android</div>
        <div class="grid">
            <div class="box"><div class="label">PHP Version</div><div class="val"><?php echo phpversion(); ?></div></div>
            <div class="box"><div class="label">Server Software</div><div class="val"><?php echo ${'$'}_SERVER['SERVER_SOFTWARE'] ?? 'Localhost Engine'; ?></div></div>
            <div class="box"><div class="label">Server Port</div><div class="val">{{PORT}}</div></div>
            <div class="box"><div class="label">Host Time</div><div class="val"><?php echo date('H:i:s T'); ?></div></div>
        </div>
        <div class="console">
            <?php
                echo "Status: Online\n";
                echo "Environment: Android Mobile VPS\n";
                echo "Date: " . date('Y-m-d') . "\n";
            ?>
        </div>
    </div>
</body>
</html>
""".trimIndent()
                )
            ),
            ProjectTemplate(
                id = "java-basic",
                name = "Java HTTP Server",
                runtime = RuntimeType.JAVA,
                description = "Built-in lightweight Java server executing executable JARs.",
                defaultStartupCommand = "java -jar app.jar",
                defaultFiles = mapOf(
                    "App.java" to """
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class App {
    public static void main(String[] args) throws IOException {
        int port = {{PORT}};
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        final String html = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>{{PROJECT_NAME}} - Java Server</title>\n" +
            "    <style>\n" +
            "        :root { --bg: #0c0e14; --surface: #141720; --surface-elevated: #1b1f2c; --border: #252a3a; --primary: #f59e0b; --success: #10b981; --text: #f3f4f8; --text-muted: #646e85; }\n" +
            "        * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
            "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: var(--bg); color: var(--text); display: flex; justify-content: center; align-items: center; min-height: 100vh; padding: 1.25rem; }\n" +
            "        .card { max-width: 520px; width: 100%; background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 2rem; box-shadow: 0 16px 40px rgba(0,0,0,0.4); }\n" +
            "        @media (max-width: 480px) { .card { padding: 1.5rem 1.25rem; } }\n" +
            "        h1 { font-size: 1.35rem; font-weight: 700; margin-bottom: 0.35rem; }\n" +
            "        .badge { display: inline-block; background: rgba(16, 185, 129, 0.15); color: var(--success); border: 1px solid rgba(16, 185, 129, 0.3); padding: 0.2rem 0.6rem; border-radius: 9999px; font-size: 0.72rem; font-weight: 700; text-transform: uppercase; margin-bottom: 1.25rem; }\n" +
            "        .desc { background: var(--surface-elevated); border: 1px solid var(--border); border-left: 3px solid var(--primary); border-radius: 10px; padding: 1rem; font-size: 0.92rem; line-height: 1.5; margin-bottom: 1.25rem; }\n" +
            "        .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; margin-bottom: 1.25rem; }\n" +
            "        @media (max-width: 440px) { .grid { grid-template-columns: 1fr; } }\n" +
            "        .box { background: var(--surface-elevated); padding: 0.8rem 1rem; border-radius: 10px; border: 1px solid var(--border); }\n" +
            "        .label { color: var(--text-muted); font-size: 0.72rem; text-transform: uppercase; font-weight: 600; margin-bottom: 0.25rem; }\n" +
            "        .val { font-family: monospace; font-size: 0.9rem; font-weight: 600; }\n" +
            "        .btn { display: block; width: 100%; padding: 0.8rem; background: var(--primary); color: #000; font-weight: 700; font-size: 0.9rem; border: none; border-radius: 10px; text-align: center; text-decoration: none; cursor: pointer; }\n" +
            "        .footer { margin-top: 1.25rem; text-align: center; font-size: 0.75rem; color: var(--text-muted); }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"card\">\n" +
            "        <h1>{{PROJECT_NAME}}</h1>\n" +
            "        <div class=\"badge\">Java Server - Online</div>\n" +
            "        <div class=\"desc\">Your Java HTTP server is active and serving requests directly on Android.</div>\n" +
            "        <div class=\"grid\">\n" +
            "            <div class=\"box\"><div class=\"label\">Runtime</div><div class=\"val\">Java " + System.getProperty("java.version") + "</div></div>\n" +
            "            <div class=\"box\"><div class=\"label\">Port</div><div class=\"val\">:" + port + "</div></div>\n" +
            "            <div class=\"box\"><div class=\"label\">Host</div><div class=\"val\">0.0.0.0</div></div>\n" +
            "            <div class=\"box\"><div class=\"label\">Status</div><div class=\"val\" style=\"color:var(--success);\">Online</div></div>\n" +
            "        </div>\n" +
            "        <button class=\"btn\" onclick=\"window.location.reload()\">Reload Application</button>\n" +
            "        <div class=\"footer\">Localhost Mobile VPS - Android Protected Host</div>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>";

        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] bytes = html.getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Java server started on port " + port);
    }
}
""".trimIndent(),
                    "README.md" to "# {{PROJECT_NAME}}\nJava HTTP server running on port {{PORT}}.\n\nTo compile and run:\n`javac App.java && java App`\n"
                )
            )
        )
    }

    fun scaffold(template: ProjectTemplate, targetDir: File, project: Project) {
        if (!targetDir.exists()) targetDir.mkdirs()
        template.defaultFiles.forEach { (fileName, content) ->
            val file = File(targetDir, fileName)
            file.parentFile?.mkdirs()
            val replacedContent = content
                .replace("{{PROJECT_NAME}}", project.name)
                .replace("{{PORT}}", project.port.toString())
            file.writeText(replacedContent)
        }
    }
}
