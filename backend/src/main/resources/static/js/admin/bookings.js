import { badge } from '../ui.js';
import { requireRole, authenticatedFetch, logout } from '../auth.js';
const content = document.querySelector('#content');
const status = document.querySelector('#page-status');
document.querySelector('#logout').addEventListener('click', logout);
function text(tag, value) {
  const element = document.createElement(tag);
  element.textContent = value;
  return element;
}
function localDate(value) {
  let timestamp = String(value || '').replace(' ', 'T');
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?$/.test(timestamp)) timestamp += 'Z';
  const date = new Date(timestamp);
  return Number.isNaN(date.getTime()) ? 'Date unavailable' : date.toLocaleString();
}
async function load() {
  content.hidden = true;
  status.textContent = 'Loading bookings…';
  try {
    if (!await requireRole('admin')) return;
    const { data } = await authenticatedFetch('/admin/bookings');
    const list = document.querySelector('#bookings-list');
    list.replaceChildren();
    for (const booking of data) {
      const item = document.createElement('li');
      item.dataset.bookingId = booking.id;
      item.append(text('h2', `Booking #${booking.id}: ${booking.service_title}`),
        text('p', `Customer: ${booking.customer_name} (${booking.customer_email})`),
        text('p', `Worker: ${booking.worker_name} (${booking.worker_email})`),
        text('p', `Scheduled: ${localDate(booking.scheduled_at)}`),
        text('p', `Address: ${booking.address || 'Not available'}`),
        badge(text('strong', `Status: ${String(booking.status).replaceAll('_', ' ')}`), booking.status));
      list.append(item);
    }
    document.querySelector('#bookings-status').textContent = data.length ? `${data.length} bookings shown.` : 'No bookings yet.';
    status.textContent = '';
    content.hidden = false;
  } catch (error) {
    if (error.status === 401) return logout();
    status.textContent = `Could not load bookings. ${error.message}`;
  }
}
window.addEventListener('pageshow', load);
