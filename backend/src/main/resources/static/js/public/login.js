import { apiFetch } from '../api.js';
import { clearToken, completeAuthentication } from '../auth.js';

const form = document.querySelector('#login-form');
const message = document.querySelector('#form-message');
const button = form.querySelector('button[type="submit"]');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!form.reportValidity()) return;
  button.disabled = true;
  message.textContent = 'Signing in…';
  clearToken();
  try {
    const data = await apiFetch('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: form.elements.email.value.trim(),
        password: form.elements.password.value,
      }),
    });
    await completeAuthentication(data.token);
  } catch (error) {
    message.textContent = error.message;
  } finally {
    form.elements.password.value = '';
    button.disabled = false;
  }
});
