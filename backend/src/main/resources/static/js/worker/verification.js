import { badge } from '../ui.js';
import { requireRole, authenticatedFetch, downloadVerificationDocument, logout } from '../auth.js';
const content = document.querySelector('#content');
const status = document.querySelector('#page-status');
const form = document.querySelector('#upload-form');
const fields = document.querySelector('#upload-fields');
const message = document.querySelector('#upload-message');
const refresh = document.querySelector('#refresh');
let uploading = false;
document.querySelector('#logout').addEventListener('click', logout);
function text(tag, value) {
  const element = document.createElement(tag);
  element.textContent = value;
  return element;
}
async function loadDocuments() {
  const [account, result] = await Promise.all([
    authenticatedFetch('/workers/me'), authenticatedFetch('/workers/me/documents'),
  ]);
  document.querySelector('#verification-status').textContent = Number(account.worker.is_verified) ? 'Verified worker' : 'Not verified';
  badge(document.querySelector('#verification-status'), Number(account.worker.is_verified) ? 'verified' : 'unverified');
  const list = document.querySelector('#documents-list');
  list.replaceChildren();
  for (const doc of result.documents) {
    const item = document.createElement('li');
    item.dataset.documentId = doc.id;
    item.append(text('h3', `Document #${doc.id}: ${doc.doc_type}`), badge(text('strong', `Status: ${doc.status}`), doc.status),
      text('p', `Submitted (UTC): ${doc.created_at}`));
    if (doc.admin_note) item.append(text('p', `Admin note: ${doc.admin_note}`));
    const button = text('button', 'Download my document');
    button.type = 'button';
    button.addEventListener('click', async () => {
      button.disabled = true;
      try { await downloadVerificationDocument(doc.id); }
      catch (error) {
        if (error.status === 401) return logout();
        document.querySelector('#documents-status').textContent = error.message;
      } finally { button.disabled = false; }
    });
    item.append(button);
    list.append(item);
  }
  document.querySelector('#documents-status').textContent = result.documents.length ? `${result.documents.length} submitted documents.` : 'No documents submitted yet.';
}
async function load() {
  if (uploading) return;
  content.hidden = true;
  status.textContent = 'Loading verification…';
  try {
    if (!await requireRole('worker')) return;
    await loadDocuments();
    content.hidden = false;
    status.textContent = '';
  } catch (error) {
    if (error.status === 401) return logout();
    status.textContent = `Could not load verification. ${error.message} Reload to retry.`;
  }
}
form.addEventListener('submit', async event => {
  event.preventDefault();
  if (uploading || !form.reportValidity()) return;
  const file = form.elements.file.files[0];
  message.className = 'error';
  if (!file || !/\.(jpg|jpeg|png|webp|pdf)$/i.test(file.name)) {
    message.textContent = 'Choose a JPG, JPEG, PNG, WEBP or PDF file.';
    return;
  }
  if (file.size === 0 || file.size > 8 * 1024 * 1024) {
    message.textContent = 'Choose a non-empty file no larger than 8 MB.';
    return;
  }
  // Capture before disabling the fields; the browser supplies the multipart boundary.
  const data = new FormData(form);
  uploading = true;
  fields.disabled = refresh.disabled = true;
  message.className = '';
  message.textContent = 'Uploading…';
  try {
    const result = await authenticatedFetch('/workers/me/documents', { method: 'POST', body: data });
    form.reset();
    message.className = 'success';
    message.textContent = result.message;
    try { await loadDocuments(); }
    catch (error) {
      if (error.status === 401) return logout();
      message.textContent += ` Upload succeeded, but status could not refresh. ${error.message}`;
    }
  } catch (error) {
    if (error.status === 401) return logout();
    message.className = 'error';
    message.textContent = error.message;
  } finally {
    uploading = false;
    fields.disabled = refresh.disabled = false;
  }
});
refresh.addEventListener('click', load);
window.addEventListener('pageshow', load);
