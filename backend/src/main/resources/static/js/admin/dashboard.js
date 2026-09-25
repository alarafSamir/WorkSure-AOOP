import { summaryCard } from '../ui.js';
import { requireRole, authenticatedFetch, logout } from '../auth.js';
const content = document.querySelector('#content');
const status = document.querySelector('#page-status');
document.querySelector('#logout').addEventListener('click', logout);
async function load() {
  content.hidden = true;
  status.textContent = 'Checking your session…';
  try {
    const user = await requireRole('admin');
    if (!user) return;
    document.querySelector('#user-name').textContent = user.full_name;
    document.querySelector('#user-email').textContent = user.email;
    document.querySelector('#user-role').textContent = user.role;
    content.hidden = false;
    const list = document.querySelector('#totals');
    list.replaceChildren();
    const { stats } = await authenticatedFetch('/admin/stats');
    for (const [key, label] of [['users', 'Users'], ['workers', 'Workers'], ['bookings', 'Bookings'], ['pendingDocs', 'Pending verification documents']]) {
      const item = document.createElement('li');
      summaryCard(item, label, stats[key]);
      list.appendChild(item);
    }
    status.textContent = '';
  } catch (error) {
    if (error.status === 401) return logout();
    status.textContent = `Could not load dashboard. ${error.message}`;
  }
}
window.addEventListener('pageshow', load);
