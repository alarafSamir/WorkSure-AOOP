import { badge } from '../ui.js';
import { requireRole, authenticatedFetch, logout } from '../auth.js';

const content = document.querySelector('#customer-content');
const status = document.querySelector('#page-status');
const list = document.querySelector('#bookings-list');
document.querySelector('#logout').addEventListener('click', logout);

function textElement(tag, value) {
  const element = document.createElement(tag);
  element.textContent = value;
  return element;
}

function localDate(value) {
  // The API may return a UTC SQL datetime without a timezone suffix.
  let timestamp = String(value || '').replace(' ', 'T');
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?$/.test(timestamp)) timestamp += 'Z';
  const date = new Date(timestamp);
  return Number.isNaN(date.getTime()) ? 'Date unavailable' : date.toLocaleString();
}

async function loadBookings() {
  content.hidden = true;
  list.replaceChildren();
  status.textContent = 'Checking your session…';
  try {
    const user = await requireRole('customer');
    if (!user) return;
    content.hidden = false;
    status.textContent = '';
    const bookingStatus = document.querySelector('#bookings-status');
    bookingStatus.textContent = 'Loading bookings…';
    try {
      // The server selects the current customer's records using the JWT.
      const { bookings } = await authenticatedFetch('/bookings');
      let reviews = null;
      let reviewError = '';
      try { reviews = (await authenticatedFetch('/reviews/given')).reviews; }
      catch (error) {
        if (error.status === 401) return logout();
        reviewError = error.message;
      }
      for (const booking of bookings) {
        const item = document.createElement('li');
        item.dataset.bookingId = booking.id;
        const state = badge(textElement('strong', `Status: ${String(booking.status).replaceAll('_', ' ')}`), booking.status);
        item.append(
          textElement('h2', `Booking #${booking.id}: ${booking.service_title}`),
          textElement('p', `Provider: ${booking.worker_name || 'Not available'}`),
          textElement('p', `Scheduled: ${localDate(booking.scheduled_at)}`),
          textElement('p', `Address: ${booking.address || 'Not available'}`),
          state,
        );
        if (booking.status === 'completed') {
          if (reviews === null) item.append(textElement('p', `Could not check reviews. ${reviewError} Reload to retry.`));
          else {
            const existing = reviews.find(review => Number(review.booking_id) === Number(booking.id));
            if (existing) showReview(item, existing);
            else item.append(reviewForm(booking.id));
          }
        }
        list.appendChild(item);
      }
      bookingStatus.textContent = bookings.length ? `${bookings.length} bookings found.` : 'No bookings yet. Browse services to book your first visit.';
    } catch (error) {
      if (error.status === 401) return logout();
      bookingStatus.textContent = `Could not load bookings. ${error.message}`;
    }
  } catch (error) {
    status.textContent = `Could not verify your session. ${error.message}`;
  }
}

window.addEventListener('pageshow', loadBookings);

function showReview(item, review) {
  const section = document.createElement('section');
  section.className = 'submitted-review';
  section.append(textElement('h3', 'Your review'), badge(textElement('p', `Rating: ${review.rating}/5`), 'rating'),
    textElement('p', review.comment || 'No comment.'));
  item.append(section);
}

function reviewForm(bookingId) {
  const form = document.createElement('form');
  form.className = 'review-form';
  const fields = document.createElement('fieldset');
  const heading = textElement('h3', 'Review this completed booking');
  const ratingLabel = textElement('label', 'Rating');
  const rating = document.createElement('select');
  rating.id = `rating-${bookingId}`;
  rating.name = 'rating';
  rating.required = true;
  ratingLabel.htmlFor = rating.id;
  const placeholder = textElement('option', 'Choose a rating');
  placeholder.value = '';
  rating.append(placeholder);
  for (let value = 1; value <= 5; value++) {
    const option = textElement('option', `${value} / 5`);
    option.value = value;
    rating.append(option);
  }
  const commentLabel = textElement('label', 'Comment (optional)');
  const comment = document.createElement('textarea');
  comment.id = `comment-${bookingId}`;
  comment.name = 'comment';
  comment.maxLength = 2000;
  commentLabel.htmlFor = comment.id;
  const button = textElement('button', 'Submit review');
  button.type = 'submit';
  const message = textElement('p', '');
  message.setAttribute('role', 'status');
  message.setAttribute('aria-live', 'polite');
  fields.append(heading, ratingLabel, rating, commentLabel, comment, button);
  form.append(fields, message);
  let busy = false;
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (busy || !form.reportValidity()) return;
    busy = true;
    fields.disabled = true;
    message.textContent = 'Submitting review…';
    try {
      const { review } = await authenticatedFetch('/reviews', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ booking_id: bookingId, rating: Number(rating.value), comment: comment.value.trim() }),
      });
      fields.remove();
      showReview(form, review);
      message.className = 'success';
      message.textContent = 'Review submitted successfully.';
    } catch (error) {
      if (error.status === 401) return logout();
      message.className = 'error';
      message.textContent = error.status === 409 ? 'This booking has already been reviewed. Reload to see the review.' : error.message;
      fields.disabled = error.status === 409;
    } finally { busy = false; }
  });
  return form;
}
