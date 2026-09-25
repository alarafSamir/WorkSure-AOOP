import { badge } from '../ui.js';
import { apiFetch } from '../api.js';
import { loadCurrentUser, hasRole, authenticatedFetch, clearToken } from '../auth.js';

const id = new URLSearchParams(location.search).get('id');
const status = document.querySelector('#service-status');
const message = document.querySelector('#booking-message');
const form = document.querySelector('#booking-form');
const fields = document.querySelector('#booking-fields');
let service = null;
let submitting = false;
let booked = false;

async function checkCustomer() {
  const user = await loadCurrentUser();
  if (!user) {
    message.textContent = 'Log in as a customer to book this service.';
    document.querySelector('#login-link').hidden = false;
    fields.disabled = true;
    return null;
  }
  document.querySelector('#login-link').hidden = true;
  if (!hasRole(user, 'customer')) {
    fields.disabled = true;
    message.textContent = 'Only customer accounts can book services.';
    return null;
  }
  fields.disabled = booked;
  message.textContent = '';
  return user;
}

async function loadService() {
  if (!id || !/^[1-9]\d*$/.test(id) || !Number.isSafeInteger(Number(id))) {
    status.textContent = 'Choose a valid service from the Services page.';
    status.classList.add('error');
    return;
  }
  try {
    const data = await apiFetch(`/services/${id}`);
    service = data.service;
    document.querySelector('#service-title').textContent = service.title;
    document.querySelector('#description').textContent = service.description;
    document.querySelector('#category-name').textContent = service.category_name;
    document.querySelector('#price').textContent = `৳${Number(service.base_price).toFixed(2)}`;
    document.querySelector('#provider').textContent = service.worker_name;
    document.querySelector('#provider-info').textContent = [service.worker_headline, service.worker_city].filter(Boolean).join(' · ');
    document.querySelector('#verification').textContent = Number(service.is_verified) === 1 ? 'Verified worker' : 'Worker not verified';
    badge(document.querySelector('#verification'), Number(service.is_verified) ? 'verified' : 'unverified');
    document.querySelector('#worker-link').href = `/worker.html?id=${encodeURIComponent(service.worker_id)}`;
    document.querySelector('#service-details').hidden = false;
    document.querySelector('#booking-section').hidden = false;
    status.textContent = '';
  } catch (error) {
    status.textContent = error.status === 404 ? 'Service not found or no longer available.' : `Could not load service. ${error.message}`;
    status.classList.add('error');
    return;
  }
  try {
    await checkCustomer();
  } catch (error) {
    message.textContent = `Could not check your session. ${error.message} Reload to try again.`;
  }
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (submitting || booked || !service) return;
  if (!form.reportValidity()) return;
  const address = form.elements.address.value.trim();
  const scheduled = new Date(form.elements.scheduled_at.value);
  if (!address || Number.isNaN(scheduled.getTime()) || scheduled <= new Date()) {
    message.textContent = 'Enter an address and choose a valid future date and time.';
    return;
  }
  submitting = true;
  fields.disabled = true;
  try {
    const user = await checkCustomer();
    fields.disabled = true;
    if (!user) {
      if (!document.querySelector('#login-link').hidden) location.assign('/login.html');
      return;
    }
    message.textContent = 'Submitting booking…';
    const data = await authenticatedFetch('/bookings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        service_id: Number(id),
        scheduled_at: scheduled.toISOString(),
        address,
        notes: form.elements.notes.value.trim(),
      }),
    });
    booked = true;
    message.textContent = `Booking #${data.booking.id} created successfully. Status: ${data.booking.status}.`;
    message.classList.add('success');
    document.querySelector('#dashboard-link').hidden = false;
  } catch (error) {
    if (error.status === 401) {
      clearToken();
      location.assign('/login.html');
      return;
    }
    message.textContent = `Could not create booking. ${error.message}`;
    fields.disabled = error.status === 403;
  } finally {
    submitting = false;
    if (booked) fields.disabled = true;
  }
});

loadService();
