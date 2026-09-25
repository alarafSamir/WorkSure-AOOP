import { badge, serviceImage } from '../ui.js';
import { apiFetch } from '../api.js';

const params = new URLSearchParams(location.search);
const category = params.get('category') || '';
const page = Math.max(1, Number.parseInt(params.get('page'), 10) || 1);
const select = document.querySelector('#category');
const status = document.querySelector('#services-status');
const list = document.querySelector('#services-list');

function textElement(tag, text) {
  const element = document.createElement(tag);
  element.textContent = text;
  return element;
}

async function loadCategories() {
  try {
    const data = await apiFetch('/services/categories');
    for (const major of data.majors) {
      const option = textElement('option', major.name);
      option.value = major.slug;
      select.appendChild(option);
      for (const sub of major.subfeatures || []) {
        const child = textElement('option', `${major.name} — ${sub.name}`);
        child.value = sub.slug;
        select.appendChild(child);
      }
    }
    if (category && !Array.from(select.options).some(option => option.value === category)) {
      const unknown = textElement('option', 'Unknown category');
      unknown.value = category;
      select.appendChild(unknown);
    }
    select.value = category;
    document.querySelector('#categories-status').textContent = `${data.majors.length} main categories available.`;
  } catch (error) {
    document.querySelector('#categories-status').textContent = `Could not load categories. ${error.message}`;
  }
}

async function loadServices() {
  try {
    const query = new URLSearchParams({ page: String(page), limit: '12' });
    if (category) query.set('category', category);
    const data = await apiFetch(`/services?${query}`);
    for (const service of data.data) {
      const item = document.createElement('li');
      const title = textElement('h2', service.title);
      const category = textElement('p', service.category_name);
      category.className = 'service-category';
      const provider = textElement('p', service.worker_name || 'Provider');
      const price = textElement('p', `৳${Number(service.base_price).toFixed(2)}`);
      price.className = 'service-price';
      const verification = textElement('p', Number(service.is_verified) === 1 ? 'Verified worker' : 'Worker not verified');
      const link = textElement('a', 'View service and book');
      link.href = `/service.html?id=${encodeURIComponent(service.id)}`;
      const photo = serviceImage(service.category_slug);
      if (photo) { photo.className = 'service-photo'; item.append(photo); }
      badge(verification, Number(service.is_verified) ? 'verified' : 'unverified');
      item.append(category, title, provider, price, verification, link);
      list.appendChild(item);
    }
    status.textContent = data.data.length
      ? `${data.pagination.total} services found. Page ${data.pagination.page} of ${data.pagination.pages}.`
      : 'No services found for this selection.';
    const navigation = document.querySelector('#pagination');
    for (const [label, target] of [['Previous', page - 1], ['Next', page + 1]]) {
      if (target < 1 || target > data.pagination.pages) continue;
      const link = textElement('a', label);
      query.set('page', String(target));
      query.delete('limit');
      link.href = `/services.html?${query}`;
      navigation.appendChild(link);
    }
  } catch (error) {
    status.textContent = `Could not load services. ${error.message}`;
    status.classList.add('error');
  }
}

loadCategories();
loadServices();
