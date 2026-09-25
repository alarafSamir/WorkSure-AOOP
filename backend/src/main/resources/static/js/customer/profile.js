import { requireRole, authenticatedFetch, logout } from '../auth.js';

const content = document.querySelector('#customer-content');
const status = document.querySelector('#page-status');
const form = document.querySelector('#profile-form');
const fields = document.querySelector('#profile-fields');
const message = document.querySelector('#profile-message');
const editable = ['full_name', 'phone', 'address', 'city', 'country'];
let saving = false;
document.querySelector('#logout').addEventListener('click', logout);
form.elements.full_name.addEventListener('input', () => form.elements.full_name.setCustomValidity(''));

function showProfile(user) {
  document.querySelector('#profile-email').textContent = user.email;
  document.querySelector('#profile-role').textContent = user.role;
  for (const name of editable) form.elements[name].value = user[name] || '';
}

async function loadProfile() {
  content.hidden = true;
  fields.disabled = true;
  status.textContent = 'Checking your session…';
  try {
    if (!await requireRole('customer')) return;
    const { user } = await authenticatedFetch('/users/profile');
    showProfile(user);
    content.hidden = false;
    fields.disabled = false;
    status.textContent = '';
  } catch (error) {
    if (error.status === 401) return logout();
    status.textContent = `Could not load profile. ${error.message}`;
  }
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (saving) return;
  const name = form.elements.full_name;
  name.setCustomValidity(name.value.trim() ? '' : 'Enter your full name.');
  if (!form.reportValidity()) return;
  const body = Object.fromEntries(editable.map(key => [key, form.elements[key].value.trim()]));
  saving = true;
  fields.disabled = true;
  message.classList.remove('success', 'error');
  message.textContent = 'Saving profile…';
  try {
    if (!await requireRole('customer')) return;
    const { user } = await authenticatedFetch('/users/profile', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    showProfile(user);
    message.textContent = 'Profile saved successfully.';
    message.classList.add('success');
  } catch (error) {
    if (error.status === 401) return logout();
    message.textContent = `Could not save profile. ${error.message}`;
    message.classList.add('error');
  } finally {
    saving = false;
    fields.disabled = false;
  }
});

window.addEventListener('pageshow', loadProfile);
