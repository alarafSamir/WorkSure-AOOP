import { badge, summaryCard } from '../ui.js';
import { requireRole, authenticatedFetch, logout } from '../auth.js';
const content = document.querySelector('#worker-content');
const status = document.querySelector('#page-status');
document.querySelector('#logout').addEventListener('click', logout);
async function load() {
  content.hidden = true;
  status.textContent = 'Checking your session…';
  try {
    const user = await requireRole('worker');
    if (!user) return;
    const { worker } = await authenticatedFetch('/workers/me');
    document.querySelector('#user-name').textContent = user.full_name;
    document.querySelector('#user-email').textContent = user.email;
    document.querySelector('#user-role').textContent = user.role;
    document.querySelector('#verification').textContent = Number(worker.is_verified) === 1 ? 'Verified worker' : 'Not verified';
    badge(document.querySelector('#verification'), Number(worker.is_verified) ? 'verified' : 'unverified');
    content.hidden = false;
    status.textContent = '';
    const list = document.querySelector('#job-summary');
    list.replaceChildren();
    try {
      const { bookings } = await authenticatedFetch('/bookings');
      document.querySelector('#summary-status').textContent = bookings.length ? `${bookings.length} jobs in total.` : 'No jobs yet.';
      const counts = new Map();
      for (const b of bookings) counts.set(b.status, (counts.get(b.status) || 0) + 1);
      for (const [state, count] of counts) {
        const item = document.createElement('li');
        summaryCard(item, state.replaceAll('_', ' '), count);
        list.appendChild(item);
      }
    } catch (error) {
      if (error.status === 401) return logout();
      document.querySelector('#summary-status').textContent = `Could not load jobs. ${error.message}`;
    }
  } catch (error) {
    if (error.status === 401) return logout();
    status.textContent = `Could not load your account. ${error.message}`;
  }
}
window.addEventListener('pageshow', load);
