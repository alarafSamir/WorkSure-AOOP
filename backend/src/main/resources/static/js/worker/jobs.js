import { badge } from '../ui.js';
import { requireRole, authenticatedFetch, logout } from '../auth.js';
const content = document.querySelector('#worker-content');
const status = document.querySelector('#page-status');
const list = document.querySelector('#jobs-list');
const message = document.querySelector('#action-message');
let busy = false;
const actions = {
  pending: [['Accept', 'accepted'], ['Reject', 'rejected']],
  accepted: [['Start job', 'in_progress']],
  in_progress: [['Complete job', 'completed']],
};
document.querySelector('#logout').addEventListener('click', logout);
function text(tag, value) { const element = document.createElement(tag); element.textContent = value; return element; }
function localDate(value) {
  let timestamp = String(value || '').replace(' ', 'T');
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?$/.test(timestamp)) timestamp += 'Z';
  const date = new Date(timestamp);
  return Number.isNaN(date.getTime()) ? 'Date unavailable' : date.toLocaleString();
}
async function loadJobs() {
  const { bookings } = await authenticatedFetch('/bookings');
  list.replaceChildren();
  for (const job of bookings) {
    const item = document.createElement('li');
    item.dataset.bookingId = job.id;
    item.append(text('h2', `Job #${job.id}: ${job.service_title}`), text('p', `Customer: ${job.customer_name || 'Not available'}`), text('p', `Scheduled: ${localDate(job.scheduled_at)}`), text('p', `Address: ${job.address}`), badge(text('p', `Status: ${job.status.replaceAll('_', ' ')}`), job.status));
    const controls = document.createElement('div');
    for (const [label, next] of actions[job.status] || []) {
      const button = text('button', label);
      button.type = 'button'; button.dataset.status = next;
      button.addEventListener('click', () => updateJob(job.id, next));
      controls.appendChild(button);
    }
    if (!controls.childElementCount) controls.appendChild(text('p', 'No actions available.'));
    item.appendChild(controls); list.appendChild(item);
  }
  document.querySelector('#jobs-status').textContent = bookings.length ? `${bookings.length} assigned jobs.` : 'No jobs assigned yet.';
}
async function updateJob(id, next) {
  if (busy) return;
  busy = true;
  list.querySelectorAll('button').forEach(button => { button.disabled = true; });
  message.textContent = 'Updating job…';
  try {
    if (!await requireRole('worker')) return;
    await authenticatedFetch(`/bookings/${id}/status`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ status: next }) });
    message.textContent = `Job #${id} updated to ${next.replaceAll('_', ' ')}.`;
    await loadJobs();
  } catch (error) {
    if (error.status === 401) return logout();
    message.textContent = `Could not finish the update. ${error.message} Refresh to see the latest status.`;
  } finally {
    busy = false;
    list.querySelectorAll('button').forEach(button => { button.disabled = false; });
  }
}
async function load() {
  content.hidden = true; list.replaceChildren();
  try {
    if (!await requireRole('worker')) return;
    content.hidden = false; status.textContent = '';
    await loadJobs();
  } catch (error) {
    if (error.status === 401) return logout();
    status.textContent = `Could not load jobs. ${error.message}`;
  }
}
window.addEventListener('pageshow', load);
