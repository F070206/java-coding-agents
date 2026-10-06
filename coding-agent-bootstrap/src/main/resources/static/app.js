const $ = (id) => document.getElementById(id);
let token = sessionStorage.getItem('agentToken');
let currentTaskId = null;
let pollTimer = null;

function notice(message, error = false) {
  const element = $('notice');
  element.textContent = message;
  element.classList.toggle('error', error);
  element.hidden = false;
}

async function api(path, options = {}) {
  const headers = {'Content-Type': 'application/json', ...options.headers};
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(path, {...options, headers});
  const body = await response.json();
  if (!response.ok || !body.success) throw new Error(body.message || `请求失败 (${response.status})`);
  return body.data;
}

function setSession(loggedIn) {
  $('login-panel').hidden = loggedIn;
  $('task-workspace').hidden = !loggedIn;
  $('add-repository-panel').hidden = !loggedIn;
  $('session').hidden = !loggedIn;
}

async function loadRepositories() {
  const repositories = await api('/api/repositories');
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
  for (const item of $('repositories').querySelectorAll('.repo')) {
    item.classList.toggle('active', item.dataset.repositoryId === select.value);
  }
}

async function refreshTask() {
  if (!currentTaskId) return;
  const task = await api(`/api/agent/tasks/${currentTaskId}`);
  const trace = await api(`/api/agent/tasks/${currentTaskId}/trace`);
  $('task-empty').hidden = true;
  $('task-result').hidden = false;
  $('refresh-task').hidden = false;
  $('task-id').textContent = `任务 #${task.taskId}`;
  $('task-requirement').textContent = task.requirement;
  const status = $('task-status');
  status.textContent = task.status;
  status.className = `badge ${task.status === 'SUCCEEDED' ? 'done' : ['FAILED', 'CANCELLED'].includes(task.status) ? 'failed' : ''}`;
  const timeline = $('trace');
  timeline.replaceChildren();
  for (const entry of trace) {
    const event = document.createElement('div');
    event.className = 'event';
    const title = document.createElement('strong');
    title.textContent = entry.type;
    const summary = document.createElement('p');
    summary.textContent = entry.summary;
    event.append(title, summary);
    timeline.append(event);
  }
  if (['SUCCEEDED', 'FAILED', 'CANCELLED', 'WAITING_CONFIRMATION'].includes(task.status)) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
  return task.status;
}

$('login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  try {
    const data = await api('/api/auth/token', {method: 'POST', body: JSON.stringify(Object.fromEntries(form))});
    token = data.accessToken;
    sessionStorage.setItem('agentToken', token);
    setSession(true);
    await loadRepositories();
    notice('登录成功');
  } catch (error) { notice(error.message, true); }
});

$('logout').addEventListener('click', () => {
  token = null;
  sessionStorage.removeItem('agentToken');
  clearInterval(pollTimer);
  currentTaskId = null;
  setSession(false);
  $('repositories').replaceChildren();
  const empty = document.createElement('p');
  empty.className = 'sidebar-empty';
  empty.textContent = '登录后查看仓库';
  $('repositories').append(empty);
  $('repository-id').replaceChildren(new Option('先添加仓库', ''));
  $('repository-count').textContent = '0';
  $('task-empty').hidden = false;
  $('task-result').hidden = true;
  $('refresh-task').hidden = true;
  updateRepositorySelection();
  notice('已退出登录');
});

$('repository-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    const data = Object.fromEntries(new FormData(event.currentTarget));
    data.userId = 1;
    const repository = await api('/api/repositories', {method: 'POST', body: JSON.stringify(data)});
    await loadRepositories();
    $('repository-id').value = String(repository.id);
    updateRepositorySelection();
    event.currentTarget.reset();
    notice('仓库已添加');
  } catch (error) { notice(error.message, true); }
});

$('task-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    const data = Object.fromEntries(new FormData(event.currentTarget));
    data.repositoryId = Number(data.repositoryId);
    const task = await api('/api/agent/tasks', {method: 'POST', body: JSON.stringify(data)});
    currentTaskId = task.taskId;
    clearInterval(pollTimer);
    const status = await refreshTask();
    if (!['SUCCEEDED', 'FAILED', 'CANCELLED', 'WAITING_CONFIRMATION'].includes(status)) {
      pollTimer = setInterval(() => refreshTask().catch((error) => {clearInterval(pollTimer); notice(error.message, true);}), 2000);
    }
    notice('任务已提交');
  } catch (error) { notice(error.message, true); }
});

$('refresh-task').addEventListener('click', () => refreshTask().catch((error) => notice(error.message, true)));
$('repository-id').addEventListener('change', updateRepositorySelection);
setSession(Boolean(token));
fetch('/api/health').then((response) => response.json()).then((body) => {
  $('health').textContent = body.success ? '服务运行中' : '服务异常';
  document.querySelector('.status-dot').classList.toggle('online', body.success);
}).catch(() => { $('health').textContent = '服务不可用'; });
if (token) loadRepositories().catch(() => { token = null; sessionStorage.removeItem('agentToken'); setSession(false); });
