const $ = (id) => document.getElementById(id);
const themeQuery = matchMedia('(prefers-color-scheme: dark)');
function applyTheme(preference, persist = false) {
  if (!['light', 'dark', 'system'].includes(preference)) preference = 'system';
  document.documentElement.dataset.themePreference = preference;
  document.documentElement.dataset.theme = preference === 'system' ? (themeQuery.matches ? 'dark' : 'light') : preference;
  $('theme-select').value = preference;
  if (persist) {
    try { localStorage.setItem('agentTheme', preference); } catch (error) { /* Theme still works when storage is unavailable. */ }
  }
}
applyTheme(document.documentElement.dataset.themePreference || 'system');
$('theme-select').addEventListener('change', (event) => applyTheme(event.currentTarget.value, true));
themeQuery.addEventListener('change', () => {
  if (document.documentElement.dataset.themePreference === 'system') applyTheme('system');
});
window.addEventListener('storage', (event) => {
  if (event.key === 'agentTheme' || event.key === null) applyTheme(event.newValue || 'system');
});

let token = null;
try { token = sessionStorage.getItem('agentToken'); } catch (error) { /* Login can use an in-memory session. */ }
let sessionVersion = 0;
let taskVersion = 0;
let currentTaskId = null;
let pollTimer = null;
let pollVersion = 0;
let pendingRefresh = null;
const terminalStatuses = new Set(['SUCCEEDED', 'FAILED', 'CANCELLED', 'WAITING_CONFIRMATION', 'ROLLED_BACK']);
const statusLabels = {PENDING: '等待执行', SCANNING: '扫描代码', INDEXING: '建立索引', PLANNING: '规划任务', RETRIEVING: '检索代码', EXECUTING: '执行中', REVIEWING: '检查变更', TESTING: '测试中', REPAIRING: '修复中', SUCCEEDED: '已完成', FAILED: '执行失败', CANCELLED: '已取消', WAITING_CONFIRMATION: '等待确认', ROLLED_BACK: '已回滚'};
const traceLabels = {REPOSITORY_SCANNED: '仓库扫描完成', INDEX_COMPLETED: '索引建立完成', PLAN_CREATED: '任务计划已生成', DIFF_REVIEWED: '变更检查完成', TEST_PASSED: '测试通过', TEST_FAILED: '测试失败', FILE_CHANGED: '文件变更', WAITING_CONFIRMATION: '等待确认'};
class StaleRequest extends Error {}

function notice(message, error = false) {
  $('notice').textContent = message;
  $('notice').classList.toggle('error', error);
  $('notice').hidden = false;
}
function reportError(error) {
  if (!(error instanceof StaleRequest)) notice(error.message || '请求失败，请稍后重试。', true);
}
function stopPolling() { clearTimeout(pollTimer); pollTimer = null; pollVersion++; }
function saveToken(value) {
  token = value;
  try {
    if (value) sessionStorage.setItem('agentToken', value);
    else sessionStorage.removeItem('agentToken');
  } catch (error) { /* Keep the session usable without browser storage. */ }
}
function setSession(loggedIn) {
  $('login-panel').hidden = loggedIn;
  $('task-workspace').hidden = !loggedIn;
  $('add-repository-panel').hidden = !loggedIn;
  $('session').hidden = !loggedIn;
}
function clearSession() {
  sessionVersion++;
  taskVersion++;
  saveToken(null);
  stopPolling();
  currentTaskId = null;
  setSession(false);
  const empty = document.createElement('p');
  empty.className = 'sidebar-empty';
  empty.textContent = '登录后查看仓库';
  $('repositories').replaceChildren(empty);
  $('repository-id').replaceChildren(new Option('先添加仓库', ''));
  $('repository-count').textContent = '0';
  $('task-empty').hidden = false;
  $('task-result').hidden = true;
  $('refresh-task').hidden = true;
  $('password').value = '';
  updateRepositorySelection();
}

async function api(path, options = {}) {
  const version = sessionVersion;
  const headers = {'Content-Type': 'application/json', ...options.headers};
  if (token && path !== '/api/auth/token') headers.Authorization = `Bearer ${token}`;
  let response;
  try { response = await fetch(path, {...options, headers}); }
  catch (error) {
    if (version !== sessionVersion) throw new StaleRequest();
    throw new Error('无法连接服务，请确认后端已启动后重试。');
  }
  if (version !== sessionVersion) throw new StaleRequest();
  if (response.status === 401) {
    clearSession();
    throw new Error('登录已失效，请重新登录。');
  }
  const text = await response.text();
  if (version !== sessionVersion) throw new StaleRequest();
  let body;
  try { body = text ? JSON.parse(text) : null; } catch (error) { body = null; }
  if (!response.ok || !body?.success) {
    const fallback = response.status === 403 ? '当前账号没有此操作权限。' : `请求失败（HTTP ${response.status}），请稍后重试。`;
    throw new Error(body?.message || fallback);
  }
  return body.data;
}

async function loadRepositories() {
  const version = sessionVersion;
  const repositories = await api('/api/repositories');
  if (version !== sessionVersion) throw new StaleRequest();
  const select = $('repository-id');
  const selected = select.value;
  select.replaceChildren(new Option(repositories.length ? '选择仓库' : '先添加仓库', ''));
  const list = $('repositories');
  list.replaceChildren();
  $('repository-count').textContent = String(repositories.length);
  if (!repositories.length) {
    const empty = document.createElement('p');
    empty.className = 'sidebar-empty';
    empty.textContent = '还没有仓库';
    list.append(empty);
  }
  for (const repository of repositories) {
    select.add(new Option(repository.name, repository.id));
    const item = document.createElement('button');
    item.className = 'repo';
    item.type = 'button';
    item.dataset.repositoryId = String(repository.id);
    const symbol = document.createElement('span');
    symbol.className = 'repo-symbol';
    symbol.textContent = '⌁';
    symbol.setAttribute('aria-hidden', 'true');
    const details = document.createElement('span');
    details.className = 'repo-details';
    const name = document.createElement('strong');
    name.textContent = repository.name;
    const path = document.createElement('small');
    path.textContent = repository.workspacePath;
    item.title = repository.workspacePath;
    details.append(name, path);
    item.append(symbol, details);
    item.addEventListener('click', () => { select.value = String(repository.id); updateRepositorySelection(); $('requirement').focus(); });
    list.append(item);
  }
  if (selected && repositories.some((repository) => String(repository.id) === selected)) select.value = selected;
  updateRepositorySelection();
}
function updateRepositorySelection() {
  const select = $('repository-id');
  $('active-repository').textContent = select.value ? select.selectedOptions[0].textContent : '新任务';
  for (const item of $('repositories').querySelectorAll('.repo')) item.classList.toggle('active', item.dataset.repositoryId === select.value);
}

function refreshTask() {
  if (!currentTaskId) return Promise.resolve(null);
  const taskId = currentTaskId;
  const version = taskVersion;
  const session = sessionVersion;
  if (pendingRefresh?.version === version) return pendingRefresh.promise;
  const promise = (async () => {
    const task = await api(`/api/agent/tasks/${taskId}`);
    if (version !== taskVersion || session !== sessionVersion) return null;
    // A terminal status is published after the final trace; read trace afterwards to include it.
    const trace = await api(`/api/agent/tasks/${taskId}/trace`);
    if (version !== taskVersion || session !== sessionVersion) return null;
    $('task-empty').hidden = true;
    $('task-result').hidden = false;
    $('refresh-task').hidden = false;
    $('task-id').textContent = `#${task.taskId}`;
    $('task-requirement').textContent = task.requirement;
    $('task-status').textContent = statusLabels[task.status] || task.status;
    $('task-status').title = task.status;
    $('task-status').className = `badge ${task.status === 'SUCCEEDED' ? 'done' : ['FAILED', 'CANCELLED'].includes(task.status) ? 'failed' : ''}`;
    $('task-outcome').textContent = task.message || task.stopReason || '';
    $('task-outcome').hidden = !$('task-outcome').textContent;
    $('task-outcome').classList.toggle('failed', task.status === 'FAILED');
    const timeline = $('trace');
    timeline.replaceChildren();
    for (const entry of trace) {
      const event = document.createElement('div');
      event.className = 'event';
      const title = document.createElement('strong');
      title.textContent = traceLabels[entry.type] || entry.type;
      title.title = entry.type;
      const summary = document.createElement('p');
      summary.textContent = entry.summary;
      event.append(title, summary);
      timeline.append(event);
    }
    if (terminalStatuses.has(task.status)) stopPolling();
    return task.status;
  })().catch((error) => {
    if (version !== taskVersion || session !== sessionVersion) {
      if (!(error instanceof StaleRequest) && !token) reportError(error);
      return null;
    }
    throw error;
  }).finally(() => { if (pendingRefresh?.version === version) pendingRefresh = null; });
  pendingRefresh = {version, promise};
  return promise;
}
async function pollTask() {
  stopPolling();
  const cycle = pollVersion;
  const version = taskVersion;
  try {
    const status = await refreshTask();
    if (version === taskVersion && cycle === pollVersion && status && !terminalStatuses.has(status)) pollTimer = setTimeout(pollTask, 2000);
  } catch (error) { reportError(error); }
}

function onSubmit(id, action) {
  $(id).addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    if (form.dataset.submitting === 'true') return;
    form.dataset.submitting = 'true';
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    try { await action(form); } catch (error) { reportError(error); }
    finally { button.disabled = false; form.dataset.submitting = 'false'; }
  });
}
onSubmit('login-form', async (form) => {
  const data = await api('/api/auth/token', {method: 'POST', body: JSON.stringify(Object.fromEntries(new FormData(form)))});
  saveToken(data.accessToken);
  $('password').value = '';
  setSession(true);
  await loadRepositories();
  notice('登录成功');
});
$('logout').addEventListener('click', () => { clearSession(); notice('已退出登录'); });
onSubmit('repository-form', async (form) => {
  const data = Object.fromEntries(new FormData(form));
  data.userId = 1;
  const repository = await api('/api/repositories', {method: 'POST', body: JSON.stringify(data)});
  form.reset();
  await loadRepositories();
  $('repository-id').value = String(repository.id);
  updateRepositorySelection();
  notice('仓库已添加');
});
onSubmit('task-form', async (form) => {
  const data = Object.fromEntries(new FormData(form));
  data.repositoryId = Number(data.repositoryId);
  const task = await api('/api/agent/tasks', {method: 'POST', body: JSON.stringify(data)});
  currentTaskId = task.taskId;
  taskVersion++;
  $('refresh-task').hidden = false;
  notice('任务已提交');
  await pollTask();
});
$('refresh-task').addEventListener('click', pollTask);
$('repository-id').addEventListener('change', updateRepositorySelection);
setSession(Boolean(token));
fetch('/api/health').then(async (response) => {
  if (!response.ok) throw new Error('Service unavailable');
  const body = await response.json();
  if (!body.success) throw new Error('Service unavailable');
  $('health').textContent = '服务运行中';
  document.querySelector('.status-dot').classList.add('online');
  const mode = body.data.mode;
  $('runtime-mode').textContent = mode === 'OFFLINE' ? '离线兜底' : mode === 'ONLINE' ? '在线模型' : '运行模式未知';
  $('runtime-description').textContent = mode === 'OFFLINE' ? '选择仓库并描述任务。离线模型会扫描代码、记录执行轨迹并运行测试。' : '选择仓库并描述任务，查看代码分析、执行进度与测试结果。';
  $('runtime-hint').textContent = mode === 'OFFLINE' ? '离线模式不会调用大模型或修改文件；任务会运行所选仓库的测试。' : mode === 'ONLINE' ? '模型将分析仓库并提出代码变更，敏感操作需要确认。' : '无法确认模型模式，提交前请检查后端配置。';
}).catch(() => {
  $('health').textContent = '服务不可用';
  $('runtime-mode').textContent = '无法连接';
});
if (token) loadRepositories().catch(reportError);
