const API_BASE = '/api';
let logsSocket = null;
let activeLogsProjectId = null;
let activeEditingProjectId = null;
let activeEditingFilePath = null;
let activePkgProjectId = null;
let activeEnvProjectId = null;
let lastLogText = '';

function getAuthToken() {
    return localStorage.getItem('localhost_token') || '';
}

function setAuthToken(token, username) {
    if (token) {
        localStorage.setItem('localhost_token', token);
        if (username) localStorage.setItem('localhost_user', username);
    } else {
        localStorage.removeItem('localhost_token');
        localStorage.removeItem('localhost_user');
    }
    updateAuthUI();
}

function updateAuthUI() {
    const token = getAuthToken();
    const userBadge = document.getElementById('user-badge');
    const loginBtn = document.getElementById('btn-login-modal');
    const userDisplay = document.getElementById('user-name-display');
    
    if (token) {
        userBadge.classList.remove('hidden');
        loginBtn.classList.add('hidden');
        userDisplay.textContent = localStorage.getItem('localhost_user') || 'Admin';
    } else {
        userBadge.classList.add('hidden');
        loginBtn.classList.remove('hidden');
    }
}

async function apiFetch(url, options = {}) {
    const token = getAuthToken();
    const headers = options.headers || {};
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    options.headers = headers;
    
    const res = await fetch(url, options);
    if (res.status === 401) {
        setAuthToken(null);
        showToast('Session expired. Please sign in.', 'error');
    }
    return res;
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    const icon = type === 'success' ? '' : (type === 'error' ? '' : 'ℹ');
    toast.innerHTML = `<span style="font-weight: bold;">${icon}</span> <span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

async function fetchStatus() {
    try {
        const res = await fetch(`${API_BASE}/status`);
        if (!res.ok) return;
        const data = await res.json();
        
        document.getElementById('stat-running-count').textContent = `${data.runningProjects} / ${data.totalProjects}`;
        document.getElementById('stat-device-ip').textContent = data.deviceIp || window.location.hostname;
        
        const tunnelBadge = document.getElementById('tunnel-badge');
        const tunnelText = document.getElementById('tunnel-text');
        const statTunnel = document.getElementById('stat-tunnel-url');
        
        if (data.tunnelStatus === 'CONNECTED') {
            tunnelBadge.className = 'tunnel-badge connected';
            tunnelText.textContent = 'Tunnel: Connected';
            statTunnel.innerHTML = `<a href="${escapeAttr(data.tunnelUrl)}" target="_blank" style="color: var(--status-green); text-decoration: none;">${escapeHtml(data.tunnelUrl)}</a>`;
        } else {
            tunnelBadge.className = 'tunnel-badge disconnected';
            tunnelText.textContent = `Tunnel: ${data.tunnelStatus || 'Disconnected'}`;
            statTunnel.textContent = 'None';
        }

        const vpnBadge = document.getElementById('vpn-badge');
        const vpnText = document.getElementById('vpn-text');
        if (vpnBadge && vpnText) {
            if (data.vpnActive) {
                vpnBadge.className = 'vpn-badge active';
                vpnText.textContent = 'VPN: Active';
            } else {
                vpnBadge.className = 'vpn-badge';
                vpnText.textContent = 'VPN: Inactive';
            }
        }
    } catch (_) {}
}

async function fetchProjects() {
    try {
        const res = await fetch(`${API_BASE}/projects`);
        if (!res.ok) return;
        const projects = await res.json();
        
        document.getElementById('project-count-badge').textContent = `${projects.length} Projects`;
        const list = document.getElementById('projects-list');
        
        if (projects.length === 0) {
            list.innerHTML = '<div style="color: var(--text-muted); padding: 2rem; grid-column: 1 / -1; text-align: center;">No hosted projects configured. Create one in the mobile app.</div>';
            return;
        }
        
        list.innerHTML = projects.map(p => {
            const isRunning = p.status === 'RUNNING';
            const statusClass = isRunning ? 'status-green' : (p.status === 'CRASHED' ? 'status-red' : 'status-yellow');
            const port = p.port;
            const openUrl = `http://${window.location.hostname}:${port}`;
            
            return `
                <div class="project-card ${isRunning ? 'running' : ''}">
                    <div class="project-header">
                        <div>
                            <div class="project-title">${escapeHtml(p.name)}</div>
                            <div class="project-runtime">${escapeHtml(p.runtime)}</div>
                        </div>
                        <span class="badge" style="border-color: var(--${statusClass}); color: var(--${statusClass});">
                             ${escapeHtml(p.status)}
                        </span>
                    </div>
                    
                    <div class="project-meta">
                        <span class="port-tag">:${port}</span>
                        ${isRunning ? `
                            <a href="${escapeAttr(openUrl)}" target="_blank" class="btn btn-ghost btn-sm" style="font-size: 0.75rem;">Open Site</a>
                            <button class="btn btn-ghost btn-sm" onclick="copyLocalLink('${escapeAttr(openUrl)}')" style="font-size: 0.75rem;" title="Copy Local Link">Copy Link</button>
                            <button class="btn btn-ghost btn-sm" onclick="openQrModal('${escapeAttr(p.id)}', '${escapeAttr(p.name)}', '${escapeAttr(openUrl)}')" style="font-size: 0.75rem;" title="Show QR Code">QR Code</button>
                        ` : ''}
                    </div>
                    
                    <div class="project-actions">
                        ${isRunning 
                            ? `<button class="btn btn-ghost btn-sm" onclick="stopProject('${escapeAttr(p.id)}')">Stop</button>
                               <button class="btn btn-ghost btn-sm" onclick="restartProject('${escapeAttr(p.id)}')">Restart</button>`
                            : `<button class="btn btn-primary btn-sm" onclick="startProject('${escapeAttr(p.id)}')">Start</button>`
                        }
                        <button class="btn btn-ghost btn-sm" onclick="openLogs('${escapeAttr(p.id)}', '${escapeAttr(p.name)}')">Logs</button>
                        <button class="btn btn-ghost btn-sm" onclick="openFiles('${escapeAttr(p.id)}', '${escapeAttr(p.name)}')">Files</button>
                        <button class="btn btn-ghost btn-sm" onclick="openPkgModal('${escapeAttr(p.id)}', '${escapeAttr(p.name)}', '${escapeAttr(p.runtime)}')">Packages</button>
                        <button class="btn btn-ghost btn-sm" onclick="openEnvModal('${escapeAttr(p.id)}', '${escapeAttr(p.name)}')">.env</button>
                    </div>
                </div>
            `;
        }).join('');
    } catch (_) {
        document.getElementById('projects-list').innerHTML = '<div style="color: var(--status-red); padding: 2rem;">Failed to load projects.</div>';
    }
}

async function startProject(id) {
    const res = await apiFetch(`${API_BASE}/projects/${id}/start`, { method: 'POST' });
    if (res.ok) {
        showToast('Project started successfully', 'success');
    } else {
        const d = await res.json();
        showToast(d.message || 'Failed to start project', 'error');
    }
    fetchProjects();
    fetchStatus();
}

async function stopProject(id) {
    await apiFetch(`${API_BASE}/projects/${id}/stop`, { method: 'POST' });
    showToast('Project stopped', 'info');
    fetchProjects();
    fetchStatus();
}

async function restartProject(id) {
    const res = await apiFetch(`${API_BASE}/projects/${id}/restart`, { method: 'POST' });
    if (res.ok) {
        showToast('Project restarted', 'success');
    } else {
        const d = await res.json();
        showToast(d.message || 'Restart failed', 'error');
    }
    fetchProjects();
    fetchStatus();
}

function openLogs(id, name) {
    activeLogsProjectId = id;
    document.getElementById('logs-title').textContent = `Logs - ${name}`;
    document.getElementById('logs-modal').classList.remove('hidden');
    const terminal = document.getElementById('logs-terminal');
    terminal.textContent = 'Connecting to real-time log stream...\n';
    lastLogText = '';
    
    if (logsSocket) logsSocket.close();
    
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const token = getAuthToken();
    const wsUrl = `${proto}
    
    logsSocket = new WebSocket(wsUrl);
    
    logsSocket.onmessage = (e) => {
        try {
            const logs = JSON.parse(e.data);
            terminal.innerHTML = '';
            lastLogText = '';
            logs.forEach(l => {
                const line = document.createElement('div');
                const streamClass = l.stream ? l.stream.toLowerCase() : 'stdout';
                line.className = `terminal-line ${streamClass}`;
                const time = new Date(l.timestamp).toLocaleTimeString();
                line.textContent = `[${time}] [${l.stream}] ${l.message}`;
                terminal.appendChild(line);
                lastLogText += `[${time}] [${l.stream}] ${l.message}\n`;
            });
            terminal.scrollTop = terminal.scrollHeight;
        } catch (_) {}
    };
    
    logsSocket.onerror = () => {
        terminal.textContent += '\n[System] WebSocket disconnected or requires authentication.\n';
    };
}

async function openFiles(id, name) {
    activeEditingProjectId = id;
    document.getElementById('files-title').textContent = `Files - ${name}`;
    document.getElementById('files-modal').classList.remove('hidden');
    
    const sidebar = document.getElementById('files-sidebar');
    sidebar.innerHTML = '<div style="padding: 0.5rem; color: var(--text-secondary);">Loading files...</div>';
    
    try {
        const res = await apiFetch(`${API_BASE}/projects/${id}/files`);
        const files = await res.json();
        
        sidebar.innerHTML = '';
        files.forEach(f => {
            const item = document.createElement('div');
            item.className = 'file-item';
            item.textContent = `${f.isDirectory ? '' : ''} ${f.name}`;
            item.onclick = () => loadFileContent(f.path, f.isDirectory);
            sidebar.appendChild(item);
        });
    } catch (_) {
        sidebar.innerHTML = '<div style="padding: 0.5rem; color: var(--status-red);">Failed to load files.</div>';
    }
}

async function loadFileContent(path, isDir) {
    if (isDir) return;
    activeEditingFilePath = path;
    document.getElementById('editor-filename').textContent = path;
    const textarea = document.getElementById('editor-textarea');
    const saveBtn = document.getElementById('btn-save-file');
    
    textarea.disabled = true;
    saveBtn.disabled = true;
    textarea.value = 'Loading file content...';

    try {
        const res = await apiFetch(`${API_BASE}/projects/${activeEditingProjectId}/files/read?path=${encodeURIComponent(path)}`);
        if (res.ok) {
            const data = await res.json();
            textarea.value = data.content;
            textarea.disabled = false;
            saveBtn.disabled = false;
        } else {
            const err = await res.json();
            textarea.value = `Error: ${err.message || 'Could not load file'}`;
        }
    } catch (e) {
        textarea.value = `Error loading file: ${e.message}`;
    }
}

function copyLocalLink(url) {
    navigator.clipboard.writeText(url).then(() => {
        showToast(`Copied local link: ${url}`, 'success');
    }).catch(() => {
        showToast(`Link: ${url}`, 'info');
    });
}

function openQrModal(id, name, url) {
    document.getElementById('qr-modal-title').textContent = `QR Code - ${name}`;
    document.getElementById('qr-modal-img').src = `${API_BASE}/projects/${id}/qrcode?t=${Date.now()}`;
    document.getElementById('qr-modal-url').textContent = url;
    document.getElementById('btn-open-qr-url').href = url;
    
    document.getElementById('btn-copy-qr-url').onclick = () => {
        copyLocalLink(url);
    };
    
    document.getElementById('qr-modal').classList.remove('hidden');
}

function openPkgModal(id, name, runtime) {
    activePkgProjectId = id;
    document.getElementById('pkg-modal-title').textContent = `Packages - ${name}`;
    const isPython = runtime === 'PYTHON';
    document.getElementById('pkg-req-section').style.display = isPython ? 'block' : 'none';
    document.getElementById('pkg-label').textContent = isPython ? 'Single Pip Package' : 'NPM Package Name';
    document.getElementById('pkg-name-input').placeholder = isPython ? 'e.g. requests, fastapi, pandas' : 'e.g. cors, dotenv, axios';
    document.getElementById('pkg-install-result').style.display = 'none';
    document.getElementById('pkg-modal').classList.remove('hidden');
}

async function runPkgInstall(packageName = '', installAll = false) {
    if (!activePkgProjectId) return;
    const resultBox = document.getElementById('pkg-install-result');
    resultBox.style.display = 'block';
    resultBox.style.color = '#fff';
    resultBox.textContent = 'Running installation... please wait...';
    
    try {
        const res = await apiFetch(`${API_BASE}/projects/${activePkgProjectId}/dependencies/install`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ packageName, installAll })
        });
        const data = await res.json();
        if (res.ok) {
            resultBox.style.color = 'var(--status-green, #4ade80)';
            resultBox.textContent = data.message || 'Installed successfully!';
            showToast(data.message || 'Installed successfully', 'success');
        } else {
            resultBox.style.color = 'var(--status-red, #f87171)';
            resultBox.textContent = `${data.message || 'Installation error'}`;
            showToast(data.message || 'Installation failed', 'error');
        }
    } catch (e) {
        resultBox.style.color = 'var(--status-red, #f87171)';
        resultBox.textContent = `Error: ${e.message}`;
    }
}

async function openEnvModal(id, name) {
    activeEnvProjectId = id;
    document.getElementById('env-modal-title').textContent = `.env Variables - ${name}`;
    document.getElementById('env-modal').classList.remove('hidden');
    loadEnvVars();
}

async function loadEnvVars() {
    if (!activeEnvProjectId) return;
    const list = document.getElementById('env-vars-list');
    list.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem;">Loading variables...</div>';
    
    try {
        const res = await apiFetch(`${API_BASE}/projects/${activeEnvProjectId}/env`);
        if (!res.ok) return;
        const envVars = await res.json();
        
        if (envVars.length === 0) {
            list.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem;">No variables configured yet.</div>';
            return;
        }
        
        list.innerHTML = '';
        envVars.forEach(ev => {
            const row = document.createElement('div');
            row.style.display = 'flex';
            row.style.justifyContent = 'space-between';
            row.style.alignItems = 'center';
            row.style.padding = '0.5rem 0.75rem';
            row.style.background = 'var(--surface-elevated)';
            row.style.borderRadius = '6px';
            
            const displayVal = ev.isSecret ? '••••••••' : ev.value;
            row.innerHTML = `
                <div style="font-family: monospace; font-size: 0.85rem;">
                    <strong style="color: var(--primary);">${escapeHtml(ev.key)}</strong> = <span style="color: var(--text-secondary);">${escapeHtml(displayVal)}</span>
                </div>
                <button class="btn btn-ghost btn-sm" onclick="deleteEnvVar('${escapeAttr(ev.key)}')"></button>
            `;
            list.appendChild(row);
        });
    } catch (_) {
        list.innerHTML = '<div style="color: var(--status-red);">Failed to load variables.</div>';
    }
}

async function deleteEnvVar(key) {
    if (!activeEnvProjectId) return;
    await apiFetch(`${API_BASE}/projects/${activeEnvProjectId}/env/delete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ key })
    });
    showToast(`Deleted ${key}`, 'info');
    loadEnvVars();
}

document.getElementById('btn-save-file').addEventListener('click', async () => {
    if (!activeEditingProjectId || !activeEditingFilePath) return;
    const content = document.getElementById('editor-textarea').value;
    try {
        await apiFetch(`${API_BASE}/projects/${activeEditingProjectId}/files/save`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ relativePath: activeEditingFilePath, content })
        });
        showToast('File saved successfully', 'success');
    } catch (e) {
        showToast('Failed to save file', 'error');
    }
});

document.getElementById('btn-run-pkg-install').addEventListener('click', () => {
    const pkg = document.getElementById('pkg-name-input').value.trim();
    if (pkg) runPkgInstall(pkg, false);
});

document.getElementById('btn-run-manifest-install').addEventListener('click', () => {
    runPkgInstall('', true);
});

document.getElementById('btn-add-env-var').addEventListener('click', async () => {
    if (!activeEnvProjectId) return;
    const key = document.getElementById('new-env-key').value.trim().toUpperCase();
    const value = document.getElementById('new-env-val').value;
    if (!key) return;
    
    await apiFetch(`${API_BASE}/projects/${activeEnvProjectId}/env/set`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ key, value, isSecret: false })
    });
    document.getElementById('new-env-key').value = '';
    document.getElementById('new-env-val').value = '';
    showToast(`Added ${key}`, 'success');
    loadEnvVars();
});

document.getElementById('btn-import-raw-env').addEventListener('click', async () => {
    if (!activeEnvProjectId) return;
    const rawEnv = document.getElementById('raw-env-textarea').value.trim();
    if (!rawEnv) return;
    
    const res = await apiFetch(`${API_BASE}/projects/${activeEnvProjectId}/env/import`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ rawEnv })
    });
    if (res.ok) {
        document.getElementById('raw-env-textarea').value = '';
        showToast('Imported .env variables successfully', 'success');
        loadEnvVars();
    }
});

document.getElementById('btn-copy-logs').addEventListener('click', () => {
    if (lastLogText) {
        navigator.clipboard.writeText(lastLogText);
        showToast('Logs copied to clipboard', 'success');
    }
});

document.getElementById('btn-clear-logs').addEventListener('click', () => {
    document.getElementById('logs-terminal').textContent = '';
    lastLogText = '';
});

document.getElementById('btn-close-logs').addEventListener('click', () => {
    document.getElementById('logs-modal').classList.add('hidden');
    if (logsSocket) logsSocket.close();
});

document.getElementById('btn-close-files').addEventListener('click', () => {
    document.getElementById('files-modal').classList.add('hidden');
});

document.getElementById('btn-close-pkg').addEventListener('click', () => {
    document.getElementById('pkg-modal').classList.add('hidden');
});

document.getElementById('btn-close-env').addEventListener('click', () => {
    document.getElementById('env-modal').classList.add('hidden');
});

document.getElementById('btn-quick-tunnel').addEventListener('click', async () => {
    await apiFetch(`${API_BASE}/tunnel/quick`, { method: 'POST' });
    showToast('Quick tunnel starting...', 'info');
    fetchStatus();
});

document.getElementById('btn-login-modal').addEventListener('click', () => {
    document.getElementById('login-modal').classList.remove('hidden');
    setTimeout(() => document.getElementById('login-password').focus(), 100);
});

document.getElementById('btn-close-login').addEventListener('click', () => {
    document.getElementById('login-modal').classList.add('hidden');
});

document.getElementById('btn-logout').addEventListener('click', async () => {
    await apiFetch(`${API_BASE}/auth/logout`, { method: 'POST' });
    setAuthToken(null);
    showToast('Logged out successfully', 'info');
});

document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    const errorEl = document.getElementById('login-error');
    
    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        
        if (res.ok) {
            const data = await res.json();
            setAuthToken(data.token, username);
            document.getElementById('login-modal').classList.add('hidden');
            errorEl.classList.add('hidden');
            showToast('Logged in successfully', 'success');
            fetchStatus();
            fetchProjects();
        } else if (res.status === 429) {
            errorEl.textContent = 'Too many attempts. Wait a moment.';
            errorEl.classList.remove('hidden');
        } else {
            errorEl.textContent = 'Invalid credentials';
            errorEl.classList.remove('hidden');
        }
    } catch (_) {
        errorEl.textContent = 'Connection failed';
        errorEl.classList.remove('hidden');
    }
});

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        document.querySelectorAll('.modal:not(.hidden)').forEach(m => m.classList.add('hidden'));
        if (logsSocket) logsSocket.close();
    }
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        const filesModal = document.getElementById('files-modal');
        if (!filesModal.classList.contains('hidden') && activeEditingFilePath) {
            e.preventDefault();
            document.getElementById('btn-save-file').click();
        }
    }
});

function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function escapeAttr(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

let pollInterval = null;

function startPolling() {
    if (pollInterval) return;
    fetchStatus();
    fetchProjects();
    pollInterval = setInterval(() => {
        fetchStatus();
        fetchProjects();
    }, 4000);
}

function stopPolling() {
    if (pollInterval) {
        clearInterval(pollInterval);
        pollInterval = null;
    }
}

document.addEventListener('visibilitychange', () => {
    if (document.hidden) stopPolling();
    else startPolling();
});

document.getElementById('stat-device-ip').textContent = window.location.hostname;
updateAuthUI();
startPolling();

document.getElementById('btn-upload-zip')?.addEventListener('click', () => {
    document.getElementById('zip-upload-input')?.click();
});

document.getElementById('zip-upload-input')?.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file || !activeEditingProjectId) return;
    
    showToast('Uploading and unpacking ZIP archive...', 'info');
    const formData = new FormData();
    formData.append('file', file);
    
    try {
        const res = await apiFetch(`${API_BASE}/projects/${activeEditingProjectId}/files/upload-zip`, {
            method: 'POST',
            body: formData
        });
        const data = await res.json();
        if (res.ok) {
            showToast('ZIP archive unpacked successfully!', 'success');
            openFiles(activeEditingProjectId, document.getElementById('files-title').textContent.replace('Files - ', ''));
        } else {
            showToast(data.message || 'Upload failed', 'error');
        }
    } catch (err) {
        showToast(`Error: ${err.message}`, 'error');
    }
    e.target.value = '';
});

document.getElementById('btn-run-req-install')?.addEventListener('click', () => {
    const file = document.getElementById('pkg-req-file-input').value.trim() || 'requirements.txt';
    runPkgInstall(file, false);
});

document.getElementById('btn-close-qr')?.addEventListener('click', () => {
    document.getElementById('qr-modal').classList.add('hidden');
});
