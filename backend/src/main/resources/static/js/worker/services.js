import { requireRole, authenticatedFetch, logout } from '../auth.js';
const form = document.querySelector('#service-form');
const fields = document.querySelector('#service-fields');
const message = document.querySelector('#service-message');
const list = document.querySelector('#services-list');
let editingId = null;
let busy = false;
const editable = ['title', 'description', 'base_price', 'duration_minutes', 'tags'];
document.querySelector('#logout').addEventListener('click', logout);
for (const key of ['title', 'description']) form.elements[key].addEventListener('input', () => form.elements[key].setCustomValidity(''));
function text(tag, value) { const element = document.createElement(tag); element.textContent = value; return element; }
function resetForm() {
  editingId = null; form.reset();
  for (const key of ['title', 'description']) form.elements[key].setCustomValidity('');
  form.elements.category_id.disabled = false; form.elements.is_active.disabled = true;
  document.querySelector('#form-heading').textContent = 'Create service';
  document.querySelector('#save-service').textContent = 'Create service';
  document.querySelector('#cancel-edit').hidden = true;
}
document.querySelector('#cancel-edit').addEventListener('click', resetForm);
function setBusy(value) {
  busy = value; fields.disabled = value;
  list.querySelectorAll('button').forEach(button => { button.disabled = value; });
}
function edit(service) {
  if (busy) return;
  resetForm(); editingId = service.id;
  for (const key of editable) form.elements[key].value = service[key] ?? '';
  form.elements.category_id.value = String(service.category_id);
  form.elements.category_id.disabled = true;
  form.elements.is_active.disabled = false;
  form.elements.is_active.value = Number(service.is_active) === 1 ? '1' : '0';
  document.querySelector('#form-heading').textContent = `Edit service #${service.id}`;
  document.querySelector('#save-service').textContent = 'Save changes';
  document.querySelector('#cancel-edit').hidden = false;
  message.textContent = ''; form.elements.title.focus();
}
async function loadServices() {
  const { services } = await authenticatedFetch('/workers/me/services');
  list.replaceChildren();
  for (const service of services) {
    const item = document.createElement('li'); item.dataset.serviceId = service.id;
    item.append(text('h3', service.title), text('p', service.description), text('p', `${service.category_name} · ৳${Number(service.base_price).toFixed(2)} · ${Number(service.is_active) === 1 ? 'Active' : 'Inactive'}`));
    const editButton = text('button', 'Edit'); editButton.type = 'button'; editButton.dataset.action = 'edit'; editButton.addEventListener('click', () => edit(service));
    const deleteButton = text('button', 'Delete'); deleteButton.type = 'button'; deleteButton.dataset.action = 'delete'; deleteButton.addEventListener('click', () => remove(service));
    item.append(editButton, deleteButton); list.appendChild(item);
  }
  document.querySelector('#services-status').textContent = services.length ? `${services.length} owned services (including inactive).` : 'No services yet. Create your first listing above.';
}
async function remove(service) {
  if (busy || !window.confirm(`Delete "${service.title}"? Services with bookings cannot be deleted.`)) return;
  setBusy(true); message.textContent = 'Deleting…';
  try {
    if (!await requireRole('worker')) return;
    await authenticatedFetch(`/services/${service.id}`, { method: 'DELETE' });
    if (editingId === service.id) resetForm();
    message.textContent = 'Service deleted.'; await loadServices();
  } catch (error) {
    if (error.status === 401) return logout();
    message.textContent = `Could not delete service. ${error.message}`;
  } finally { setBusy(false); }
}
form.addEventListener('submit', async event => {
  event.preventDefault(); if (busy) return;
  for (const key of ['title', 'description']) form.elements[key].setCustomValidity(form.elements[key].value.trim() ? '' : 'Please enter a value.');
  if (!form.reportValidity()) return;
  const body = Object.fromEntries(editable.map(key => [key, ['base_price', 'duration_minutes'].includes(key) ? Number(form.elements[key].value) : form.elements[key].value.trim()]));
  if (editingId) body.is_active = form.elements.is_active.value === '1';
  else body.category_id = Number(form.elements.category_id.value);
  setBusy(true); message.textContent = 'Saving…';
  try {
    if (!await requireRole('worker')) return;
    await authenticatedFetch(editingId ? `/services/${editingId}` : '/services', { method: editingId ? 'PATCH' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    message.textContent = editingId ? 'Service updated.' : 'Service created.';
    resetForm(); await loadServices();
  } catch (error) {
    if (error.status === 401) return logout();
    message.textContent = `Could not save service. ${error.message}`;
  } finally { setBusy(false); }
});
async function load() {
  document.querySelector('#worker-content').hidden = true; fields.disabled = true;
  try {
    if (!await requireRole('worker')) return;
    const { categories } = await authenticatedFetch('/services/categories');
    const select = form.elements.category_id;
    select.replaceChildren(new Option('Select a category', ''));
    for (const category of categories) {
      // Keep parent categories available for existing listings too.
      select.appendChild(new Option(category.name, category.id));
    }
    await loadServices(); resetForm();
    document.querySelector('#worker-content').hidden = false;
    document.querySelector('#page-status').textContent = ''; fields.disabled = false;
  } catch (error) {
    if (error.status === 401) return logout();
    document.querySelector('#page-status').textContent = `Could not load services. ${error.message}`;
  }
}
window.addEventListener('pageshow', load);
