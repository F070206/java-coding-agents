const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const staticRoot = path.resolve(__dirname, '../../main/resources/static');
const html = fs.readFileSync(path.join(staticRoot, 'index.html'), 'utf8');
const source = fs.readFileSync(path.join(staticRoot, 'app.js'), 'utf8');
const themeBootstrap = html.match(/<script>([\s\S]*?)<\/script>/)[1];

class Element extends EventTarget {
  constructor(tag = 'div') {
    super(); Object.assign(this, {tag, dataset: {}, children: [], value: '', textContent: '', hidden: false, disabled: false, data: {}, resetCount: 0});
    const classes = new Set();
    this.classList = {add: (name) => classes.add(name), toggle: (name, on) => on ? classes.add(name) : classes.delete(name)};
  }
  append(...nodes) { this.children.push(...nodes); }
  replaceChildren(...nodes) { this.children = nodes; this.value = nodes[0]?.value || ''; }
  add(node) { this.append(node); }
  setAttribute(name, value) { this[name] = value; }
  get selectedOptions() { return this.children.filter((node) => String(node.value) === this.value); }
  querySelectorAll() { return this.children.filter((node) => node.className === 'repo'); }
  querySelector() { return this.button ||= new Element('button'); }
  reset() { this.resetCount++; }
  focus() {}
}
const response = (data, status = 200) => new Response(JSON.stringify({success: true, data}), {status});
const deferred = () => { let resolve; const promise = new Promise((done) => { resolve = done; }); return {promise, resolve}; };

function harness({fetcher = () => response([]), dark = false, preference, storageDisabled = false} = {}) {
  const elements = new Map([...html.matchAll(/id="([^"]+)"/g)].map((match) => [match[1], new Element()]));
  const localValues = new Map(preference ? [['agentTheme', preference]] : []);
  const sessionValues = new Map();
  const storage = (values) => ({getItem: (key) => { if (storageDisabled) throw new Error('Storage blocked'); return values.get(key) || null; }, setItem: (key, value) => { if (storageDisabled) throw new Error('Storage blocked'); values.set(key, value); }, removeItem: (key) => values.delete(key)});
  const media = new EventTarget(); media.matches = dark;
  const root = new Element();
  const timers = new Map(); let nextTimer = 0;
  const window = new EventTarget();
  const context = vm.createContext({
    document: {getElementById: (id) => elements.get(id), documentElement: root, createElement: (tag) => new Element(tag), querySelector: () => elements.get('health')},
    window, localStorage: storage(localValues), sessionStorage: storage(sessionValues), matchMedia: () => media,
    fetch: (url, options) => Promise.resolve(url === '/api/health' ? response({mode: 'OFFLINE', status: 'UP'}) : fetcher(url, options)),
    Option: class extends Element { constructor(text, value) { super('option'); this.textContent = text; this.value = String(value); } },
    FormData: class { constructor(form) { this.values = Object.entries(form.data); } [Symbol.iterator]() { return this.values[Symbol.iterator](); } },
    setTimeout: (callback) => { timers.set(++nextTimer, callback); return nextTimer; }, clearTimeout: (id) => timers.delete(id),
  });
  vm.runInContext(themeBootstrap, context);
  vm.runInContext(source, context);
  return {context, elements, root, localValues, sessionValues, media, timers, window, run: (code) => vm.runInContext(code, context)};
}
async function settled() { for (let i = 0; i < 8; i++) await new Promise(setImmediate); }

test('theme follows system only when selected, persists and restores before paint', () => {
  const app = harness({dark: true});
  const select = app.elements.get('theme-select');
  assert.equal(app.root.dataset.theme, 'dark');
  assert.equal(select.value, 'system');
  select.value = 'light'; select.dispatchEvent(new Event('change'));
  assert.equal(app.root.dataset.theme, 'light');
  assert.equal(app.localValues.get('agentTheme'), 'light');
  app.media.dispatchEvent(new Event('change'));
  assert.equal(app.root.dataset.theme, 'light');
  select.value = 'system'; select.dispatchEvent(new Event('change'));
  app.media.matches = false; app.media.dispatchEvent(new Event('change'));
  assert.equal(app.root.dataset.theme, 'light');
  app.media.matches = true; app.media.dispatchEvent(new Event('change'));
  assert.equal(app.root.dataset.theme, 'dark');
  select.value = 'dark'; select.dispatchEvent(new Event('change'));
  const reloaded = harness({preference: app.localValues.get('agentTheme')});
  assert.equal(reloaded.root.dataset.theme, 'dark');
  assert.equal(reloaded.elements.get('theme-select').value, 'dark');
  const event = new Event('storage'); Object.assign(event, {key: 'agentTheme', newValue: 'light'}); app.window.dispatchEvent(event);
  assert.equal(app.root.dataset.theme, 'light');
});

test('blocked storage and invalid preferences still allow theme switching', () => {
  const app = harness({storageDisabled: true, dark: true});
  app.elements.get('theme-select').value = 'light';
  app.elements.get('theme-select').dispatchEvent(new Event('change'));
  assert.equal(app.root.dataset.theme, 'light');
  assert.equal(harness({preference: 'invalid', dark: true}).root.dataset.theme, 'dark');
});

test('asynchronous repository submit resets the captured form and prevents duplicates', async () => {
  let posts = 0;
  const repo = {id: 1, name: 'demo', workspacePath: 'C:/demo'};
  const app = harness({fetcher: (url, options) => { if (options?.method === 'POST') { posts++; return response(repo); } return response([repo]); }});
  app.run('saveToken("test-session"); setSession(true)');
  const form = app.elements.get('repository-form'); form.data = {name: 'demo', workspacePath: 'C:/demo'};
  const event = new Event('submit', {cancelable: true});
  form.dispatchEvent(event); form.dispatchEvent(new Event('submit', {cancelable: true}));
  assert.equal(event.currentTarget, null);
  await settled();
  assert.equal(posts, 1);
  assert.equal(form.resetCount, 1);
  assert.equal(form.querySelector().disabled, false);
  assert.equal(app.elements.get('notice').textContent, '仓库已添加');
});

test('empty 401 response clears login and leaves theme preference intact', async () => {
  const app = harness({preference: 'dark', fetcher: () => new Response('', {status: 401})});
  app.run('saveToken("expired-session"); setSession(true)');
  await assert.rejects(app.run('api("/api/repositories")'), /登录已失效/);
  assert.equal(app.elements.get('login-panel').hidden, false);
  assert.equal(app.sessionValues.has('agentToken'), false);
  assert.equal(app.root.dataset.theme, 'dark');
});

test('network failure and 403 retain login and produce readable messages', async () => {
  const forbidden = harness({fetcher: () => new Response('', {status: 403})});
  forbidden.run('saveToken("session"); setSession(true)');
  await assert.rejects(forbidden.run('api("/api/repositories")'), /没有此操作权限/);
  assert.equal(forbidden.sessionValues.get('agentToken'), 'session');
  const offline = harness({fetcher: () => Promise.reject(new Error('network error'))});
  offline.run('saveToken("session"); setSession(true)');
  await assert.rejects(offline.run('api("/api/repositories")'), /无法连接服务/);
  assert.equal(offline.elements.get('login-panel').hidden, true);
});

test('late task results cannot replace a newer task or restore a logged-out view', async () => {
  const requests = new Map();
  const app = harness({fetcher: (url) => { const pending = deferred(); requests.set(url, pending); return pending.promise; }});
  app.run('saveToken("session"); setSession(true); currentTaskId = 1; taskVersion++');
  const old = app.run('refreshTask()');
  app.run('currentTaskId = 2; taskVersion++');
  const next = app.run('refreshTask()');
  requests.get('/api/agent/tasks/2').resolve(response({taskId: 2, requirement: 'new task', status: 'SUCCEEDED'}));
  await settled();
  requests.get('/api/agent/tasks/2/trace').resolve(response([{type: 'TEST_PASSED', summary: 'new trace'}]));
  await next;
  requests.get('/api/agent/tasks/1').resolve(response({taskId: 1, requirement: 'old task', status: 'TESTING'}));
  await old;
  assert.equal(requests.has('/api/agent/tasks/1/trace'), false);
  assert.equal(app.elements.get('task-id').textContent, '#2');
  assert.equal(app.elements.get('trace').children[0].children[1].textContent, 'new trace');
  app.run('currentTaskId = 3; taskVersion++'); const pending = app.run('refreshTask()');
  app.run('clearSession()');
  requests.get('/api/agent/tasks/3').resolve(response({taskId: 3, status: 'TESTING'}));
  await pending;
  assert.equal(requests.has('/api/agent/tasks/3/trace'), false);
  assert.equal(app.elements.get('task-result').hidden, true);
  assert.equal(app.elements.get('login-panel').hidden, false);
});

test('manual refresh during polling shares requests and creates only one next poll', async () => {
  const task = deferred(); const trace = deferred(); let calls = 0;
  const app = harness({fetcher: (url) => { calls++; return url.endsWith('/trace') ? trace.promise : task.promise; }});
  app.run('currentTaskId = 1; taskVersion++');
  const first = app.run('pollTask()'); const second = app.run('pollTask()');
  assert.equal(calls, 1);
  task.resolve(response({taskId: 1, status: 'TESTING'})); trace.resolve(response([]));
  await Promise.all([first, second]);
  assert.equal(calls, 2);
  assert.equal(app.timers.size, 1);
  app.run('clearSession()');
  assert.equal(app.timers.size, 0);
});

test('terminal task reads trace after the completed status is available', async () => {
  const task = deferred(); let completed = false;
  const app = harness({fetcher: (url) => url.endsWith('/trace')
    ? response(completed ? [{type: 'TEST_PASSED', summary: 'final test result'}] : []) : task.promise});
  app.run('currentTaskId = 1; taskVersion++');
  const pending = app.run('refreshTask()');
  completed = true;
  task.resolve(response({taskId: 1, status: 'SUCCEEDED'}));
  await pending;
  assert.equal(app.elements.get('trace').children.length, 1);
  assert.equal(app.elements.get('trace').children[0].children[1].textContent, 'final test result');
});
