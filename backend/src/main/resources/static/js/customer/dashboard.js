import { summaryCard } from '../ui.js';
import { requireRole, authenticatedFetch, logout } from '../auth.js';

const content = document.querySelector('#customer-content');
const status = document.querySelector('#page-status');
document.querySelector('#logout').addEventListener('click', logout);

async function loadDashboard() {
  content.hidden = true;
  status.textContent = 'Checking your session…';
  try {
    const user = await requireRole('customer');
    if (!user) return;
    document.querySelector('#user-name').textContent = user.full_name;
    document.querySelector('#user-email').textContent = user.email;
    document.querySelector('#user-role').textContent = user.role;
    content.hidden = false;
    status.textContent = '';
    const summary = document.querySelector('#booking-summary');
    const summaryStatus = document.querySelector('#summary-status');
    summary.replaceChildren();
    summaryStatus.textContent = 'Loading bookings…';
    try {
      const { bookings } = await authenticatedFetch('/bookings');
      summaryStatus.textContent = bookings.length ? `${bookings.length} total bookings.` : 'No bookings yet.';
      const counts = new Map();
      for (const booking of bookings) counts.set(booking.status, (counts.get(booking.status) || 0) + 1);
      for (const [name, count] of counts) {
        const item = document.createElement('li');
        summaryCard(item, name.replaceAll('_', ' '), count);
        summary.appendChild(item);
      }
    } catch (error) {
      if (error.status === 401) return logout();
      summaryStatus.textContent = `Could not load booking summary. ${error.message}`;
    }
  } catch (error) {
    status.textContent = `Could not load your account. ${error.message}`;
  }
}

window.addEventListener('pageshow', loadDashboard);
