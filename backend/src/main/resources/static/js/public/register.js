import { apiFetch } from '../api.js';
import { clearToken, completeAuthentication } from '../auth.js';

const form = document.querySelector('#register-form');
const message = document.querySelector('#form-message');
const button = form.querySelector('button[type="submit"]');
const name = form.elements.full_name;
name.addEventListener('input', () => name.setCustomValidity(''));

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  name.setCustomValidity(name.value.trim() ? '' : 'Enter your full name.');
  if (!form.reportValidity()) return;
  button.disabled = true;
  message.textContent = 'Creating your account…';
  clearToken();
  let created = false;
  try {
    const data = await apiFetch('/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        full_name: name.value.trim(),
        email: form.elements.email.value.trim(),
        password: form.elements.password.value,
        phone: form.elements.phone.value.trim(),
        role: form.elements.role.value,
      }),
    });
    created = true;
    await completeAuthentication(data.token);
  } catch (error) {
    message.textContent = created
      ? `Account created, but sign-in could not finish. Please use Login. ${error.message}`
      : error.message;
  } finally {
    form.elements.password.value = '';
    button.disabled = false;
  }
});
