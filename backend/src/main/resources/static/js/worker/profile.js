import { badge } from '../ui.js';
import { requireRole, authenticatedFetch, logout } from '../auth.js';
const form = document.querySelector('#profile-form');
const fields = document.querySelector('#profile-fields');
const message = document.querySelector('#profile-message');
let saving = false;
const editable = ['headline', 'bio', 'hourly_rate', 'service_radius_km', 'years_experience'];
document.querySelector('#logout').addEventListener('click', logout);
for (const key of ['headline', 'bio']) form.elements[key].addEventListener('input', () => form.elements[key].setCustomValidity(''));
function show(worker) {
  for (const key of editable) form.elements[key].value = worker[key] ?? '';
  document.querySelector('#user-name').textContent = worker.full_name;
  document.querySelector('#user-email').textContent = worker.email;
  document.querySelector('#verification').textContent = Number(worker.is_verified) === 1 ? 'Verified worker' : 'Not verified';
  badge(document.querySelector('#verification'), Number(worker.is_verified) ? 'verified' : 'unverified');
}
async function load() {
  document.querySelector('#worker-content').hidden = true; fields.disabled = true;
  try {
    if (!await requireRole('worker')) return;
    show((await authenticatedFetch('/workers/me')).worker);
    document.querySelector('#worker-content').hidden = false;
    document.querySelector('#page-status').textContent = ''; fields.disabled = false;
  } catch (error) {
    if (error.status === 401) return logout();
    document.querySelector('#page-status').textContent = `Could not load profile. ${error.message}`;
  }
}
form.addEventListener('submit', async event => {
  event.preventDefault(); if (saving) return;
  for (const key of ['headline', 'bio']) form.elements[key].setCustomValidity(form.elements[key].value.trim() ? '' : 'Please enter a value.');
  if (!form.reportValidity()) return;
  const body = Object.fromEntries(editable.map(key => [key, ['headline', 'bio'].includes(key) ? form.elements[key].value.trim() : Number(form.elements[key].value)]));
  saving = true; fields.disabled = true; message.textContent = 'Saving…';
  try {
    if (!await requireRole('worker')) return;
    show((await authenticatedFetch('/workers/me', { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })).worker);
    message.textContent = 'Profile saved successfully.';
  } catch (error) {
    if (error.status === 401) return logout();
    message.textContent = `Could not save profile. ${error.message}`;
  } finally { saving = false; fields.disabled = false; }
});
window.addEventListener('pageshow', load);
