import { badge } from '../ui.js';
import { requireRole, authenticatedFetch, logout } from '../auth.js';
const content = document.querySelector('#content');
const status = document.querySelector('#page-status');
document.querySelector('#logout').addEventListener('click', logout);
function text(tag, value) { const node = document.createElement(tag); node.textContent = value; return node; }
async function load() {
  content.hidden = true;
  status.textContent = 'Loading reviews…';
  try {
    if (!await requireRole('worker')) return;
    const result = await authenticatedFetch('/reviews/worker');
    const rows = result.reviews;
    const list = document.querySelector('#reviews-list');
    list.replaceChildren();
    for (const review of rows) {
      const item = document.createElement('li');
      item.dataset.reviewId = review.id;
      item.append(text('h2', `Review #${review.id} — booking #${review.booking_id}`),
        text('p', `Customer: ${review.reviewer_name || 'Not available'}`),
        badge(text('p', `Rating: ${review.rating}/5`), 'rating'), text('p', review.comment || 'No comment.'));
      if (review.service_title) item.append(text('p', `Service: ${review.service_title}`));
      if (review.worker_name) item.append(text('p', `Worker: ${review.worker_name}`));
      list.append(item);
    }
    document.querySelector('#reviews-status').textContent = rows.length ? `${rows.length} reviews shown.` : 'No reviews yet.';
    content.hidden = false;
    status.textContent = '';
  } catch (error) {
    if (error.status === 401) return logout();
    status.textContent = `Could not load reviews. ${error.message} Reload to retry.`;
  }
}
window.addEventListener('pageshow', load);
