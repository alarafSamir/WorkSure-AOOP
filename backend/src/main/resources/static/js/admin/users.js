import { badge } from '../ui.js';
import { requireRole, authenticatedFetch, logout } from '../auth.js';
const content = document.querySelector('#content');
const status = document.querySelector('#page-status');
const message = document.querySelector('#action-message');
const previous = document.querySelector('#previous');
const next = document.querySelector('#next');
let page = 1;
let busy = false;
document.querySelector('#logout').addEventListener('click', logout);
function text(tag, value) {
  const element = document.createElement(tag);
  element.textContent = value;
  return element;
}
async function loadLists() {
  previous.disabled = next.disabled = true;
  const [users, workers] = await Promise.all([
    authenticatedFetch(`/admin/users?page=${page}&limit=20`), authenticatedFetch('/admin/workers'),
  ]);
  const list = document.querySelector('#users-list');
  list.replaceChildren();
  for (const user of users.data) {
    const item = document.createElement('li');
    item.dataset.userId = user.id;
    const worker = workers.data.find(w => Number(w.user_id) === Number(user.id));
    item.append(text('h3', `${user.full_name} (#${user.id})`), text('p', user.email),
      text('p', `Role: ${user.role}`), badge(text('p', `Account: ${Number(user.is_banned) ? 'Banned' : 'Not banned'}`), Number(user.is_banned) ? 'banned' : 'active'));
    if (user.suspended_until) item.append(text('p', `Suspension until (UTC): ${user.suspended_until}`));
    if (worker) item.append(badge(text('p', `Verification: ${Number(worker.is_verified) ? 'Verified' : 'Not verified'}`), Number(worker.is_verified) ? 'verified' : 'unverified'));
    if (user.role !== 'admin') {
      const button = text('button', Number(user.is_banned) ? 'Unban' : 'Ban');
      button.type = 'button';
      button.addEventListener('click', () => ban(user));
      item.append(button);
    }
    list.append(item);
  }
  document.querySelector('#users-status').textContent = `${users.total} users. ${users.data.length ? '' : 'No users on this page.'}`;
  document.querySelector('#page-number').textContent = `Page ${page} of ${Math.max(1, Math.ceil(users.total / 20))}`;
  previous.disabled = page === 1;
  next.disabled = page * 20 >= users.total;
  const workerList = document.querySelector('#workers-list');
  workerList.replaceChildren();
  for (const worker of workers.data) {
    const item = document.createElement('li');
    item.dataset.workerId = worker.id;
    item.append(text('h3', `${worker.full_name} (worker #${worker.id})`), text('p', worker.email),
      text('p', 'Role: worker'), badge(text('p', `Account: ${Number(worker.is_banned) ? 'Banned' : 'Not banned'}`), Number(worker.is_banned) ? 'banned' : 'active'), badge(text('p', `Verification: ${Number(worker.is_verified) ? 'Verified' : 'Not verified'}`), Number(worker.is_verified) ? 'verified' : 'unverified'));
    workerList.append(item);
  }
  document.querySelector('#workers-status').textContent = workers.data.length ? `${workers.data.length} workers.` : 'No workers yet.';
}
async function ban(user) {
  if (busy || !window.confirm(`${Number(user.is_banned) ? 'Unban' : 'Ban'} ${user.full_name}?`)) return;
  busy = true;
  content.querySelectorAll('button').forEach(button => { button.disabled = true; });
  try {
    const result = await authenticatedFetch(`/admin/users/${user.id}/ban`, {
      method: 'PATCH', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ is_banned: !Number(user.is_banned) }),
    });
    message.textContent = result.message;
    message.className = 'success';
    await loadLists();
  } catch (error) {
    if (error.status === 401) return logout();
    message.textContent = `Could not update or refresh account status. ${error.message}`;
    message.className = 'error';
    // Reloading the page retrieves the authoritative state before another action.
  } finally { busy = false; }
}
async function load() {
  content.hidden = true;
  status.textContent = 'Loading accounts…';
  try {
    if (!await requireRole('admin')) return;
    await loadLists();
    content.hidden = false;
    status.textContent = '';
  } catch (error) {
    if (error.status === 401) return logout();
    status.textContent = `Could not load accounts. ${error.message} Please reload to retry.`;
  }
}
previous.addEventListener('click', () => { if (!busy && page > 1) { page--; load(); } });
next.addEventListener('click', () => { if (!busy) { page++; load(); } });
window.addEventListener('pageshow', load);
