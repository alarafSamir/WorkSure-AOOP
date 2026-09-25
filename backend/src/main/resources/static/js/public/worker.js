import { badge } from '../ui.js';
import { apiFetch } from '../api.js';

const id = new URLSearchParams(location.search).get('id');
const status = document.querySelector('#worker-status');

function textElement(tag, text) {
  const element = document.createElement(tag);
  element.textContent = text;
  return element;
}

async function loadWorker() {
  if (!id || !/^[1-9]\d*$/.test(id) || !Number.isSafeInteger(Number(id))) {
    status.textContent = 'Choose a valid worker from a service page.';
    status.classList.add('error');
    return;
  }
  try {
    const data = await apiFetch(`/workers/public/${id}`);
    const worker = data.worker;
    document.querySelector('#worker-name').textContent = worker.full_name;
    document.querySelector('#headline').textContent = worker.headline || '';
    document.querySelector('#bio').textContent = worker.bio || 'No profile description yet.';
    document.querySelector('#worker-info').textContent = [worker.city, `${worker.years_experience || 0} years of experience`, `Hourly rate: ৳${Number(worker.hourly_rate).toFixed(2)}`].filter(Boolean).join(' · ');
    document.querySelector('#verification').textContent = Number(worker.is_verified) === 1 ? 'Verified worker' : 'Worker not verified';
    badge(document.querySelector('#verification'), Number(worker.is_verified) ? 'verified' : 'unverified');
    for (const service of data.services || []) {
      const item = document.createElement('li');
      const link = textElement('a', service.title);
      link.href = `/service.html?id=${encodeURIComponent(service.id)}`;
      item.append(link, textElement('p', `${service.category_name} · ৳${Number(service.base_price).toFixed(2)}`));
      document.querySelector('#worker-services').appendChild(item);
    }
    if (!data.services?.length) document.querySelector('#services-empty').hidden = false;
    for (const review of data.reviews || []) {
      const item = document.createElement('li');
      item.append(textElement('p', `${review.reviewer_name}: ${review.rating}/5`), textElement('p', review.comment || 'No comment.'));
      document.querySelector('#worker-reviews').appendChild(item);
    }
    if (!data.reviews?.length) document.querySelector('#reviews-empty').hidden = false;
    document.querySelector('#worker-details').hidden = false;
    status.textContent = '';
  } catch (error) {
    status.textContent = error.status === 404 ? 'Worker not found or no longer available.' : `Could not load worker. ${error.message}`;
    status.classList.add('error');
  }
}

loadWorker();
